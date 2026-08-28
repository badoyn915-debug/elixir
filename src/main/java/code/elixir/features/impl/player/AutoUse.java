package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemToolkit;
import code.elixir.utils.math.script.Script;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public class AutoUse extends Module {
   private final Script script = new Script();
   private final MultiSelectSetting multiSetting = new MultiSelectSetting("Авто использование", "Выберите, что будет использоваться")
      .value("Еды", "Невидимости");

   public AutoUse() {
      super("AutoUse", "AutoUse", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.multiSetting});
   }

   @Override
   public void deactivate() {
      this.script.update();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      for (String var4 : this.multiSetting.getSelected()) {
         switch (var4) {
            case "Еды":
               Slot slotx = InventoryTask.getFoodMaxSaturationSlot();
               if (slotx != null && mc.player.getHungerManager().isNotFull() && this.swapAndEat(slotx)) {
                  return;
               }
               break;
            case "Невидимости":
               Slot slot = InventoryTask.getPotion(StatusEffects.INVISIBILITY);
               if (slot != null && !PlayerInteractionHelper.isPotionActive(StatusEffects.INVISIBILITY) && this.swapAndEat(slot)) {
                  return;
               }
         }
      }

      this.script.update();
   }

   public boolean swapAndEat(Slot slot) {
      ItemStack stack = slot.getStack();
      if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
         if (!mc.player.getOffHandStack().equals(stack)) {
            if (InventoryFlowManager.script.isFinished()) {
               InventoryTask.swapHand(slot, Hand.OFF_HAND, true, true);
               this.script.cleanup().addTickStep(0, () -> InventoryTask.swapHand(slot, Hand.OFF_HAND, true, true));
            }
         } else {
            ItemToolkit.INSTANCE.useHand(Hand.OFF_HAND);
         }

         return true;
      } else {
         return false;
      }
   }
}
