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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class HelixTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Helix";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float height = ctx.target.getHeight();
      float width = ctx.target.getWidth();
      float radius = (width * 0.65F + 0.2F) * ctx.getEffectiveSize();
      float speed = ctx.getEffectiveSpeed() * ctx.esp.getRotationSpeed().getValue() * ctx.getRotationDirMultiplier();
      long time = ctx.timeMs;

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      int steps = 36;
      double baseRot = time * 0.0028 * speed;
      float ribbonHalfW = 0.038F * ctx.getEffectiveSize();

      // Render each strand in its own clean TRIANGLE_STRIP batch to avoid triangle strip crossover lag
      for (int strand = 0; strand < 2; strand++) {
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         double strandPhase = strand * Math.PI;
         int strandCol = strand == 0 ? color1 : color2;

         for (int i = 0; i <= steps; i++) {
            float progress = (float)i / steps;
            float y = (float)targetPos.y + progress * (height + 0.3F);
            double angle = baseRot + progress * Math.PI * 3.5 + strandPhase;

            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);

            float r = radius * (0.85F + 0.15F * MathHelper.sin(progress * (float)Math.PI));
            float x = (float)targetPos.x + cos * r;
            float z = (float)targetPos.z + sin * r;

            float fade = (float)Math.sin(progress * Math.PI);
            int alpha = (int)(230.0F * fade * ctx.anim);
            int col = ColorAssist.setAlpha(strandCol, alpha);

            buffer.vertex(mat, x, y - ribbonHalfW, z).color(col);
            buffer.vertex(mat, x, y + ribbonHalfW, z).color(col);
         }

         BufferDraw.draw(buffer);
      }

      TargetEspRenderUtil.restoreColorRenderState();

      // Bloom particle points on each strand
      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (int strand = 0; strand < 2; strand++) {
            double strandPhase = strand * Math.PI;
            int strandCol = strand == 0 ? color1 : color2;

            for (int k = 1; k <= 4; k++) {
               float progress = k * 0.2F;
               float y = (float)targetPos.y + progress * (height + 0.3F);
               double angle = baseRot + progress * Math.PI * 3.5 + strandPhase;
               float r = radius * (0.85F + 0.15F * MathHelper.sin(progress * (float)Math.PI));
               float x = (float)targetPos.x + (float)Math.cos(angle) * r;
               float z = (float)targetPos.z + (float)Math.sin(angle) * r;

               float glowSize = 0.45F * ctx.getEffectiveSize() * (float)Math.sin(progress * Math.PI);
               TargetEspRenderUtil.drawBloomBillboard(
                  bloomBuffer,
                  ctx.matrices,
                  ctx.camera,
                  x,
                  y,
                  z,
                  glowSize,
                  ColorAssist.setAlpha(strandCol, (int)(180.0F * ctx.anim))
               );
            }
         }

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
