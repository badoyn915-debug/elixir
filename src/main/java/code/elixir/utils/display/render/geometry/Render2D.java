package code.elixir.utils.display.render.geometry;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.item.ItemRender;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class Render2D implements QuickImports {
   private static final List<Render2DQuad> QUAD = new ArrayList<>();
   static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("minecraft", "core/sampler_texture/sampler_texture"), VertexFormats.POSITION, Defines.EMPTY
   );

   public static void onRender(DrawContext context) {
      flushQuads(context.getMatrices());
   }

   public static void flushQuads(MatrixStack matrix) {
      if (!QUAD.isEmpty()) {
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD.forEach(quad -> drawEngine.quad(matrix4f, buffer, quad.x(), quad.y(), quad.width(), quad.height(), quad.color()));
         BufferDraw.draw(buffer);
         RenderSystem.disableBlend();
         QUAD.clear();
      }
   }

   public static void defaultDrawStack(DrawContext context, ItemStack stack, float x, float y, boolean rect, boolean drawItemInSlot, float scale) {
      defaultDrawStack(context, stack, x, y, rect, drawItemInSlot, scale, 0.0F);
   }

   public static void defaultDrawStack(DrawContext context, ItemStack stack, float x, float y, boolean rect, boolean drawItemInSlot, float scale, float z) {
      if (Rectangle.isBatching()) {
         Rectangle.flushPending();
      }

      MatrixStack matrix = context.getMatrices();
      matrix.push();
      matrix.translate(x + 1.0F, y + 1.0F, z);
      matrix.scale(scale, scale, 1.0F);
      context.drawItem(stack, 0, 0);
      int count;
      if (drawItemInSlot && (count = stack.getCount()) > 1) {
         String countText = String.valueOf(count);
         FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
         float textWidth = font.getStringWidth(countText);
         float textX = 16.0F - textWidth - 1.0F;
         float textY = 16.0F - font.getStringHeight(countText) - 1.0F;
         font.drawString(matrix, countText, textX - 0.5F, textY - 0.5F, -16777216);
         font.drawString(matrix, countText, textX + 0.5F, textY - 0.5F, -16777216);
         font.drawString(matrix, countText, textX - 0.5F, textY + 0.5F, -16777216);
         font.drawString(matrix, countText, textX + 0.5F, textY + 0.5F, -16777216);
         font.drawString(matrix, countText, textX, textY, -1);
      }

      matrix.pop();
   }

   public static void rectangleWithMask(Matrix4f matrix4f, float x, float y, float w, float h, float round, int color, int texture) {
      try {
         RenderSystem.setShaderTexture(0, texture);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
         ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
         if (shader == null || mc.getWindow() == null || mc.getFramebuffer() == null) {
            return;
         }
         float scale = (float)mc.getWindow().getScaleFactor();
         Vector3f pos = matrix4f.transformPosition(x, y, 0.0F, new Vector3f()).mul(scale);
         Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
         Vector4f r = new Vector4f(round).mul(size.y);
         float width = w * size.x;
         float height = h * size.y;
         if (shader.getUniform("size") != null) {
            shader.getUniform("size").set(width, height);
         }
         if (shader.getUniform("location") != null) {
            shader.getUniform("location").set(pos.x, (float)(mc.getWindow().getHeight() - height - pos.y));
         }
         if (shader.getUniform("texSize") != null) {
            shader.getUniform("texSize").set((float)mc.getFramebuffer().textureWidth, (float)mc.getFramebuffer().textureHeight);
         }
         if (shader.getUniform("color") != null) {
            shader.getUniform("color")
               .set(ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), RenderSystem.getShaderColor()[3]);
         }
         if (shader.getUniform("round") != null) {
            shader.getUniform("round").set(r);
         }
         drawEngine.quad(matrix4f, buffer, x, y, w, h);
         BufferDraw.draw(buffer);
         RenderSystem.disableBlend();
      } catch (Throwable ignored) {
      }
   }

   public static void drawStack(MatrixStack matrix, ItemStack stack, float x, float y, boolean rect, float scale) {
      if (Rectangle.isBatching()) {
         Rectangle.flushPending();
      }

      float posX = x + 1.0F;
      float posY = y + 1.0F;
      float padding = 1.0F;
      matrix.push();
      matrix.translate(posX, posY, 0.0F);
      if (rect) {
         blur.render(
            ShapeProperties.create(matrix, -padding, -padding, 16.0F * scale + padding * 2.0F, 16.0F * scale + padding * 2.0F)
               .round(1.5F)
               .color(ColorAssist.HALF_BLACK)
               .build()
         );
      }

      matrix.scale(scale, scale, 1.0F);
      ItemRender.drawItem(matrix, stack, 0.0F, 0.0F, true, true);
      matrix.pop();
   }

   public static void drawTexture(DrawContext context, Identifier id, float x, float y, int size) {
      MatrixStack matrix = context.getMatrices();
      if (id != null) {
         matrix.push();
         matrix.translate(x, y, 0.0F);
         matrix.scale(size, size, 1.0F);
         RenderSystem.enableBlend();
         drawTexture(matrix, id, 0, 0, 1.0F, 1.0F, size, size, size, size, size, size, -1);
         RenderSystem.disableBlend();
         matrix.translate(-x, -y, 0.0F);
         matrix.pop();
      }
   }

   public static Color applyOpacity(Color color, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity));
   }

   public static int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity)).getRGB();
   }

   public static void drawTexture(
      DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor
   ) {
      drawTexture(context, id, x, y, size, round, uvSize, regionSize, textureSize, backgroundColor, -1);
   }

   public static void drawTexture(
      DrawContext context,
      Identifier id,
      float x,
      float y,
      float size,
      float round,
      int uvSize,
      int regionSize,
      int textureSize,
      int backgroundColor,
      int color
   ) {
      if (Rectangle.isBatching()) {
         Rectangle.flushPending();
      }

      MatrixStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(round).color(backgroundColor).build());
      if (id != null) {
         matrix.push();
         matrix.translate(x, y, 0.0F);
         matrix.scale(size, size, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(772, 773);
         drawTexture(matrix, id, 0, 0, 1.0F, 1.0F, uvSize, uvSize, regionSize, regionSize, textureSize, textureSize, color);
         RenderSystem.defaultBlendFunc();
         matrix.translate(-x, -y, 0.0F);
         matrix.pop();
      }
   }

   public static void drawSprite(MatrixStack matrix, Sprite sprite, float x, float y, float width, int height) {
      if (width != 0.0F && height != 0) {
         drawTexturedQuad(matrix, sprite.getAtlasId(), x, x + width, y, y + height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), -1);
      }
   }

   public static void drawSprite(MatrixStack matrix, Sprite sprite, float x, float y, float width, int height, int color) {
      if (width != 0.0F && height != 0) {
         drawTexturedQuad(
            matrix, sprite.getAtlasId(), x, x + width, y, y + height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color
         );
      }
   }

   public static void drawTexture(
      MatrixStack matrix,
      Identifier texture,
      int x,
      int y,
      float width,
      float height,
      float u,
      float v,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      drawTexture(matrix, texture, x, x + width, y, y + height, 0.0F, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
   }

   public static void drawTexture(
      MatrixStack matrix,
      Identifier texture,
      float x1,
      float x2,
      float y1,
      float y2,
      float z,
      int regionWidth,
      int regionHeight,
      float u,
      float v,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      drawTexturedQuad(
         matrix,
         texture,
         x1,
         x2,
         y1,
         y2,
         (u + 0.0F) / textureWidth,
         (u + regionWidth) / textureWidth,
         (v + 0.0F) / textureHeight,
         (v + regionHeight) / textureHeight,
         color
      );
   }

   public static void drawSpriteColored(
      MatrixStack matrix, Sprite sprite, float x, float y, float width, float height, int colorTL, int colorBL, int colorBR, int colorTR
   ) {
      if (Rectangle.isBatching()) {
         Rectangle.flushPending();
      }

      RenderSystem.setShaderTexture(0, sprite.getAtlasId());
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      buffer.vertex(matrix4f, x, y, 0.0F).texture(sprite.getMinU(), sprite.getMinV()).color(colorTL);
      buffer.vertex(matrix4f, x, y + height, 0.0F).texture(sprite.getMinU(), sprite.getMaxV()).color(colorBL);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).texture(sprite.getMaxU(), sprite.getMaxV()).color(colorBR);
      buffer.vertex(matrix4f, x + width, y, 0.0F).texture(sprite.getMaxU(), sprite.getMinV()).color(colorTR);
      BufferDraw.draw(buffer);
   }

   public static void drawTexturedQuad(
      MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color
   ) {
      if (Rectangle.isBatching()) {
         Rectangle.flushPending();
      }

      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      buffer.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1).color(color);
      buffer.vertex(matrix4f, x1, y2, 0.0F).texture(u1, v2).color(color);
      buffer.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
      buffer.vertex(matrix4f, x2, y1, 0.0F).texture(u2, v1).color(color);
      BufferDraw.draw(buffer);
   }

   public static void drawCircle(MatrixStack matrix, float x, float y, float radius, int color) {
      int segments = 16;
      float angleStep = (float)((Math.PI * 2) / segments);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < segments; i++) {
         float angle1 = i * angleStep;
         float angle2 = (i + 1) * angleStep;
         float x1 = x + radius * (float)Math.cos(angle1);
         float y1 = y + radius * (float)Math.sin(angle1);
         float x2 = x + radius * (float)Math.cos(angle2);
         float y2 = y + radius * (float)Math.sin(angle2);
         buffer.vertex(matrix4f, x, y, 0.0F).color(color);
         buffer.vertex(matrix4f, x1, y1, 0.0F).color(color);
         buffer.vertex(matrix4f, x2, y2, 0.0F).color(color);
         buffer.vertex(matrix4f, x, y, 0.0F).color(color);
      }

      BufferDraw.draw(buffer);
      RenderSystem.disableBlend();
   }

   public static void drawCircleOutline(MatrixStack matrix, float x, float y, float radius, float thickness, int color) {
      int segments = 48;
      float angleStep = (float)((Math.PI * 2) / segments);
      float inner = Math.max(0.0F, radius - thickness);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < segments; i++) {
         float angle1 = i * angleStep;
         float angle2 = (i + 1) * angleStep;
         float cos1 = (float)Math.cos(angle1);
         float sin1 = (float)Math.sin(angle1);
         float cos2 = (float)Math.cos(angle2);
         float sin2 = (float)Math.sin(angle2);
         float ox1 = x + radius * cos1;
         float oy1 = y + radius * sin1;
         float ox2 = x + radius * cos2;
         float oy2 = y + radius * sin2;
         float ix1 = x + inner * cos1;
         float iy1 = y + inner * sin1;
         float ix2 = x + inner * cos2;
         float iy2 = y + inner * sin2;
         buffer.vertex(matrix4f, ox1, oy1, 0.0F).color(color);
         buffer.vertex(matrix4f, ox2, oy2, 0.0F).color(color);
         buffer.vertex(matrix4f, ix2, iy2, 0.0F).color(color);
         buffer.vertex(matrix4f, ix1, iy1, 0.0F).color(color);
      }

      BufferDraw.draw(buffer);
      RenderSystem.disableBlend();
   }

   public static void endBuilding(BufferBuilder bb) {
      BuiltBuffer builtBuffer = bb.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   public static void drawLine(MatrixStack matrix, float x1, float y1, float x2, float y2, float thickness, int color) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float length = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(length < 1.0E-4F)) {
         float half = thickness * 0.5F;
         float nx = -dy / length * half;
         float ny = dx / length * half;
         int finalColor = ColorAssist.multAlpha(color, RenderSystem.getShaderColor()[3]);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix4f, x1 + nx, y1 + ny, 0.0F).color(finalColor);
         buffer.vertex(matrix4f, x1 - nx, y1 - ny, 0.0F).color(finalColor);
         buffer.vertex(matrix4f, x2 - nx, y2 - ny, 0.0F).color(finalColor);
         buffer.vertex(matrix4f, x2 + nx, y2 + ny, 0.0F).color(finalColor);
         BufferDraw.draw(buffer);
         RenderSystem.disableBlend();
      }
   }

   public static void drawQuad(float x, float y, float width, float height, int color) {
      QUAD.add(new Render2DQuad(x, y, width, height, ColorAssist.multAlpha(color, RenderSystem.getShaderColor()[3])));
   }

   private Render2D() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
