package code.elixir.features.impl.misc.serverhelper;

import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ServerHelperOverlay {
   private float posX = 30.0F;
   private float posY = 25.0F;

   public void render(DrawContext context, String title, List<ServerHelperRenderItem> items) {
      if (items != null && !items.isEmpty()) {
         MatrixStack matrices = context.getMatrices();
         float headerHeight = 14.0F;
         float itemHeight = 11.5F;
         float padding = 2.0F;
         float iconSize = 8.0F;
         float separator = 0.5F;
         float contentWidth = padding * 2.0F;
         float[] itemWidths = new float[items.size()];

         for (int i = 0; i < items.size(); i++) {
            ServerHelperRenderItem item = items.get(i);
            String bind = item.bindKey() != -1 ? StringHelper.getBindName(item.bindKey()) : "";
            float bindWidth = bind.isEmpty() ? 0.0F : Fonts.getSize(12).getStringWidth(bind);
            itemWidths[i] = 2.0F + iconSize + 2.0F + (bind.isEmpty() ? 0.0F : separator + 2.0F + bindWidth + 3.5F);
            contentWidth += itemWidths[i];
            if (i < items.size() - 1) {
               contentWidth += separator;
            }
         }

         float totalHeight = headerHeight + itemHeight + padding * 2.0F;
         int bg = ColorAssist.getColor(30, 25, 40, 220);
         int header = ColorAssist.getColor(30, 25, 40, 255);
         int text = ColorAssist.getColor(180, 140, 255, 255);
         Render2D.drawQuad(this.posX, this.posY, contentWidth, totalHeight, bg);
         Render2D.drawQuad(this.posX, this.posY, contentWidth, headerHeight, header);
         Fonts.getSize(15).drawString(matrices, title, this.posX + 4.5F, this.posY + 5.5F, text);
         float itemsY = this.posY + headerHeight + 1.0F;
         float currentX = this.posX + (contentWidth - contentWidth) / 2.0F + padding;

         for (int ix = 0; ix < items.size(); ix++) {
            ServerHelperRenderItem item = items.get(ix);
            float itemWidth = itemWidths[ix];
            int itemBg = ColorAssist.getColor(50, 50, 50, 178);
            Render2D.drawQuad(currentX, itemsY, itemWidth, itemHeight, itemBg);
            float iconX = currentX + 2.0F;
            float iconY = itemsY + (itemHeight - iconSize) / 2.0F;
            ItemStack stack = item.icon().getDefaultStack();
            if (item.hasTint()) {
               Render2D.defaultDrawStack(context, stack, iconX, iconY, false, false, 0.45F);
            } else {
               Render2D.defaultDrawStack(context, stack, iconX, iconY, false, false, 0.45F);
            }

            String bind = item.bindKey() != -1 ? StringHelper.getBindName(item.bindKey()) : "";
            if (!bind.isEmpty()) {
               float bindX = iconX + iconSize + 2.0F + separator + 2.0F;
               Fonts.getSize(12).drawString(matrices, bind, bindX, itemsY + 4.5F, text);
            }

            currentX += itemWidth + separator;
         }
      }
   }
}
