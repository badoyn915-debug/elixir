package code.elixir.features.impl.movement.autoMystGlider.handers;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.elixir.features.impl.movement.autoMystGlider.states.GlideState;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.packet.Packet;

public class NoClipHandler implements QuickImports {
   private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
   private final ConnectionState connectionState;
   private final GlideState glideState;
   private long lastPulseTime;
   private long fullyDoneTime;

   public NoClipHandler(GlideState glideState, ConnectionState connectionState) {
      this.glideState = glideState;
      this.connectionState = connectionState;
   }

   public void onPacket(PacketEvent packetEvent) {
      if (this.isDone()) {
         if (PlayerInteractionHelper.nullCheck()) {
            return;
         }

         if (packetEvent.isSend()) {
            this.packets.add(packetEvent.getPacket());
            packetEvent.cancel();
         }
      }
   }

   public void onTick() {
      if (mc.player != null) {
         if (this.isDone()) {
            if (this.lastPulseTime == -1L) {
               this.start();
            }

            InternalNoclipFly.get().onTick();
            this.update();
         } else {
            this.end();
         }
      }
   }

   public void onMove(MoveEvent moveEvent) {
      if (mc.player != null) {
         if (this.isDone()) {
            InternalNoclipFly.get().onMove(moveEvent);
         }
      }
   }

   private boolean isDone() {
      if (this.glideState.fullyDone()) {
         if (this.fullyDoneTime == -1L) {
            this.fullyDoneTime = System.currentTimeMillis();
         }
      } else {
         this.fullyDoneTime = -1L;
      }

      return System.currentTimeMillis() - this.fullyDoneTime > 3000L && !this.connectionState.isReconnected();
   }

   public void start() {
      if (mc.player != null) {
         this.lastPulseTime = System.currentTimeMillis();
      }
   }

   public void end() {
      InternalNoclipFly.get().reset();
      if (!this.packets.isEmpty()) {
         this.packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
         this.packets.clear();
      }

      this.lastPulseTime = -1L;
   }

   public void update() {
      if (!PlayerInteractionHelper.nullCheck()) {
         long now = System.currentTimeMillis();
         if (now - this.lastPulseTime >= 400L) {
            this.packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            this.packets.clear();
            this.lastPulseTime = now;
         }
      }
   }
}
