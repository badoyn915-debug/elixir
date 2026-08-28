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

public class RadarTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Radar";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float speed = ctx.getEffectiveSpeed() * ctx.esp.getRotationSpeed().getValue() * ctx.getRotationDirMultiplier();
      float radius = (ctx.target.getWidth() * 0.75F + 0.65F) * ctx.getEffectiveSize() * ctx.getEffectiveScale();
      long time = ctx.timeMs;

      double scanAngle = (time * 0.0035 * speed) % (Math.PI * 2);

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      float baseY = (float)targetPos.y + 0.08F;
      float centerX = (float)targetPos.x;
      float centerZ = (float)targetPos.z;

      int fanSegments = 24;
      double fanAngleSpan = Math.toRadians(70.0);

      for (int i = 0; i < fanSegments; i++) {
         double segProgress1 = (double)i / fanSegments;
         double segProgress2 = (double)(i + 1) / fanSegments;

         double a1 = scanAngle - segProgress1 * fanAngleSpan;
         double a2 = scanAngle - segProgress2 * fanAngleSpan;

         float alpha1 = (float)Math.pow(1.0 - segProgress1, 1.8) * 180.0F * ctx.anim;
         float alpha2 = (float)Math.pow(1.0 - segProgress2, 1.8) * 180.0F * ctx.anim;

         int c1 = ColorAssist.setAlpha(color1, (int)alpha1);
         int c2 = ColorAssist.setAlpha(color1, (int)alpha2);

         float x1 = centerX + (float)Math.cos(a1) * radius;
         float z1 = centerZ + (float)Math.sin(a1) * radius;
         float x2 = centerX + (float)Math.cos(a2) * radius;
         float z2 = centerZ + (float)Math.sin(a2) * radius;

         buffer.vertex(mat, centerX, baseY, centerZ).color(ColorAssist.setAlpha(color2, (int)((alpha1 + alpha2) * 0.4F)));
         buffer.vertex(mat, x1, baseY, z1).color(c1);
         buffer.vertex(mat, x2, baseY, z2).color(c2);
      }

      BufferDraw.draw(buffer);

      BufferBuilder ringBuffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      int ringSegments = 48;
      int ringColor = ColorAssist.setAlpha(color2, (int)(140.0F * ctx.anim));
      float innerR = radius - 0.05F;
      float outerR = radius + 0.05F;

      for (int i = 0; i <= ringSegments; i++) {
         float angle = (float)(i * (Math.PI * 2) / ringSegments);
         float cos = MathHelper.cos(angle);
         float sin = MathHelper.sin(angle);

         ringBuffer.vertex(mat, centerX + cos * innerR, baseY, centerZ + sin * innerR).color(ringColor);
         ringBuffer.vertex(mat, centerX + cos * outerR, baseY, centerZ + sin * outerR).color(ColorAssist.setAlpha(ringColor, 0));
      }

      for (int t = 0; t < 4; t++) {
         float tickAngle = (float)(t * Math.PI / 2.0);
         float cos = MathHelper.cos(tickAngle);
         float sin = MathHelper.sin(tickAngle);
         float r1 = radius * 0.4F;
         float r2 = radius * 1.05F;

         ringBuffer.vertex(mat, centerX + cos * r1, baseY, centerZ + sin * r1).color(ColorAssist.setAlpha(color1, (int)(120.0F * ctx.anim)));
         ringBuffer.vertex(mat, centerX + cos * r2, baseY, centerZ + sin * r2).color(ColorAssist.setAlpha(color1, 0));
      }

      BufferDraw.draw(ringBuffer);
      TargetEspRenderUtil.restoreColorRenderState();

      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float headX = centerX + (float)Math.cos(scanAngle) * radius;
         float headZ = centerZ + (float)Math.sin(scanAngle) * radius;

         TargetEspRenderUtil.drawBloomBillboard(
            bloomBuffer,
            ctx.matrices,
            ctx.camera,
            headX,
            baseY,
            headZ,
            0.65F * ctx.getEffectiveSize(),
            ColorAssist.setAlpha(color1, (int)(220.0F * ctx.anim))
         );

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
