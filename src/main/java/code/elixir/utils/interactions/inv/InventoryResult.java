package code.elixir.utils.interactions.inv;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record InventoryResult(int slot, boolean found, ItemStack stack) {
   private static final InventoryResult NOT_FOUND_RESULT = new InventoryResult(-1, false, null);

   public static InventoryResult notFound() {
      return NOT_FOUND_RESULT;
   }

   @NotNull
   public static InventoryResult inOffhand(ItemStack stack) {
      return new InventoryResult(999, true, stack);
   }

   public boolean isHolding() {
      return QuickImports.mc.player == null ? false : QuickImports.mc.player.getInventory().selectedSlot == this.slot;
   }

   public boolean isInHotBar() {
      return this.slot < 9;
   }

   public boolean isInLeftHand() {
      return (this.slot > 36 ? this.slot : this.slot - 36) == 40;
   }

   public void switchTo() {
      if (this.found && this.isInHotBar()) {
         InventoryToolkit.switchTo(this.slot);
      }
   }

   public void switchToSilent() {
      if (this.found && this.isInHotBar()) {
         InventoryToolkit.switchToSilent(this.slot);
      }
   }
}
