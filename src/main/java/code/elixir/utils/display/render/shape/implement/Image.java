package code.elixir.utils.display.render.shape.implement;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.shape.Shape;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class Image implements Shape, QuickImports {
   private String texture;

   @Override
   public void render(ShapeProperties shape) {
      MatrixStack matrix = shape.getMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderTexture(0, Identifier.of(this.texture));
      float width = shape.getWidth();
      float x = shape.getX() + width;
      float y = shape.getY();
      matrix.push();
      matrix.translate(x, y, 0.0F);
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
      matrix.translate(-x, -y, 0.0F);
      drawEngine.quad(matrix.peek().getPositionMatrix(), x, y, shape.getHeight(), width, shape.getColor().x);
      matrix.pop();
      RenderSystem.disableBlend();
   }

   public Image setTexture(String texture) {
      this.texture = texture;
      return this;
   }
}
