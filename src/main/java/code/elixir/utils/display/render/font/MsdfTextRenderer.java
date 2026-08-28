package code.elixir.utils.display.render.font;

import code.elixir.events.render.TextFactoryEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.atlasfont.msdf.MsdfFont;
import code.elixir.utils.display.atlasfont.providers.ResourceProvider;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.GuiRenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class MsdfTextRenderer {
   private static final float THICKNESS = 0.05F;
   private static final float SMOOTHNESS = 0.5F;
   private static final float FLAT_THICKNESS = 0.03F;
   private static final float FLAT_SMOOTHNESS = 0.2F;
   private static final float BASELINE_FACTOR = 0.7F;
   private static final float X_NUDGE = -0.75F;
   private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("msdf_font"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );

   private MsdfTextRenderer() {
   }

   public static float visualHeight(float size) {
      return size * 0.7F;
   }

   public static float lineHeight(MsdfFont font, float size) {
      return font.getMetrics().lineHeight() * size;
   }

   public static float getStringWidth(MsdfFont font, float size, String text) {
      text = resolveText(text);
      float effectiveThickness = 0.025F * size;
      float width = 0.0F;

      for (MsdfTextRenderer.TextSegment segment : parseSegments(text, -1)) {
         width += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0F);
      }

      return width;
   }

   public static float getFlatStringWidth(MsdfFont font, float size, String text) {
      text = resolveText(text);
      float effectiveThickness = 0.015F * size;
      float width = 0.0F;

      for (MsdfTextRenderer.TextSegment segment : parseSegments(text, -1)) {
         width += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0F);
      }

      return width;
   }

   public static float getStringHeight(MsdfFont font, float size, String text) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         String[] lines = text.split("\n", -1);
         return lines.length <= 1 ? visualHeight(size) : lineHeight(font, size) * (lines.length - 1) + visualHeight(size);
      } else {
         return visualHeight(size);
      }
   }

   public static void drawString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         beginDraw(font);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float drawY = (float)y;
         float cursorX = (float)x;

         for (String line : text.split("\n", -1)) {
            drawLine(matrix4f, builder, font, size, line, cursorX, drawY, color);
            drawY += lineHeight(font, size);
         }

         endDraw(builder);
      }
   }

   public static void drawFlatString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         beginDraw(font, 0.03F, 0.2F);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float drawY = (float)y;
         float cursorX = (float)x;

         for (String line : text.split("\n", -1)) {
            drawLine(matrix4f, builder, font, size, line, cursorX, drawY, color);
            drawY += lineHeight(font, size);
         }

         endDraw(builder);
      }
   }

   public static void drawGradientString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int colorStart, int colorEnd) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         beginDraw(font);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float drawY = (float)y;
         float cursorX = (float)x;

         for (String line : text.split("\n", -1)) {
            drawGradientLine(matrix4f, builder, font, size, line, cursorX, drawY, colorStart, colorEnd);
            drawY += lineHeight(font, size);
         }

         endDraw(builder);
      }
   }

   public static void drawRainbowString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color1, int color2) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         beginDraw(font);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float drawY = (float)y;
         float cursorX = (float)x;
         float effectiveThickness = 0.025F * size;

         for (String line : text.split("\n", -1)) {
            font.applyRainbowGlyphs(matrix4f, builder, line, size, effectiveThickness, 0.0F, cursorX + -0.75F, drawY + size * 0.7F, 0.0F, color1, color2);
            drawY += lineHeight(font, size);
         }

         endDraw(builder);
      }
   }

   public static void drawStringWithSpacing(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color, float spacing) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         beginDraw(font);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float drawY = (float)y;
         float cursorX = (float)x;
         float effectiveThickness = 0.025F * size;

         for (String line : text.split("\n", -1)) {
            float lineX = cursorX;

            for (MsdfTextRenderer.TextSegment segment : parseSegments(line, color)) {
               if (!segment.text().isEmpty()) {
                  font.applyGlyphs(
                     matrix4f, builder, segment.text(), size, effectiveThickness, spacing, lineX + -0.75F, drawY + size * 0.7F, 0.0F, segment.color()
                  );
                  lineX += font.getLayoutWidth(segment.text(), size, effectiveThickness, spacing);
               }
            }

            drawY += lineHeight(font, size);
         }

         endDraw(builder);
      }
   }

   private static void drawLine(Matrix4f matrix, BufferBuilder builder, MsdfFont font, float size, String line, float x, float y, int defaultColor) {
      float effectiveThickness = 0.025F * size;
      float cursorX = x;

      for (MsdfTextRenderer.TextSegment segment : parseSegments(line, defaultColor)) {
         if (!segment.text().isEmpty()) {
            font.applyGlyphs(matrix, builder, segment.text(), size, effectiveThickness, 0.0F, cursorX + -0.75F, y + size * 0.7F, 0.0F, segment.color());
            cursorX += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0F);
         }
      }
   }

   private static void drawGradientLine(
      Matrix4f matrix, BufferBuilder builder, MsdfFont font, float size, String line, float x, float y, int colorStart, int colorEnd
   ) {
      if (!line.isEmpty()) {
         float effectiveThickness = 0.025F * size;
         float cursorX = x;
         int length = line.length();

         for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            String glyph = String.valueOf(c);
            if (c != ' ') {
               float t = length <= 1 ? 0.0F : (float)i / (length - 1);
               int color = interpolateColor(colorStart, colorEnd, t);
               font.applyGlyphs(matrix, builder, glyph, size, effectiveThickness, 0.0F, cursorX + -0.75F, y + size * 0.7F, 0.0F, color);
            }

            cursorX += font.getLayoutWidth(glyph, size, effectiveThickness, 0.0F);
         }
      }
   }

   public static void drawHalfSplitString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color1, int color2) {
      if ((text = resolveText(text)) != null && !text.isEmpty()) {
         String flat = text.replace("\n", "");
         if (!flat.isEmpty()) {
            float totalWidth = getStringWidth(font, size, flat);
            float midX = (float)x + totalWidth / 2.0F;
            beginDraw(font);
            Matrix4f matrix4f = matrix.peek().getPositionMatrix();
            BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float effectiveThickness = 0.025F * size;
            float cursorX = (float)x;
            float drawY = (float)y + size * 0.7F;

            for (int i = 0; i < flat.length(); i++) {
               char c = flat.charAt(i);
               String glyph = String.valueOf(c);
               float glyphWidth = font.getLayoutWidth(glyph, size, effectiveThickness, 0.0F);
               if (c != ' ') {
                  int color = cursorX + glyphWidth / 2.0F < midX ? color1 : color2;
                  font.applyGlyphs(matrix4f, builder, glyph, size, effectiveThickness, 0.0F, cursorX + -0.75F, drawY, 0.0F, color);
               }

               cursorX += glyphWidth;
            }

            endDraw(builder);
         }
      }
   }

   private static void beginDraw(MsdfFont font) {
      beginDraw(font, 0.05F, 0.5F);
   }

   private static void beginDraw(MsdfFont font, float thickness, float smoothness) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, font.getTextureId());
      ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
      shader.getUniform("Range").set(font.getAtlas().range());
      shader.getUniform("Thickness").set(thickness);
      shader.getUniform("Smoothness").set(smoothness);
      shader.getUniform("Outline").set(0);
   }

   private static void endDraw(BufferBuilder builder) {
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      GuiRenderState.restoreFontAtlasesNearest();
   }

   private static String resolveText(String text) {
      if (text != null && !text.isEmpty() && SafeEvents.hasListeners(TextFactoryEvent.class)) {
         TextFactoryEvent event = SafeEvents.dispatch(TextFactoryEvent.class, () -> new TextFactoryEvent(text));
         return event != null ? event.getText() : text;
      } else {
         return text;
      }
   }

   private static List<MsdfTextRenderer.TextSegment> parseSegments(String text, int defaultColor) {
      ArrayList<MsdfTextRenderer.TextSegment> segments = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      int color = defaultColor == -1 ? ColorAssist.getText() : defaultColor;
      boolean colorFormat = false;
      boolean customColor = false;
      StringBuilder customColorBuffer = new StringBuilder();

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == 167) {
            flushSegment(segments, current, color);
            colorFormat = true;
         } else if (colorFormat) {
            colorFormat = false;
            char code = Character.toUpperCase(c);
            if (ColorAssist.colorCodes.containsKey(code)) {
               color = new Color(ColorAssist.colorCodes.get(code)).getRGB();
            } else if (code == 'R' && defaultColor != -1) {
               color = defaultColor;
            }
         } else if (c == 9167) {
            if (customColor) {
               color = new Color(Integer.parseInt(customColorBuffer.toString())).getRGB();
               customColorBuffer.setLength(0);
            }

            customColor = !customColor;
         } else if (customColor) {
            customColorBuffer.append(c);
         } else {
            current.append(c);
         }
      }

      flushSegment(segments, current, color);
      return segments;
   }

   private static void flushSegment(List<MsdfTextRenderer.TextSegment> segments, StringBuilder current, int color) {
      if (!current.isEmpty()) {
         segments.add(new MsdfTextRenderer.TextSegment(current.toString(), color));
         current.setLength(0);
      }
   }

   private static int interpolateColor(int colorStart, int colorEnd, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int a1 = colorStart >> 24 & 0xFF;
      int r1 = colorStart >> 16 & 0xFF;
      int g1 = colorStart >> 8 & 0xFF;
      int b1 = colorStart & 0xFF;
      int a2 = colorEnd >> 24 & 0xFF;
      int r2 = colorEnd >> 16 & 0xFF;
      int g2 = colorEnd >> 8 & 0xFF;
      int b2 = colorEnd & 0xFF;
      int a = (int)(a1 + (a2 - a1) * t);
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private record TextSegment(String text, int color) {
   }
}
