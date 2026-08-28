package code.elixir.utils.display.render;

import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;

public class BloodyBackgroundRenderer {
   private static ShaderProgramKey BLOODY_SHADER = null;
   private static boolean initialized = false;

   public static void render(DrawContext context, int mouseX, int mouseY, int width, int height, float time, float alpha) {
      if (!initialized) {
         try {
            BLOODY_SHADER = new ShaderProgramKey(Identifier.of("elixir", "core/bloody_bg"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
         } catch (Exception var12) {
            System.err.println("Failed to load bloody_bg shader!");
            var12.printStackTrace();
         }

         initialized = true;
      }

      if (BLOODY_SHADER != null) {
         MatrixStack matrices = context.getMatrices();
         Entry entry = matrices.peek();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         ShaderProgram shader = RenderSystem.setShader(BLOODY_SHADER);
         if (shader == null) {
            restoreRenderState();
         } else {
            if (shader.getUniform("Time") != null) {
               shader.getUniform("Time").set(time);
            }

            if (shader.getUniform("Resolution") != null) {
               shader.getUniform("Resolution").set(width, height);
            }

            if (shader.getUniform("Mouse") != null) {
               shader.getUniform("Mouse").set(mouseX, height - mouseY);
            }

            if (shader.getUniform("Alpha") != null) {
               shader.getUniform("Alpha").set(alpha);
            }

            BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int color = -1;
            builder.vertex(entry, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(color);
            builder.vertex(entry, 0.0F, height, 0.0F).texture(0.0F, 1.0F).color(color);
            builder.vertex(entry, width, height, 0.0F).texture(1.0F, 1.0F).color(color);
            builder.vertex(entry, width, 0.0F, 0.0F).texture(1.0F, 0.0F).color(color);
            BufferDraw.draw(builder);
            restoreRenderState();
         }
      }
   }

   private static void restoreRenderState() {
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
