package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class XCarry extends Module {
   public XCarry() {
      super("XCarry", ModuleCategory.MISC);
   }

   @EventHandler
   public void onS(PacketEvent e) {
      Packet<?> class_25962 = e.getPacket();
      CloseHandledScreenC2SPacket zalupa2;
      if (class_25962 instanceof CloseHandledScreenC2SPacket && (zalupa2 = (CloseHandledScreenC2SPacket)class_25962).getSyncId() == 0) {
         e.cancel();
      }
   }
}
