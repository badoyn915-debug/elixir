package code.elixir.features.impl.movement;

import code.elixir.events.block.BlockCollisionEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class NoClip extends Module {
   private List<Packet<?>> packets = new CopyOnWriteArrayList<>();
   private Box box;
   private int tickCounter;

   public NoClip() {
      super("NoClip", ModuleCategory.MOVEMENT);
   }

   private boolean shouldPhase() {
      if (mc.player != null && mc.world != null) {
         Box hitbox = mc.player.getBoundingBox();
         BlockPos min = new BlockPos((int)Math.floor(hitbox.minX), (int)Math.floor(hitbox.minY), (int)Math.floor(hitbox.minZ));
         BlockPos max = new BlockPos((int)Math.floor(hitbox.maxX), (int)Math.floor(hitbox.maxY), (int)Math.floor(hitbox.maxZ));

         for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
               for (int z = min.getZ(); z <= max.getZ(); z++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  BlockState state = mc.world.getBlockState(pos);
                  if (!state.isAir()
                     && mc.world
                        .getBlockState(pos)
                        .getCollisionShape(mc.world, pos)
                        .getBoundingBoxes()
                        .stream()
                        .anyMatch(box -> box.intersects(hitbox.offset(-pos.getX(), -pos.getY(), -pos.getZ())))) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void resumePackets() {
      if (mc.player != null && mc.world != null && !this.state) {
         if (!this.packets.isEmpty()) {
            for (Packet<?> packet : new ArrayList<>(this.packets)) {
               mc.getNetworkHandler().sendPacket(packet);
            }

            this.packets.clear();
            this.box = mc.player.getBoundingBox();
         }
      }
   }

   @EventHandler
   public void onCollide(BlockCollisionEvent e) {
      if (this.state) {
         BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
         if (!e.getBlockPos().equals(playerPos.down())) {
            e.setState(Blocks.AIR.getDefaultState());
         }
      }
   }

   private void adjustToSneakingSpeed() {
      if (mc.player != null && this.state) {
         double SNEAK_SPEED_MULTIPLIER = 0.3;
         Vec3d motion = mc.player.getVelocity();
         double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
         if (horizontalSpeed > 0.0) {
            double baseSpeed = 0.6;
            double targetSpeed = baseSpeed * 0.3;
            double scale = targetSpeed / horizontalSpeed;
            mc.player.setVelocity(motion.x * scale, motion.y, motion.z * scale);
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.state) {
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         boolean onGround = mc.player.isOnGround();
         if (event.getType() == PacketEvent.Type.SEND) {
            Packet<?> p = event.getPacket();
            if (this.shouldPhase() && !(p instanceof KeepAliveC2SPacket) && !(p instanceof CommonPongC2SPacket)) {
               this.packets.add(p);
               event.cancel();
            }
         }

         if (event.getType() == PacketEvent.Type.RECEIVE && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.resumePackets();
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new Full(x - 1000.0, y, z - 1000.0, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new Full(x, y, z, yaw, pitch, mc.player.isOnGround(), false));
            mc.player
               .networkHandler
               .sendPacket(
                  new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false)
               );
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.resumePackets();
      this.box = null;
      this.tickCounter = 0;
   }

   public List<Packet<?>> getPackets() {
      return this.packets;
   }

   public Box getBox() {
      return this.box;
   }

   public int getTickCounter() {
      return this.tickCounter;
   }
}
