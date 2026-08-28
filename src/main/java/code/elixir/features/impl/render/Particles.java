package code.elixir.features.impl.render;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.animation.util.Animation;
import code.elixir.utils.animation.util.Easings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.projection.Projection;
import code.elixir.utils.math.time.TimerUtil;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import org.joml.Vector4i;

public class Particles extends Module implements QuickImports {
   private static final int TOTEM_COLOR_GREEN = 65280;
   private static final int TOTEM_COLOR_YELLOW = 16776960;
   private static Particles instance;
   public final MultiSelectSetting typep = new MultiSelectSetting("Спавнить при", "Когда спавнить партиклы")
      .value("Бездействии", "Движении", "Крите", "Броске", "Тотеме")
      .selected("Бездействии", "Движении", "Крите", "Тотеме");
   private final SliderSettings countAFK = new SliderSettings("Кол-во при бездействии", "Количество партиклов при бездействии")
      .range(1, 25)
      .setValue(5.0F)
      .step(1.0F)
      .visible(() -> this.typep.isSelected("Бездействии"));
   private final SliderSettings countAttack = new SliderSettings("Кол-во при крите", "Количество партиклов при крите")
      .range(1, 50)
      .setValue(2.0F)
      .step(1.0F)
      .visible(() -> this.typep.isSelected("Крите"));
   private final SliderSettings countMove = new SliderSettings("Кол-во при движении", "Количество партиклов при движении")
      .range(1, 25)
      .setValue(2.0F)
      .step(1.0F)
      .visible(() -> this.typep.isSelected("Движении"));
   private final SliderSettings moveLifetime = new SliderSettings("Время жизни при движении", "Время жизни партиклов при движении в мс")
      .range(500, 5000)
      .setValue(3500.0F)
      .step(250.0F)
      .visible(() -> this.typep.isSelected("Движении"));
   private final SliderSettings moveSpeed = new SliderSettings("Скорость при движении", "Скорость партиклов при движении")
      .range(0.1F, 4.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Движении"));
   private final SliderSettings moveSize = new SliderSettings("Размер при движении", "Размер партиклов при движении")
      .range(0.0F, 1.0F)
      .setValue(0.5F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Движении"));
   private final SliderSettings moveOpacity = new SliderSettings("Прозрачность при движении", "Прозрачность партиклов при движении")
      .range(0.1F, 1.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Движении"));
   private final SliderSettings size = new SliderSettings("Размер", "Размер партиклов").range(0.0F, 1.0F).setValue(0.5F).step(0.1F);
   private final SliderSettings range = new SliderSettings("Дистанция", "Дистанция спавна").range(4, 32).setValue(16.0F).step(1.0F);
   private final SliderSettings duration = new SliderSettings("Время жизни", "Время жизни партиклов").range(500, 5000).setValue(3500.0F).step(250.0F);
   private final SliderSettings strength = new SliderSettings("Скорость движения", "Скорость движения партиклов").range(0.1F, 2.0F).setValue(1.0F).step(0.1F);
   private final SliderSettings opacity = new SliderSettings("Прозрачность", "Прозрачность партиклов").range(0.1F, 1.0F).setValue(1.0F).step(0.1F);
   private final SliderSettings critSize = new SliderSettings("Размер при крите", "Размер партиклов при крите")
      .range(0.0F, 1.0F)
      .setValue(0.5F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Крите"));
   private final SliderSettings critDuration = new SliderSettings("Время жизни при крите", "Время жизни партиклов при крите")
      .range(500, 5000)
      .setValue(3500.0F)
      .step(250.0F)
      .visible(() -> this.typep.isSelected("Крите"));
   private final SliderSettings critStrength = new SliderSettings("Скорость движения при крите", "Скорость движения партиклов при крите")
      .range(0.1F, 4.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Крите"));
   private final SliderSettings critOpacity = new SliderSettings("Прозрачность при крите", "Прозрачность партиклов при крите")
      .range(0.1F, 1.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Крите"));
   private final SliderSettings totemCount = new SliderSettings("Кол-во при тотеме", "Количество партиклов при тотеме")
      .range(1, 200)
      .setValue(75.0F)
      .step(1.0F)
      .visible(() -> this.typep.isSelected("Тотеме"));
   private final SliderSettings totemSpeed = new SliderSettings("Скорость при тотеме", "Скорость партиклов при тотеме")
      .range(0.1F, 10.0F)
      .setValue(4.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Тотеме"));
   private final SliderSettings totemLifetime = new SliderSettings("Время жизни при тотеме", "Время жизни партиклов при тотеме в мс")
      .range(500, 10000)
      .setValue(4000.0F)
      .step(100.0F)
      .visible(() -> this.typep.isSelected("Тотеме"));
   private final SliderSettings totemSize = new SliderSettings("Размер при тотеме", "Размер партиклов при тотеме")
      .range(0.0F, 1.0F)
      .setValue(0.25F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Тотеме"));
   private final SliderSettings totemOpacity = new SliderSettings("Прозрачность при тотеме", "Прозрачность партиклов при тотеме")
      .range(0.1F, 1.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Тотеме"));
   private final SliderSettings throwCount = new SliderSettings("Кол-во при броске", "Количество партиклов при броске")
      .range(1, 25)
      .setValue(2.0F)
      .step(1.0F)
      .visible(() -> this.typep.isSelected("Броске"));
   private final SliderSettings throwLifetime = new SliderSettings("Время жизни при броске", "Время жизни партиклов при броске в мс")
      .range(500, 5000)
      .setValue(3500.0F)
      .step(250.0F)
      .visible(() -> this.typep.isSelected("Броске"));
   private final SliderSettings throwSpeed = new SliderSettings("Скорость при броске", "Скорость партиклов при броске")
      .range(0.1F, 4.0F)
      .setValue(2.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Броске"));
   private final SliderSettings throwSize = new SliderSettings("Размер при броске", "Размер партиклов при броске")
      .range(0.0F, 1.0F)
      .setValue(0.4F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Броске"));
   private final SliderSettings throwOpacity = new SliderSettings("Прозрачность при броске", "Прозрачность партиклов при броске")
      .range(0.1F, 1.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.typep.isSelected("Броске"));
   private final SelectSetting particleMode = new SelectSetting("Тип частиц", "Тип частиц")
      .value("Шарики", "Кубы", "Доллары", "Сердечки", "Снежинки", "Звезды", "Звезды2", "Рандом")
      .selected("Шарики");
   private final BooleanSetting glowing = new BooleanSetting("Свечение", "Свечение партиклов")
      .setValue(true)
      .visible(() -> this.particleMode.isSelected("Кубы"));
   private final BooleanSetting onlyMove = new BooleanSetting("Только в движении", "Спавнить только в движении").setValue(false);
   private final BooleanSetting ground = new BooleanSetting("Спавнить на земле", "Спавнить партиклы на земле").setValue(false);
   private final BooleanSetting physic = new BooleanSetting("Физика", "Физика партиклов").setValue(false);
   private final SelectSetting colorMode = new SelectSetting("Режим цвета", "Режим цвета партиклов")
      .value("Клиентский", "Радужный", "Кастом")
      .selected("Клиентский");
   public final ColorSetting customMainColor = new ColorSetting("Главный цвет", "Главный цвет").value(-1).visible(() -> this.colorMode.isSelected("Кастом"));
   public final ColorSetting customSecondaryColor = new ColorSetting("Вторичный цвет", "Вторичный цвет")
      .value(-1)
      .visible(() -> this.colorMode.isSelected("Кастом"));
   private final List<Particles.Particle> particles = new ArrayList<>();
   private final List<Particles.ParticleAttack> targetParticles = new ArrayList<>();
   private final List<Particles.ParticleAttack> flameParticles = new ArrayList<>();
   private final List<Particles.ParticleAttack> thrownParticles = new ArrayList<>();
   private final List<Particles.ParticleAttack> totemParticles = new ArrayList<>();
   private double lastPlayerX = 0.0;
   private double lastPlayerY = 0.0;
   private double lastPlayerZ = 0.0;
   private boolean positionInitialized = false;
   private static final double CUBE_ROTATION_RAD_PER_MS = 1.2E-8;
   private static final double CUBE_GLOW_DISTANCE_SQ = 2304.0;
   private static final float CUBE_LINE_WIDTH = 1.5F;
   private static final float CUBE_DIAG_WIDTH = 1.0F;
   private static final double[][] CUBE_OFFSETS = new double[][]{
      {-1.0, -1.0, -1.0}, {1.0, -1.0, -1.0}, {1.0, 1.0, -1.0}, {-1.0, 1.0, -1.0}, {-1.0, -1.0, 1.0}, {1.0, -1.0, 1.0}, {1.0, 1.0, 1.0}, {-1.0, 1.0, 1.0}
   };
   private static final int[][] CUBE_EDGES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private static final int[][] CUBE_DIAGONALS = new int[][]{{0, 6}, {1, 7}, {2, 4}, {3, 5}};
   private final Vec3d[] cubeCornerBuffer = new Vec3d[8];

   public Particles() {
      super("Particles", ModuleCategory.RENDER);
      this.setup(
         new Setting[]{
            this.particleMode,
            this.typep,
            this.countAFK,
            this.range,
            this.duration,
            this.strength,
            this.opacity,
            this.size,
            this.onlyMove,
            this.ground,
            this.countMove,
            this.moveLifetime,
            this.moveSpeed,
            this.moveSize,
            this.moveOpacity,
            this.countAttack,
            this.critSize,
            this.critDuration,
            this.critStrength,
            this.critOpacity,
            this.throwCount,
            this.throwLifetime,
            this.throwSpeed,
            this.throwSize,
            this.throwOpacity,
            this.totemCount,
            this.totemSpeed,
            this.totemLifetime,
            this.totemSize,
            this.totemOpacity,
            this.glowing,
            this.physic,
            this.colorMode,
            this.customMainColor,
            this.customSecondaryColor
         }
      );
      instance = this;
   }

   public static Particles getInstance() {
      return instance;
   }

   private void clear() {
      this.particles.clear();
      this.targetParticles.clear();
      this.flameParticles.clear();
      this.thrownParticles.clear();
      this.totemParticles.clear();
   }

   @Override
   public void setState(boolean state) {
      super.setState(state);
      if (!state) {
         this.clear();
      }
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      if (this.typep.isSelected("Крите")) {
         if (mc.player.fallDistance != 0.0F) {
            Entity target = e.getEntity();
            if (target != null) {
               float motion = this.strength.getValue();

               for (int i = 0; i < this.countAttack.getInt(); i++) {
                  this.spawnParticleAttack(
                     this.targetParticles,
                     new Vec3d(target.getX(), target.getY() + this.randomValue(0.0, target.getHeight()), target.getZ()),
                     new Vec3d(this.randomValue(-motion, motion), this.randomValue(-motion, motion / 4.0F), this.randomValue(-motion, motion))
                  );
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.typep.isSelected("Движении") && this.hasPlayerMoved() && !mc.options.getPerspective().isFirstPerson()) {
            for (int i = 0; i < this.countMove.getInt(); i++) {
               Vec3d playerVel = mc.player.getVelocity();
               this.spawnParticleAttack(
                  this.flameParticles,
                  new Vec3d(
                     mc.player.getX() + this.randomValue(-0.5, 0.5),
                     mc.player.getY() + this.randomValue(0.0, mc.player.getHeight()),
                     mc.player.getZ() + this.randomValue(-0.5, 0.5)
                  ),
                  new Vec3d(playerVel.x + this.randomValue(-0.25, 0.25), this.randomValue(-0.15, 0.15), playerVel.z + this.randomValue(-0.25, 0.25))
                     .multiply(this.moveSpeed.getValue())
               );
            }
         }

         if (this.typep.isSelected("Бездействии") && (!this.onlyMove.isValue() || this.hasPlayerMoved())) {
            int r = this.range.getInt();

            for (int i = 0; i < this.countAFK.getInt(); i++) {
               Vec3d additional = mc.player.getPos().add(this.randomValue(-r, r), 0.0, this.randomValue(-r, r));
               BlockPos pos = mc.world.getTopPosition(Type.MOTION_BLOCKING, BlockPos.ofFloored(additional));
               this.spawnParticle(
                  new Vec3d(
                     pos.getX() + this.randomValue(0.0, 1.0),
                     this.ground.isValue() ? pos.getY() : mc.player.getY() + this.randomValue(mc.player.getHeight(), r),
                     pos.getZ() + this.randomValue(0.0, 1.0)
                  ),
                  new Vec3d(0.0, this.randomValue(0.0, this.strength.getValue()) * (this.ground.isValue() ? 1 : -1), 0.0)
               );
            }
         }

         if (this.typep.isSelected("Броске")) {
            for (Entity entity : mc.world.getEntities()) {
               TridentEntity trident;
               if ((entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity)
                  && (!(entity instanceof TridentEntity) || (trident = (TridentEntity)entity).getOwner() == null)) {
                  boolean isMoving = entity.prevX != entity.getX() || entity.prevY != entity.getY() || entity.prevZ != entity.getZ();
                  if (isMoving) {
                     Vec3d pos = entity.getPos();
                     Vec3d entityVel = entity.getVelocity();

                     for (int i = 0; i < this.throwCount.getInt(); i++) {
                        this.spawnParticleThrown(
                           new Vec3d(pos.x + this.randomValue(-0.2, 0.2), pos.y + this.randomValue(-0.2, 0.2), pos.z + this.randomValue(-0.2, 0.2)),
                           new Vec3d(
                              entityVel.x * 0.1 + this.randomValue(-0.1, 0.1),
                              entityVel.y * 0.1 + this.randomValue(-0.1, 0.1),
                              entityVel.z * 0.1 + this.randomValue(-0.1, 0.1)
                           )
                        );
                     }
                  }
               }
            }
         }

         this.removeExpiredParticlesAttack(this.targetParticles, (long)this.critDuration.getValue());
         this.removeExpiredParticlesAttack(this.flameParticles, (long)this.moveLifetime.getValue());
         this.removeExpiredParticlesAttack(this.thrownParticles, (long)this.throwLifetime.getValue());
         this.removeExpiredParticlesAttack(this.totemParticles, (long)this.totemLifetime.getValue());
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> class_25962 = e.getPacket();
      Entity entity;
      EntityStatusS2CPacket packet;
      if (class_25962 instanceof EntityStatusS2CPacket
         && (packet = (EntityStatusS2CPacket)class_25962).getStatus() == 35
         && (entity = packet.getEntity(mc.world)) instanceof LivingEntity
         && this.isState()
         && this.typep.isSelected("Тотеме")) {
         this.createTotemEffect(entity.getX(), entity.getY() + entity.getHeight() / 2.0F, entity.getZ());
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.isState()) {
         MatrixStack matrix = e.getStack();
         float partialTicks = e.getPartialTicks();
         this.setupRenderState();
         if (this.typep.isSelected("Бездействии")) {
            this.renderParticles(matrix, this.particles, this.duration.getValue(), this.duration.getMin(), partialTicks);
         }

         if (this.typep.isSelected("Крите")) {
            this.renderParticlesAttack(matrix, this.targetParticles, 500L, 2000L, partialTicks);
         }

         if (this.typep.isSelected("Движении") && !mc.options.getPerspective().isFirstPerson()) {
            this.renderParticlesAttack(matrix, this.flameParticles, 500L, (long)this.moveLifetime.getValue(), partialTicks);
         }

         if (this.typep.isSelected("Броске")) {
            this.renderParticlesAttack(matrix, this.thrownParticles, 500L, (long)this.throwLifetime.getValue(), partialTicks);
         }

         if (this.typep.isSelected("Тотеме")) {
            this.renderParticlesAttack(matrix, this.totemParticles, 1000L, (long)this.totemLifetime.getValue(), partialTicks);
         }

         this.resetRenderState();
      }
   }

   private void renderParticles(MatrixStack matrix, List<Particles.Particle> particles, double lifetime, double duration, float partialTicks) {
      this.removeExpiredParticles(particles, lifetime + duration);
      if (!particles.isEmpty()) {
         matrix.push();
         RenderSystem.enableDepthTest();

         for (Particles.Particle particle : particles) {
            particle.update(this.physic.isValue());
            Animation animation = particle.animation();
            animation.update();
            float alpha = animation.get();
            if (alpha != this.opacity.getValue() && !particle.time().isReached((long)duration)) {
               animation.run(this.opacity.getValue(), duration / 1000.0, Easings.CUBIC_OUT);
            }

            if (alpha != 0.0F && particle.time().isReached((long)lifetime)) {
               animation.run(0.0, duration / 1000.0, Easings.CUBIC_OUT);
            }

            int color = ColorAssist.multAlpha(
               ColorAssist.replAlpha(particle.color(), alpha), (float)((Math.sin((System.currentTimeMillis() - particle.spawnTime()) / 200.0) + 1.0) / 2.0)
            );
            Vec3d vec = particle.position();
            float x = (float)vec.x;
            float y = (float)vec.y;
            float z = (float)vec.z;
            this.renderParticle(matrix, particle, x, y, z, color, partialTicks);
         }

         matrix.pop();
      }
   }

   private void removeExpiredParticles(List<Particles.Particle> particles, double lifespan) {
      particles.removeIf(particle -> !Projection.canSee(particle.box));
      particles.removeIf(particle -> particle.time().isReached((long)lifespan));
   }

   private void removeExpiredParticlesAttack(List<Particles.ParticleAttack> particles, long lifespan) {
      if (particles == this.totemParticles || particles == this.targetParticles) {
         particles.removeIf(particle -> particle.time().isReached(lifespan));
      } else if (particles == this.flameParticles) {
         particles.removeIf(particle -> particle.time().isReached(lifespan));
      } else {
         particles.removeIf(particle -> !Projection.canSee(particle.box));
         particles.removeIf(particle -> particle.time().isReached(lifespan));
      }
   }

   private void renderParticle(MatrixStack matrix, Particles.Particle particle, float x, float y, float z, int color, float partialTicks) {
      if (particle.type() == Particles.ParticleType.cube) {
         Vec3d prevPos = particle.prevPosition();
         Vec3d pos = particle.position();
         double renderX = prevPos.x + (pos.x - prevPos.x) * partialTicks;
         double renderY = prevPos.y + (pos.y - prevPos.y) * partialTicks;
         double renderZ = prevPos.z + (pos.z - prevPos.z) * partialTicks;
         Vec3d prevRot = particle.prevRotateVec();
         Vec3d baseRot = particle.rotateVec();
         long elapsed = particle.time().getTime();
         double timeY = elapsed * 1.2E-8;
         double timeX = elapsed * 1.2E-8 * 0.6;
         double rotX = prevRot.x + (baseRot.x - prevRot.x) * partialTicks + timeX;
         double rotY = prevRot.y + (baseRot.y - prevRot.y) * partialTicks + timeY;
         this.renderCube(renderX, renderY, renderZ, rotX, rotY, particle.size * 2.0F, color, this.glowing.isValue());
      } else {
         float pos = particle.size;
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d camPos = camera.getPos();
         double posX = x - camPos.x;
         double posY = y - camPos.y;
         double posZ = z - camPos.z;
         MatrixStack matrices = new MatrixStack();
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         matrices.translate(posX, posY, posZ);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         if (particle.type() != Particles.ParticleType.star) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         }

         if (particle.type() == Particles.ParticleType.heart) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         }

         if (particle.type() == Particles.ParticleType.dollar) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         }

         if (particle.type().rotatable()) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotate()));
         }

         matrices.translate(0.0F, -pos, -pos);
         if (particle.type() == Particles.ParticleType.shariki) {
            Render3D.drawTexture(
               matrices.peek().copy(),
               Particles.ParticleType.shariki.texture(),
               -pos * 4.0F,
               -pos * 4.0F,
               pos * 8.0F,
               pos * 8.0F,
               new Vector4i(ColorAssist.multAlpha(color, 0.1F)),
               true
            );
         }

         Render3D.drawTexture(matrices.peek().copy(), particle.type().texture(), -pos, -pos, pos * 2.0F, pos * 2.0F, new Vector4i(color), true);
         if (particle.type.equals(Particles.ParticleType.shariki)) {
            Render3D.drawTexture(matrices.peek().copy(), particle.type().texture(), -pos / 2.0F, -pos / 2.0F, pos, pos, new Vector4i(color), true);
         }

         matrices.pop();
      }
   }

   private void setupRenderState() {
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
   }

   private void resetRenderState() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
   }

   private void renderParticlesAttack(MatrixStack matrix, List<Particles.ParticleAttack> particles, long fadeInTime, long fadeOutTime, float partialTicks) {
      if (!particles.isEmpty()) {
         matrix.push();
         RenderSystem.enableDepthTest();

         for (Particles.ParticleAttack particle : particles) {
            boolean usePhysics = this.physic.isValue() || particles == this.totemParticles;
            particle.update(usePhysics);
            particle.animation.update();
            float targetOpacity;
            long lifetime;
            if (particles == this.targetParticles) {
               lifetime = (long)this.critDuration.getValue();
               targetOpacity = this.critOpacity.getValue();
            } else if (particles == this.flameParticles) {
               lifetime = (long)this.moveLifetime.getValue();
               targetOpacity = this.moveOpacity.getValue();
            } else if (particles == this.thrownParticles) {
               lifetime = (long)this.throwLifetime.getValue();
               targetOpacity = this.throwOpacity.getValue();
            } else if (particles == this.totemParticles) {
               lifetime = (long)this.totemLifetime.getValue();
               targetOpacity = this.totemOpacity.getValue();
            } else {
               lifetime = fadeOutTime;
               targetOpacity = this.opacity.getValue();
            }

            if (particle.animation().get() != targetOpacity) {
               if (!particle.time().isReached(fadeInTime)) {
                  particle.animation().run(targetOpacity, 0.5, Easings.CUBIC_OUT);
               } else if (particle.animation().get() == 0.0F && particle.time().getTime() < fadeInTime) {
                  particle.animation().run(targetOpacity, 0.5, Easings.CUBIC_OUT);
               }
            }

            long fadeOutStart = lifetime - 1000L;
            if (particle.animation().get() != 0.0F && particle.time().isReached(fadeOutStart)) {
               particle.animation().run(0.0, 1.0, Easings.CUBIC_OUT);
            }

            int color = ColorAssist.replAlpha(particle.color(), particle.animation.get());
            Vec3d vec = particle.position();
            float x = (float)vec.x;
            float y = (float)vec.y;
            float z = (float)vec.z;
            this.renderParticleAttack(matrix, particle, x, y, z, particle.size, color, partialTicks);
         }

         matrix.pop();
      }
   }

   private void renderParticleAttack(MatrixStack matrix, Particles.ParticleAttack particle, float x, float y, float z, float pos, int color, float partialTicks) {
      if (particle.type() == Particles.ParticleType.cube) {
         Vec3d prevPos = particle.prevPosition();
         Vec3d position = particle.position();
         double renderX = prevPos.x + (position.x - prevPos.x) * partialTicks;
         double renderY = prevPos.y + (position.y - prevPos.y) * partialTicks;
         double renderZ = prevPos.z + (position.z - prevPos.z) * partialTicks;
         Vec3d prevRot = particle.prevRotateVec();
         Vec3d baseRot = particle.rotateVec();
         long elapsed = particle.time().getTime();
         double timeY = elapsed * 1.2E-8;
         double timeX = elapsed * 1.2E-8 * 0.6;
         double rotX = prevRot.x + (baseRot.x - prevRot.x) * partialTicks + timeX;
         double rotY = prevRot.y + (baseRot.y - prevRot.y) * partialTicks + timeY;
         this.renderCube(renderX, renderY, renderZ, rotX, rotY, particle.size * 2.0F, color, this.glowing.isValue());
      } else {
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d camPos = camera.getPos();
         double posX = x - camPos.x;
         double posY = y - camPos.y;
         double posZ = z - camPos.z;
         MatrixStack matrices = new MatrixStack();
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         matrices.translate(posX, posY, posZ);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         if (particle.type() != Particles.ParticleType.star) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         }

         if (particle.type() == Particles.ParticleType.heart) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         }

         if (particle.type() == Particles.ParticleType.dollar) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         }

         if (particle.type().rotatable()) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotate()));
         }

         matrices.translate(0.0F, -pos, 0.0F);
         if (particle.type() == Particles.ParticleType.shariki) {
            Render3D.drawTexture(
               matrices.peek().copy(),
               Particles.ParticleType.shariki.texture(),
               -pos * 4.0F,
               -pos * 4.0F,
               pos * 8.0F,
               pos * 8.0F,
               new Vector4i(ColorAssist.multAlpha(color, 0.1F)),
               true
            );
         }

         Render3D.drawTexture(matrices.peek().copy(), particle.type().texture(), -pos, -pos, pos * 2.0F, pos * 2.0F, new Vector4i(color), true);
         if (particle.type.equals(Particles.ParticleType.shariki)) {
            Render3D.drawTexture(matrices.peek().copy(), particle.type().texture(), -pos / 2.0F, -pos / 2.0F, pos, pos, new Vector4i(color), true);
         }

         matrices.pop();
      }
   }

   private void fillCubeCorners(double px, double py, double pz, double rotX, double rotY, float half) {
      double cosY = Math.cos(rotY);
      double sinY = Math.sin(rotY);
      double cosX = Math.cos(rotX);
      double sinX = Math.sin(rotX);

      for (int i = 0; i < 8; i++) {
         double ox = CUBE_OFFSETS[i][0] * half;
         double oy = CUBE_OFFSETS[i][1] * half;
         double oz = CUBE_OFFSETS[i][2] * half;
         double rx = ox * cosY - oz * sinY;
         double rz = ox * sinY + oz * cosY;
         double ry = oy * cosX - rz * sinX;
         rz = oy * sinX + rz * cosX;
         this.cubeCornerBuffer[i] = new Vec3d(px + rx, py + ry, pz + rz);
      }
   }

   private void renderCube(double px, double py, double pz, double rotX, double rotY, float scale, int color, boolean withGlow) {
      float half = scale * 0.5F;
      this.fillCubeCorners(px, py, pz, rotX, rotY, half);
      if (withGlow) {
         Camera class_41842 = mc.gameRenderer.getCamera();
         Vec3d camPos = class_41842.getPos();
         double dx = px - camPos.x;
         double dy = py - camPos.y;
         double dz = pz - camPos.z;
         if (dx * dx + dy * dy + dz * dz <= 2304.0) {
            MatrixStack glowMatrix = new MatrixStack();
            glowMatrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(class_41842.getPitch()));
            glowMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(class_41842.getYaw() + 180.0F));
            glowMatrix.translate(dx, dy, dz);
            glowMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-class_41842.getYaw()));
            glowMatrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(class_41842.getPitch()));
            float glowSize = scale * 0.85F;
            Render3D.drawTexture(
               glowMatrix.peek().copy(),
               Particles.ParticleType.shariki.texture(),
               -glowSize,
               -glowSize,
               glowSize * 2.0F,
               glowSize * 2.0F,
               new Vector4i(ColorAssist.multAlpha(color, 0.15F)),
               true
            );
         }
      }

      for (int[] edge : CUBE_EDGES) {
         Render3D.drawLine(this.cubeCornerBuffer[edge[0]], this.cubeCornerBuffer[edge[1]], color, 1.5F, true);
      }

      int n = ColorAssist.multAlpha(color, 0.5F);

      for (int[] diag : CUBE_DIAGONALS) {
         Render3D.drawLine(this.cubeCornerBuffer[diag[0]], this.cubeCornerBuffer[diag[1]], n, 1.0F, true);
      }
   }

   private void spawnParticle(Vec3d position, Vec3d velocity) {
      float size = 0.05F + this.size.getValue() * 0.2F;
      String colorSelected = this.colorMode.getSelected();

      int color = switch (colorSelected) {
         case "Клиентский" -> ColorAssist.fade(this.particles.size() * 100);
         case "Радужный" -> ColorAssist.rainbow(10, this.particles.size() * 100, 0.5F, 1.0F, 1.0F);
         default -> {
            int mainColor = this.customMainColor.getColor();
            int secondaryColor = this.customSecondaryColor.getColor();
            float factor = this.particles.size() % 100 / 100.0F;
            yield ColorAssist.interpolateColor(mainColor, secondaryColor, factor);
         }
      };
      String modeSelected = this.particleMode.getSelected();

      Particles.ParticleType type = switch (modeSelected) {
         case "Шарики" -> Particles.ParticleType.shariki;
         case "Кубы" -> Particles.ParticleType.cube;
         case "Звезды" -> Particles.ParticleType.star;
         case "Звезды2" -> Particles.ParticleType.startwo;
         case "Доллары" -> Particles.ParticleType.dollar;
         case "Сердечки" -> Particles.ParticleType.heart;
         case "Снежинки" -> Particles.ParticleType.snowflake;
         default -> Particles.ParticleType.getRandom();
      };
      this.particles
         .add(
            new Particles.Particle(
               type, position.add(0.0, size, 0.0), velocity, this.particles.size(), color, size, (int)(Math.round(this.randomValue(0.0, 360.0) / 15.0) * 15L)
            )
         );
   }

   private void spawnParticleAttack(List<Particles.ParticleAttack> particles, Vec3d position, Vec3d velocity) {
      float size = particles == this.targetParticles
         ? 0.05F + this.critSize.getValue() * 0.2F
         : (particles == this.flameParticles ? 0.05F + this.moveSize.getValue() * 0.2F : 0.05F + this.size.getValue() * 0.2F);
      String colorSelected = this.colorMode.getSelected();

      int color = switch (colorSelected) {
         case "Клиентский" -> ColorAssist.fade(particles.size() * 100);
         case "Радужный" -> ColorAssist.rainbow(4, particles.size() * 100, 0.5F, 1.0F, 1.0F);
         default -> {
            int mainColor = this.customMainColor.getColor();
            int secondaryColor = this.customSecondaryColor.getColor();
            yield ColorAssist.interpolateColor(mainColor, secondaryColor, particles.size() % 100 / 100.0F);
         }
      };
      String modeSelected = this.particleMode.getSelected();

      Particles.ParticleType type = switch (modeSelected) {
         case "Шарики" -> Particles.ParticleType.shariki;
         case "Кубы" -> Particles.ParticleType.cube;
         case "Звезды" -> Particles.ParticleType.star;
         case "Звезды2" -> Particles.ParticleType.startwo;
         case "Доллары" -> Particles.ParticleType.dollar;
         case "Сердечки" -> Particles.ParticleType.heart;
         case "Снежинки" -> Particles.ParticleType.snowflake;
         default -> Particles.ParticleType.getRandom();
      };
      Vec3d motion = particles == this.targetParticles
         ? velocity.multiply(this.critStrength.getValue() * 5.0F)
         : (particles == this.flameParticles ? velocity.multiply(this.moveSpeed.getValue()) : velocity.multiply(this.strength.getValue()));
      Particles.ParticleAttack particle = new Particles.ParticleAttack(
         type, position.add(0.0, size, 0.0), motion, particles.size(), (int)(Math.round(this.randomValue(0.0, 360.0) / 15.0) * 15L), color, size
      );
      particles.add(particle);
   }

   private void spawnParticleThrown(Vec3d position, Vec3d velocity) {
      float size = 0.05F + this.throwSize.getValue() * 0.2F;
      String colorSelected = this.colorMode.getSelected();

      int color = switch (colorSelected) {
         case "Клиентский" -> ColorAssist.fade(this.thrownParticles.size() * 100);
         case "Радужный" -> ColorAssist.rainbow(4, this.thrownParticles.size() * 100, 0.5F, 1.0F, 1.0F);
         default -> {
            int mainColor = this.customMainColor.getColor();
            int secondaryColor = this.customSecondaryColor.getColor();
            yield ColorAssist.interpolateColor(mainColor, secondaryColor, this.thrownParticles.size() % 100 / 100.0F);
         }
      };
      String modeSelected = this.particleMode.getSelected();

      Particles.ParticleType type = switch (modeSelected) {
         case "Шарики" -> Particles.ParticleType.shariki;
         case "Кубы" -> Particles.ParticleType.cube;
         case "Звезды" -> Particles.ParticleType.star;
         case "Звезды2" -> Particles.ParticleType.startwo;
         case "Доллары" -> Particles.ParticleType.dollar;
         case "Сердечки" -> Particles.ParticleType.heart;
         case "Снежинки" -> Particles.ParticleType.snowflake;
         default -> Particles.ParticleType.getRandom();
      };
      Vec3d motion = velocity.multiply(this.throwSpeed.getValue());
      this.thrownParticles
         .add(
            new Particles.ParticleAttack(
               type,
               position.add(0.0, size, 0.0),
               motion,
               this.thrownParticles.size(),
               (int)(Math.round(this.randomValue(0.0, 360.0) / 15.0) * 15L),
               color,
               size
            )
         );
   }

   private void spawnTotemParticle(Vec3d position, Vec3d velocity, int color) {
      String modeSelected = this.particleMode.getSelected();

      Particles.ParticleType type = switch (modeSelected) {
         case "Шарики" -> Particles.ParticleType.shariki;
         case "Кубы" -> Particles.ParticleType.cube;
         case "Звезды" -> Particles.ParticleType.star;
         case "Звезды2" -> Particles.ParticleType.startwo;
         case "Доллары" -> Particles.ParticleType.dollar;
         case "Сердечки" -> Particles.ParticleType.heart;
         case "Снежинки" -> Particles.ParticleType.snowflake;
         default -> Particles.ParticleType.getRandom();
      };
      float size = 0.05F + this.totemSize.getValue() * 0.2F;
      if (type == Particles.ParticleType.star || type == Particles.ParticleType.heart) {
         size = 0.1F;
      }

      this.totemParticles
         .add(
            new Particles.ParticleAttack(
               type,
               position.add(this.randomValue(-0.3, 0.3), this.randomValue(-0.3, 0.3), this.randomValue(-0.3, 0.3)),
               velocity.multiply(this.totemSpeed.getValue()),
               this.totemParticles.size(),
               (int)(Math.round(this.randomValue(0.0, 360.0) / 15.0) * 15L),
               color,
               size
            )
         );
   }

   private boolean hasPlayerMoved() {
      if (mc.player == null) {
         return false;
      } else if (!this.positionInitialized) {
         this.lastPlayerX = mc.player.getX();
         this.lastPlayerY = mc.player.getY();
         this.lastPlayerZ = mc.player.getZ();
         this.positionInitialized = true;
         return false;
      } else {
         boolean moved = Math.abs(mc.player.getX() - this.lastPlayerX) > 0.001
            || Math.abs(mc.player.getY() - this.lastPlayerY) > 0.001
            || Math.abs(mc.player.getZ() - this.lastPlayerZ) > 0.001;
         if (moved) {
            this.lastPlayerX = mc.player.getX();
            this.lastPlayerY = mc.player.getY();
            this.lastPlayerZ = mc.player.getZ();
         }

         return moved;
      }
   }

   private double randomValue(double min, double max) {
      return min + (max - min) * ThreadLocalRandom.current().nextDouble();
   }

   public void createTotemEffect(double x, double y, double z) {
      if (this.isState() && this.typep.isSelected("Тотеме")) {
         for (int i = 0; i < this.totemCount.getInt(); i++) {
            double angleXZ = Math.random() * Math.PI * 2.0;
            double angleY = Math.random() * Math.PI;
            double strength = 0.5 + Math.random() * 0.5;
            Vec3d velocity = new Vec3d(
               Math.sin(angleXZ) * Math.sin(angleY) * strength, Math.cos(angleY) * strength, Math.cos(angleXZ) * Math.sin(angleY) * strength
            );
            int color = Math.random() < 0.7 ? '\uff00' : 16776960;
            this.spawnTotemParticle(new Vec3d(x, y, z), velocity, color);
         }
      }
   }

   public static class Particle {
      private final long spawnTime = System.currentTimeMillis();
      private final Particles.ParticleType type;
      private final Box box;
      private final int rotate;
      private final int index;
      private final int color;
      private final float size;
      private final TimerUtil time = new TimerUtil();
      private final Animation animation = new Animation();
      private Vec3d position;
      private Vec3d prevPosition;
      private Vec3d velocity;
      private Vec3d rotateVec;
      private Vec3d prevRotateVec;
      private Vec3d rotateMotion;

      public Particle(Particles.ParticleType type, Vec3d position, Vec3d velocity, int index, int color, float size, int rotate) {
         this.type = type;
         this.rotate = rotate;
         this.box = new Box(position, position).expand(size);
         this.position = position;
         this.prevPosition = position;
         this.velocity = velocity.multiply(0.01F);
         this.index = index;
         this.color = ColorAssist.clampSaturation(color, 0.0F, 0.8F);
         this.size = size;
         this.time.resetCounter();
         this.rotateVec = Vec3d.ZERO;
         this.prevRotateVec = Vec3d.ZERO;
         this.rotateMotion = new Vec3d(randomDouble(-1.0, 1.0) * 0.04, randomDouble(-1.0, 1.0) * 0.04, randomDouble(-1.0, 1.0) * 0.04);
      }

      private static double randomDouble(double min, double max) {
         return min + (max - min) * new Random().nextDouble();
      }

      public void update(boolean physic) {
         this.prevPosition = this.position;
         this.prevRotateVec = this.rotateVec;
         if (physic) {
            if (this.isBlockSolid(this.position.x, this.position.y, this.position.z + this.velocity.z)) {
               this.velocity = new Vec3d(this.velocity.x, this.velocity.y, -this.velocity.z * 0.8);
            }

            if (this.isBlockSolid(this.position.x, this.position.y + this.velocity.y, this.position.z)) {
               this.velocity = new Vec3d(this.velocity.x * 0.999, -this.velocity.y * 0.6, this.velocity.z * 0.999);
            }

            if (this.isBlockSolid(this.position.x + this.velocity.x, this.position.y, this.position.z)) {
               this.velocity = new Vec3d(-this.velocity.x * 0.8, this.velocity.y, this.velocity.z);
            }

            this.velocity = this.velocity.multiply(0.999999).subtract(0.0, 5.0E-5, 0.0);
         }

         this.position = this.position.add(this.velocity);
         this.rotateVec = this.rotateVec.add(this.rotateMotion);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }

      private boolean isBlockSolid(double x, double y, double z) {
         BlockPos pos = BlockPos.ofFloored(x, y, z);
         return QuickImports.mc.world != null && !QuickImports.mc.world.getBlockState(pos).getCollisionShape(QuickImports.mc.world, pos).isEmpty();
      }

      public long spawnTime() {
         return this.spawnTime;
      }

      public Particles.ParticleType type() {
         return this.type;
      }

      public Box box() {
         return this.box;
      }

      public int rotate() {
         return this.rotate;
      }

      public int index() {
         return this.index;
      }

      public int color() {
         return this.color;
      }

      public float size() {
         return this.size;
      }

      public TimerUtil time() {
         return this.time;
      }

      public Animation animation() {
         return this.animation;
      }

      public Vec3d position() {
         return this.position;
      }

      public Vec3d prevPosition() {
         return this.prevPosition;
      }

      public Vec3d velocity() {
         return this.velocity;
      }

      public Vec3d rotateVec() {
         return this.rotateVec;
      }

      public Vec3d prevRotateVec() {
         return this.prevRotateVec;
      }

      public Vec3d rotateMotion() {
         return this.rotateMotion;
      }
   }

   public static class ParticleAttack {
      private final Particles.ParticleType type;
      private final int index;
      private final int rotate;
      private final int color;
      private final float size;
      private final TimerUtil time = new TimerUtil();
      private final Animation animation = new Animation();
      private Box box;
      private Vec3d position;
      private Vec3d prevPosition;
      private Vec3d velocity;
      private Vec3d rotateVec;
      private Vec3d prevRotateVec;
      private Vec3d rotateMotion;

      public ParticleAttack(Particles.ParticleType type, Vec3d position, Vec3d velocity, int index, int rotate, int color, float size) {
         this.box = new Box(position, position).expand(size);
         this.type = type;
         this.position = position;
         this.prevPosition = position;
         this.velocity = velocity.multiply(0.01F);
         this.index = index;
         this.rotate = rotate;
         this.color = color;
         this.size = size;
         this.time.resetCounter();
         this.animation.set(0.0);
         this.rotateVec = Vec3d.ZERO;
         this.prevRotateVec = Vec3d.ZERO;
         Random rand = new Random();
         this.rotateMotion = new Vec3d((rand.nextDouble() * 2.0 - 1.0) * 0.04, (rand.nextDouble() * 2.0 - 1.0) * 0.04, (rand.nextDouble() * 2.0 - 1.0) * 0.04);
      }

      public void update(boolean physic) {
         this.prevPosition = this.position;
         this.prevRotateVec = this.rotateVec;
         if (physic) {
            if (this.isBlockSolid(this.position.x, this.position.y, this.position.z + this.velocity.z)) {
               this.velocity = new Vec3d(this.velocity.x, this.velocity.y, -this.velocity.z * 0.8);
            }

            if (this.isBlockSolid(this.position.x, this.position.y + this.velocity.y, this.position.z)) {
               this.velocity = new Vec3d(this.velocity.x * 0.999, -this.velocity.y * 0.6, this.velocity.z * 0.999);
            }

            if (this.isBlockSolid(this.position.x + this.velocity.x, this.position.y, this.position.z)) {
               this.velocity = new Vec3d(-this.velocity.x * 0.8, this.velocity.y, this.velocity.z);
            }

            this.velocity = this.velocity.multiply(0.999999).subtract(0.0, 5.0E-5, 0.0);
         }

         this.position = this.position.add(this.velocity);
         this.box = new Box(this.position, this.position).expand(this.size);
         this.rotateVec = this.rotateVec.add(this.rotateMotion);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }

      private boolean isBlockSolid(double x, double y, double z) {
         BlockPos pos = BlockPos.ofFloored(x, y, z);
         return QuickImports.mc.world != null && !QuickImports.mc.world.getBlockState(pos).getCollisionShape(QuickImports.mc.world, pos).isEmpty();
      }

      public Particles.ParticleType type() {
         return this.type;
      }

      public int index() {
         return this.index;
      }

      public int rotate() {
         return this.rotate;
      }

      public int color() {
         return this.color;
      }

      public float size() {
         return this.size;
      }

      public TimerUtil time() {
         return this.time;
      }

      public Animation animation() {
         return this.animation;
      }

      public Box box() {
         return this.box;
      }

      public Vec3d position() {
         return this.position;
      }

      public Vec3d prevPosition() {
         return this.prevPosition;
      }

      public Vec3d velocity() {
         return this.velocity;
      }

      public Vec3d rotateVec() {
         return this.rotateVec;
      }

      public Vec3d prevRotateVec() {
         return this.prevRotateVec;
      }

      public Vec3d rotateMotion() {
         return this.rotateMotion;
      }
   }

   public static enum ParticleType {
      shariki("glow", false),
      cube(null, true),
      dollar("bucks1", false),
      heart("heart1", false),
      snowflake("show1", false),
      star("star1", false),
      startwo("sparkle", false);

      private final Identifier texture;
      private final boolean rotatable;
      private static final Particles.ParticleType[] VALUES = values();

      private ParticleType(String name, boolean rotatable) {
         this.texture = name != null ? Identifier.of("textures/particles/" + name + ".png") : null;
         this.rotatable = rotatable;
      }

      public static Particles.ParticleType getRandom() {
         ThreadLocalRandom random = ThreadLocalRandom.current();

         Particles.ParticleType result;
         while ((result = VALUES[random.nextInt(VALUES.length)]) == cube) {
         }

         return result;
      }

      public Identifier texture() {
         return this.texture;
      }

      public boolean rotatable() {
         return this.rotatable;
      }
   }
}
