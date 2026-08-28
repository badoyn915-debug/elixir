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

public class PulseTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Pulse";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float baseRadius = (ctx.target.getWidth() * 0.65F + 0.2F) * ctx.getEffectiveSize();
      float height = ctx.target.getHeight();
      float speed = ctx.getEffectiveSpeed();
      long time = ctx.timeMs;

      float pulse = ctx.getPulseFactor();
      float radius = baseRadius * pulse * ctx.getEffectiveScale();

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      float alphaEase = ctx.anim * ctx.anim;
      int baseAlpha = (int)(180.0F * alphaEase);
      int fadeAlpha = (int)(25.0F * alphaEase);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      Vec3d baseCenter = new Vec3d(targetPos.x, targetPos.y + 0.05, targetPos.z);
      TargetEspRenderUtil.drawFlatRing(
         buffer,
         ctx.matrices,
         baseCenter,
         Math.max(0.05F, radius - 0.22F * ctx.getEffectiveSize()),
         radius + 0.15F * ctx.getEffectiveSize(),
         ColorAssist.setAlpha(color1, baseAlpha),
         ColorAssist.setAlpha(color2, fadeAlpha),
         48
      );

      double midPhase = Math.sin(time * 0.003 * speed) * 0.5 + 0.5;
      float midY = (float)(targetPos.y + 0.2 + midPhase * (height * 0.6));
      float midRadius = radius * (0.85F + 0.15F * (float)Math.cos(time * 0.004 * speed));
      Vec3d midCenter = new Vec3d(targetPos.x, midY, targetPos.z);
      TargetEspRenderUtil.drawFlatRing(
         buffer,
         ctx.matrices,
         midCenter,
         Math.max(0.05F, midRadius - 0.18F * ctx.getEffectiveSize()),
         midRadius + 0.1F * ctx.getEffectiveSize(),
         ColorAssist.setAlpha(color2, (int)(baseAlpha * 0.8F)),
         ColorAssist.setAlpha(color1, fadeAlpha),
         48
      );

      TargetEspRenderUtil.drawCylinderStrip(
         buffer,
         ctx.matrices,
         baseCenter,
         radius * 0.95F,
         Math.max(0.1F, midY - (float)targetPos.y),
         ColorAssist.setAlpha(color1, (int)(baseAlpha * 0.45F)),
         ColorAssist.setAlpha(color2, (int)(fadeAlpha * 0.2F)),
         40
      );

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreColorRenderState();

      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float glowRadius = radius * 2.2F * ctx.esp.getGlowSize().getValue();
         int glowColor = ColorAssist.setAlpha(color1, (int)(110.0F * alphaEase));

         TargetEspRenderUtil.drawBloomBillboard(
            bloomBuffer,
            ctx.matrices,
            ctx.camera,
            targetPos.x,
            targetPos.y + height * 0.5,
            targetPos.z,
            glowRadius,
            glowColor
         );

         TargetEspRenderUtil.drawBloomBillboard(
            bloomBuffer,
            ctx.matrices,
            ctx.camera,
            targetPos.x,
            targetPos.y + 0.05,
            targetPos.z,
            glowRadius * 0.75F,
            ColorAssist.setAlpha(color2, (int)(95.0F * alphaEase))
         );

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
