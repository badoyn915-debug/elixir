package code.elixir.utils.display.render.shape.implement;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shape.Shape;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Blur implements Shape, QuickImports {
   private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("mre", "core/blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
   public Framebuffer input;
   public Vector2f resolution = new Vector2f();

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
      float quality = shape.getQuality();
      float softness = shape.getSoftness();
      float thickness = shape.getThickness();
      float width = shape.getWidth() * size.x;
      float height = shape.getHeight() * size.y;
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      int color = ColorAssist.rgba(255, 255, 255, (int)(alpha * 255.0F));
      drawEngine.quad(
         matrix4f, buffer, shape.getX() - softness / 2.0F, shape.getY() - softness / 2.0F, shape.getWidth() + softness, shape.getHeight() + softness, color
      );
      GlStateManager._activeTexture(33984);
      if (this.input != null) {
         RenderSystem.bindTexture(this.input.getColorAttachment());
      }

      ShaderProgram shader = RenderSystem.setShader(this.SHADER_KEY);
      shader.getUniformOrDefault("Size").set(width, height);
      shader.getUniformOrDefault("Radius").set(round);
      shader.getUniformOrDefault("Smoothness").set(softness);
      shader.getUniformOrDefault("BlurRadius").set(shape.getBlurRadius());
      BufferDraw.draw(buffer);
      RenderSystem.disableBlend();
   }

   public void setup() {
      Framebuffer buffer = mc.getFramebuffer();
      if (this.input == null) {
         this.input = new SimpleFramebuffer(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false);
      }

      this.input.beginWrite(false);
      buffer.draw(this.input.textureWidth, this.input.textureHeight);
      buffer.beginWrite(false);
      if (this.input != null
         && (this.input.textureWidth != mc.getWindow().getFramebufferWidth() || this.input.textureHeight != mc.getWindow().getFramebufferHeight())) {
         this.input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
      }

      this.resolution.set(buffer.textureWidth, buffer.textureHeight);
   }
}
