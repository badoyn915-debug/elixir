package code.elixir.utils.display.atlasfont.msdf;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public final class MsdfGlyph {
   private final int code;
   private final float minU;
   private final float maxU;
   private final float minV;
   private final float maxV;
   private final float advance;
   private final float topPosition;
   private final float width;
   private final float height;

   public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
      this.code = data.unicode();
      this.advance = data.advance();
      FontData.BoundsData atlasBounds = data.atlasBounds();
      if (atlasBounds != null) {
         this.minU = atlasBounds.left() / atlasWidth;
         this.maxU = atlasBounds.right() / atlasWidth;
         this.minV = 1.0F - atlasBounds.top() / atlasHeight;
         this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
      } else {
         this.maxV = 0.0F;
         this.minV = 0.0F;
         this.maxU = 0.0F;
         this.minU = 0.0F;
      }

      FontData.BoundsData planeBounds = data.planeBounds();
      if (planeBounds != null) {
         this.width = planeBounds.right() - planeBounds.left();
         this.height = planeBounds.top() - planeBounds.bottom();
         this.topPosition = planeBounds.top();
      } else {
         this.topPosition = 0.0F;
         this.height = 0.0F;
         this.width = 0.0F;
      }
   }

   public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
      float width = this.width * size;
      float height = this.height * size;
      float var10;
      consumer.vertex(matrix, x, var10 = y - this.topPosition * size, z).texture(this.minU, this.minV).color(color);
      consumer.vertex(matrix, x, var10 + height, z).texture(this.minU, this.maxV).color(color);
      consumer.vertex(matrix, x + width, var10 + height, z).texture(this.maxU, this.maxV).color(color);
      consumer.vertex(matrix, x + width, var10, z).texture(this.maxU, this.minV).color(color);
      return this.advance * size;
   }

   public float getWidth(float size) {
      return this.advance * size;
   }

   public float getVisualWidth(float size) {
      return this.width * size;
   }

   public int getCharCode() {
      return this.code;
   }
}
