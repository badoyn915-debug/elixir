package code.elixir.features.impl.movement;

import code.elixir.events.player.InputEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraRecast extends Module {
   public ElytraRecast() {
      super("ElytraRecast", "Elytra Recast", ModuleCategory.MOVEMENT);
      this.setup(new Setting[0]);
   }

   private boolean wearingElytra() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         return chest.isOf(Items.ELYTRA);
      }
   }

   private boolean grimContext() {
      return true;
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (!PlayerInteractionHelper.nullCheck() && this.wearingElytra() && this.grimContext()) {
         if (mc.player.isOnGround()) {
            event.setJumping(true);
         } else if (!mc.player.isGliding()) {
            PlayerInteractionHelper.startFallFlying();
         }
      }
   }
}
