package code.elixir.utils.display.render.post;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.GuiRenderState;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class KawaseBlur implements QuickImports {
   public static final KawaseBlur INSTANCE = new KawaseBlur();
   private static final ShaderProgramKey upShader = new ShaderProgramKey(Identifier.of("minecraft", "core/blur/upscale"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey downShader = new ShaderProgramKey(
      Identifier.of("minecraft", "core/blur/downscale"), VertexFormats.POSITION, Defines.EMPTY
   );
   public final List<SimpleFramebuffer> fbos = new ArrayList<>();
   private int steps;

   private void checkupFbo() {
      for (SimpleFramebuffer fbo : this.fbos) {
         if (mc.getFramebuffer().textureWidth != fbo.textureWidth || mc.getFramebuffer().textureHeight != fbo.textureHeight) {
            fbo.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         }
      }
   }

   public void recreate() {
      this.fbos.forEach(Framebuffer::delete);
      this.fbos.clear();

      for (int i = 0; i <= this.steps; i++) {
         this.fbos.add(this.createFbo());
      }
   }

   public void render(MatrixStack matrixStack, int steps, float offset) {
      if (this.steps != steps) {
         this.steps = steps;
         this.recreate();
      }

      this.checkupFbo();
      int actualPasses = Math.max(this.fbos.size() - 1, 1);
      this.applyBlurPass(matrixStack, downShader, mc.getFramebuffer(), (Framebuffer)this.fbos.getFirst(), 0, offset);

      for (int i = 0; i < actualPasses; i++) {
         this.applyBlurPass(matrixStack, downShader, (Framebuffer)this.fbos.get(i), (Framebuffer)this.fbos.get(i + 1), i + 1, offset);
      }

      for (int var6 = actualPasses; var6 > 0; var6--) {
         this.applyBlurPass(matrixStack, upShader, (Framebuffer)this.fbos.get(var6), (Framebuffer)this.fbos.get(var6 - 1), var6, offset);
      }

      mc.getFramebuffer().beginWrite(false);
      GuiRenderState.restoreAfterCustomDraw();
   }

   private void applyBlurPass(MatrixStack matrixStack, ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, int pass, float offset) {
      destination.beginWrite(false);
      RenderSystem.setShaderTexture(0, source.getColorAttachment());
      ShaderProgram shader = RenderSystem.setShader(shaderKey);
      shader.getUniform("uHalfTexelSize").set(0.5F / source.textureWidth, 0.5F / source.textureHeight);
      shader.getUniform("uOffset").set(offset * ((float)pass / this.steps));
      this.drawFullQuad(matrixStack.peek().getPositionMatrix());
      destination.endWrite();
   }

   private void drawFullQuad(Matrix4f matrix4f) {
      float width = mc.getWindow().getScaledWidth();
      float height = mc.getWindow().getScaledHeight();
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      builder.vertex(matrix4f, 0.0F, 0.0F, 0.0F);
      builder.vertex(matrix4f, 0.0F, height, 0.0F);
      builder.vertex(matrix4f, width, height, 0.0F);
      builder.vertex(matrix4f, width, 0.0F, 0.0F);
      BufferDraw.draw(builder);
   }

   private SimpleFramebuffer createFbo() {
      return new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
   }

   public int getBlurTexture() {
      return this.fbos.isEmpty() ? 0 : this.fbos.getFirst().getColorAttachment();
   }
}
