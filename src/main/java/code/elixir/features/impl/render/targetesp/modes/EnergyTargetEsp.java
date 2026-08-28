package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class EnergyTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Energy";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float height = ctx.target.getHeight();
      float baseRadius = (ctx.target.getWidth() * 0.55F + 0.25F) * ctx.getEffectiveSize();
      float speed = ctx.getEffectiveSpeed();
      long time = ctx.timeMs;

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      int numArcs = 3;
      int pointsPerArc = 20;

      for (int a = 0; a < numArcs; a++) {
         double arcOffset = a * (Math.PI * 2 / numArcs);
         double baseRot = time * 0.0025 * speed * ctx.getRotationDirMultiplier() + arcOffset;

         for (int p = 0; p < pointsPerArc; p++) {
            float progress = (float)p / (pointsPerArc - 1);
            float y = (float)targetPos.y + progress * height;

            double angle = baseRot + progress * Math.PI * 3.0;
            float jitterX = (float)Math.sin(p * 12.34 + time * 0.02 * speed) * 0.08F * ctx.getEffectiveScale();
            float jitterZ = (float)Math.cos(p * 15.67 + time * 0.02 * speed) * 0.08F * ctx.getEffectiveScale();
            float jitterY = (float)Math.sin(p * 9.81 + time * 0.015 * speed) * 0.04F * ctx.getEffectiveScale();

            float r = baseRadius * (1.0F + 0.15F * (float)Math.sin(progress * Math.PI));
            float x = (float)targetPos.x + (float)Math.cos(angle) * r + jitterX;
            float z = (float)targetPos.z + (float)Math.sin(angle) * r + jitterZ;
            float finalY = y + jitterY;

            float fade = (float)Math.sin(progress * Math.PI);
            int arcColor = ColorAssist.interpolateColor(color1, color2, progress);
            int alpha = (int)(220.0F * fade * ctx.anim);
            int col = ColorAssist.setAlpha(arcColor, alpha);

            float ribbonWidth = 0.04F * ctx.getEffectiveSize();
            buffer.vertex(mat, x - ribbonWidth, finalY - ribbonWidth, z).color(col);
            buffer.vertex(mat, x + ribbonWidth, finalY + ribbonWidth, z).color(col);
         }
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreColorRenderState();

      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (int a = 0; a < numArcs; a++) {
            double arcOffset = a * (Math.PI * 2 / numArcs);
            double baseRot = time * 0.0025 * speed * ctx.getRotationDirMultiplier() + arcOffset;

            for (int k = 1; k <= 3; k++) {
               float progress = k * 0.25F;
               float y = (float)targetPos.y + progress * height;
               double angle = baseRot + progress * Math.PI * 3.0;
               float r = baseRadius * (1.0F + 0.15F * (float)Math.sin(progress * Math.PI));
               float x = (float)targetPos.x + (float)Math.cos(angle) * r;
               float z = (float)targetPos.z + (float)Math.sin(angle) * r;

               float sparkSize = (0.35F + 0.1F * (float)Math.sin(time * 0.01 + a * 3 + k)) * ctx.getEffectiveSize();
               int sparkColor = ColorAssist.setAlpha(color1, (int)(190.0F * ctx.anim));

               TargetEspRenderUtil.drawBloomBillboard(
                  bloomBuffer,
                  ctx.matrices,
                  ctx.camera,
                  x,
                  y,
                  z,
                  sparkSize,
                  sparkColor
               );
            }
         }

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
