package code.elixir.features.impl.combat.autoswapUtil;

import code.elixir.features.impl.combat.AutoSwap;
import code.elixir.utils.display.interfaces.QuickImports;
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
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class AutoSwapWheelScreen extends Screen implements QuickImports {
   private static final int SLOT_COUNT = 3;
   private static final float CENTER_Y_OFFSET = -6.0F;
   private static final float INNER_RING_R = 34.0F;
   private static final float OUTER_RING_R = 118.0F;
   private static final int SECTOR_BASE = -870703064;
   private static final int SECTOR_HOVER = -399163316;
   private static final int DIVIDER = 1442840575;
   private final AutoSwap autoSwap;
   private int hoveredSlot = -1;
   private final ItemStack[] displayStacks = new ItemStack[3];

   public AutoSwapWheelScreen(AutoSwap autoSwap) {
      super(Text.empty());
      this.autoSwap = autoSwap;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.hoveredSlot >= 0 && this.hoveredSlot < 3) {
         boolean slotEmpty = this.autoSwap.isWheelSlotEmpty(this.hoveredSlot);
         if (button == 0) {
            if (slotEmpty) {
               this.autoSwap.startSelectingItem(this.hoveredSlot);
            } else {
               ItemStack stack = this.displayStacks[this.hoveredSlot];
               if (stack != null && !stack.isEmpty()) {
                  this.close();
                  this.autoSwap.startSwapToItemStack(stack);
               }
            }

            return true;
         } else if (button == 1 && !slotEmpty) {
            this.autoSwap.clearWheelSlot(this.hoveredSlot);
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float centerX = this.width / 2.0F;
      float centerY = this.height / 2.0F + -6.0F;
      this.hoveredSlot = this.findHoveredSlot(mouseX, mouseY, centerX, centerY);
      context.fill(0, 0, this.width, this.height, 2080374784);
      float sectorSize = 120.0F;
      float startOffset = -90.0F;

      for (int i = 0; i < 3; i++) {
         float start = startOffset + i * sectorSize;
         float end = start + sectorSize;
         int color = i == this.hoveredSlot ? -399163316 : -870703064;
         this.drawRingSector(context, centerX, centerY, 34.0F, 118.0F, start, end, color);
      }

      for (int var13 = 0; var13 < 3; var13++) {
         this.drawRadialLine(context, centerX, centerY, 34.0F, 118.0F, startOffset + var13 * sectorSize, 1442840575);
      }

      this.renderCenterHub(context, centerX, centerY);
      this.renderSlotItems(context, centerX, centerY, startOffset, sectorSize);
      Fonts.getSize(11, Fonts.Type.SuisseIntlMedium)
         .drawCenteredString(context.getMatrices(), "ЛКМ — выбрать / добавить  |  ПКМ — очистить слот", centerX, centerY + 118.0F + 28.0F, -1800947253);
      super.render(context, mouseX, mouseY, delta);
   }

   private void renderCenterHub(DrawContext context, float centerX, float centerY) {
      float hub = 32.0F;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), centerX - hub, centerY - hub, hub * 2.0F, hub * 2.0F)
            .round(hub)
            .thickness(1.0F)
            .outlineColor(1157627903)
            .color(1428301872)
            .build()
      );
   }

   private void renderSlotItems(DrawContext context, float centerX, float centerY, float startOffset, float sectorSize) {
      if (this.client != null && this.client.player != null) {
         float iconRadius = 82.08F;

         for (int i = 0; i < 3; i++) {
            ItemStack stack;
            this.displayStacks[i] = stack = this.autoSwap.getWheelSlotStack(i);
            float midDeg = startOffset + i * sectorSize + sectorSize * 0.5F;
            double rad = Math.toRadians(midDeg);
            float ix = centerX + (float)Math.cos(rad) * iconRadius;
            float iy = centerY + (float)Math.sin(rad) * iconRadius;
            context.getMatrices().push();
            context.getMatrices().translate(ix, iy, 200.0F);
            if (!stack.isEmpty()) {
               context.getMatrices().scale(1.6F, 1.6F, 1.0F);
               context.drawItem(stack, -8, -8);
            } else {
               Fonts.getSize(18, Fonts.Type.SuisseIntlSemiBold).drawCenteredString(context.getMatrices(), "+", 0.0, -6.0, -1996488705);
            }

            context.getMatrices().pop();
         }
      }
   }

   private int findHoveredSlot(double mouseX, double mouseY, float centerX, float centerY) {
      float dx = (float)mouseX - centerX;
      float dy = (float)mouseY - centerY;
      float dist = MathHelper.sqrt(dx * dx + dy * dy);
      if (!(dist < 34.0F) && !(dist > 118.0F)) {
         float angleDeg = (float)((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0);
         float normalized = (angleDeg + 90.0F) % 360.0F;
         float sectorSize = 120.0F;
         int slot = (int)(normalized / sectorSize);
         return Math.min(slot, 2);
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
}
