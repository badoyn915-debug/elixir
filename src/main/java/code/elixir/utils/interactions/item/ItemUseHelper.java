package code.elixir.utils.interactions.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

public final class ItemUseHelper {
   private ItemUseHelper() {
   }

   public static boolean isPriorityUseItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         UseAction action = stack.getUseAction();
         return action == UseAction.EAT
            || action == UseAction.DRINK
            || action == UseAction.BOW
            || action == UseAction.CROSSBOW
            || action == UseAction.SPYGLASS
            || action == UseAction.BLOCK;
      } else {
         return false;
      }
   }

   public static boolean isConsumable(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         UseAction action = stack.getUseAction();
         return action == UseAction.EAT || action == UseAction.DRINK;
      } else {
         return false;
      }
   }

   public static boolean isUsingPriorityItem(PlayerEntity player) {
      return player != null && player.isUsingItem() ? isPriorityUseItem(player.getActiveItem()) : false;
   }

   public static ItemStack getStackForHand(PlayerEntity player, Hand hand) {
      return player != null && hand != null ? player.getStackInHand(hand) : ItemStack.EMPTY;
   }
}
