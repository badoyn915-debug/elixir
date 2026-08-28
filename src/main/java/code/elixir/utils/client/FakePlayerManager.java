package code.elixir.utils.client;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FakePlayerManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final UUID FAKE_UUID = UUID.nameUUIDFromBytes("ElixirFakePlayer".getBytes(StandardCharsets.UTF_8));
   private static final String DEFAULT_NAME = "FakePlayer";
   private static OtherClientPlayerEntity fakePlayer;

   private FakePlayerManager() {
   }

   public static boolean add(String name) {
      if (mc.world != null && mc.player != null) {
         remove();
         String cleanName = sanitizeName(name);
         GameProfile profile = new GameProfile(FAKE_UUID, cleanName);
         OtherClientPlayerEntity entity = new OtherClientPlayerEntity(mc.world, profile);
         float yawRad = mc.player.getYaw() * (float) Math.PI / 180.0F;
         Vec3d forward = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad)).normalize();
         Vec3d pos = mc.player.getPos().add(forward.multiply(2.5));
         entity.refreshPositionAndAngles(pos.x, mc.player.getY(), pos.z, mc.player.getYaw() + 180.0F, 0.0F);
         entity.headYaw = entity.getYaw();
         entity.bodyYaw = entity.getYaw();
         entity.setPose(EntityPose.STANDING);
         entity.setHealth(entity.getMaxHealth());
         entity.setAbsorptionAmount(0.0F);
         entity.setInvulnerable(true);
         entity.setCustomName(Text.literal(cleanName));
         entity.setCustomNameVisible(true);
         entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
         mc.world.addEntity(entity);
         fakePlayer = entity;
         return true;
      } else {
         return false;
      }
   }

   public static boolean remove() {
      if (fakePlayer == null) {
         return false;
      } else {
         if (mc.world != null) {
            mc.world.removeEntity(fakePlayer.getId(), RemovalReason.DISCARDED);
         }

         fakePlayer = null;
         return true;
      }
   }

   public static boolean handleAttack(Entity target) {
      if (!isFakePlayer(target)) {
         return false;
      } else {
         restoreHealth();
         return true;
      }
   }

   public static boolean isSpawned() {
      return fakePlayer != null && !fakePlayer.isRemoved();
   }

   public static boolean isFakePlayer(Entity entity) {
      return fakePlayer != null && entity != null && entity.getId() == fakePlayer.getId();
   }

   public static boolean isFakePlayer(UUID uuid) {
      return fakePlayer != null && uuid != null && uuid.equals(fakePlayer.getUuid());
   }

   private static void restoreHealth() {
      if (fakePlayer != null) {
         fakePlayer.setHealth(fakePlayer.getMaxHealth());
         fakePlayer.setAbsorptionAmount(0.0F);
         fakePlayer.hurtTime = 0;
         fakePlayer.maxHurtTime = 0;
      }
   }

   private static String sanitizeName(String name) {
      if (name != null && !name.isBlank()) {
         String cleanName = name.trim();
         if (cleanName.length() > 16) {
            cleanName = cleanName.substring(0, 16);
         }

         return cleanName;
      } else {
         return "FakePlayer";
      }
   }
}
