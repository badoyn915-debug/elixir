package code.elixir.utils.display.render;

import code.elixir.utils.display.atlasfont.providers.ResourceProvider;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HudShine implements QuickImports {
   private static final float SWEEP_MS = 2200.0F;
   private static final float PAUSE_MS = 1100.0F;
   private static final ShaderProgramKey SHINE_SHADER = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("hud_shine"), VertexFormats.POSITION, Defines.EMPTY
   );

   private HudShine() {
   }

   public static void render(MatrixStack matrices, float x, float y, float w, float h, float round) {
      render(matrices, x, y, w, h, round, 1.0F, 1.0F, 0.42F);
   }

   public static void render(MatrixStack matrices, float x, float y, float w, float h, float round, float sizeScale, float speedScale, float brightness) {
      if (!(w <= 2.0F) && !(h <= 2.0F)) {
         double cycle = 3300.0;
         double now = (double)System.currentTimeMillis() * speedScale;
         double t = now % cycle;
         if (!(t > 2200.0)) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float scale = (float)window.getScaleFactor();
            Vector3f pos = matrix.transformPosition(x, y, 0.0F, new Vector3f()).mul(scale);
            Vector3f sz = matrix.getScale(new Vector3f()).mul(scale);
            float scaledWidth = w * sz.x;
            float scaledHeight = h * sz.y;
            float locationX = pos.x;
            float locationY = window.getHeight() - scaledHeight - pos.y;
            float radius = Math.min(round, Math.min(w, h) * 0.5F) * sz.y;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            ShaderProgram shader = RenderSystem.setShader(SHINE_SHADER);
            shader.getUniformOrDefault("size").set(scaledWidth, scaledHeight);
            shader.getUniformOrDefault("location").set(locationX, locationY);
            shader.getUniformOrDefault("radius").set(radius, radius, radius, radius);
            shader.getUniformOrDefault("beamWidth").set(0.2F * sizeScale);
            shader.getUniformOrDefault("brightness").set(brightness);
            if (shader.getUniform("Time") != null) {
               shader.getUniform("Time").set((float)(now % (4.0 * cycle) / 1000.0));
            }

            float m = 8.0F;
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
            buffer.vertex(matrix, x - m, y - m, 0.0F);
            buffer.vertex(matrix, x - m, y + h + m, 0.0F);
            buffer.vertex(matrix, x + w + m, y + h + m, 0.0F);
            buffer.vertex(matrix, x + w + m, y - m, 0.0F);
            BufferDraw.draw(buffer);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
         }
      }
   }
}
