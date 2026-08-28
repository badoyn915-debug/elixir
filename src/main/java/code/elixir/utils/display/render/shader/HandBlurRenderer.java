package code.elixir.utils.display.render.shader;

import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;

public class HandBlurRenderer {
   private static final ShaderProgramKey HAND_BLUR_KEY = new ShaderProgramKey(
      Identifier.of("elixir", "core/hand_blur"), VertexFormats.BLIT_SCREEN, Defines.EMPTY
   );
   private final Framebuffer sourceFbo;
   private final SimpleFramebuffer outputFbo;
   private final int depthAttachment;
   private final float blurRadius;
   private final float threshold;
   private final float[] tintColor;
   private final float[] tintColor2;
   private final int mode;
   private final float time;
   private final float shaderScale;

   public HandBlurRenderer(
      Framebuffer sourceFbo,
      SimpleFramebuffer outputFbo,
      int depthAttachment,
      float blurRadius,
      float threshold,
      float[] tintColor,
      float[] tintColor2,
      int mode,
      float time,
      float shaderScale
   ) {
      this.sourceFbo = sourceFbo;
      this.outputFbo = outputFbo;
      this.depthAttachment = depthAttachment;
      this.blurRadius = blurRadius;
      this.threshold = threshold;
      this.tintColor = tintColor;
      this.tintColor2 = tintColor2;
      this.mode = mode;
      this.time = time;
      this.shaderScale = shaderScale;
   }

   public void renderPostProcess() {
      MinecraftClient mc = MinecraftClient.getInstance();
      Framebuffer mainFbo = mc.getFramebuffer();
      if (mainFbo != null && mainFbo.textureWidth > 0 && mainFbo.textureHeight > 0) {
         if (this.sourceFbo.getColorAttachment() > 0 && this.outputFbo.getColorAttachment() > 0 && this.depthAttachment > 0) {
            this.renderStandardPass();
            mainFbo.beginWrite(false);
            RenderSystem.viewport(0, 0, mainFbo.textureWidth, mainFbo.textureHeight);
            this.outputFbo.drawInternal(mainFbo.textureWidth, mainFbo.textureHeight);
         }
      }
   }

   private void renderStandardPass() {
      int sourceTex = this.sourceFbo.getColorAttachment();
      int depthTex = this.depthAttachment;
      this.outputFbo.beginWrite(false);
      RenderSystem.viewport(0, 0, this.outputFbo.textureWidth, this.outputFbo.textureHeight);
      RenderSystem.disableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, sourceTex);
      RenderSystem.setShaderTexture(1, depthTex);
      ShaderProgram shader = RenderSystem.setShader(HAND_BLUR_KEY);
      if (shader != null) {
         shader.addSamplerTexture("Sampler0", sourceTex);
         shader.addSamplerTexture("DepthSampler", depthTex);
         this.applyBlurUniforms(shader);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.BLIT_SCREEN);
         builder.vertex(0.0F, 0.0F, 0.0F);
         builder.vertex(1.0F, 0.0F, 0.0F);
         builder.vertex(1.0F, 1.0F, 0.0F);
         builder.vertex(0.0F, 1.0F, 0.0F);
         BufferDraw.draw(builder);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.setShaderTexture(1, 0);
      this.outputFbo.endWrite();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void applyBlurUniforms(ShaderProgram shader) {
      GlUniform blurU = shader.getUniform("BlurRadius");
      GlUniform thrU = shader.getUniform("Threshold");
      GlUniform tintU = shader.getUniform("TintColor");
      GlUniform tint2U = shader.getUniform("TintColor2");
      GlUniform modeU = shader.getUniform("Mode");
      GlUniform timeU = shader.getUniform("Time");
      GlUniform scaleU = shader.getUniform("ShaderScale");
      if (blurU != null) {
         blurU.set(this.blurRadius);
      }

      if (thrU != null) {
         thrU.set(this.threshold);
      }

      if (tintU != null) {
         tintU.set(this.tintColor[0], this.tintColor[1], this.tintColor[2], this.tintColor[3]);
      }

      if (tint2U != null) {
         tint2U.set(this.tintColor2[0], this.tintColor2[1], this.tintColor2[2], this.tintColor2[3]);
      }

      if (modeU != null) {
         modeU.set(this.mode);
      }

      if (timeU != null) {
         timeU.set(this.time);
      }

      if (scaleU != null) {
         scaleU.set(this.shaderScale);
      }
   }
}
