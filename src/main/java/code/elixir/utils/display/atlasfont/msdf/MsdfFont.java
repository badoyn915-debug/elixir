package code.elixir.utils.display.atlasfont.msdf;

import code.elixir.utils.display.atlasfont.providers.ResourceProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class MsdfFont {
   private final String name;
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Map<Integer, MsdfGlyph> glyphs;
   private final Map<Integer, Map<Integer, Float>> kernings;

   private static String resolveMsdfResourceName(String name) {
      return switch (name) {
         case "suisseintl-semibold", "suisseintlsemibold" -> "suisseintlsemibold";
         default -> name;
      };
   }

   private MsdfFont(
      String name,
      AbstractTexture texture,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      Map<Integer, MsdfGlyph> glyphs,
      Map<Integer, Map<Integer, Float>> kernings
   ) {
      this.name = name;
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
   }

   public int getTextureId() {
      return this.texture.getGlId();
   }

   public void applyGlyphs(
      Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color
   ) {
      int prevChar = -1;

      for (int i = 0; i < text.length(); i++) {
         char _char = text.charAt(i);
         MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(_char));
         if (glyph == null) {
            if (_char == ' ') {
               x += this.getSpaceAdvance(size) + thickness + spacing;
               prevChar = _char;
            }
         } else {
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
               x += kerning.getOrDefault(_char, 0.0F) * size;
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = _char;
         }
      }
   }

   public void applyRainbowGlyphs(
      Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color1, int color2
   ) {
      int prevChar = -1;
      long time = System.currentTimeMillis();

      for (int i = 0; i < text.length(); i++) {
         char _char = text.charAt(i);
         MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(_char));
         if (glyph == null) {
            if (_char == ' ') {
               x += this.getSpaceAdvance(size) + thickness + spacing;
               prevChar = _char;
            }
         } else {
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
               x += kerning.getOrDefault(_char, 0.0F) * size;
            }

            float t = (float)((Math.sin((time + i * 100) / 500.0) + 1.0) / 2.0);
            t = Math.max(0.0F, Math.min(1.0F, t));
            int color = this.interpolateColor(color1, color2, t);
            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = _char;
         }
      }
   }

   private int interpolateColor(int colorStart, int colorEnd, float t) {
      float startAlpha = (colorStart >> 24 & 0xFF) / 255.0F;
      float startRed = (colorStart >> 16 & 0xFF) / 255.0F;
      float startGreen = (colorStart >> 8 & 0xFF) / 255.0F;
      float startBlue = (colorStart & 0xFF) / 255.0F;
      float endAlpha = (colorEnd >> 24 & 0xFF) / 255.0F;
      float endRed = (colorEnd >> 16 & 0xFF) / 255.0F;
      float endGreen = (colorEnd >> 8 & 0xFF) / 255.0F;
      float endBlue = (colorEnd & 0xFF) / 255.0F;
      float alpha = startAlpha + t * (endAlpha - startAlpha);
      float red = startRed + t * (endRed - startRed);
      float green = startGreen + t * (endGreen - startGreen);
      float blue = startBlue + t * (endBlue - startBlue);
      return (int)(alpha * 255.0F) << 24 | (int)(red * 255.0F) << 16 | (int)(green * 255.0F) << 8 | (int)(blue * 255.0F);
   }

   public float getWidth(String text, float size) {
      return this.getLayoutWidth(text, size, 0.0F, 0.0F);
   }

   public float getLayoutWidth(String text, float size, float thickness, float spacing) {
      if (text != null && !text.isEmpty()) {
         int prevChar = -1;
         float width = 0.0F;

         for (int i = 0; i < text.length(); i++) {
            char _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(_char));
            if (glyph == null) {
               if (_char == ' ') {
                  width += this.getSpaceAdvance(size) + thickness + spacing;
                  prevChar = _char;
               }
            } else {
               Map<Integer, Float> kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  width += kerning.getOrDefault(_char, 0.0F) * size;
               }

               width += glyph.getWidth(size) + thickness + spacing;
               prevChar = _char;
            }
         }

         return width;
      } else {
         return 0.0F;
      }
   }

   private float getSpaceAdvance(float size) {
      MsdfGlyph space = this.glyphs.get(32);
      return space != null ? space.getWidth(size) : 0.25F * size;
   }

   public float getVisualRightEdge(String text, float size, float effectiveThickness, float spacing) {
      if (text != null && !text.isEmpty()) {
         int prevChar = -1;
         float x = 0.0F;

         for (int i = 0; i < text.length(); i++) {
            char _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(_char));
            if (glyph != null) {
               Map<Integer, Float> kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  x += kerning.getOrDefault(_char, 0.0F) * size;
               }

               float var11;
               x = i < text.length() - 1 ? (var11 = x + (glyph.getWidth(size) + effectiveThickness + spacing)) : x + glyph.getVisualWidth(size);
               prevChar = _char;
            }
         }

         return x;
      } else {
         return 0.0F;
      }
   }

   public String getName() {
      return this.name;
   }

   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   public static class Builder {
      private String name = "?";
      private Identifier dataIdentifer;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String dataFileName) {
         String resolvedName = MsdfFont.resolveMsdfResourceName(dataFileName);
         this.dataIdentifer = Identifier.of("mre", "fonts/" + resolvedName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String atlasFileName) {
         String resolvedName = MsdfFont.resolveMsdfResourceName(atlasFileName);
         this.atlasIdentifier = Identifier.of("mre", "fonts/" + resolvedName + ".png");
         return this;
      }

      public MsdfFont build() {
         FontData data = null;

         try {
            data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
         } catch (Exception var10) {
            System.err.println("[MsdfFont] CRITICAL: Failed to load font data: " + this.dataIdentifer);
            var10.printStackTrace();
            throw new RuntimeException(
               "Failed to read font data file: " + this.dataIdentifer.toString() + "; Make sure the file exists in assets/mre/fonts/", var10
            );
         }

         if (data == null) {
            throw new RuntimeException(
               "Failed to read font data file: "
                  + this.dataIdentifer.toString()
                  + "; Are you sure this is json file? Try to check the correctness of its syntax."
            );
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            AbstractTexture texture = client.getTextureManager().getTexture(this.atlasIdentifier);
            if (texture == null) {
               System.out.println("[MsdfFont] Registering font texture: " + this.atlasIdentifier);

               try {
                  if (client.getResourceManager().getResource(this.atlasIdentifier).isEmpty()) {
                     throw new RuntimeException(
                        "Font texture resource not found: "
                           + this.atlasIdentifier.toString()
                           + "; Make sure the file exists at assets/mre/fonts/"
                           + this.atlasIdentifier.getPath()
                     );
                  }

                  client.getTextureManager().registerTexture(this.atlasIdentifier, new ResourceTexture(this.atlasIdentifier));
                  texture = client.getTextureManager().getTexture(this.atlasIdentifier);
                  if (texture != null) {
                     System.out.println("[MsdfFont] Successfully registered font texture: " + this.atlasIdentifier);
                  }
               } catch (Exception var9) {
                  System.err.println("[MsdfFont] CRITICAL: Failed to register texture: " + this.atlasIdentifier);
                  var9.printStackTrace();
                  throw new RuntimeException(
                     "Failed to register texture: "
                        + this.atlasIdentifier.toString()
                        + "; Make sure the texture file exists at assets/mre/fonts/"
                        + this.atlasIdentifier.getPath(),
                     var9
                  );
               }
            }

            if (texture == null) {
               throw new RuntimeException(
                  "Failed to load texture after registration: "
                     + this.atlasIdentifier.toString()
                     + "; Make sure the texture file exists at assets/mre/fonts/"
                     + this.atlasIdentifier.getPath()
               );
            } else {
               AbstractTexture finalTexture = texture;
               RenderSystem.recordRenderCall(() -> finalTexture.setFilter(true, false));
               float aWidth = data.atlas().width();
               float aHeight = data.atlas().height();
               Map<Integer, MsdfGlyph> glyphs = data.glyphs()
                  .stream()
                  .collect(Collectors.toMap(glyphData -> glyphData.unicode(), glyphData -> new MsdfGlyph(glyphData, aWidth, aHeight)));
               HashMap<Integer, Map<Integer, Float>> kernings = new HashMap<>();
               data.kernings().forEach(kerning -> {
                  HashMap<Integer, Float> map = (HashMap<Integer, Float>)kernings.get(kerning.leftChar());
                  if (map == null) {
                     map = new HashMap<>();
                     kernings.put(kerning.leftChar(), map);
                  }

                  map.put(kerning.rightChar(), kerning.advance());
               });
               return new MsdfFont(this.name, finalTexture, data.atlas(), data.metrics(), glyphs, kernings);
            }
         }
      }
   }
}
