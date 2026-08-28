package code.elixir.utils.features.combat;

import code.elixir.mixins.ClientPlayerEntityAccessor;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public final class LegitTestSprintReset implements QuickImports {
   private LegitTestSprintReset() {
   }

   public static void prepareForAttack() {
      if (mc.player != null) {
         mc.options.sprintKey.setPressed(false);
         if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
         }

         ClientPlayerEntityAccessor accessor = (ClientPlayerEntityAccessor)mc.player;
         if (accessor.getLastSprinting() && mc.player.networkHandler != null) {
            accessor.setLastSprinting(false);
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
         }
      }
   }
}
