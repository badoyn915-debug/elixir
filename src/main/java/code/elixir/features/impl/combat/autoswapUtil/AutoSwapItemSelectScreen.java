package code.elixir.features.impl.combat.autoswapUtil;

import code.elixir.features.impl.combat.AutoSwap;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.shape.ShapeProperties;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class AutoSwapItemSelectScreen extends Screen implements QuickImports {
   private final AutoSwap autoSwap;
   private final int wheelSlotIndex;
   private final int rows = 4;
   private final int cols = 9;
   private final int slotSize = 18;
   private final int slotPadding = 4;

   public AutoSwapItemSelectScreen(AutoSwap autoSwap, int wheelSlotIndex) {
      super(Text.literal("Выбор предмета"));
      this.autoSwap = autoSwap;
      this.wheelSlotIndex = wheelSlotIndex;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(new AutoSwapWheelScreen(this.autoSwap));
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

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.client != null && this.client.player != null) {
         int panelW = 214;
         int panelH = 128;
         int x = (this.width - panelW) / 2;
         int y = (this.height - panelH) / 2;
         rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, panelW, panelH).color(new Color(20, 20, 25, 230).getRGB()).round(8.0F).build());
         context.drawTextWithShadow(this.textRenderer, Text.literal("ЛКМ – выбрать предмет для слота " + this.wheelSlotIndex), x + 10, y + 8, -1);
         int invX = x + 8;
         int invYStart = y + 24;
         PlayerInventory inv = this.client.player.getInventory();

         for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
               int index = row * 9 + col;
               int sx = invX + col * 22;
               int sy = invYStart + row * 22;
               rectangle.render(
                  ShapeProperties.create(context.getMatrices(), sx, sy, 18.0, 18.0).color(new Color(35, 35, 45, 200).getRGB()).round(4.0F).build()
               );
               ItemStack stack = inv.getStack(index);
               if (!stack.isEmpty()) {
                  context.drawItem(stack, sx + 1, sy + 1);
               }
            }
         }

         super.render(context, mouseX, mouseY, delta);
      } else {
         super.render(context, mouseX, mouseY, delta);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.client != null && this.client.player != null) {
         if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
         } else {
            int panelW = 214;
            int panelH = 128;
            int x = (this.width - panelW) / 2;
            int y = (this.height - panelH) / 2;
            int invX = x + 8;
            int invYStart = y + 24;
            PlayerInventory inv = this.client.player.getInventory();

            for (int row = 0; row < 4; row++) {
               for (int col = 0; col < 9; col++) {
                  int index = row * 9 + col;
                  int sx = invX + col * 22;
                  int sy = invYStart + row * 22;
                  if (mouseX >= sx && mouseX <= sx + 18 && mouseY >= sy && mouseY <= sy + 18) {
                     ItemStack stack = inv.getStack(index);
                     if (!stack.isEmpty()) {
                        this.applyItem(stack);
                     }

                     return true;
                  }
               }
            }

            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void applyItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         String itemName = stack.getName().getString();
         this.autoSwap.setWheelSlotItem(this.wheelSlotIndex, stack.getItem(), itemName);
         this.close();
      }
   }

   private String mapItemName(Item item) {
      if (item == Items.TOTEM_OF_UNDYING) {
         return "Totem of Undying";
      } else if (item == Items.PLAYER_HEAD) {
         return "Player Head";
      } else if (item == Items.GOLDEN_APPLE) {
         return "Golden Apple";
      } else {
         return item == Items.SHIELD ? "Shield" : null;
      }
   }
}
