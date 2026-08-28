package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class OrbitTargetEsp implements TargetEspMode {
   private final List<LinkedList<Vec3d>> orbitTrails = new ArrayList<>();

   public OrbitTargetEsp() {
      for (int i = 0; i < 8; i++) {
         this.orbitTrails.add(new LinkedList<>());
      }
   }

   @Override
   public String getName() {
      return "Orbit";
   }

   @Override
   public void reset() {
      for (LinkedList<Vec3d> trail : this.orbitTrails) {
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
      float height = ctx.target.getHeight();
      float width = ctx.target.getWidth();
      long time = ctx.timeMs;
      float speed = ctx.getEffectiveSpeed() * 0.0025F * ctx.getRotationDirMultiplier();
      float baseRadius = (width * 0.55F + 0.35F) * ctx.getEffectiveSize();
      float pulse = ctx.getPulseFactor();
      float radius = baseRadius * pulse * ctx.getEffectiveScale();

      int numSatellites = MathHelper.clamp(ctx.esp.getParticleAmount().getInt() / 4, 2, 6);
      int maxTrailLength = MathHelper.clamp(ctx.esp.getTrailLength().getInt(), 5, 45);
      float trailFadeFactor = ctx.esp.getTrailFade().getValue();

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupBloomRenderState();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int i = 0; i < numSatellites; i++) {
         LinkedList<Vec3d> trail = this.orbitTrails.get(i % this.orbitTrails.size());
         double phase = i * (Math.PI * 2 / numSatellites);
         double angle = time * speed + phase;
         
         double inclination = Math.toRadians(35.0 * ((i % 3) - 1));
         double cosInc = Math.cos(inclination);
         double sinInc = Math.sin(inclination);

         double x = Math.cos(angle) * radius;
         double z = Math.sin(angle) * radius * cosInc;
         double y = Math.sin(angle) * radius * sinInc + height * 0.5 + Math.sin(time * 0.002 + i) * 0.15 * height;

         Vec3d satelliteOffset = new Vec3d(x, y, z);
         trail.addFirst(satelliteOffset);
         while (trail.size() > maxTrailLength) {
            trail.removeLast();
         }

         int segmentIdx = 0;
         int trailSize = trail.size();
         for (Vec3d off : trail) {
            float fadeProgress = 1.0F - (float)segmentIdx / trailSize;
            float easedFade = (float)Math.pow(fadeProgress, trailFadeFactor);
            int segColor = ColorAssist.interpolateColor(color1, color2, (float)i / numSatellites);
            
            float orbSize = (segmentIdx == 0 ? 0.42F : 0.28F) * ctx.getEffectiveSize() * easedFade * ctx.anim;
            int alpha = (int)(255.0F * easedFade * ctx.anim);

            TargetEspRenderUtil.drawBloomBillboard(
               buffer,
               ctx.matrices,
               ctx.camera,
               targetPos.x + off.x,
               targetPos.y + off.y,
               targetPos.z + off.z,
               orbSize,
               ColorAssist.setAlpha(segColor, alpha)
            );

            if (segmentIdx == 0) {
               TargetEspRenderUtil.drawBloomBillboard(
                  buffer,
                  ctx.matrices,
                  ctx.camera,
                  targetPos.x + off.x,
                  targetPos.y + off.y,
                  targetPos.z + off.z,
                  orbSize * 0.45F,
                  ColorAssist.setAlpha(-1, (int)(240.0F * ctx.anim))
               );
            }
            segmentIdx++;
         }
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreBloomRenderState();
   }
}
