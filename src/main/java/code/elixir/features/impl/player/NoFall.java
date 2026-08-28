package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.Hand;

public class NoFall extends Module {
   public static NoFall getInstance() {
      return Instance.get(NoFall.class);
   }

   public NoFall() {
      super("NoFall", ModuleCategory.PLAYER);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player.fallDistance > 2.4) {
         mc.player
            .networkHandler
            .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-6, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, false));
         mc.player
            .networkHandler
            .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-6, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, false));
         mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
         mc.player.fallDistance = 0.0F;
      }
   }
}
