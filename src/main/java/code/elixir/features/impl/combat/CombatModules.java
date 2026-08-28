package code.elixir.features.impl.combat;

import code.elixir.features.impl.movement.ElytraTarget;
import code.elixir.features.impl.player.NoDelay;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.util.math.MathHelper;

public final class CombatModules implements QuickImports {
   public static AttackAura getAura() {
      return AttackAura.getInstance();
   }

   public static ElytraResolver getElytraResolver() {
      return ElytraResolver.getInstance();
   }

   public static ElytraTarget getElytraTarget() {
      return ElytraTarget.getInstance();
   }

   public static PacketCriticals getPacketCriticals() {
      return PacketCriticals.getInstance();
   }

   public static NoDelay getNoJumpDelay() {
      return NoDelay.getInstance();
   }

   public static boolean isElytraPredictActive() {
      ElytraTarget elytraTarget = getElytraTarget();
      return elytraTarget != null && elytraTarget.isOvertakeActive() && mc.player != null && mc.player.isGliding();
   }

   public static float getElytraForward() {
      ElytraTarget elytraTarget = getElytraTarget();
      return elytraTarget == null ? 0.0F : elytraTarget.getPredictStrength();
   }

   public static int getElytraForwardTicks() {
      return Math.max(0, Math.round(getElytraForward()));
   }

   public static long getAdjustedCooldown(long baseCooldown, boolean enabled) {
      if (!enabled) {
         return baseCooldown;
      } else {
         float tps = MathHelper.clamp(Network.TPS, 0.1F, 20.0F);
         if (tps >= 20.0F) {
            return baseCooldown;
         } else {
            float multiplier = 20.0F / tps;
            float additionalFactor = 1.0F + (20.0F - tps) * 0.05F;
            return Math.min((long)((float)baseCooldown * multiplier * additionalFactor), 3000L);
         }
      }
   }

   private CombatModules() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
