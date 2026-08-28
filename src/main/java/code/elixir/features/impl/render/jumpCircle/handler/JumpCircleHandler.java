package code.elixir.features.impl.render.jumpCircle.handler;

import code.elixir.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.elixir.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import code.elixir.main.client.Api;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public record JumpCircleHandler(JumpCircleModule module) implements Api {
   public static Identifier IDENTIFIER = Identifier.of("textures/circle.png");

   public void provideRender(MatrixStack matrixStack, float delta) {
      BufferBuilder bufferBuilder = this.createBuilder();
      this.setupGlConst();
      this.setupTexture();
      ShaderProgram program = this.setupShader();

      for (JumpCircleLayer layer : this.module.getStorage().getJumpCircleLayers()) {
         this.setupVertices(bufferBuilder, layer, matrixStack);
      }

      this.closeBuffer(bufferBuilder);
      this.returnGlConst();
   }

   private void setupVertices(BufferBuilder builder, JumpCircleLayer layer, MatrixStack matrixStack) {
      float animValue = layer.animation().getOutput().floatValue();
      float sinTime = (float)(System.nanoTime() / 1.0E9);
      int selectedColor = JumpCircleModule.colorMode.get().equals("Клиентский") ? ThemeManager.primaryColor.getColor() : JumpCircleModule.color.getColor();
      Camera camera = mc.getEntityRenderDispatcher().camera;
      if (camera != null) {
         int firstColor = ColorAssist.multAlpha(selectedColor, animValue);
         int secondColor = ColorAssist.multAlpha(ColorAssist.multSaturation(selectedColor, -0.3F), animValue);
         matrixStack.push();
         matrixStack.translate(layer.vec3d());
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(360.0F * sinTime));
         Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
         float size = this.module.getSize().getValue() / (this.module.getSize().getMax() - this.module.getSize().getMin()) * animValue;
         builder.vertex(matrix4f, -size, -size, 0.0F).texture(0.0F, 0.0F).color(firstColor);
         builder.vertex(matrix4f, -size, size, 0.0F).texture(0.0F, 1.0F).color(firstColor);
         builder.vertex(matrix4f, size, size, 0.0F).texture(1.0F, 1.0F).color(secondColor);
         builder.vertex(matrix4f, size, -size, 0.0F).texture(1.0F, 0.0F).color(secondColor);
         builder.vertex(matrix4f, -size, -size, 0.0F).texture(0.0F, 0.0F).color(firstColor);
         builder.vertex(matrix4f, -size, size, 0.0F).texture(0.0F, 1.0F).color(firstColor);
         builder.vertex(matrix4f, size, size, 0.0F).texture(1.0F, 1.0F).color(secondColor);
         builder.vertex(matrix4f, size, -size, 0.0F).texture(1.0F, 0.0F).color(secondColor);
         matrixStack.pop();
      }
   }

   private BufferBuilder createBuilder() {
      return Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   private void closeBuffer(BufferBuilder bufferBuilder) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null && builtBuffer.getDrawParameters().vertexCount() > 0) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   private void setupGlConst() {
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
   }

   private void returnGlConst() {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
   }

   private ShaderProgram setupShader() {
      return RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
   }

   private void setupTexture() {
      AbstractTexture texture = mc.getTextureManager().getTexture(IDENTIFIER);
      texture.setFilter(false, false);
      RenderSystem.setShaderTexture(0, texture.getGlId());
   }
}
