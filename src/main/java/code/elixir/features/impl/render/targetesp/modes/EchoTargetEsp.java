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
import org.joml.Matrix4f;

public class EchoTargetEsp implements TargetEspMode {
   private static class EchoSnapshot {
      final Vec3d pos;
      final long timestamp;

      EchoSnapshot(Vec3d pos, long timestamp) {
         this.pos = pos;
         this.timestamp = timestamp;
      }
   }

   private final LinkedList<EchoSnapshot> echoHistory = new LinkedList<>();
   private long lastRecordTime = 0L;

   @Override
   public String getName() {
      return "Echo";
   }

   @Override
   public void reset() {
      this.echoHistory.clear();
      this.lastRecordTime = 0L;
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         this.reset();
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      long time = ctx.timeMs;
      float height = ctx.target.getHeight();
      float width = ctx.target.getWidth();
      float radius = (width * 0.55F + 0.15F) * ctx.getEffectiveSize();

      if (time - this.lastRecordTime > 60L) {
         this.echoHistory.addFirst(new EchoSnapshot(targetPos, time));
         this.lastRecordTime = time;
         while (this.echoHistory.size() > 18) {
            this.echoHistory.removeLast();
         }
      }

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      int echoIdx = 0;
      long lifetime = (long)(800L / ctx.getEffectiveSpeed() * ctx.esp.getTrailFade().getValue());

      for (EchoSnapshot echo : this.echoHistory) {
         long age = time - echo.timestamp;
         if (age > lifetime) continue;

         float ageFactor = 1.0F - (float)age / lifetime;
         float alphaFactor = (float)Math.pow(ageFactor, 1.4);
         int echoColor = ColorAssist.interpolateColor(color1, color2, (float)echoIdx / 18.0F);
         int col = ColorAssist.setAlpha(echoColor, (int)(170.0F * alphaFactor * ctx.anim));

         int segments = 24;
         for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * (Math.PI * 2) / segments);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);

            float x = (float)echo.pos.x + cos * radius * ageFactor;
            float z = (float)echo.pos.z + sin * radius * ageFactor;
            float y1 = (float)echo.pos.y + 0.05F;
            float y2 = y1 + height * 0.4F * ageFactor;

            buffer.vertex(mat, x, y1, z).color(col);
            buffer.vertex(mat, x, y2, z).color(ColorAssist.setAlpha(col, 0));
         }

         echoIdx++;
      }

      BufferDraw.draw(buffer);
      TargetEspRenderUtil.restoreColorRenderState();

      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (EchoSnapshot echo : this.echoHistory) {
            long age = time - echo.timestamp;
            if (age > lifetime) continue;
            float ageFactor = 1.0F - (float)age / lifetime;
            int glowCol = ColorAssist.setAlpha(color1, (int)(110.0F * ageFactor * ctx.anim));
            float echoGlowSize = (width + 0.6F) * ageFactor * ctx.getEffectiveSize();

            TargetEspRenderUtil.drawBloomBillboard(
               bloomBuffer,
               ctx.matrices,
               ctx.camera,
               echo.pos.x,
               echo.pos.y + height * 0.4,
               echo.pos.z,
               echoGlowSize,
               glowCol
            );
         }

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
