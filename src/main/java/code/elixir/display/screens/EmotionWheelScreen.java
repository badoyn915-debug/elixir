package code.elixir.display.screens;

import code.elixir.emotions.EmotionType;
import code.elixir.emotions.EmotionWheelManager;
import code.elixir.features.impl.misc.Globals;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public class EmotionWheelScreen extends Screen implements QuickImports {
   private static final EmotionType[] WHEEL_SLOTS = EmotionType.values();
   private static final float CENTER_Y_OFFSET = -6.0F;
   private static final float INNER_RING_R = 72.0F;
   private static final float OUTER_RING_R = 178.0F;
   private static final int SECTOR_BASE = -870703064;
   private static final int SECTOR_HOVER = -399163316;
   private static final int DIVIDER = 1442840575;
   private final EmotionWheelManager manager;
   private int hoveredSlot = -1;

   public EmotionWheelScreen(EmotionWheelManager manager) {
      super(Text.empty());
      this.manager = manager;
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float centerX = this.width / 2.0F;
      float centerY = this.height / 2.0F + -6.0F;
      this.hoveredSlot = this.findHoveredSlot(mouseX, mouseY, centerX, centerY);
      EmotionType preview = this.getHoveredEmotion() != null ? this.getHoveredEmotion() : EmotionType.HELLO;
      this.manager.setPreviewEmotion(preview);
      context.fill(0, 0, this.width, this.height, 2080374784);
      float sectorSize = sectorSizeDeg();
      float startOffset = -90.0F;

      for (int i = 0; i < WHEEL_SLOTS.length; i++) {
         float start = startOffset + i * sectorSize;
         float end = start + sectorSize;
         int color = i == this.hoveredSlot ? -399163316 : -870703064;
         this.drawRingSector(context, centerX, centerY, 72.0F, 178.0F, start, end, color);
      }

      for (int var14 = 0; var14 < WHEEL_SLOTS.length; var14++) {
         this.drawRadialLine(context, centerX, centerY, 72.0F, 178.0F, startOffset + var14 * sectorSize, 1442840575);
      }

      this.renderCenterHub(context, centerX, centerY);
      if (mc.player != null) {
         this.drawEntityPreview(context, centerX, centerY + 8.0F, 36.0F, 180.0F, 6.0F, mc.player);
      }

      this.renderInnerText(context, centerX, centerY, preview);
      this.renderSectorLabels(context, centerX, centerY, startOffset, sectorSize);
      Fonts.getSize(11, Fonts.Type.SuisseIntlMedium)
         .drawCenteredString(
            context.getMatrices(), "Удерживай " + this.getBindName() + " и отпусти на нужной эмоции", centerX, centerY + 178.0F + 36.0F, -1800947253
         );
      super.render(context, mouseX, mouseY, delta);
   }

   private void renderCenterHub(DrawContext context, float centerX, float centerY) {
      float hub = 70.0F;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), centerX - hub, centerY - hub, hub * 2.0F, hub * 2.0F)
            .round(hub)
            .thickness(1.0F)
            .outlineColor(1157627903)
            .color(-14538704)
            .build()
      );
   }

   private void renderInnerText(DrawContext context, float centerX, float centerY, EmotionType preview) {
      MatrixStack matrices = context.getMatrices();
      FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
      FontRenderer descFont = Fonts.getSize(10, Fonts.Type.SuisseIntlMedium);
      float textY = centerY + 34.0F;
      titleFont.drawCenteredString(matrices, preview.getTitle(), centerX, textY, -1);
      drawWrappedCenteredText(matrices, descFont, preview.getDescription(), centerX, textY + 12.0F, 118.0F, -1177888794);
   }

   private void renderSectorLabels(DrawContext context, float centerX, float centerY, float startOffset, float sectorSize) {
      MatrixStack matrices = context.getMatrices();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      FontRenderer font = Fonts.getSize(14, Fonts.Type.SuisseIntlSemiBold);
      float labelRadius = 135.0F;
      float maxWidth = (float)((Math.PI * 2) * labelRadius * (sectorSize / 360.0) * 0.88F);

      for (int i = 0; i < WHEEL_SLOTS.length; i++) {
         float midDeg = startOffset + i * sectorSize + sectorSize * 0.5F;
         double rad = Math.toRadians(midDeg);
         float lx = centerX + (float)Math.cos(rad) * labelRadius;
         float ly = centerY + (float)Math.sin(rad) * labelRadius;
         int color = i == this.hoveredSlot ? -1 : -985857;
         drawSectorLabel(font, matrices, WHEEL_SLOTS[i].getTitle(), lx, ly, maxWidth, color);
      }
   }

   private static void drawSectorLabel(FontRenderer font, MatrixStack matrices, String text, float lx, float ly, float maxWidth, int color) {
      float lineHeight = font.getStringHeight("Ag") + 2.0F;
      if (font.getStringWidth(text) <= maxWidth) {
         float visualH = font.getStringHeight(text);
         float drawY = ly - visualH / 2.0F + 3.0F;
         font.drawCenteredString(matrices, text, lx, drawY, color);
      } else {
         int split = text.indexOf(32);
         if (split > 0) {
            String line1 = text.substring(0, split);
            String line2 = fitLabel(font, text.substring(split + 1), maxWidth);
            if (font.getStringWidth(line1) <= maxWidth && font.getStringWidth(line2) <= maxWidth) {
               float totalH = lineHeight * 2.0F;
               float topY = ly - totalH / 2.0F + 3.0F;
               font.drawCenteredString(matrices, line1, lx, topY, color);
               font.drawCenteredString(matrices, line2, lx, topY + lineHeight, color);
               return;
            }
         }

         String fitted = fitLabel(font, text, maxWidth);
         float fittedH = font.getStringHeight(fitted);
         font.drawCenteredString(matrices, fitted, lx, ly - fittedH / 2.0F + 3.0F, color);
      }
   }

   private static String fitLabel(FontRenderer font, String text, float maxWidth) {
      if (font.getStringWidth(text) <= maxWidth) {
         return text;
      } else {
         String trimmed = text;

         while (trimmed.length() > 1 && font.getStringWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
         }

         return trimmed + "…";
      }
   }

   private static float sectorSizeDeg() {
      return 360.0F / WHEEL_SLOTS.length;
   }

   private int findHoveredSlot(double mouseX, double mouseY, float centerX, float centerY) {
      float dx = (float)mouseX - centerX;
      float dy = (float)mouseY - centerY;
      float dist = MathHelper.sqrt(dx * dx + dy * dy);
      if (!(dist < 72.0F) && !(dist > 178.0F)) {
         float angleDeg = (float)((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0);
         float normalized = (angleDeg + 90.0F) % 360.0F;
         float sectorSize = sectorSizeDeg();
         int slot = (int)(normalized / sectorSize);
         return Math.min(slot, WHEEL_SLOTS.length - 1);
      } else {
         return -1;
      }
   }

   private void drawRingSector(DrawContext context, float cx, float cy, float innerR, float outerR, float startDeg, float endDeg, int color) {
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      boolean cullWasEnabled = GL11.glIsEnabled(2884);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      int segments = 48;
      float startRad = (float)Math.toRadians(startDeg);
      float endRad = (float)Math.toRadians(endDeg);
      float step = (endRad - startRad) / segments;

      for (int i = 0; i < segments; i++) {
         float a1 = startRad + step * i;
         float a2 = startRad + step * (i + 1);
         float cos1 = MathHelper.cos(a1);
         float sin1 = MathHelper.sin(a1);
         float cos2 = MathHelper.cos(a2);
         float sin2 = MathHelper.sin(a2);
         float ox1 = cx + outerR * cos1;
         float oy1 = cy + outerR * sin1;
         float ox2 = cx + outerR * cos2;
         float oy2 = cy + outerR * sin2;
         float ix1 = cx + innerR * cos1;
         float iy1 = cy + innerR * sin1;
         float ix2 = cx + innerR * cos2;
         float iy2 = cy + innerR * sin2;
         buffer.vertex(matrix, ox1, oy1, 0.0F).color(color);
         buffer.vertex(matrix, ox2, oy2, 0.0F).color(color);
         buffer.vertex(matrix, ix2, iy2, 0.0F).color(color);
         buffer.vertex(matrix, ox1, oy1, 0.0F).color(color);
         buffer.vertex(matrix, ix2, iy2, 0.0F).color(color);
         buffer.vertex(matrix, ix1, iy1, 0.0F).color(color);
      }

      BufferDraw.draw(buffer);
      if (cullWasEnabled) {
         RenderSystem.enableCull();
      } else {
         RenderSystem.disableCull();
      }

      RenderSystem.disableBlend();
   }

   private void drawRadialLine(DrawContext context, float cx, float cy, float innerR, float outerR, float angleDeg, int color) {
      float rad = (float)Math.toRadians(angleDeg);
      float cos = MathHelper.cos(rad);
      float x2 = cx + outerR * cos;
      float x1;
      float dx = x2 - (x1 = cx + innerR * cos);
      float y1;
      float sin;
      float y2;
      float dy;
      float length = MathHelper.sqrt(dx * dx + (dy = (y2 = cy + outerR * (sin = MathHelper.sin(rad))) - (y1 = cy + innerR * sin)) * dy);
      if (!(length <= 0.001F)) {
         float lineWidth = 1.0F;
         float perpX = -dy / length * lineWidth;
         float perpY = dx / length * lineWidth;
         Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, x1 + perpX, y1 + perpY, 0.0F).color(color);
         buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0.0F).color(color);
         buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0.0F).color(color);
         buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0.0F).color(color);
         buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0.0F).color(color);
         buffer.vertex(matrix, x2 - perpX, y2 - perpY, 0.0F).color(color);
         BufferDraw.draw(buffer);
         RenderSystem.disableBlend();
      }
   }

   private static void drawWrappedCenteredText(MatrixStack matrices, FontRenderer font, String text, float centerX, float y, float maxWidth, int color) {
      if (text != null && !text.isBlank()) {
         if (font.getStringWidth(text) <= maxWidth) {
            font.drawCenteredString(matrices, text, centerX, y, color);
         } else {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            float lineY = y;
            float lineHeight = font.getStringHeight("Ag") + 2.0F;

            for (String word : words) {
               String candidate = line.isEmpty() ? word : line + " " + word;
               if (font.getStringWidth(candidate) > maxWidth && !line.isEmpty()) {
                  font.drawCenteredString(matrices, line.toString(), centerX, lineY, color);
                  line = new StringBuilder(word);
                  lineY += lineHeight;
               } else {
                  if (!line.isEmpty()) {
                     line.append(' ');
                  }

                  line.append(word);
               }
            }

            if (!line.isEmpty()) {
               font.drawCenteredString(matrices, line.toString(), centerX, lineY, color);
            }
         }
      }
   }

   public void close() {
      this.manager.closeWheel(false);
   }

   public EmotionType getHoveredEmotion() {
      return this.hoveredSlot >= 0 && this.hoveredSlot < WHEEL_SLOTS.length ? WHEEL_SLOTS[this.hoveredSlot] : null;
   }

   private String getBindName() {
      Globals globals = Instance.get(Globals.class);
      if (globals == null) {
         return "кнопку";
      } else {
         int key = globals.getEmotionWheelBind().getKey();
         if (key == -1) {
            return "кнопку";
         } else {
            String translation = Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
            return translation != null && !translation.isBlank() ? translation : "кнопку";
         }
      }
   }

   private void drawEntityPreview(DrawContext context, float x, float y, float size, float yaw, float pitch, LivingEntity entity) {
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(x, y, 1050.0);
      matrices.scale(1.0F, 1.0F, -1.0F);
      matrices.scale(size, size, size);
      float pitchRad = (float)Math.toRadians(pitch);
      Quaternionf cameraRotation = new Quaternionf().rotateX(-pitchRad);
      Quaternionf entityRotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraRotation);
      matrices.multiply(entityRotation);
      float prevBodyYaw = entity.bodyYaw;
      float prevYaw = entity.getYaw();
      float prevPitch = entity.getPitch();
      float prevHeadYaw = entity.headYaw;
      float prevPrevHeadYaw = entity.prevHeadYaw;
      float prevPrevBodyYaw = entity.prevBodyYaw;
      float prevPrevYaw = entity.prevYaw;
      float prevPrevPitch = entity.prevPitch;
      float bodyYaw = MathHelper.wrapDegrees(yaw);
      float modelPitch = -pitch * 0.9F;
      entity.bodyYaw = bodyYaw;
      entity.prevBodyYaw = bodyYaw;
      entity.setYaw(bodyYaw);
      entity.prevYaw = bodyYaw;
      entity.setPitch(modelPitch);
      entity.prevPitch = modelPitch;
      entity.headYaw = bodyYaw;
      entity.prevHeadYaw = bodyYaw;
      EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      dispatcher.setRotation(new Quaternionf(cameraRotation).conjugate());
      dispatcher.setRenderShadows(false);
      RenderSystem.enableDepthTest();
      Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

      try {
         dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, matrices, vertexConsumers, 15728880);
         if (vertexConsumers instanceof Immediate) {
            vertexConsumers.draw();
         } else {
            context.draw();
         }
      } catch (Throwable var26) {
         LogManager.getLogger("Elixir").error("Failed to render emotion wheel entity preview", var26);
      }

      dispatcher.setRotation(new Quaternionf());
      dispatcher.setRenderShadows(true);
      RenderSystem.disableDepthTest();
      entity.bodyYaw = prevBodyYaw;
      entity.prevBodyYaw = prevPrevBodyYaw;
      entity.setYaw(prevYaw);
      entity.prevYaw = prevPrevYaw;
      entity.setPitch(prevPitch);
      entity.prevPitch = prevPrevPitch;
      entity.headYaw = prevHeadYaw;
      entity.prevHeadYaw = prevPrevHeadYaw;
      matrices.pop();
   }
}
