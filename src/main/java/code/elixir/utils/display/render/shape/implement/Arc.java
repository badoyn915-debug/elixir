package code.elixir.utils.display.render.shape.implement;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shape.Shape;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Arc implements Shape, QuickImports {
   private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/arc"), VertexFormats.POSITION, Defines.EMPTY);

   @Override
   public void render(ShapeProperties shape) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      float scale = (float)mc.getWindow().getScaleFactor();
      float alpha = RenderSystem.getShaderColor()[3];
      Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
      Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, new Vector3f()).mul(scale);
      Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
      Vector4f round = shape.getRound().mul(size.y);
      float width = shape.getWidth() * size.x;
      float height = shape.getHeight() * size.y;
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      drawEngine.quad(matrix4f, buffer, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());
      ShaderProgram shader = RenderSystem.setShader(this.SHADER_KEY);
      if (shader != null && window != null) {
         shader.getUniformOrDefault("size").set(width, height);
         shader.getUniformOrDefault("location").set(pos.x, (float)(window.getHeight() - height - pos.y));
         shader.getUniformOrDefault("radius").set(round.x);
         shader.getUniformOrDefault("thickness").set(shape.getThickness());
         shader.getUniformOrDefault("start").set(shape.getStart());
         shader.getUniformOrDefault("end").set(shape.getEnd());
         shader.getUniformOrDefault("color1")
            .set(
               ColorAssist.redf(shape.getColor().x),
               ColorAssist.greenf(shape.getColor().x),
               ColorAssist.bluef(shape.getColor().x),
               ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().x, alpha))
            );
         shader.getUniformOrDefault("color2")
            .set(
               ColorAssist.redf(shape.getColor().y),
               ColorAssist.greenf(shape.getColor().y),
               ColorAssist.bluef(shape.getColor().y),
               ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().y, alpha))
            );
      }
      BufferDraw.draw(buffer);
      RenderSystem.disableBlend();
   }
}
