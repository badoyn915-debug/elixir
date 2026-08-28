package code.elixir.utils.display.render.draw;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix4f;
import org.joml.Vector4i;

public class DrawEngineImpl implements DrawEngine, QuickImports {
   @Override
   public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height) {
      buffer.vertex(matrix4f, x, y, 0.0F);
      buffer.vertex(matrix4f, x, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y, 0.0F);
   }

   @Override
   public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix4f, x, y, 0.0F).color(color);
      buffer.vertex(matrix4f, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).color(color);
   }

   @Override
   public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x, y, 0.0F).texture(1.0F, 0.0F).color(color);
      BufferDraw.draw(buffer);
   }

   @Override
   public void quadTexture(Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
      buffer.vertex(entry, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color.x);
      buffer.vertex(entry, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color.y);
      buffer.vertex(entry, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color.w);
      buffer.vertex(entry, x, y, 0.0F).texture(1.0F, 0.0F).color(color.z);
   }
}
