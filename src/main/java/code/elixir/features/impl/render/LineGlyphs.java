package code.elixir.features.impl.render;

import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.animation.util.Animation;
import code.elixir.utils.animation.util.Easings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class LineGlyphs extends Module {
   static final int SPAWN_ATTEMPTS_PER_TICK = 4;
   private final List<LineGlyphs.Glyph> glyphs = new ArrayList<>();
   private final Random random = new Random(93882L);
   private final SliderSettings count = new SliderSettings("Количество", "Количество глифов").setValue(70.0F).range(10.0F, 200.0F).step(5.0F);
   private final BooleanSetting slow = new BooleanSetting("Медленная скорость", "Медленнее рисуются сегменты глифов");
   private final SliderSettings lineThickness = new SliderSettings("Толщина линий", "Толщина линий глифов").setValue(2.0F).range(0.5F, 5.0F).step(0.1F);
   private final SliderSettings opacity = new SliderSettings("Прозрачность", "Прозрачность глифов").setValue(1.0F).range(0.1F, 1.0F).step(0.05F);
   private final SliderSettings spawnRadius = new SliderSettings("Радиус спавна", "Дистанция появления глифов от игрока")
      .setValue(16.0F)
      .range(4.0F, 48.0F)
      .step(1.0F);
   private final SelectSetting colorMode = new SelectSetting("Режим цвета", "Режим окраски глифов")
      .value("Клиентский", "Радужный", "Кастом")
      .selected("Клиентский");
   private final ColorSetting customColor = new ColorSetting("Цвет", "Кастомный цвет глифов")
      .value(ColorAssist.getClientColor())
      .visible(() -> this.colorMode.isSelected("Кастом"));

   public LineGlyphs() {
      super("LineGlyphs", "Line Glyphs", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.count, this.slow, this.lineThickness, this.opacity, this.spawnRadius, this.colorMode, this.customColor});
   }

   public static LineGlyphs getInstance() {
      return Instance.get(LineGlyphs.class);
   }

   @Override
   public void activate() {
      super.activate();
      this.glyphs.clear();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.glyphs.clear();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.isState() && mc.player != null && mc.world != null) {
         this.glyphs.removeIf(LineGlyphs.Glyph::isDead);
         int cap = this.count.getInt();
         int attempts = 4;

         while (attempts-- > 0 && this.glyphs.size() < cap) {
            this.glyphs.add(new LineGlyphs.Glyph(this.spawnPos(), this.random.nextInt(7, 13)));
         }

         this.glyphs.forEach(LineGlyphs.Glyph::tick);
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (this.isState() && mc.player != null && !this.glyphs.isEmpty()) {
         this.drawAll(event.getStack(), event.getPartialTicks());
      }
   }

   private void drawAll(MatrixStack matrix, float partialTicks) {
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float baseOpacity = this.opacity.getValue();
      float halfWidth = this.lineThickness.getValue() * 0.04F;
      float cullRadius = this.spawnRadius.getValue() * 1.75F;
      double cullDistSq = cullRadius * cullRadius;
      int glyphIndex = 0;
      boolean hasLines = false;

      for (LineGlyphs.Glyph glyph : this.glyphs) {
         if (glyph.nodes.isEmpty()) {
            glyphIndex++;
         } else {
            int[] first = glyph.nodes.get(0);
            double dx = first[0] - cameraPos.x;
            double dz;
            double dy;
            if (dx * dx + (dy = first[1] - cameraPos.y) * dy + (dz = first[2] - cameraPos.z) * dz > cullDistSq) {
               glyphIndex++;
            } else {
               if (glyph.appendSegments(buffer, matrix4f, partialTicks, glyphIndex, baseOpacity, halfWidth)) {
                  hasLines = true;
               }

               glyphIndex++;
            }
         }
      }

      if (hasLines) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferDraw.draw(buffer);
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   private static void appendThickSegment(
      BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float halfWidth, int color
   ) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(len < 1.0E-6)) {
         double var44;
         double var45;
         double px = (var44 = dy / len) * 0.0 - (var45 = dz / len) * 1.0;
         double pz;
         double py;
         double plen;
         if ((plen = Math.sqrt(px * px + (py = var45 * 0.0 - (dx = dx / len) * 0.0) * py + (pz = dx * 1.0 - var44 * 0.0) * pz)) < 1.0E-6) {
            px = var44 * 0.0 - var45 * 0.0;
            py = var45 * 1.0 - dx * 0.0;
            pz = dx * 0.0 - var44 * 1.0;
            plen = Math.sqrt(px * px + py * py + pz * pz);
         }

         px = px / plen * halfWidth;
         py = py / plen * halfWidth;
         pz = pz / plen * halfWidth;
         float fx1 = (float)x1;
         float fy1 = (float)y1;
         float fz1 = (float)z1;
         float fx2 = (float)x2;
         float fy2 = (float)y2;
         float fz2 = (float)z2;
         float fpx = (float)px;
         float fpy = (float)py;
         float fpz = (float)pz;
         buffer.vertex(matrix, fx1 + fpx, fy1 + fpy, fz1 + fpz).color(color);
         buffer.vertex(matrix, fx1 - fpx, fy1 - fpy, fz1 - fpz).color(color);
         buffer.vertex(matrix, fx2 - fpx, fy2 - fpy, fz2 - fpz).color(color);
         buffer.vertex(matrix, fx2 + fpx, fy2 + fpy, fz2 + fpz).color(color);
      }
   }

   private int glyphColor(int index, float alpha) {
      String var4 = this.colorMode.getSelected();

      int rgb = switch (var4) {
         case "Радужный" -> ColorAssist.rainbow(20, index, 1.0F, 1.0F, 1.0F);
         case "Кастом" -> this.customColor.getColor();
         default -> ColorAssist.fade(index);
      };
      return ColorAssist.multAlpha(rgb, alpha);
   }

   private int[] spawnPos() {
      double fov = ((Integer)mc.options.getFov().getValue()).intValue();
      double yaw = Math.toRadians(this.random.nextInt((int)(mc.player.getYaw() - fov * 0.75), (int)(mc.player.getYaw() + fov * 0.75)));
      float radius = this.spawnRadius.getValue();
      float minDistance = Math.max(4.0F, radius * 0.25F);
      double distance = minDistance + this.random.nextDouble() * (radius - minDistance);
      int dx = (int)(-(Math.sin(yaw) * distance));
      int dy = this.random.nextInt(0, 12);
      int dz = (int)(Math.cos(yaw) * distance);
      Vec3d eye = mc.player.getEyePos();
      return new int[]{(int)eye.x + dx, (int)eye.y + dy, (int)eye.z + dz};
   }

   private int[] randomDirection() {
      return new int[]{this.random.nextInt(0, 4) * 90, this.random.nextInt(-1, 2) * 90};
   }

   private int[] nextDirection(int[] previous) {
      int nextB = switch (previous[1]) {
         case 0 -> this.random.nextBoolean() ? 90 : -90;
         case 90 -> this.random.nextBoolean() ? 0 : -90;
         default -> this.random.nextBoolean() ? 0 : 90;
      };
      int nextA = (previous[0] + 90 * (1 + this.random.nextInt(3))) % 360;
      if (nextA == previous[0]) {
         nextA = (nextA + 90) % 360;
      }

      return new int[]{nextA, nextB};
   }

   private int[] step(int[] position, int[] direction, int radius) {
      double yaw = Math.toRadians(direction[0]);
      double pitch = Math.toRadians(direction[1]);
      double horizontalRadius = radius;
      int deltaY = (int)(Math.sin(pitch) * horizontalRadius);
      if (pitch != 0.0) {
         horizontalRadius = 0.0;
      }

      int deltaX = (int)(-(Math.sin(yaw) * horizontalRadius));
      int deltaZ = (int)(Math.cos(yaw) * horizontalRadius);
      return new int[]{position[0] + deltaX, position[1] + deltaY, position[2] + deltaZ};
   }

   private class Glyph {
      final List<int[]> nodes = new ArrayList<>();
      final double[] pointScratch = new double[3];
      final double[] prevScratch = new double[3];
      int[] direction;
      int stepsLeft;
      int ticksLeft;
      int lastSet;
      boolean dying;
      final Animation animation = new Animation();

      Glyph(int[] spawn, int steps) {
         this.nodes.add(spawn);
         this.direction = LineGlyphs.this.randomDirection();
         this.stepsLeft = steps;
         this.animation.set(0.0);
         this.animation.run(1.0, 0.6, Easings.SINE_OUT);
      }

      void tick() {
         this.animation.update();
         if (this.stepsLeft == 0) {
            if (!this.dying) {
               this.dying = true;
               this.animation.run(0.0, 0.4, Easings.SINE_IN);
            }
         } else if (this.ticksLeft > 0) {
            this.ticksLeft = this.ticksLeft - (LineGlyphs.this.slow.isValue() ? 1 : 2);
            if (this.ticksLeft < 0) {
               this.ticksLeft = 0;
            }
         } else {
            this.direction = LineGlyphs.this.nextDirection(this.direction);
            this.lastSet = this.ticksLeft = LineGlyphs.this.random.nextInt(0, 3);
            this.nodes.add(LineGlyphs.this.step(this.nodes.get(this.nodes.size() - 1), this.direction, Math.max(1, this.ticksLeft)));
            this.stepsLeft--;
         }
      }

      float alpha() {
         return this.animation.get();
      }

      boolean isDead() {
         return this.stepsLeft == 0 && this.animation.get() < 0.01F && this.animation.isFinished();
      }

      boolean appendSegments(BufferBuilder buffer, Matrix4f matrix4f, float partialTicks, int colorIndex, float baseOpacity, float halfWidth) {
         int size = this.nodes.size();
         if (size < 2) {
            return false;
         } else {
            boolean wrote = false;
            this.getNodeCoord(0, partialTicks, this.prevScratch);

            for (int i = 1; i < size; i++) {
               this.getNodeCoord(i, partialTicks, this.pointScratch);
               float alpha = this.alpha() * baseOpacity * (0.25F + (float)i / size / 1.75F);
               int color = LineGlyphs.this.glyphColor(colorIndex + i * 180, alpha);
               LineGlyphs.appendThickSegment(
                  buffer,
                  matrix4f,
                  this.prevScratch[0],
                  this.prevScratch[1],
                  this.prevScratch[2],
                  this.pointScratch[0],
                  this.pointScratch[1],
                  this.pointScratch[2],
                  halfWidth,
                  color
               );
               this.prevScratch[0] = this.pointScratch[0];
               this.prevScratch[1] = this.pointScratch[1];
               this.prevScratch[2] = this.pointScratch[2];
               wrote = true;
            }

            return wrote;
         }
      }

      void getNodeCoord(int index, float partialTicks, double[] out) {
         int[] node = this.nodes.get(index);
         double x = node[0];
         double y = node[1];
         double z = node[2];
         if (index == this.nodes.size() - 1 && this.nodes.size() >= 2) {
            int[] previous = this.nodes.get(index - 1);
            float advance = this.lastSet > 0 ? Math.max(0.0F, Math.min(1.0F, 1.0F - (float)this.ticksLeft / this.lastSet)) : 1.0F;
            x = MathHelper.lerp(advance, previous[0], x);
            y = MathHelper.lerp(advance, previous[1], y);
            z = MathHelper.lerp(advance, previous[2], z);
         }

         out[0] = x;
         out[1] = y;
         out[2] = z;
      }
   }
}
