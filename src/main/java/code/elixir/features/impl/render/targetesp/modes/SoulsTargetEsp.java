package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import java.util.LinkedList;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SoulsTargetEsp implements TargetEspMode {
   private final LinkedList<Vec3d>[] ghostTrails = new LinkedList[]{
      new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>()
   };

   @Override
   public String getName() {
      return "Души";
   }

   @Override
   public void reset() {
      for (LinkedList<Vec3d> trail : this.ghostTrails) {
         trail.clear();
      }
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         this.reset();
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      double targetX = targetPos.x;
      double targetY = targetPos.y;
      double targetZ = targetPos.z;

      TargetEspRenderUtil.setupBloomRenderState();

      int c1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int c2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      long timeMs = ctx.timeMs;
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      
      float p = MathHelper.clamp(ctx.anim, 0.0F, 1.0F);
      float ease = p * p * (3.0F - 2.0F * p);
      float scaleEase = (float)Math.sin(p * Math.PI * 0.5);

      float speedFactor = ctx.getEffectiveSpeed() * ctx.getRotationDirMultiplier();
      float sizeFactor = ctx.getEffectiveSize() * ctx.getPulseFactor();
      int maxTrailSize = MathHelper.clamp(ctx.esp.getTrailLength().getInt() * 2, 20, 80);
      float trailFadeFactor = ctx.esp.getTrailFade().getValue();

      for (int i = 0; i < 4; i++) {
         double speed = timeMs * 0.0022 * speedFactor;
         double offset = i * (Math.PI / 2.0);
         double baseRadius = (ctx.target.getWidth() * 0.55 + 0.16) * sizeFactor;
         double radius = baseRadius * (0.25 + 0.75 * scaleEase);
         double tx = Math.sin(speed + offset) * radius;
         double tz = Math.cos(speed + offset) * radius;
         double verticalDir = i % 2 == 0 ? 1.0 : -1.0;
         double yOscillation = Math.sin((speed + offset) * 0.9) * verticalDir;
         double ty = yOscillation * (ctx.target.getHeight() * 0.32) * scaleEase + ctx.target.getHeight() * (0.35 + 0.15 * scaleEase);
         
         Vec3d relativePos = new Vec3d(tx, ty, tz);
         this.ghostTrails[i].addFirst(relativePos);
         while (this.ghostTrails[i].size() > maxTrailSize) {
            this.ghostTrails[i].removeLast();
         }

         int segmentIdx = 0;
         int trailSize = this.ghostTrails[i].size();

         for (Vec3d off : this.ghostTrails[i]) {
            float fade = 1.0F - (float)segmentIdx / trailSize;
            float easedFade = (float)Math.pow(fade, 1.05 * trailFadeFactor);
            float renderX = (float)(targetX + off.x);
            float renderY = (float)(targetY + off.y);
            float renderZ = (float)(targetZ + off.z);
            
            int trailColor = ColorAssist.interpolateColor(c1, c2, fade);
            float baseScale = (segmentIdx == 0 ? 0.52F : 0.4F) * sizeFactor;
            float trailScale = baseScale * (0.25F + 0.75F * (float)Math.pow(fade, 0.85)) * scaleEase;
            int alpha = (int)(255.0 * easedFade * ease);

            TargetEspRenderUtil.drawBloomBillboard(
               buffer,
               ctx.matrices,
               ctx.camera,
               renderX,
               renderY,
               renderZ,
               trailScale,
               ColorAssist.setAlpha(trailColor, alpha)
            );

            if (segmentIdx == 0) {
               float coreScale = 0.26F * scaleEase * sizeFactor;
               TargetEspRenderUtil.drawBloomBillboard(
                  buffer,
                  ctx.matrices,
                  ctx.camera,
                  renderX,
                  renderY,
                  renderZ,
                  coreScale,
                  ColorAssist.setAlpha(-1, (int)(255.0F * ease))
               );
            }

            segmentIdx++;
         }
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreBloomRenderState();
   }
}
