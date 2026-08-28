package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.EntitySpawnEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.FakePlayerManager;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AntiBot extends Module {
   private static final Set<UUID> BOTS = new HashSet<>();
   private final SelectSetting mode = new SelectSetting("Мод", "Режим определения ботов").value("Matrix", "UniAC", "Reallyworld");
   private final BooleanSetting removeFromWorld = new BooleanSetting("Удалять из мира", "Удаляет ботов UniAC из мира")
      .setValue(false)
      .visible(() -> this.mode.isSelected("UniAC"));

   public AntiBot() {
      super("AntiBot", "Anti Bot", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.mode, this.removeFromWorld});
   }

   public static AntiBot getInstance() {
      return Instance.get(AntiBot.class);
   }

   public static boolean isBot(PlayerEntity entity) {
      if (entity == null) {
         return false;
      } else {
         return FakePlayerManager.isFakePlayer(entity) ? false : BOTS.contains(entity.getUuid());
      }
   }

   public static boolean checkBot(LivingEntity entity) {
      PlayerEntity player;
      return entity instanceof PlayerEntity && isBot(player = (PlayerEntity)entity);
   }

   public boolean isBot(UUID uuid) {
      return FakePlayerManager.isFakePlayer(uuid) ? false : BOTS.contains(uuid);
   }

   @EventHandler
   public void onEntitySpawn(EntitySpawnEvent event) {
      if (event.getEntity() instanceof PlayerEntity player && mc.player != null) {
         if (player != mc.player) {
            this.markBot(player, this.evaluate(player));
         }
      }
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      BOTS.clear();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.isState() && mc.player != null && mc.world != null) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player) {
               boolean isBot = this.evaluate(player);
               this.markBot(player, isBot);
               if (this.mode.isSelected("UniAC") && this.removeFromWorld.isValue() && isBot) {
                  mc.world.removeEntity(player.getId(), RemovalReason.DISCARDED);
               }
            }
         }
      }
   }

   @Override
   public void activate() {
      super.activate();
      if (mc.world != null) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player) {
               this.markBot(player, this.evaluate(player));
            }
         }
      }
   }

   @Override
   public void deactivate() {
      BOTS.clear();
      super.deactivate();
   }

   private void markBot(PlayerEntity player, boolean bot) {
      if (bot) {
         BOTS.add(player.getUuid());
      } else {
         BOTS.remove(player.getUuid());
      }
   }

   private boolean evaluate(PlayerEntity player) {
      if (!(player instanceof AbstractClientPlayerEntity)) {
         return false;
      } else if (player == mc.player) {
         return false;
      } else if (this.mode.isSelected("Matrix")) {
         return this.isMatrixBot(player);
      } else {
         return this.mode.isSelected("UniAC") ? this.isUniAcBot(player) : this.isReallyworldBot(player);
      }
   }

   private boolean isMatrixBot(PlayerEntity player) {
      String name = player.getGameProfile().getName();
      UUID expected = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
      return !player.getUuid().equals(expected);
   }

   private boolean isUniAcBot(PlayerEntity player) {
      if (FriendUtils.isFriend(player)) {
         return false;
      } else if (player.isSpectator()) {
         return false;
      } else {
         boolean bootsEmpty = this.getArmor(player, 0).isOf(Items.AIR);
         boolean leggingsEmpty = this.getArmor(player, 1).isOf(Items.AIR);
         boolean chestEmpty = this.getArmor(player, 2).isOf(Items.AIR);
         boolean helmetEmpty = this.getArmor(player, 3).isOf(Items.AIR);
         boolean bootsFilled = !bootsEmpty;
         boolean leggingsFilled = !leggingsEmpty;
         boolean chestFilled = !chestEmpty;
         boolean helmetFilled = !helmetEmpty;
         boolean bootsRule = bootsEmpty || bootsFilled && !this.isEnchantable(this.getArmor(player, 0));
         boolean leggingsRule = leggingsEmpty || leggingsFilled && !this.isEnchantable(this.getArmor(player, 1));
         boolean chestRule = chestEmpty || chestFilled && !this.isEnchantable(this.getArmor(player, 2));
         boolean helmetRule = helmetEmpty || helmetFilled && !this.isEnchantable(this.getArmor(player, 3));
         boolean allArmorDamaged = !bootsEmpty
            && this.getArmor(player, 0).isDamaged()
            && !leggingsEmpty
            && this.getArmor(player, 1).isDamaged()
            && !chestEmpty
            && this.getArmor(player, 2).isDamaged()
            && !helmetEmpty
            && this.getArmor(player, 3).isDamaged();
         String name = player.getName().getString();
         boolean shortName = name.length() <= 6;
         boolean suspiciousName = name.matches("^[a-z]{6}$");
         boolean fullArmorCheck = bootsRule && leggingsRule && chestRule && helmetRule;
         boolean nameCheck = shortName || suspiciousName;
         boolean hasTablistEntry = mc.getNetworkHandler() != null
            && mc.getNetworkHandler().getPlayerList().stream().anyMatch(info -> info.getProfile().getName().equals(name));
         return nameCheck && !allArmorDamaged && fullArmorCheck && hasTablistEntry;
      }
   }

   private boolean isReallyworldBot(PlayerEntity player) {
      GameProfile profile = player.getGameProfile();
      String name = profile.getName();
      if (name.contains("NPC")) {
         return true;
      } else {
         return player.isInvisible() ? true : profile.getProperties().isEmpty();
      }
   }

   private boolean isEnchantable(ItemStack stack) {
      return !stack.isEmpty() && stack.isEnchantable();
   }

   private ItemStack getArmor(PlayerEntity player, int slot) {
      return player.getInventory().getArmorStack(slot);
   }
}
