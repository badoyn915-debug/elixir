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

public class WaveTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Wave";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float height = ctx.target.getHeight();
      float speed = ctx.getEffectiveSpeed() * ctx.esp.getWaveSpeed().getValue();
      float maxRadius = (ctx.target.getWidth() + 1.2F) * ctx.getEffectiveSize() * ctx.getEffectiveScale();
      float waveStrength = ctx.esp.getWaveStrength().getValue();
      long time = ctx.timeMs;

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      int waveCount = 4;
      int segments = 48;

      for (int w = 0; w < waveCount; w++) {
         double waveTime = (time * 0.0012 * speed + (double)w / waveCount) % 1.0;
         float waveProgress = (float)waveTime;
         float currentRadius = waveProgress * maxRadius;
         float ringThickness = 0.22F * (1.0F - waveProgress * 0.5F) * ctx.getEffectiveSize();
         
         float waveAlphaFactor = (1.0F - waveProgress) * (1.0F - (float)Math.pow(waveProgress, 2.0));
         int alpha = (int)(200.0F * waveAlphaFactor * ctx.anim);
         if (alpha <= 2) continue;

         int waveColor = ColorAssist.interpolateColor(color1, color2, waveProgress);
         int innerCol = ColorAssist.setAlpha(waveColor, alpha);
         int outerCol = ColorAssist.setAlpha(waveColor, (int)(alpha * 0.15F));

         float baseY = (float)targetPos.y + 0.04F;

         for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * (Math.PI * 2) / segments);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);

            float waveYOffset = MathHelper.sin(angle * 3.0F + (float)time * 0.005F * speed) * 0.08F * waveStrength * (1.0F - waveProgress);

            float rIn = Math.max(0.02F, currentRadius - ringThickness);
            float rOut = currentRadius + ringThickness;

            float xIn = (float)targetPos.x + cos * rIn;
            float zIn = (float)targetPos.z + sin * rIn;
            float xOut = (float)targetPos.x + cos * rOut;
            float zOut = (float)targetPos.z + sin * rOut;
            float y = baseY + waveYOffset;

            buffer.vertex(mat, xIn, y, zIn).color(innerCol);
            buffer.vertex(mat, xOut, y, zOut).color(outerCol);
         }
      }

      double vertCycle = (time * 0.0009 * speed) % 1.0;
      float vertY = (float)(targetPos.y + vertCycle * height);
      float vertFade = (float)Math.sin(vertCycle * Math.PI);
      float vertRadius = (ctx.target.getWidth() * 0.55F + 0.15F) * ctx.getEffectiveSize();
      int vertAlpha = (int)(180.0F * vertFade * ctx.anim);
      int vertCol = ColorAssist.setAlpha(color2, vertAlpha);

      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i * (Math.PI * 2) / segments);
         float cos = MathHelper.cos(angle);
         float sin = MathHelper.sin(angle);

         float x = (float)targetPos.x + cos * vertRadius;
         float z = (float)targetPos.z + sin * vertRadius;

         buffer.vertex(mat, x, vertY - 0.06F, z).color(ColorAssist.setAlpha(vertCol, 0));
         buffer.vertex(mat, x, vertY + 0.06F, z).color(vertCol);
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreColorRenderState();
   }
}
