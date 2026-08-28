package code.elixir.display.hud;

import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemBar extends AbstractDraggable {
   private int totemCount = 0;

   public TotemBar() {
      super("TotemBar", 10, 150, 29, 36, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("TotemBar") && Hud.getInstance().state;
   }

   @Override
   public void tick() {
      if (mc.player == null) {
         this.totemCount = 0;
      } else {
         this.totemCount = 0;

         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
               this.totemCount = this.totemCount + stack.getCount();
            }
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.visible()) {
         MatrixStack matrix = context.getMatrices();
         float slotSize = 26.0F;
         float padding = 4.0F;
         float textHeight = 12.0F;
         float totalWidth = slotSize + padding * 2.0F;
         float totalHeight = slotSize + textHeight + padding * 3.0F;
         this.setWidth((int)totalWidth);
         this.setHeight((int)totalHeight);
         float x = this.getX();
         float y = this.getY();
         Hud.getInstance();
         if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
            Render2D.rectangleWithMask(
               matrix.peek().getPositionMatrix(), x, y, totalWidth, totalHeight, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrix, x, y, totalWidth, totalHeight).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build()
         );
         float slotX = x + padding;
         float slotY = y + padding;
         rectangle.render(
            ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize).round(4.0F).color(ThemeManager.BackgroundSettings.getColor()).build()
         );
         ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
         float itemSize = 16.0F;
         float itemX = slotX + (slotSize - itemSize) / 2.0F - 1.0F;
         float itemY = slotY + (slotSize - itemSize) / 2.0F - 1.0F;
         Render2D.defaultDrawStack(context, totemStack, itemX, itemY, false, false, 1.0F);
         FontRenderer countFont = Fonts.getSize(21, Fonts.Type.SuisseIntlMedium);
         String countText = String.valueOf(this.totemCount);
         float textWidth = countFont.getStringWidth(countText);
         float countTextHeight = countFont.getStringHeight(countText);
         float textX = x + totalWidth / 2.0F - textWidth / 2.0F;
         float textY = slotY + slotSize + padding + (textHeight - countTextHeight) / 2.0F - 1.5F;
         int textColor = this.totemCount == 0
            ? new Color(255, 85, 75, 255).getRGB()
            : (this.totemCount < 5 ? new Color(255, 170, 0, 255).getRGB() : ThemeManager.textColor.getColor());
         countFont.drawString(matrix, countText, textX, textY, textColor);
      }
   }
}
