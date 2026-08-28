package code.elixir.utils.features.combat;

import code.elixir.events.player.InputEvent;
import code.elixir.main.listener.impl.EventListener;
import code.elixir.mixins.ClientPlayerEntityAccessor;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public final class LegitSprintReset implements QuickImports {
   private static boolean pendingInputReset;

   private LegitSprintReset() {
   }

   public static boolean isServerSprinting() {
      return mc.player == null ? false : EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
   }

   public static boolean prepareForAttack() {
      if (mc.player == null) {
         return true;
      } else if (!mc.player.isSubmergedInWater() && !mc.player.isTouchingWater() && !mc.player.isSwimming() && !mc.player.isGliding()) {
         ClientPlayerEntityAccessor accessor = (ClientPlayerEntityAccessor)mc.player;
         mc.options.sprintKey.setPressed(false);
         if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
         }

         if (mc.player.isOnGround()) {
            if (accessor.getLastSprinting()) {
               pendingInputReset = true;
               return false;
            } else {
               return true;
            }
         } else {
            if (accessor.getLastSprinting() && mc.player.networkHandler != null) {
               accessor.setLastSprinting(false);
               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
            }

            return !mc.player.isSprinting();
         }
      } else {
         return true;
      }
   }

   public static void applyInputReset(InputEvent event) {
      if (pendingInputReset && mc.player != null && mc.player.isOnGround()) {
         event.setForward(0.0F);
         event.setStrafe(0.0F);
         pendingInputReset = false;
      }
   }

   public static void reset() {
      pendingInputReset = false;
   }
}
