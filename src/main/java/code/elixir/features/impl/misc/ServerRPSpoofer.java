package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.math.time.TimerUtil;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

public class ServerRPSpoofer extends Module {
   private ServerRPSpoofer.ResourcePackAction currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
   private final TimerUtil counter = TimerUtil.create();

   public ServerRPSpoofer() {
      super("ServerRPSpoof", "RPSpoofer", ModuleCategory.MISC);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
         this.currentAction = ServerRPSpoofer.ResourcePackAction.ACCEPT;
         e.cancel();
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
      if (networkHandler != null) {
         if (this.currentAction == ServerRPSpoofer.ResourcePackAction.ACCEPT) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.ACCEPTED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.SEND;
            this.counter.resetCounter();
         } else if (this.currentAction == ServerRPSpoofer.ResourcePackAction.SEND && this.counter.isReached(300L)) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.SUCCESSFULLY_LOADED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
         }
      }
   }

   @Override
   public void deactivate() {
      this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
      super.deactivate();
   }

   public static enum ResourcePackAction {
      ACCEPT,
      SEND,
      WAIT;
   }
}
