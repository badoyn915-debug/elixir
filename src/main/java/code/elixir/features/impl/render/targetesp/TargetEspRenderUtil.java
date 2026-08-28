package code.elixir.features.impl.render.targetesp;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class TargetEspRenderUtil {
   public static final Identifier BLOOM_TEXTURE = Identifier.of("textures/bloom.png");

   public static void setupBloomRenderState() {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void restoreBloomRenderState() {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   public static void setupColorRenderState(boolean additive) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableBlend();
      if (additive) {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      } else {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      }
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
   }

   public static void restoreColorRenderState() {
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   public static void drawBloomBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, double x, double y, double z, float scale, int color) {
      float half = scale * 0.5F;
      matrices.push();
      matrices.translate(x, y, z);
      matrices.multiply(camera.getRotation());
      Matrix4f mat = matrices.peek().getPositionMatrix();
      buffer.vertex(mat, -half, -half, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(mat, half, -half, 0.0F).texture(1.0F, 0.0F).color(color);
      buffer.vertex(mat, half, half, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(mat, -half, half, 0.0F).texture(0.0F, 1.0F).color(color);
      matrices.pop();
   }

   public static void drawFlatRing(BufferBuilder buffer, MatrixStack matrices, Vec3d center, float innerRadius, float outerRadius, int innerColor, int outerColor, int segments) {
      Matrix4f mat = matrices.peek().getPositionMatrix();
      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i * (Math.PI * 2) / segments);
         float cos = MathHelper.cos(angle);
         float sin = MathHelper.sin(angle);

         float xIn = (float)center.x + cos * innerRadius;
         float zIn = (float)center.z + sin * innerRadius;
         float xOut = (float)center.x + cos * outerRadius;
         float zOut = (float)center.z + sin * outerRadius;
         float y = (float)center.y;

         buffer.vertex(mat, xIn, y, zIn).color(innerColor);
         buffer.vertex(mat, xOut, y, zOut).color(outerColor);
      }
   }

   public static void drawCylinderStrip(BufferBuilder buffer, MatrixStack matrices, Vec3d center, float radius, float height, int bottomColor, int topColor, int segments) {
      Matrix4f mat = matrices.peek().getPositionMatrix();
      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i * (Math.PI * 2) / segments);
         float cos = MathHelper.cos(angle);
         float sin = MathHelper.sin(angle);

         float x = (float)center.x + cos * radius;
         float z = (float)center.z + sin * radius;
         float yBottom = (float)center.y;
         float yTop = (float)center.y + height;

         buffer.vertex(mat, x, yBottom, z).color(bottomColor);
         buffer.vertex(mat, x, yTop, z).color(topColor);
      }
   }

   public static int getBlendedColor(TargetEspContext ctx, float factor) {
      int primary = ctx.primaryColor;
      int secondary = ctx.secondaryColor;
      int blended = ColorAssist.interpolateColor(primary, secondary, MathHelper.clamp(factor, 0.0F, 1.0F));
      if (ctx.esp.redOnHit.isValue() && ctx.red > 0.0F) {
         blended = ColorAssist.interpolateColor(blended, -49088, ctx.red);
      }
      return blended;
   }
}
