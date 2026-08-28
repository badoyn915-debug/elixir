package code.elixir.utils.display.render.effect;

import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import net.minecraft.client.util.math.MatrixStack;

public class LiquidGlassEffect {
   private static final Rectangle rectangle = new Rectangle();

   private static int whiteAlpha(float a) {
      int alpha = Math.max(0, Math.min(255, (int)(a * 255.0F)));
      return alpha << 24 | 16777215;
   }

   public static void render(MatrixStack matrix, float x, float y, float width, float height, float radius, float alpha) {
      if (!(alpha <= 0.0F)) {
         int baseColor = whiteAlpha(alpha * 0.08F);
         rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(radius).color(baseColor).build());
         float topGlowHeight = height * 0.3F;
         int topColor = whiteAlpha(alpha * 0.15F);
         rectangle.render(ShapeProperties.create(matrix, x, y, width, topGlowHeight).round(radius).color(topColor, topColor, 16777215, 16777215).build());
         float edgeWidth = Math.min(width * 0.05F, 2.0F);
         int edgeColor = whiteAlpha(alpha * 0.12F);
         rectangle.render(ShapeProperties.create(matrix, x, y, edgeWidth, height).round(radius).color(edgeColor, 16777215, 16777215, edgeColor).build());
      }
   }

   public static void render(MatrixStack matrix, float x, float y, float width, float height, float radius, float alpha, float intensity) {
      if (!(alpha <= 0.0F) && !(intensity <= 0.0F)) {
         float scaledAlpha = alpha * intensity;
         int baseColor = whiteAlpha(scaledAlpha * 0.08F);
         rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(radius).color(baseColor).build());
         float topGlowHeight = height * 0.3F;
         int topColor = whiteAlpha(scaledAlpha * 0.15F);
         rectangle.render(ShapeProperties.create(matrix, x, y, width, topGlowHeight).round(radius).color(topColor, topColor, 16777215, 16777215).build());
         float edgeWidth = Math.min(width * 0.05F, 2.0F);
         int edgeColor = whiteAlpha(scaledAlpha * 0.12F);
         rectangle.render(ShapeProperties.create(matrix, x, y, edgeWidth, height).round(radius).color(edgeColor, 16777215, 16777215, edgeColor).build());
      }
   }
}
