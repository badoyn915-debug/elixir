package code.elixir.features.impl.render.zenisesp;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class StickRenderer {
   private static final int RADIAL_SEGMENTS = 14;

   public static void renderStick(
      MatrixStack matrices,
      List<StickSegment> nodes,
      float partialTicks,
      float thickness,
      int shaftColor,
      int tipColor
   ) {
      if (nodes == null || nodes.size() < 2) return;

      List<Vec3d> points = new ArrayList<>(nodes.size());
      for (StickSegment node : nodes) {
         points.add(node.getInterpolated(partialTicks));
      }

      setup3DRenderState();
      Matrix4f mat = matrices.peek().getPositionMatrix();

      // 1. Render Base Spheres (Testicles) at Root
      Vec3d root = points.get(0);
      Vec3d firstDir = points.get(1).subtract(root).normalize();
      Vec3d up = new Vec3d(0, 1, 0);
      Vec3d right = firstDir.crossProduct(up).normalize();
      if (right.lengthSquared() < 0.001) {
         right = new Vec3d(1, 0, 0);
      }

      float ballRadius = thickness * 1.08F;
      Vec3d leftBallPos = root.add(right.multiply(-ballRadius * 0.95)).add(new Vec3d(0, -ballRadius * 0.35, 0));
      Vec3d rightBallPos = root.add(right.multiply(ballRadius * 0.95)).add(new Vec3d(0, -ballRadius * 0.35, 0));

      drawSphere(mat, leftBallPos, ballRadius, shaftColor);
      drawSphere(mat, rightBallPos, ballRadius, shaftColor);

      // 2. Render Deformed Extruded Cylinder Shaft
      BufferBuilder shaftBuffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < points.size() - 1; i++) {
         Vec3d p1 = points.get(i);
         Vec3d p2 = points.get(i + 1);
         float progress1 = (float)i / (points.size() - 1);
         float progress2 = (float)(i + 1) / (points.size() - 1);

         int c1 = ColorAssist.interpolateColor(shaftColor, tipColor, progress1);
         int c2 = ColorAssist.interpolateColor(shaftColor, tipColor, progress2);

         Vec3d dir1 = (i < points.size() - 2) ? points.get(i + 1).subtract(p1).normalize() : p2.subtract(p1).normalize();
         Vec3d normalA = getOrthogonal(dir1);
         Vec3d normalB = dir1.crossProduct(normalA).normalize();

         float r1 = thickness * (1.0F - 0.15F * progress1);
         float r2 = thickness * (1.0F - 0.15F * progress2);

         for (int j = 0; j <= RADIAL_SEGMENTS; j++) {
            double angle = j * (Math.PI * 2 / RADIAL_SEGMENTS);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            Vec3d offset1 = normalA.multiply(cos * r1).add(normalB.multiply(sin * r1));
            Vec3d offset2 = normalA.multiply(cos * r2).add(normalB.multiply(sin * r2));

            // Lighting shading factor based on normal
            float shade1 = (float)(0.75 + 0.25 * offset1.normalize().y);
            float shade2 = (float)(0.75 + 0.25 * offset2.normalize().y);

            int col1 = applyShade(c1, shade1);
            int col2 = applyShade(c2, shade2);

            shaftBuffer.vertex(mat, (float)(p1.x + offset1.x), (float)(p1.y + offset1.y), (float)(p1.z + offset1.z)).color(col1);
            shaftBuffer.vertex(mat, (float)(p2.x + offset2.x), (float)(p2.y + offset2.y), (float)(p2.z + offset2.z)).color(col2);
         }
      }

      BufferDraw.draw(shaftBuffer);

      // 3. Render Rounded Dome Tip (Glans)
      Vec3d tipPos = points.get(points.size() - 1);
      drawSphere(mat, tipPos, thickness * 1.15F, tipColor);

      restore3DRenderState();
   }

   private static void drawSphere(Matrix4f mat, Vec3d center, float radius, int color) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      int rings = 8;
      int sectors = 14;

      for (int i = 0; i < rings; i++) {
         float lat0 = (float)Math.PI * (-0.5F + (float)i / rings);
         float z0 = MathHelper.sin(lat0);
         float zr0 = MathHelper.cos(lat0);

         float lat1 = (float)Math.PI * (-0.5F + (float)(i + 1) / rings);
         float z1 = MathHelper.sin(lat1);
         float zr1 = MathHelper.cos(lat1);

         for (int j = 0; j <= sectors; j++) {
            float lng = (float)(2 * Math.PI * (float)j / sectors);
            float x = MathHelper.cos(lng);
            float y = MathHelper.sin(lng);

            float shade0 = (float)(0.75 + 0.25 * z0);
            float shade1 = (float)(0.75 + 0.25 * z1);

            int c0 = applyShade(color, shade0);
            int c1 = applyShade(color, shade1);

            buffer.vertex(mat, (float)(center.x + x * zr0 * radius), (float)(center.y + y * zr0 * radius), (float)(center.z + z0 * radius)).color(c0);
            buffer.vertex(mat, (float)(center.x + x * zr1 * radius), (float)(center.y + y * zr1 * radius), (float)(center.z + z1 * radius)).color(c1);
         }
      }

      BufferDraw.draw(buffer);
   }

   private static Vec3d getOrthogonal(Vec3d v) {
      if (Math.abs(v.x) < 0.9 && Math.abs(v.z) < 0.9) {
         return v.crossProduct(new Vec3d(1, 0, 0)).normalize();
      }
      return v.crossProduct(new Vec3d(0, 1, 0)).normalize();
   }

   private static int applyShade(int color, float shade) {
      int a = color >> 24 & 0xFF;
      int r = MathHelper.clamp((int)((color >> 16 & 0xFF) * shade), 0, 255);
      int g = MathHelper.clamp((int)((color >> 8 & 0xFF) * shade), 0, 255);
      int b = MathHelper.clamp((int)((color & 0xFF) * shade), 0, 255);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static void setup3DRenderState() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void restore3DRenderState() {
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
