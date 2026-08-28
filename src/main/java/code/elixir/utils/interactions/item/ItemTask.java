package code.elixir.utils.interactions.item;

import code.elixir.display.hud.CoolDowns;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemTask implements QuickImports {
   public static int maxUseTick(Item item) {
      return maxUseTick(item.getDefaultStack());
   }

   public static int maxUseTick(ItemStack stack) {
      return switch (stack.getUseAction()) {
         case EAT, DRINK -> 32;
         case CROSSBOW, SPEAR -> 10;
         case BOW -> 20;
         case BLOCK -> 0;
         default -> stack.getMaxUseTime(mc.player);
      };
   }

   public static float getCooldownProgress(Item item) {
      CoolDowns coolDowns = CoolDowns.getInstance();
      return coolDowns != null
         ? coolDowns.list
            .stream()
            .filter(c -> c.item().equals(item))
            .findFirst()
            .map(coolDown -> mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack()) ? (float)(-coolDown.time().elapsedTime() / 1000.0) : 0.0F)
            .orElse(0.0F)
         : 0.0F;
   }

   private ItemTask() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
