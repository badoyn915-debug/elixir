package code.elixir.utils.features.aura.utils;

import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Pressing implements QuickImports {
   private long lastClickTime = System.currentTimeMillis();

   public boolean isCooldownComplete(boolean syncTps, int ticks) {
      boolean isMace = this.isHoldingMace();
      boolean cooldownReady = isMace || mc.player.getAttackCooldownProgress(ticks) > 0.9F;
      long delay = syncTps ? (long)(450.0F * (20.0F / Math.max(Network.TPS, 0.1F))) : 450L;
      return cooldownReady && this.lastClickPassed() >= delay;
   }

   public long lastClickPassed() {
      return System.currentTimeMillis() - this.lastClickTime;
   }

   public void recalculate() {
      this.lastClickTime = System.currentTimeMillis();
   }

   private boolean isHoldingMace() {
      ItemStack mainHand = mc.player.getMainHandStack();
      return mainHand.isOf(Items.MACE);
   }
}
