package code.elixir.features.impl.misc;

import code.elixir.events.keyboard.KeyEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ShulkerBypass extends Module {
   public ShulkerBypass() {
      super("ShulkerBypass", ModuleCategory.MISC);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (mc.currentScreen instanceof ShulkerBoxScreen screen) {
         if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            Slot hoveredSlot = this.getSlotUnderMouse(screen);
            if (hoveredSlot != null && hoveredSlot.hasStack()) {
               int emptySlot = this.findEmptyHotbarOrInventorySlot();
               if (emptySlot != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoveredSlot.id, emptySlot, SlotActionType.SWAP, mc.player);
               }
            }
         }
      }
   }

   private Slot getSlotUnderMouse(HandledScreen<?> screen) {
      double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
      double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

      for (Slot slot : screen.getScreenHandler().slots) {
         if (this.isPointOverSlot(slot, mouseX, mouseY, screen)) {
            return slot;
         }
      }

      return null;
   }

   private boolean isPointOverSlot(Slot slot, double pointX, double pointY, HandledScreen<?> screen) {
      int x = screen.x + slot.x;
      int y = screen.y + slot.y;
      return pointX >= x - 1 && pointX < x + 16 + 1 && pointY >= y - 1 && pointY < y + 16 + 1;
   }

   private int findEmptyHotbarOrInventorySlot() {
      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (!slot.hasStack() && slot.inventory == mc.player.getInventory()) {
            return slot.getIndex();
         }
      }

      return -1;
   }
}
