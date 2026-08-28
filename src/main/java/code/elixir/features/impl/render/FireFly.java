package code.elixir.features.impl.render;

import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.math.time.StopWatch;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class FireFly extends Module {
   private static final int MAX_SPARKLES_PER_ENTITY = 6;
   private static final int MAX_TRAIL_RENDER_POINTS = 4;
   private static final float MAX_RENDER_DISTANCE_SQ = 5184.0F;
   private static final float TRAIL_RENDER_DISTANCE_SQ = 1600.0F;
   private final List<FireFly.FireFlyEntity> particles = new ArrayList<>();
   private final Random random = new Random();
   private final Vector3d renderScratch = new Vector3d();
   private SliderSettings count = new SliderSettings("Количество", "Количество светлячков").setValue(35.0F).range(10, 100);
   private SliderSettings speed = new SliderSettings("Скорость", "Скорость движения").setValue(0.35F).range(0.01F, 1.0F);
   private SliderSettings radius = new SliderSettings("Радиус спавна", "Радиус появления светлячков").setValue(40.0F).range(10.0F, 100.0F);
   private SliderSettings floorHeight = new SliderSettings("Высота от пола", "Максимальная высота над поверхностью земли").setValue(12.0F).range(1.0F, 40.0F);
   private SliderSettings trailLength = new SliderSettings("Длина шлейфа", "Длина хвоста светлячка").setValue(6.0F).range(2.0F, 15.0F);
   private SliderSettings trailWidth = new SliderSettings("Толщина шлейфа", "Толщина хвоста светлячка").setValue(1.5F).range(0.5F, 4.0F).step(0.5F);
   private SliderSettings fireflySize = new SliderSettings("Размер", "Размер светлячков").setValue(2.0F).range(0.5F, 5.0F).step(0.1F);
   private SliderSettings gravity = new SliderSettings("Гравитация", "Сила притяжения вниз").setValue(0.001F).range(0.0F, 0.01F);
   private SliderSettings depth3D = new SliderSettings("3D глубина", "Сила движения к камере").setValue(0.0F).range(0.0F, 2.5F);
   private BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", "Случайные цвета светлячков");
   private BooleanSetting collision = new BooleanSetting("Коллизия", "Столкновение с блоками");
   private BooleanSetting trail = new BooleanSetting("Шлейф", "Рисовать хвост светлячка");
   private BooleanSetting smoothTrail = new BooleanSetting("Плавный шлейф", "Более плавный и незаметный хвост").visible(() -> this.trail.isValue());
   private BooleanSetting sparkles = new BooleanSetting("Искры", "Маленькие частицы от светлячков");
   private SliderSettings sparkleAmount = new SliderSettings("Количество искр", "Количество искр от каждого светлячка")
      .setValue(2.0F)
      .range(1.0F, 6.0F)
      .visible(() -> this.sparkles.isValue());
   private Identifier texture = Identifier.of("textures/glow.png");

   public FireFly() {
      super("FireFly", "FireFly", ModuleCategory.RENDER);
      this.randomColor.setValue(true);
      this.collision.setValue(false);
      this.trail.setValue(true);
      this.smoothTrail.setValue(true);
      this.sparkles.setValue(true);
      this.setup(
         new Setting[]{
            this.count,
            this.speed,
            this.radius,
            this.floorHeight,
            this.trailLength,
            this.trailWidth,
            this.fireflySize,
            this.gravity,
            this.depth3D,
            this.randomColor,
            this.collision,
            this.trail,
            this.smoothTrail,
            this.sparkles,
            this.sparkleAmount
         }
      );
   }

   public static FireFly getInstance() {
      return Instance.get(FireFly.class);
   }

   @Override
   public void activate() {
      super.activate();
      this.particles.clear();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.particles.clear();
   }

   private double resolveSurfaceY(double x, double z) {
      BlockPos surface = mc.world.getTopPosition(Type.MOTION_BLOCKING, BlockPos.ofFloored(x, 0.0, z));
      return surface.getY();
   }

   private double randomHeightAboveSurface(double surfaceY) {
      return surfaceY + 0.2 + this.random.nextDouble() * this.floorHeight.getValue();
   }

   private double getMinPlayerDistance() {
      float spawnRadius = this.radius.getValue();
      return MathHelper.clamp(spawnRadius * 0.3F, 4.0F, spawnRadius * 0.8F);
   }

   private void spawnParticle() {
      if (mc.player != null && mc.world != null) {
         int spawnRadius = (int)this.radius.getValue();
         double minPlayerDist = this.getMinPlayerDistance();
         double minPlayerDistSq = minPlayerDist * minPlayerDist;

         for (int attempt = 0; attempt < 20; attempt++) {
            double spawnX = mc.player.getX() + (this.random.nextDouble() * spawnRadius * 2.0 - spawnRadius);
            double spawnZ = mc.player.getZ() + (this.random.nextDouble() * spawnRadius * 2.0 - spawnRadius);
            double dx = spawnX - mc.player.getX();
            double surfaceY;
            double spawnY;
            double dz;
            if (!(dx * dx + (dz = spawnZ - mc.player.getZ()) * dz < minPlayerDistSq)
               && mc.world
                  .getBlockState(BlockPos.ofFloored(spawnX, spawnY = this.randomHeightAboveSurface(surfaceY = this.resolveSurfaceY(spawnX, spawnZ)), spawnZ))
                  .isAir()) {
               double velocitySpeed = this.speed.getValue() * (0.85 + this.random.nextDouble() * 0.3);
               double velocityYaw = Math.toRadians(this.random.nextDouble() * 360.0);
               Vector3d initialVelocity = new Vector3d(
                  -Math.sin(velocityYaw) * velocitySpeed, (this.random.nextDouble() - 0.5) * velocitySpeed * 0.15, Math.cos(velocityYaw) * velocitySpeed
               );
               this.steerAwayFromPlayer(initialVelocity, spawnX, spawnY, spawnZ, minPlayerDist);
               int color = this.randomColor.isValue() ? this.randomColor() : ColorAssist.getClientColor();
               this.particles.add(new FireFly.FireFlyEntity(spawnX, spawnY, spawnZ, initialVelocity, color, surfaceY));
               return;
            }
         }
      }
   }

   private void steerAwayFromPlayer(Vector3d velocity, double x, double y, double z, double minDist) {
      double dx = x - mc.player.getX();
      double dz;
      double dy;
      double distSq = dx * dx + (dy = y - mc.player.getEyeY()) * dy + (dz = z - mc.player.getZ()) * dz;
      if (!(distSq < 1.0E-8)) {
         double dist = Math.sqrt(distSq);
         double awayX = dx / dist;
         double awayY = dy / dist;
         double awayZ = dz / dist;
         double dot = velocity.x * awayX + velocity.y * awayY + velocity.z * awayZ;
         if (dist < minDist && dot < 0.0) {
            velocity.x -= awayX * dot;
            velocity.y -= awayY * dot;
            velocity.z -= awayZ * dot;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.isState() && mc.player != null && mc.world != null) {
         double maxDistSq = this.radius.getValue() * this.radius.getValue();
         this.particles
            .removeIf(particle -> particle.isDead() || particle.position.distanceSquared(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > maxDistSq);
         int targetCount = (int)this.count.getValue();
         int deficit = Math.min(targetCount - this.particles.size(), 5);

         for (int i = 0; i < deficit; i++) {
            this.spawnParticle();
         }

         for (FireFly.FireFlyEntity particle2 : this.particles) {
            particle2.update();
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (this.isState() && mc.player != null && mc.world != null && !this.particles.isEmpty()) {
         float partialTicks = event.getPartialTicks();
         Camera camera = mc.getEntityRenderDispatcher().camera;
         Vec3d cameraPos = camera.getPos();
         float sizeMul = this.fireflySize.getValue();
         MatrixStack matrix = event.getStack();
         if (this.trail.isValue()) {
            this.renderBatchedTrails(matrix, cameraPos, partialTicks);
         }

         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, this.texture);
         MatrixStack billboardStack = new MatrixStack();
         billboardStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         billboardStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         boolean hasGeometry = false;

         for (FireFly.FireFlyEntity particle : this.particles) {
            particle.lerpRenderPosition(this.renderScratch, partialTicks);
            if (!(distanceSquared(cameraPos, this.renderScratch) > 5184.0)) {
               hasGeometry = true;
               int baseAlpha = particle.getAlpha();
               int pulseAlpha = particle.getPulseAlpha(this.renderScratch);
               int finalAlpha = Math.min(baseAlpha, pulseAlpha);
               int color = particle.getColor();
               float glowRadius = 0.34F * sizeMul;
               float coreRadius = 0.18F * sizeMul;
               float brightRadius = 0.08F * sizeMul;
               int glowColor = ColorAssist.multAlpha(color, finalAlpha * 0.45F / 255.0F);
               int mainColor = ColorAssist.multAlpha(color, finalAlpha * 0.9F / 255.0F);
               int brightColor = ColorAssist.getColor(255, 255, 255, finalAlpha);
               this.drawBillboard(buffer, this.renderScratch, glowRadius, glowColor, camera, billboardStack);
               this.drawBillboard(buffer, this.renderScratch, coreRadius, mainColor, camera, billboardStack);
               this.drawBillboard(buffer, this.renderScratch, brightRadius, brightColor, camera, billboardStack);
               if (this.sparkles.isValue()) {
                  this.renderSparkles(buffer, particle, camera, billboardStack, sizeMul, partialTicks);
               }
            }
         }

         if (hasGeometry) {
            BufferDraw.draw(buffer);
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableBlend();
      }
   }

   private void renderBatchedTrails(MatrixStack matrix, Vec3d cameraPos, float partialTicks) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      boolean hasLines = false;
      boolean smooth = this.smoothTrail.isValue();

      for (FireFly.FireFlyEntity particle : this.particles) {
         particle.lerpRenderPosition(this.renderScratch, partialTicks);
         List<Vector3d> trailPoints;
         int trailCount;
         if (!(distanceSquared(cameraPos, this.renderScratch) > 1600.0) && (trailCount = (trailPoints = particle.getTrail()).size()) != 0) {
            int baseAlpha = particle.getAlpha();
            int color = particle.getColor();

            int startIndex;
            for (int i = startIndex = Math.max(0, trailCount - 4); i < trailCount - 1; i++) {
               Vector3d start = trailPoints.get(i);
               Vector3d end = trailPoints.get(i + 1);
               float fade = (float)(i + 1 - startIndex) / Math.max(1, trailCount - 1 - startIndex);
               float smoothFade = smooth ? fade * fade : fade;
               int trailAlpha = (int)(baseAlpha * smoothFade * 0.8F);
               if (trailAlpha > 8) {
                  int lineColor = ColorAssist.multAlpha(color, trailAlpha / 255.0F);
                  buffer.vertex(matrix4f, (float)start.x, (float)start.y, (float)start.z).color(lineColor);
                  buffer.vertex(matrix4f, (float)end.x, (float)end.y, (float)end.z).color(lineColor);
                  hasLines = true;
               }
            }

            Vector3d tail = trailCount > 1 ? trailPoints.get(trailCount - 1) : trailPoints.get(0);
            int headAlpha = (int)(baseAlpha * 0.75F);
            if (headAlpha > 8) {
               int headColor = ColorAssist.multAlpha(color, headAlpha / 255.0F);
               buffer.vertex(matrix4f, (float)tail.x, (float)tail.y, (float)tail.z).color(headColor);
               buffer.vertex(matrix4f, (float)this.renderScratch.x, (float)this.renderScratch.y, (float)this.renderScratch.z).color(headColor);
               hasLines = true;
            }
         }
      }

      if (hasLines) {
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(Math.max(0.5F, this.trailWidth.getValue()));
         BufferDraw.draw(buffer);
         RenderSystem.lineWidth(1.0F);
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   private void renderSparkles(BufferBuilder buffer, FireFly.FireFlyEntity particle, Camera camera, MatrixStack baseStack, float sizeMul, float partialTicks) {
      int baseAlpha = particle.getAlpha();

      for (FireFly.Sparkle sparkle : particle.getSparkles()) {
         sparkle.lerpRenderPosition(this.renderScratch, partialTicks);
         float lifeFade = 1.0F - sparkle.getRenderAge(partialTicks) / sparkle.maxAge;
         int sparkleAlpha;
         if (!(lifeFade <= 0.01F) && (sparkleAlpha = (int)(baseAlpha * lifeFade * 0.75F)) > 0) {
            int sparkleColor = ColorAssist.multAlpha(particle.getColor(), sparkleAlpha / 255.0F);
            float sparkleSize = 0.05F * lifeFade * sizeMul;
            this.drawBillboard(buffer, this.renderScratch, sparkleSize, sparkleColor, camera, baseStack);
         }
      }
   }

   private static double distanceSquared(Vec3d cameraPos, Vector3d particlePos) {
      double dx = particlePos.x - cameraPos.x;
      double dy = particlePos.y - cameraPos.y;
      double dz = particlePos.z - cameraPos.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private void drawBillboard(BufferBuilder buffer, Vector3d pos, float size, int color, Camera camera, MatrixStack baseStack) {
      Vec3d camPos = camera.getPos();
      baseStack.push();
      baseStack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
      baseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      baseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      Matrix4f mat = baseStack.peek().getPositionMatrix();
      buffer.vertex(mat, -size, -size, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(mat, -size, size, 0.0F).texture(0.0F, 1.0F).color(color);
      buffer.vertex(mat, size, size, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(mat, size, -size, 0.0F).texture(1.0F, 0.0F).color(color);
      baseStack.pop();
   }

   private int randomColor() {
      float hue = this.random.nextFloat() * 0.2F;
      float saturation = 0.7F + this.random.nextFloat() * 0.3F;
      float brightness = 0.9F + this.random.nextFloat() * 0.1F;
      int rgb = Color.HSBtoRGB(hue, saturation, brightness);
      return 0xFF000000 | rgb & 16777215;
   }

   private class FireFlyEntity {
      private final StopWatch timer = new StopWatch();
      private final int color;
      private final Vector3d position;
      private final Vector3d prevPosition;
      private final Vector3d velocity;
      private final List<Vector3d> trail = new ArrayList<>();
      private final List<FireFly.Sparkle> sparkles = new ArrayList<>();
      private Box box;
      private double surfaceY;
      private int surfaceColumnX = Integer.MIN_VALUE;
      private int surfaceColumnZ = Integer.MIN_VALUE;
      private float alpha;
      private boolean dead;
      private double targetY;
      private long directionChangeTime;
      private int sparkleSpawnCounter;
      private int trailCooldown;

      FireFlyEntity(double x, double y, double z, Vector3d velocity, int color, double surfaceY) {
         this.position = new Vector3d(x, y, z);
         this.prevPosition = new Vector3d(x, y, z);
         this.velocity = new Vector3d(velocity);
         this.color = color;
         this.surfaceY = surfaceY;
         this.surfaceColumnX = MathHelper.floor(x);
         this.surfaceColumnZ = MathHelper.floor(z);
         this.targetY = this.randomHeightInBand(surfaceY);
         this.trail.add(new Vector3d(x, y, z));
         this.box = new Box(x, y, z, x, y, z).expand(0.35);
      }

      private double randomHeightInBand(double surface) {
         return surface + 0.2 + FireFly.this.random.nextDouble() * FireFly.this.floorHeight.getValue();
      }

      private void refreshSurfaceIfNeeded() {
         int colX = MathHelper.floor(this.position.x);
         int colZ = MathHelper.floor(this.position.z);
         if (colX != this.surfaceColumnX || colZ != this.surfaceColumnZ) {
            this.surfaceColumnX = colX;
            this.surfaceColumnZ = colZ;
            this.surfaceY = FireFly.this.resolveSurfaceY(this.position.x, this.position.z);
         }
      }

      void lerpRenderPosition(Vector3d out, float partialTicks) {
         out.x = MathHelper.lerp(partialTicks, this.prevPosition.x, this.position.x);
         out.y = MathHelper.lerp(partialTicks, this.prevPosition.y, this.position.y);
         out.z = MathHelper.lerp(partialTicks, this.prevPosition.z, this.position.z);
      }

      int getPulseAlpha(Vector3d renderPos) {
         double pulse = (Math.sin(this.timer.elapsedTime() / 400.0 + renderPos.x) + 1.0) / 2.0;
         return (int)((pulse * 0.5 + 0.5) * 255.0);
      }

      void update() {
         if (QuickImports.mc.world != null && QuickImports.mc.player != null) {
            this.prevPosition.set(this.position);
            this.refreshSurfaceIfNeeded();
            double minY = this.surfaceY + 0.15;
            double maxY = this.surfaceY + FireFly.this.floorHeight.getValue();
            if (System.currentTimeMillis() - this.directionChangeTime > 1200 + FireFly.this.random.nextInt(1800)) {
               this.directionChangeTime = System.currentTimeMillis();
               double newYaw = Math.toRadians(FireFly.this.random.nextDouble() * 360.0);
               double newSpeed = FireFly.this.speed.getValue() * (0.75 + FireFly.this.random.nextDouble() * 0.5);
               if (FireFly.this.depth3D.getValue() > 0.0F) {
                  Vec3d eyePos = QuickImports.mc.player.getEyePos();
                  double toCameraX = eyePos.x - this.position.x;
                  double toCameraZ = eyePos.z - this.position.z;
                  double toCameraDist = Math.sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ);
                  if (toCameraDist > 0.1) {
                     double pull = FireFly.this.depth3D.getValue() * 0.25;
                     this.velocity.x = -Math.sin(newYaw) * newSpeed + toCameraX / toCameraDist * pull;
                     this.velocity.z = Math.cos(newYaw) * newSpeed + toCameraZ / toCameraDist * pull;
                  } else {
                     this.velocity.x = -Math.sin(newYaw) * newSpeed;
                     this.velocity.z = Math.cos(newYaw) * newSpeed;
                  }
               } else {
                  this.velocity.x = -Math.sin(newYaw) * newSpeed;
                  this.velocity.z = Math.cos(newYaw) * newSpeed;
               }

               this.targetY = this.randomHeightInBand(this.surfaceY);
            }

            this.velocity.y = this.velocity.y + (this.targetY - this.position.y) * 0.018;
            this.velocity.y = this.velocity.y - FireFly.this.gravity.getValue();
            this.velocity.x = this.velocity.x + (FireFly.this.random.nextDouble() - 0.5) * 0.008;
            this.velocity.y = this.velocity.y + (FireFly.this.random.nextDouble() - 0.5) * 0.005;
            this.velocity.z = this.velocity.z + (FireFly.this.random.nextDouble() - 0.5) * 0.008;
            FireFly.this.steerAwayFromPlayer(this.velocity, this.position.x, this.position.y, this.position.z, FireFly.this.getMinPlayerDistance());
            double maxSpeed = FireFly.this.speed.getValue() * 1.15;
            this.velocity.x = MathHelper.clamp(this.velocity.x, -maxSpeed, maxSpeed);
            this.velocity.y = MathHelper.clamp(this.velocity.y, -maxSpeed * 0.45, maxSpeed * 0.45);
            this.velocity.z = MathHelper.clamp(this.velocity.z, -maxSpeed, maxSpeed);
            this.velocity.x *= 0.988;
            this.velocity.y *= 0.982;
            this.velocity.z *= 0.988;
            if (FireFly.this.collision.isValue()) {
               this.moveWithCollision();
            } else {
               this.position.x = this.position.x + this.velocity.x;
               this.position.y = this.position.y + this.velocity.y;
               this.position.z = this.position.z + this.velocity.z;
            }

            if (this.position.y < minY) {
               this.position.y = minY;
               this.velocity.y = Math.abs(this.velocity.y) * 0.35;
            } else if (this.position.y > maxY) {
               this.position.y = maxY;
               this.velocity.y = -Math.abs(this.velocity.y) * 0.35;
            }

            this.targetY = MathHelper.clamp(this.targetY, minY, maxY);
            int maxTrailLen = (int)FireFly.this.trailLength.getValue();
            if (++this.trailCooldown >= 2) {
               this.trailCooldown = 0;
               if (this.trail.size() >= maxTrailLen) {
                  Vector3d reused = this.trail.remove(0);
                  reused.set(this.position);
                  this.trail.add(reused);
               } else {
                  this.trail.add(new Vector3d(this.position));
               }
            }

            if (FireFly.this.sparkles.isValue()) {
               this.sparkles.removeIf(sparkle -> {
                  sparkle.update();
                  return sparkle.age >= sparkle.maxAge;
               });
               this.sparkleSpawnCounter++;
               if (this.sparkleSpawnCounter >= 4 && this.sparkles.size() < 6) {
                  this.sparkleSpawnCounter = 0;
                  int sparkleCount = Math.min((int)FireFly.this.sparkleAmount.getValue(), 6 - this.sparkles.size());

                  for (int i = 0; i < sparkleCount; i++) {
                     this.spawnSparkle();
                  }
               }
            }

            this.alpha = this.timer.elapsedTime() < 800L ? Math.min(255.0F, this.alpha + 8.0F) : Math.min(255.0F, this.alpha + 2.0F);
            this.box = new Box(this.position.x, this.position.y, this.position.z, this.position.x, this.position.y, this.position.z).expand(0.35);
         }
      }

      private void spawnSparkle() {
         Vector3d sparklePos = new Vector3d(
            this.position.x + (FireFly.this.random.nextDouble() - 0.5) * 0.12,
            this.position.y + (FireFly.this.random.nextDouble() - 0.5) * 0.12,
            this.position.z + (FireFly.this.random.nextDouble() - 0.5) * 0.12
         );
         Vector3d sparkleVel = new Vector3d(
            (FireFly.this.random.nextDouble() - 0.5) * 0.018,
            (FireFly.this.random.nextDouble() - 0.5) * 0.018,
            (FireFly.this.random.nextDouble() - 0.5) * 0.018
         );
         this.sparkles.add(FireFly.this.new Sparkle(FireFly.this, sparklePos, sparkleVel));
      }

      private void moveWithCollision() {
         double nextX = this.position.x + this.velocity.x;
         double nextY = this.position.y + this.velocity.y;
         double nextZ = this.position.z + this.velocity.z;
         BlockPos nextPos = BlockPos.ofFloored(nextX, nextY, nextZ);
         if (QuickImports.mc.world.getBlockState(nextPos).isAir()) {
            this.position.x = nextX;
            this.position.y = nextY;
            this.position.z = nextZ;
         } else {
            BlockPos xPos = BlockPos.ofFloored(nextX, this.position.y, this.position.z);
            BlockPos yPos = BlockPos.ofFloored(this.position.x, nextY, this.position.z);
            BlockPos zPos = BlockPos.ofFloored(this.position.x, this.position.y, nextZ);
            if (QuickImports.mc.world.getBlockState(xPos).isAir()) {
               this.position.x = nextX;
            } else {
               this.velocity.x *= -0.55;
            }

            if (QuickImports.mc.world.getBlockState(yPos).isAir()) {
               this.position.y = nextY;
            } else {
               this.velocity.y *= -0.4;
            }

            if (QuickImports.mc.world.getBlockState(zPos).isAir()) {
               this.position.z = nextZ;
            } else {
               this.velocity.z *= -0.55;
            }
         }
      }

      int getAlpha() {
         return (int)this.alpha;
      }

      public StopWatch getTimer() {
         return this.timer;
      }

      public int getColor() {
         return this.color;
      }

      public Vector3d getPosition() {
         return this.position;
      }

      public Vector3d getPrevPosition() {
         return this.prevPosition;
      }

      public Vector3d getVelocity() {
         return this.velocity;
      }

      public List<Vector3d> getTrail() {
         return this.trail;
      }

      public List<FireFly.Sparkle> getSparkles() {
         return this.sparkles;
      }

      public Box getBox() {
         return this.box;
      }

      public double getSurfaceY() {
         return this.surfaceY;
      }

      public int getSurfaceColumnX() {
         return this.surfaceColumnX;
      }

      public int getSurfaceColumnZ() {
         return this.surfaceColumnZ;
      }

      public boolean isDead() {
         return this.dead;
      }

      public double getTargetY() {
         return this.targetY;
      }

      public long getDirectionChangeTime() {
         return this.directionChangeTime;
      }

      public int getSparkleSpawnCounter() {
         return this.sparkleSpawnCounter;
      }

      public int getTrailCooldown() {
         return this.trailCooldown;
      }
   }

   private class Sparkle {
      final Vector3d position;
      final Vector3d prevPosition;
      final Vector3d velocity;
      final float maxAge;
      float age;

      Sparkle(FireFly fireFly, Vector3d position, Vector3d velocity) {
         this.position = new Vector3d(position);
         this.prevPosition = new Vector3d(position);
         this.velocity = velocity;
         this.maxAge = 16.0F + fireFly.random.nextFloat() * 12.0F;
      }

      void update() {
         this.prevPosition.set(this.position);
         this.position.x = this.position.x + this.velocity.x;
         this.position.y = this.position.y + this.velocity.y;
         this.position.z = this.position.z + this.velocity.z;
         this.velocity.x *= 0.95;
         this.velocity.y *= 0.95;
         this.velocity.z *= 0.95;
         this.velocity.y -= 0.001;
         this.age++;
      }

      void lerpRenderPosition(Vector3d out, float partialTicks) {
         out.x = MathHelper.lerp(partialTicks, this.prevPosition.x, this.position.x);
         out.y = MathHelper.lerp(partialTicks, this.prevPosition.y, this.position.y);
         out.z = MathHelper.lerp(partialTicks, this.prevPosition.z, this.position.z);
      }

      float getRenderAge(float partialTicks) {
         return this.age + partialTicks;
      }
   }
}
