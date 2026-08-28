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

public class GlitchTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Glitch";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float height = ctx.target.getHeight();
      float halfW = (ctx.target.getWidth() * 0.55F + 0.1F) * ctx.getEffectiveSize();
      long time = ctx.timeMs;
      float speed = ctx.getEffectiveSpeed();

      long cycle = (long)(time * speed) % 350L;
      boolean isGlitching = cycle < 85L;
      float glitchOffset = isGlitching ? ((float)Math.sin(time * 0.05) * 0.12F * ctx.getEffectiveScale()) : 0.0F;

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      drawGlitchBoxCorners(
         buffer,
         mat,
         targetPos.x + glitchOffset,
         targetPos.y,
         targetPos.z,
         halfW,
         height,
         ColorAssist.setAlpha(color1, (int)(220.0F * ctx.anim))
      );

      if (isGlitching) {
         drawGlitchBoxCorners(
            buffer,
            mat,
            targetPos.x - glitchOffset * 1.3F,
            targetPos.y + glitchOffset * 0.4F,
            targetPos.z + glitchOffset * 0.7F,
            halfW * 1.04F,
            height,
            ColorAssist.setAlpha(color2, (int)(180.0F * ctx.anim))
         );
      }

      int scanlineCount = 5;
      for (int s = 0; s < scanlineCount; s++) {
         float sProgress = (float)s / (scanlineCount - 1);
         float y = (float)targetPos.y + sProgress * height;
         float scanJitter = isGlitching ? ((float)Math.sin(s * 7.7 + time * 0.03) * 0.06F) : 0.0F;

         int lineCol = ColorAssist.setAlpha(ColorAssist.interpolateColor(color1, color2, sProgress), (int)(130.0F * ctx.anim));

         buffer.vertex(mat, (float)targetPos.x - halfW + scanJitter, y, (float)targetPos.z - halfW).color(lineCol).normal(0.0F, 1.0F, 0.0F);
         buffer.vertex(mat, (float)targetPos.x + halfW + scanJitter, y, (float)targetPos.z - halfW).color(lineCol).normal(0.0F, 1.0F, 0.0F);

         buffer.vertex(mat, (float)targetPos.x - halfW, y, (float)targetPos.z + halfW + scanJitter).color(lineCol).normal(0.0F, 1.0F, 0.0F);
         buffer.vertex(mat, (float)targetPos.x + halfW, y, (float)targetPos.z + halfW + scanJitter).color(lineCol).normal(0.0F, 1.0F, 0.0F);
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreColorRenderState();

      if (ctx.esp.getGlow().isValue() && isGlitching) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         TargetEspRenderUtil.drawBloomBillboard(
            bloomBuffer,
            ctx.matrices,
            ctx.camera,
            targetPos.x + glitchOffset,
            targetPos.y + height * 0.5,
            targetPos.z,
            (halfW * 2.5F) * ctx.getEffectiveSize(),
            ColorAssist.setAlpha(color1, (int)(150.0F * ctx.anim))
         );

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }

   private void drawGlitchBoxCorners(
      BufferBuilder buffer,
      Matrix4f mat,
      double cx,
      double cy,
      double cz,
      float halfW,
      float height,
      int color
   ) {
      float cornerLen = halfW * 0.45F;
      float x1 = (float)(cx - halfW);
      float x2 = (float)(cx + halfW);
      float z1 = (float)(cz - halfW);
      float z2 = (float)(cz + halfW);
      float y1 = (float)cy;
      float y2 = (float)(cy + height);

      drawCorner(buffer, mat, x1, y1, z1, cornerLen, 1, 1, color);
      drawCorner(buffer, mat, x2, y1, z1, cornerLen, -1, 1, color);
      drawCorner(buffer, mat, x1, y1, z2, cornerLen, 1, -1, color);
      drawCorner(buffer, mat, x2, y1, z2, cornerLen, -1, -1, color);

      drawCorner(buffer, mat, x1, y2, z1, cornerLen, 1, 1, color);
      drawCorner(buffer, mat, x2, y2, z1, cornerLen, -1, 1, color);
      drawCorner(buffer, mat, x1, y2, z2, cornerLen, 1, -1, color);
      drawCorner(buffer, mat, x2, y2, z2, cornerLen, -1, -1, color);
   }

   private void drawCorner(
      BufferBuilder buffer,
      Matrix4f mat,
      float x,
      float y,
      float z,
      float len,
      int dirX,
      int dirZ,
      int color
   ) {
      buffer.vertex(mat, x, y, z).color(color).normal(0.0F, 1.0F, 0.0F);
      buffer.vertex(mat, x + len * dirX, y, z).color(color).normal(0.0F, 1.0F, 0.0F);

      buffer.vertex(mat, x, y, z).color(color).normal(0.0F, 1.0F, 0.0F);
      buffer.vertex(mat, x, y, z + len * dirZ).color(color).normal(0.0F, 1.0F, 0.0F);
   }
}
