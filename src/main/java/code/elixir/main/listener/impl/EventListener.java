package code.elixir.main.listener.impl;

import code.elixir.Elixir;
import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.main.listener.Listener;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class EventListener implements Listener {
   public static boolean serverSprint;
   public static int selectedSlot;

   @EventHandler
   public void onTick(TickEvent e) {
      Network.tick();
      InventoryFlowManager.tick();
      Elixir.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof ClientCommandC2SPacket command) {
         serverSprint = switch (command.getMode()) {
            case START_SPRINTING -> true;
            case STOP_SPRINTING -> false;
            default -> serverSprint;
         };
      } else if (packet instanceof UpdateSelectedSlotC2SPacket slot) {
         selectedSlot = slot.getSelectedSlot();
      }

      Network.packet(e);
      if (Elixir.getInstance() != null) {
         if (Elixir.getInstance().getAttackPerpetrator() != null) {
            Elixir.getInstance().getAttackPerpetrator().onPacket(e);
         }
         if (Elixir.getInstance().getDraggableRepository() != null) {
            Elixir.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
         }
      }
   }

   @EventHandler
   public void onUsingItemEvent(UsingItemEvent e) {
      Elixir.getInstance().getAttackPerpetrator().onUsingItem(e);
   }
}
