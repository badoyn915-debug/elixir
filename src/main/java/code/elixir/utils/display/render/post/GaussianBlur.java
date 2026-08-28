package code.elixir.utils.display.render.post;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.util.CyclicStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;

public class GaussianBlur implements QuickImports {
   private static final CyclicStack<Framebuffer> stack = new CyclicStack<Framebuffer>() {
      {
         this.add(new SimpleFramebuffer(1, 1, false));
         this.add(new SimpleFramebuffer(1, 1, false));
      }
   };
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("core/gaussian_blur"), VertexFormats.POSITION, Defines.EMPTY);

   public static void applyBlurEffect(Framebuffer read, Framebuffer write, int countOfPasses, float scaleMultiplier) {
      resizeFramebuffers();
      Framebuffer readBuffer = read;

      for (int i = 0; i < countOfPasses; i++) {
         Framebuffer wroteBuffer = stack.next();
         executePass(readBuffer, wroteBuffer, scaleMultiplier, i % 2 == 0 ? 1.0F : 0.0F);
         readBuffer = wroteBuffer;
      }

      write.beginWrite(false);
      readBuffer.beginRead();
      RenderSystem.setShader(ShaderProgramKeys.BLIT_SCREEN);
      drawQuad();
      readBuffer.endRead();
      write.endWrite();
      mc.getFramebuffer().beginWrite(false);
   }

   private static void resizeFramebuffers() {
      for (Framebuffer framebuffer : stack) {
         if (framebuffer.textureWidth != mc.getFramebuffer().textureWidth || framebuffer.textureHeight != mc.getFramebuffer().textureHeight) {
            framebuffer.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         }
      }
   }

   private static void executePass(Framebuffer read, Framebuffer write, float scaleMultiplier, float offsetDirection) {
      write.beginWrite(false);
      read.beginRead();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      ShaderProgram shaderProgram = RenderSystem.setShader(SHADER_KEY);
      shaderProgram.getUniform("scaleMultiplier").set(scaleMultiplier);
      shaderProgram.getUniform("offsetDirection").set(offsetDirection);
      drawQuad();
      read.endRead();
      write.endWrite();
      RenderSystem.disableBlend();
   }

   private static void drawQuad() {
      float width = mc.getWindow().getScaledWidth();
      float height = mc.getWindow().getScaledHeight();
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      builder.vertex(0.0F, 0.0F, 0.0F);
      builder.vertex(0.0F, height, 0.0F);
      builder.vertex(width, height, 0.0F);
      builder.vertex(width, 0.0F, 0.0F);
      BufferDraw.draw(builder);
   }
}
