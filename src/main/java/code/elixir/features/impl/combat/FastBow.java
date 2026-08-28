package code.elixir.features.impl.combat;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class FastBow extends Module {
   private final SliderSettings distanceSetting = new SliderSettings("Дальность", "Сила натяжения лука и дальность полёта стрел")
      .setValue(50.0F)
      .range(10, 100)
      .step(5.0F);

   public FastBow() {
      super("FastBow", "FastBow", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.distanceSetting});
   }

   @Override
   public void deactivate() {
      if (mc.player != null && mc.player.isUsingItem() && isBowStack(mc.player.getActiveItem())) {
         mc.interactionManager.stopUsingItem(mc.player);
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.currentScreen == null) {
         if (mc.options.useKey.isPressed()) {
            Hand bowHand = this.getBowHand();
            if (bowHand != null && this.hasAmmo(bowHand)) {
               mc.itemUseCooldown = 0;
               if (mc.player.isUsingItem() && isBowStack(mc.player.getActiveItem())) {
                  if (mc.player.getItemUseTime() >= this.getRequiredChargeTicks()) {
                     mc.interactionManager.stopUsingItem(mc.player);
                  }
               } else {
                  if (!mc.player.isUsingItem()) {
                     mc.interactionManager.interactItem(mc.player, bowHand);
                  }
               }
            }
         }
      }
   }

   private Hand getBowHand() {
      if (mc.player.getMainHandStack().isOf(Items.BOW)) {
         return Hand.MAIN_HAND;
      } else {
         return mc.player.getOffHandStack().isOf(Items.BOW) ? Hand.OFF_HAND : null;
      }
   }

   private boolean hasAmmo(Hand bowHand) {
      if (mc.player.getAbilities().creativeMode) {
         return true;
      } else {
         ItemStack bowStack = mc.player.getStackInHand(bowHand);
         return !mc.player.getProjectileType(bowStack).isEmpty();
      }
   }

   private int getRequiredChargeTicks() {
      float power = MathHelper.clamp(this.distanceSetting.getValue() / 100.0F, 0.1F, 1.0F);
      return Math.max(3, Math.round(power * 20.0F));
   }

   private static boolean isBowStack(ItemStack stack) {
      return stack != null && stack.isOf(Items.BOW);
   }
}
