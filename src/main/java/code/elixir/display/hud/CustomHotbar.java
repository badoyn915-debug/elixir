package code.elixir.display.hud;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class CustomHotbar extends AbstractDraggable {
   private static final float STATUS_BAR_Y_OFFSET = 39.0F;
   private static final float SLOT_SIZE = 20.0F;
   private static final float SLOT_SPACING = 2.0F;
   private static final float PANEL_PADDING = 3.0F;
   private static final int HOTBAR_SLOTS = 9;
   private static final float OFFHAND_GAP = 6.0F;
   private final Map<Integer, Animation> slotAnimations = new HashMap<>();

   public static CustomHotbar.HotbarLayout computeLayout(int screenWidth, int screenHeight) {
      float slotsWidth = 196.0F;
      float panelHeight = 26.0F;
      float hotbarPanelWidth = 6.0F + slotsWidth;
      float hotbarPanelX = (screenWidth - hotbarPanelWidth) / 2.0F;
      float hotbarPanelY = screenHeight - panelHeight - 2.0F;
      float offhandPanelWidth = 26.0F;
      float offhandPanelX = hotbarPanelX - 6.0F - offhandPanelWidth;
      if (offhandPanelX < 1.0F) {
         float shift = 1.0F - offhandPanelX;
         offhandPanelX += shift;
         hotbarPanelX += shift;
      }

      float rightEdge;
      if ((rightEdge = hotbarPanelX + hotbarPanelWidth) > screenWidth - 1.0F) {
         float shift = rightEdge - (screenWidth - 1.0F);
         offhandPanelX -= shift;
         hotbarPanelX -= shift;
      }

      return new CustomHotbar.HotbarLayout(offhandPanelX, hotbarPanelX, hotbarPanelY, hotbarPanelWidth, panelHeight);
   }

   public static float getArmorBarGap() {
      return 6.0F;
   }

   public CustomHotbar() {
      super("CustomHotbar", 0, 0, 197, 24, false);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);

      for (int i = 0; i < 9; i++) {
         this.slotAnimations.put(i, new Decelerate().setMs(200).setValue(0.0));
      }
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("CustomHotbar") && Hud.getInstance().state;
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         int currentSlot = mc.player.getInventory().selectedSlot;

         for (int i = 0; i < 9; i++) {
            Animation anim = this.slotAnimations.get(i);
            if (i == currentSlot) {
               anim.setDirection(Direction.FORWARDS);
            } else {
               anim.setDirection(Direction.BACKWARDS);
            }
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.visible() && mc.player != null) {
         MatrixStack matrix = context.getMatrices();
         int screenWidth = window.getScaledWidth();
         int screenHeight = window.getScaledHeight();
         CustomHotbar.HotbarLayout layout = computeLayout(screenWidth, screenHeight);
         float offhandPanelX = layout.offhandPanelX();
         float hotbarPanelX = layout.hotbarPanelX();
         float hotbarPanelY = layout.hotbarPanelY();
         float hotbarPanelWidth = layout.hotbarPanelWidth();
         float panelHeight = layout.panelHeight();
         float offhandPanelWidth = 26.0F;
         this.setX((int)offhandPanelX);
         this.setY((int)hotbarPanelY);
         this.setWidth((int)(hotbarPanelX + hotbarPanelWidth - offhandPanelX));
         this.setHeight((int)panelHeight);
         this.clampToScreen();
         this.drawExperienceLevel(matrix, screenWidth, screenHeight);
         this.drawPanelBackground(matrix, offhandPanelX, hotbarPanelY, offhandPanelWidth, panelHeight);
         this.drawOffhand(context, matrix, offhandPanelX + 3.0F, hotbarPanelY + 3.0F, 20.0F);
         this.drawPanelBackground(matrix, hotbarPanelX, hotbarPanelY, hotbarPanelWidth, panelHeight);
         int selectedSlot = mc.player.getInventory().selectedSlot;
         float rowY = hotbarPanelY + 3.0F;
         float hotbarStartX = hotbarPanelX + 3.0F;
         FontRenderer slotNumberFont = Fonts.getSize(20, Fonts.Type.SuisseIntlMedium);

         for (int i = 0; i < 9; i++) {
            float animValue = this.slotAnimations.get(i).getOutput().floatValue();
            float slotX = hotbarStartX + i * 22.0F;
            boolean isSelected = i == selectedSlot;
            float offset = animValue * -2.0F;
            matrix.push();
            float slotY;
            matrix.translate(slotX + 10.0F, (slotY = rowY + offset) + 10.0F, 0.0F);
            float scale = 1.0F + animValue * 0.1F;
            matrix.scale(scale, scale, 1.0F);
            matrix.translate(-(slotX + 10.0F), -(slotY + 10.0F), 0.0F);
            if (isSelected) {
               int alpha = (int)(255.0F * (0.3F + animValue * 0.7F));
               rectangle.render(
                  ShapeProperties.create(matrix, slotX, slotY, 20.0, 20.0)
                     .round(4.0F)
                     .thickness(0.5F)
                     .outlineColor(ColorAssist.multAlpha(ColorAssist.getHudColor(), alpha))
                     .softness(2.5F)
                     .color(ColorAssist.multAlpha(ThemeManager.BackgroundSettings.getColor(), 0.5F + animValue * 0.5F))
                     .build()
               );
            } else {
               rectangle.render(ShapeProperties.create(matrix, slotX, slotY, 20.0, 20.0).round(4.0F).color(ThemeManager.BackgroundSettings.getColor()).build());
            }

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
               Render2D.defaultDrawStack(context, stack, slotX + 2.0F, slotY + 2.0F, true, true, 1.0F);
            } else {
               String slotNumber = String.valueOf(i + 1);
               int textAlpha = isSelected ? 200 : 80;
               float slotCenterX = slotX + 10.0F;
               slotNumberFont.drawCenteredBoth(matrix, slotNumber, slotCenterX, slotY, 20.0F, new Color(255, 255, 255, textAlpha).getRGB());
            }

            matrix.pop();
         }
      }
   }

   private void drawPanelBackground(MatrixStack matrix, float x, float y, float width, float height) {
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), x, y, width, height, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
      }

      rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build());
   }

   private void drawOffhand(DrawContext context, MatrixStack matrix, float slotX, float slotY, float slotSize) {
      rectangle.render(
         ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize)
            .round(4.0F)
            .thickness(0.5F)
            .outlineColor(ColorAssist.multAlpha(ColorAssist.getHudColor(), 120.0F))
            .softness(2.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      ItemStack offhand = mc.player.getOffHandStack();
      if (!offhand.isEmpty()) {
         Render2D.defaultDrawStack(context, offhand, slotX + 2.0F, slotY + 2.0F, true, true, 1.0F);
      } else {
         FontRenderer iconFont = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
         String label = "L";
         iconFont.drawCenteredBoth(matrix, label, slotX + slotSize / 2.0F, slotY, slotSize, new Color(255, 255, 255, 70).getRGB());
      }
   }

   private void drawExperienceLevel(MatrixStack matrix, int screenWidth, int screenHeight) {
      int xpLevel = mc.player.experienceLevel;
      if (xpLevel > 0) {
         FontRenderer levelFont = Fonts.getSize(16, Fonts.Type.SuisseIntlMedium);
         String levelText = String.valueOf(xpLevel);
         float levelBoxY = screenHeight - 39.0F;
         levelFont.drawCenteredBoth(matrix, levelText, screenWidth / 2.0F, levelBoxY, 9.0F, new Color(126, 252, 80).getRGB());
      }
   }

   public record HotbarLayout(float offhandPanelX, float hotbarPanelX, float hotbarPanelY, float hotbarPanelWidth, float panelHeight) {
   }
}
