package code.elixir.utils.interactions.interact;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.packet.network.ServerUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class PlayerInteractionHelper implements QuickImports {
   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
   }

   public static void interactItem(Hand hand) {
      interactItem(hand, MathAngle.cameraAngle());
   }

   public static void interactItem(Hand hand, Turns angle) {
      sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
   }

   public static void interactEntity(Entity entity) {
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, false, Hand.MAIN_HAND, entity.getBoundingBox().getCenter()));
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, false, Hand.MAIN_HAND));
   }

   public static void startFallFlying() {
      mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
      mc.player.startGliding();
   }

   public static void sendPacketWithOutEvent(Packet<?> packet) {
      mc.getNetworkHandler().getConnection().send(packet, null);
   }

   public static void grimSuperBypass$$$(double y, Turns angle) {
      mc.player
         .networkHandler
         .sendPacket(
            new Full(
               mc.player.getX(),
               mc.player.getY() + y,
               mc.player.getZ(),
               angle.getYaw(),
               angle.getPitch(),
               mc.player.isOnGround(),
               mc.player.horizontalCollision
            )
         );
   }

   public static String getHealthString(LivingEntity entity) {
      return getHealthString(getHealth(entity));
   }

   public static String getHealthString(float hp) {
      return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
   }

   public static float getHealth(LivingEntity entity) {
      float hp = ServerUtil.getHealthValue(entity) + entity.getAbsorptionAmount();
      return MathHelper.clamp(hp, 0.0F, ServerUtil.getMaxHealth(entity) + entity.getAbsorptionAmount());
   }

   public static void jump() {
      if (mc.player.isSprinting()) {
         float g = mc.player.getYaw() * (float) (Math.PI / 180.0);
         mc.player.addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2F, 0.0, MathHelper.cos(g) * 0.2F));
      }

      mc.player.velocityDirty = true;
   }

   public static List<BlockPos> getCube(BlockPos center, float radius) {
      return getCube(center, radius, radius, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
      return getCube(center, radiusXZ, radiusY, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
      ArrayList<BlockPos> positions = new ArrayList<>();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      int posY = down ? centerY - (int)radiusY : centerY;

      for (int x = centerX - (int)radiusXZ; x <= centerX + radiusXZ; x++) {
         for (int z = centerZ - (int)radiusXZ; z <= centerZ + radiusXZ; z++) {
            for (int y = posY; y <= centerY + radiusY; y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
      ArrayList<BlockPos> positions = new ArrayList<>();

      for (int x = start.getX(); x <= end.getX(); x++) {
         for (int z = start.getZ(); z <= end.getZ(); z++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static Stream<Entity> streamEntities() {
      return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
   }

   public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
      return mc.player.getWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
   }

   public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
      return mc.player.getActiveStatusEffects().containsKey(statusEffect);
   }

   public static boolean isPlayerInBlock(Block block) {
      return isBoxInBlock(mc.player.getBoundingBox().expand(-0.001), block);
   }

   public static boolean isBoxInBlock(Box box, Block block) {
      return isBox(box, pos -> mc.world.getBlockState(pos).getBlock().equals(block));
   }

   public static boolean isBoxInBlocks(Box box, List<Block> blocks) {
      return isBox(box, pos -> blocks.contains(mc.world.getBlockState(pos).getBlock()));
   }

   public static boolean isBox(Box box, Predicate<BlockPos> pos) {
      return BlockPos.stream(box).anyMatch(pos);
   }

   public static boolean isKey(BindSetting setting) {
      if (mc.currentScreen == null && setting.isVisible()) {
         int key = setting.getKey();
         if (!setting.isCombo()) {
            return isKey(getKeyType(key), key);
         } else {
            int secondKey = setting.getSecondKey();
            return isKey(getKeyType(key), key) && isKey(getKeyType(secondKey), secondKey);
         }
      } else {
         return false;
      }
   }

   public static boolean isKey(KeyBinding key) {
      return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
   }

   public static boolean isKey(Type type, int keyCode) {
      if (keyCode != -1) {
         switch (type) {
            case KEYSYM:
               return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case MOUSE:
               return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
         }
      }

      return false;
   }

   public static boolean isAir(BlockPos blockPos) {
      return isAir(mc.world.getBlockState(blockPos));
   }

   public static boolean isAir(BlockState state) {
      return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }

   public static Vec3d getSafePositionNearChest(BlockPos chest, Direction side) {
      if (mc.world == null) {
         return null;
      } else {
         for (BlockPos p : new BlockPos[]{chest.offset(side), chest.up(), chest.offset(side).up(), chest.up(2)}) {
            if (!mc.world.getBlockState(p).isSolid()) {
               return Vec3d.ofCenter(p);
            }
         }

         return null;
      }
   }

   public static void interactBlockChestWithInBlockBypass(BlockPos chest, Vec3d center, Direction side) {
      Vec3d safe = getSafePositionNearChest(chest, side);
      if (safe != null) {
         mc.player.networkHandler.sendPacket(new Full(safe.x, safe.y, safe.z, mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
      }

      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, chest, false));
   }

   private PlayerInteractionHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
