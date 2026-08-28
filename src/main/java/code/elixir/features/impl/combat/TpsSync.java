package code.elixir.features.impl.combat;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.packet.network.Network;
import net.minecraft.util.math.MathHelper;

public class TpsSync extends Module {
   public static TpsSync getInstance() {
      return Instance.get(TpsSync.class);
   }

   public TpsSync() {
      super("TpsSync", "Синхронизация с TPS сервера", ModuleCategory.COMBAT);
   }

   public float getCurrentTPS() {
      return MathHelper.clamp(Network.TPS, 0.1F, 20.0F);
   }

   public long getAdjustedCooldown(long baseCooldown) {
      if (!this.isState()) {
         return baseCooldown;
      } else {
         float tps = this.getCurrentTPS();
         if (tps >= 20.0F) {
            return baseCooldown;
         } else {
            float multiplier = 20.0F / tps;
            float additionalFactor = 1.0F + (20.0F - tps) * 0.05F;
            long adjusted = (long)((float)baseCooldown * multiplier * additionalFactor);
            return Math.min(adjusted, 3000L);
         }
      }
   }

   public boolean canAttack(long lastAttackTime, long baseCooldown, long currentTime) {
      if (!this.isState()) {
         return currentTime >= lastAttackTime + baseCooldown;
      } else {
         long adjustedCooldown = this.getAdjustedCooldown(baseCooldown);
         return currentTime >= lastAttackTime + adjustedCooldown;
      }
   }
}
