package code.elixir.features.impl.misc.creeperFarm;

import code.elixir.events.packet.PacketEvent;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.jetbrains.annotations.Nullable;

public class CreeperStorage implements QuickImports {
   Deque<Entity> entityDeque = new ArrayDeque<>();

   public void onTick() {
      this.entityDeque.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
   }

   public void onPacket(PacketEvent packetEvent) {
      Packet<?> class_25962 = packetEvent.getPacket();
      EntitySpawnS2CPacket packet;
      Entity entity;
      if (class_25962 instanceof EntitySpawnS2CPacket
         && (entity = mc.world.getEntityById((packet = (EntitySpawnS2CPacket)class_25962).getEntityId())) != null
         && entity.isAlive()
         && !entity.isRemoved()
         && !this.entityDeque.contains(entity)) {
         this.entityDeque.add(entity);
      }
   }

   @Nullable
   public Entity getTarget() {
      return !this.entityDeque.isEmpty() ? this.getClosestEntity() : null;
   }

   @Nullable
   private Entity getClosestEntity() {
      return this.entityDeque.stream().min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity))).orElse(null);
   }
}
