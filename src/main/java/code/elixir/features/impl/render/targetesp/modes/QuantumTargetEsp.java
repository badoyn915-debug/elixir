package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.geometry.Render3D;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class QuantumTargetEsp implements TargetEspMode {
   @Override
   public String getName() {
      return "Квантум";
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      Vec3d targetPos = ctx.targetPos;
      float height = ctx.target.getHeight();
      float width = ctx.target.getWidth();
      float radius = (width * 0.70F + 0.25F) * ctx.getEffectiveSize();
      float speed = ctx.getEffectiveSpeed() * ctx.esp.getRotationSpeed().getValue() * ctx.getRotationDirMultiplier();
      long time = ctx.timeMs;
      double t = (double)time * 0.0025 * (double)speed;

      int color1 = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color2 = TargetEspRenderUtil.getBlendedColor(ctx, 1.0F);

      TargetEspRenderUtil.setupColorRenderState(true);
      Matrix4f mat = ctx.matrices.peek().getPositionMatrix();

      // 1. Dual Quantum Helix Strands
      int steps = 32;
      float stepY = height / (float)steps;
      float ribbonW = 0.032F * ctx.getEffectiveSize();

      for (int strand = 0; strand < 2; strand++) {
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         double strandPhase = strand * Math.PI;
         int strandCol = strand == 0 ? color1 : color2;

         for (int i = 0; i <= steps; i++) {
            float progress = (float)i / steps;
            float y = (float)targetPos.y + progress * height;
            double angle = t * 2.8 + progress * Math.PI * 3.2 + strandPhase;

            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);

            float r = radius * (0.9F + 0.1F * MathHelper.sin(progress * (float)Math.PI));
            float x = (float)targetPos.x + cos * r;
            float z = (float)targetPos.z + sin * r;

            float fade = (float)Math.sin(progress * Math.PI);
            int alpha = (int)(240.0F * fade * ctx.anim);
            int col = ColorAssist.setAlpha(strandCol, alpha);

            buffer.vertex(mat, x, y - ribbonW, z).color(col);
            buffer.vertex(mat, x, y + ribbonW, z).color(col);
         }

         BufferDraw.draw(buffer);
      }

      // 2. Cybernetic Rotating Base Rune Rings
      BufferBuilder ringBuffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      int ringSegments = 24;
      float ringY = (float)targetPos.y + 0.04F;
      float pulse = 1.0F + 0.08F * (float)Math.sin(t * 3.5);
      float innerR = radius * 0.9F * pulse;
      float outerR = radius * 1.3F * pulse;

      int ringAlpha = (int)(220.0F * ctx.anim);
      int rCol1 = ColorAssist.setAlpha(color1, ringAlpha);
      int rCol2 = ColorAssist.setAlpha(color2, ringAlpha);

      for (int i = 0; i < ringSegments; i++) {
         double a1 = ((double)i / (double)ringSegments) * Math.PI * 2.0 + t * 1.5;
         double a2 = ((double)(i + 1) / (double)ringSegments) * Math.PI * 2.0 + t * 1.5;

         float ix1 = (float)targetPos.x + (float)Math.cos(a1) * innerR;
         float iz1 = (float)targetPos.z + (float)Math.sin(a1) * innerR;
         float ix2 = (float)targetPos.x + (float)Math.cos(a2) * innerR;
         float iz2 = (float)targetPos.z + (float)Math.sin(a2) * innerR;

         ringBuffer.vertex(mat, ix1, ringY, iz1).color(rCol1);
         ringBuffer.vertex(mat, ix2, ringY, iz2).color(rCol1);

         double oa1 = ((double)i / (double)ringSegments) * Math.PI * 2.0 - t * 1.2;
         double oa2 = ((double)(i + 1) / (double)ringSegments) * Math.PI * 2.0 - t * 1.2;

         float ox1 = (float)targetPos.x + (float)Math.cos(oa1) * outerR;
         float oz1 = (float)targetPos.z + (float)Math.sin(oa1) * outerR;
         float ox2 = (float)targetPos.x + (float)Math.cos(oa2) * outerR;
         float oz2 = (float)targetPos.z + (float)Math.sin(oa2) * outerR;

         ringBuffer.vertex(mat, ox1, ringY, oz1).color(rCol2);
         ringBuffer.vertex(mat, ox2, ringY, oz2).color(rCol2);

         if (i % 3 == 0) {
            ringBuffer.vertex(mat, ix1, ringY, iz1).color(rCol1);
            ringBuffer.vertex(mat, ox1, ringY, oz1).color(rCol2);
         }
      }

      // 3. Orbiting Quantum Shards
      int shardCount = 3;
      float orbitR = radius * 1.4F;

      for (int s = 0; s < shardCount; s++) {
         double sAngle = t * 2.6 + ((double)s / shardCount) * Math.PI * 2.0;
         float sY = (float)targetPos.y + height * 0.45F + (float)Math.sin(t * 3.0 + s * 2.0) * (height * 0.25F);

         float sx = (float)targetPos.x + (float)Math.cos(sAngle) * orbitR;
         float sz = (float)targetPos.z + (float)Math.sin(sAngle) * orbitR;

         float size = 0.08F * ctx.getEffectiveSize();
         int shardCol = (s % 2 == 0) ? rCol1 : rCol2;

         ringBuffer.vertex(mat, sx - size, sY, sz).color(shardCol);
         ringBuffer.vertex(mat, sx + size, sY, sz).color(shardCol);

         ringBuffer.vertex(mat, sx, sY - size * 1.5F, sz).color(shardCol);
         ringBuffer.vertex(mat, sx, sY + size * 1.5F, sz).color(shardCol);

         ringBuffer.vertex(mat, sx, sY, sz - size).color(shardCol);
         ringBuffer.vertex(mat, sx, sY, sz + size).color(shardCol);
      }

      // 4. Laser Holographic Corner Brackets around Bounding Box
      Box box = ctx.target.getBoundingBox().offset(-ctx.target.getX() + targetPos.x, -ctx.target.getY() + targetPos.y, -ctx.target.getZ() + targetPos.z);
      float minX = (float)box.minX;
      float minY = (float)box.minY;
      float minZ = (float)box.minZ;
      float maxX = (float)box.maxX;
      float maxY = (float)box.maxY;
      float maxZ = (float)box.maxZ;

      float lenX = (maxX - minX) * 0.28F;
      float lenY = (maxY - minY) * 0.20F;
      float lenZ = (maxZ - minZ) * 0.28F;

      // Bottom-Front-Left
      ringBuffer.vertex(mat, minX, minY, minZ).color(rCol1);
      ringBuffer.vertex(mat, minX + lenX, minY, minZ).color(rCol1);
      ringBuffer.vertex(mat, minX, minY, minZ).color(rCol1);
      ringBuffer.vertex(mat, minX, minY + lenY, minZ).color(rCol1);
      ringBuffer.vertex(mat, minX, minY, minZ).color(rCol1);
      ringBuffer.vertex(mat, minX, minY, minZ + lenZ).color(rCol1);

      // Bottom-Back-Right
      ringBuffer.vertex(mat, maxX, minY, maxZ).color(rCol1);
      ringBuffer.vertex(mat, maxX - lenX, minY, maxZ).color(rCol1);
      ringBuffer.vertex(mat, maxX, minY, maxZ).color(rCol1);
      ringBuffer.vertex(mat, maxX, minY + lenY, maxZ).color(rCol1);
      ringBuffer.vertex(mat, maxX, minY, maxZ).color(rCol1);
      ringBuffer.vertex(mat, maxX, minY, maxZ - lenZ).color(rCol1);

      // Top-Front-Right
      ringBuffer.vertex(mat, maxX, maxY, minZ).color(rCol2);
      ringBuffer.vertex(mat, maxX - lenX, maxY, minZ).color(rCol2);
      ringBuffer.vertex(mat, maxX, maxY, minZ).color(rCol2);
      ringBuffer.vertex(mat, maxX, maxY - lenY, minZ).color(rCol2);
      ringBuffer.vertex(mat, maxX, maxY, minZ).color(rCol2);
      ringBuffer.vertex(mat, maxX, maxY, minZ + lenZ).color(rCol2);

      // Top-Back-Left
      ringBuffer.vertex(mat, minX, maxY, maxZ).color(rCol2);
      ringBuffer.vertex(mat, minX + lenX, maxY, maxZ).color(rCol2);
      ringBuffer.vertex(mat, minX, maxY, maxZ).color(rCol2);
      ringBuffer.vertex(mat, minX, maxY - lenY, maxZ).color(rCol2);
      ringBuffer.vertex(mat, minX, maxY, maxZ).color(rCol2);
      ringBuffer.vertex(mat, minX, maxY, maxZ - lenZ).color(rCol2);

      BufferDraw.draw(ringBuffer);
      TargetEspRenderUtil.restoreColorRenderState();

      // Bloom billboard effects
      if (ctx.esp.getGlow().isValue()) {
         TargetEspRenderUtil.setupBloomRenderState();
         BufferBuilder bloomBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (int s = 0; s < shardCount; s++) {
            double sAngle = t * 2.6 + ((double)s / shardCount) * Math.PI * 2.0;
            float sY = (float)targetPos.y + height * 0.45F + (float)Math.sin(t * 3.0 + s * 2.0) * (height * 0.25F);
            float sx = (float)targetPos.x + (float)Math.cos(sAngle) * orbitR;
            float sz = (float)targetPos.z + (float)Math.sin(sAngle) * orbitR;

            float glowSize = 0.55F * ctx.getEffectiveSize();
            int shardCol = (s % 2 == 0) ? color1 : color2;

            TargetEspRenderUtil.drawBloomBillboard(
               bloomBuffer,
               ctx.matrices,
               ctx.camera,
               sx,
               sY,
               sz,
               glowSize,
               ColorAssist.setAlpha(shardCol, (int)(190.0F * ctx.anim))
            );
         }

         BufferDraw.draw(bloomBuffer);
         TargetEspRenderUtil.restoreBloomRenderState();
      }
   }
}
