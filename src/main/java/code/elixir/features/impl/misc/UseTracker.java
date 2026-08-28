package code.elixir.features.impl.misc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.math.time.StopWatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public class UseTracker extends Module {
   private final MultiSelectSetting notifyMode = new MultiSelectSetting("Трекать", "Какие уведомления показывать")
      .value("Снос тотема", "Полученные зелья", "Съеденный предмет")
      .selected("Снос тотема", "Полученные зелья", "Съеденный предмет");
   private final Map<UUID, Map<String, StatusEffectInstance>> playerEffects = new HashMap<>();
   private static final Map<UUID, Boolean> enchantedTotems = new HashMap<>();
   private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
   private final Map<UUID, Integer> useStartTick = new HashMap<>();
   private final StopWatch updateTimer = new StopWatch();
   private static final long UPDATE_INTERVAL_MS = 500L;

   public UseTracker() {
      super("UseTracker", ModuleCategory.MISC);
      this.setup(new Setting[]{this.notifyMode});
      this.updateTimer.reset();
   }

   private boolean allowTotem() {
      return this.notifyMode.isSelected("Снос тотема");
   }

   private boolean allowPotions() {
      return this.notifyMode.isSelected("Полученные зелья");
   }

   private boolean allowConsume() {
      return this.notifyMode.isSelected("Съеденный предмет");
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.allowConsume()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (player != null && (mc.player == null || !player.getUuid().equals(mc.player.getUuid()))) {
                  UUID id = player.getUuid();
                  boolean using = player.isUsingItem();
                  if (using) {
                     if (!this.activeUseItem.containsKey(id)) {
                        this.activeUseItem.put(id, player.getActiveItem().copy());
                        this.useStartTick.put(id, player.age);
                     }
                  } else {
                     ItemStack used = this.activeUseItem.remove(id);
                     Integer startTick = this.useStartTick.remove(id);
                     if (used != null && !used.isEmpty() && startTick != null) {
                        UseAction action = used.getUseAction();

                        String verb;
                        if ((verb = switch (action) {
                           case DRINK -> "выпил";
                           case EAT -> "съел";
                           default -> null;
                        }) != null) {
                           int var25 = player.age - startTick;
                           int minDuration = 31;
                           if (var25 >= 31) {
                              String itemName = used.getName().getString().replaceAll("§.", "");
                              String effectsStr = this.getPotionEffectsStringWithDuration(used);
                              String playerName = player.getName().getString();
                              Object effectsPart = effectsStr.isEmpty() ? "" : " §8(§7" + effectsStr + "§8)";
                              String chatMsg = "§r" + playerName + "§7 " + verb + " §r" + itemName + ((String)effectsPart).replace("§8", "§7") + "§r";
                              ChatMessage.brandmessage(chatMsg);
                              if (this.shouldSendHudNotification("Использовании предмета")) {
                                 String notificationMessage = effectsStr.isEmpty()
                                    ? playerName + " " + verb + " " + itemName
                                    : playerName + " " + verb + " " + itemName + " (" + effectsStr.replaceAll("§.", "") + ")";
                                 Notifications.getInstance()
                                    .addList(Text.literal(notificationMessage), 3500, SoundManager.SETTING_INTERACT, false, used.copyWithCount(1));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.updateTimer.finished(500.0)) {
            this.updateTimer.reset();

            for (PlayerEntity playerx : mc.world.getPlayers()) {
               if (playerx != null && (mc.player == null || !playerx.getUuid().equals(mc.player.getUuid()))) {
                  UUID playerId = playerx.getUuid();
                  ArrayList<StatusEffectInstance> currentEffectsCopy = new ArrayList<>(playerx.getStatusEffects());
                  HashMap<String, StatusEffectInstance> currentEffectsMap = new HashMap<>();

                  for (StatusEffectInstance e : currentEffectsCopy) {
                     String key = ((StatusEffect)e.getEffectType().value()).getTranslationKey() + ":" + e.getAmplifier();
                     currentEffectsMap.put(key, e);
                  }

                  this.playerEffects.put(playerId, currentEffectsMap);
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.world != null) {
         Packet<?> class_25962 = e.getPacket();
         EntityStatusS2CPacket statusPacket;
         if (class_25962 instanceof EntityStatusS2CPacket && (statusPacket = (EntityStatusS2CPacket)class_25962).getStatus() == 35) {
            if (!(statusPacket.getEntity(mc.world) instanceof PlayerEntity player)) {
               return;
            }

            if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) {
               return;
            }

            boolean isEnchanted = EnchantmentHelper.hasEnchantments(player.getOffHandStack()) || EnchantmentHelper.hasEnchantments(player.getMainHandStack());
            enchantedTotems.put(player.getUuid(), isEnchanted);
            if (this.allowTotem()) {
               String message = "§r" + player.getName().getString() + "§7 потерял тотем бессмертия, зачарован: " + (isEnchanted ? "§a" : "§c") + "⬤§r";
               ChatMessage.brandmessage(message);
               if (this.shouldSendHudNotification("Сносе тотема")) {
                  Notifications.getInstance()
                     .addList(
                        Text.literal(player.getName().getString() + " потерял тотем"),
                        3500,
                        SoundManager.DISABLE_MODULE,
                        false,
                        Items.TOTEM_OF_UNDYING.getDefaultStack()
                     );
               }
            }
         }

         if (e.getPacket() instanceof EntityStatusEffectS2CPacket) {
            EntityStatusEffectS2CPacket effectPacket = (EntityStatusEffectS2CPacket)e.getPacket();
            if (this.allowPotions()) {
               if (!(mc.world.getEntityById(effectPacket.getEntityId()) instanceof PlayerEntity player)) {
                  return;
               }

               if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) {
                  return;
               }

               UUID playerId = player.getUuid();
               Map currentEffectsMap = this.playerEffects.getOrDefault(playerId, new HashMap<>());
               ArrayList<StatusEffectInstance> currentEffectsCopy = new ArrayList<>(player.getStatusEffects());
               HashMap<String, StatusEffectInstance> updatedEffectsMap = new HashMap<>();

               for (StatusEffectInstance effect : currentEffectsCopy) {
                  String key = ((StatusEffect)effect.getEffectType().value()).getTranslationKey() + ":" + effect.getAmplifier();
                  updatedEffectsMap.put(key, effect);
               }

               String newEffectKey = ((StatusEffect)effectPacket.getEffectId().value()).getTranslationKey() + ":" + effectPacket.getAmplifier();
               StatusEffectInstance newEffect = new StatusEffectInstance(
                  effectPacket.getEffectId(),
                  effectPacket.getDuration(),
                  effectPacket.getAmplifier(),
                  effectPacket.isAmbient(),
                  effectPacket.shouldShowParticles(),
                  effectPacket.shouldShowIcon()
               );
               updatedEffectsMap.put(newEffectKey, newEffect);
               ArrayList<UseTracker.SpecialPotionType> detectedPotions = new ArrayList<>();
               HashSet<String> newEffectKeys = new HashSet<>(Set.of(newEffectKey));
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.KILLER,
                  "effect.minecraft.strength:3",
                  "effect.minecraft.resistance:0"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.URINE,
                  "effect.minecraft.jump_boost:0",
                  "effect.minecraft.speed:2"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.MEDIC,
                  "effect.minecraft.health_boost:2",
                  "effect.minecraft.regeneration:2"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.BURP,
                  "effect.minecraft.blindness:0",
                  "effect.minecraft.glowing:0",
                  "effect.minecraft.hunger:9",
                  "effect.minecraft.slowness:2",
                  "effect.minecraft.wither:4"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.FLASH,
                  "effect.minecraft.blindness:0",
                  "effect.minecraft.glowing:0"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.SULFURIC_ACID,
                  "effect.minecraft.poison:1",
                  "effect.minecraft.slowness:3",
                  "effect.minecraft.weakness:2",
                  "effect.minecraft.wither:4"
               );
               this.create(
                  updatedEffectsMap,
                  newEffectKeys,
                  detectedPotions,
                  UseTracker.SpecialPotionType.WINNER,
                  "effect.minecraft.health_boost:1",
                  "effect.minecraft.invisibility:0",
                  "effect.minecraft.regeneration:1",
                  "effect.minecraft.resistance:0"
               );
               if (!detectedPotions.isEmpty()) {
                  for (UseTracker.SpecialPotionType type : detectedPotions) {
                     ItemStack potionStack = new ItemStack(Items.POTION);
                     potionStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(type.getDisplayName()));
                     this.displayPotionNotificationFromEffect(
                        player,
                        potionStack,
                        type.getEffects()
                           .stream()
                           .map(d -> new StatusEffectInstance(d.getEffect(), d.getDurationTicks(), d.getAmplifier(), false, false, true))
                           .collect(Collectors.toList())
                     );
                  }
               } else {
                  String localizedName = Text.translatable(((StatusEffect)effectPacket.getEffectId().value()).getTranslationKey())
                     .getString()
                     .replaceAll("§.", "");
                  String duration = this.getPotionDurationString(newEffect, 1);
                  int amplifier = effectPacket.getAmplifier();
                  int level = Math.max(0, amplifier) + 1;
                  ChatMessage.brandmessage("§r" + player.getName().getString() + "§7 получил §f" + localizedName + " " + level + "§r на §7" + duration + "§r");
                  if (this.shouldSendHudNotification("Полученном зелье")) {
                     ItemStack potionStack = new ItemStack(Items.POTION);
                     potionStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(localizedName + " " + level));
                     Notifications.getInstance()
                        .addList(
                           Text.literal(player.getName().getString() + " получил " + localizedName + " " + level + " (" + duration + ")"),
                           3500,
                           SoundManager.NOTIF,
                           false,
                           potionStack
                        );
                  }
               }

               this.playerEffects.put(playerId, updatedEffectsMap);
            }
         }
      }
   }

   private boolean create(
      Map<String, StatusEffectInstance> currentEffectsMap,
      Set<String> newOrRefreshedKeys,
      List<UseTracker.SpecialPotionType> detectedPotions,
      UseTracker.SpecialPotionType type,
      String... comboKeys
   ) {
      HashSet<String> comboSet = new HashSet<>(Arrays.asList(comboKeys));
      boolean allPresent = comboSet.stream().allMatch(currentEffectsMap::containsKey);
      boolean atLeastOneNew = comboSet.stream().anyMatch(newOrRefreshedKeys::contains);
      if (allPresent && atLeastOneNew) {
         detectedPotions.add(type);
         newOrRefreshedKeys.removeAll(comboSet);
         return true;
      } else {
         return false;
      }
   }

   private void displayPotionNotificationFromEffect(PlayerEntity player, ItemStack itemStack, List<StatusEffectInstance> appliedEffects) {
      String cleanName = itemStack.getName().getString().replaceAll("§.", "");
      ChatMessage.brandmessage("§r" + player.getName().getString() + "§7 получил §r" + cleanName);
      if (this.shouldSendHudNotification("Полученном зелье")) {
         Notifications.getInstance()
            .addList(Text.literal(player.getName().getString() + " получил " + cleanName), 3500, SoundManager.NOTIF, false, itemStack.copy());
      }
   }

   private boolean shouldSendHudNotification(String settingName) {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.notificationSettings != null) {
         String normalized = settingName;
         if (settingName.contains("Использовании")) {
            normalized = "Использовании предмета";
         }

         if (normalized.contains("Сносе")) {
            normalized = "Сносе тотема";
         }

         if (normalized.contains("Получ")) {
            normalized = "Полученном зелье";
         }

         return hud.notificationSettings.isSelected(normalized);
      } else {
         return false;
      }
   }

   private String getPotionDurationString(StatusEffectInstance effect, int multiplier) {
      if (effect.isInfinite()) {
         return "∞";
      } else {
         int duration = effect.getDuration() / multiplier;
         int seconds = duration / 20;
         int minutes = seconds / 60;
         int var6;
         return minutes > 0 ? minutes + "m " + seconds + "s" : (var6 = seconds % 60) + "s";
      }
   }

   private String getPotionEffectsString(ItemStack stack) {
      PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
      if (component == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) {
               sb.append(", ");
            }

            String name = Text.translatable(((StatusEffect)eff.getEffectType().value()).getTranslationKey()).getString();
            int level = eff.getAmplifier() + 1;
            sb.append(name).append(" ").append(level);
         }

         return sb.toString();
      }
   }

   private String getPotionEffectsStringWithDuration(ItemStack stack) {
      PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
      if (component == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) {
               sb.append("§8, §7");
            }

            String name = Text.translatable(((StatusEffect)eff.getEffectType().value()).getTranslationKey()).getString().replaceAll("§.", "");
            int amplifier = eff.getAmplifier();
            int level = Math.max(0, amplifier) + 1;
            String duration = this.getPotionDurationString(eff, 1);
            sb.append(name).append(" ").append(level).append("§r на §7").append(duration);
         }

         return sb.toString();
      }
   }

   public static enum SpecialPotionType {
      FLASH(
         16777215,
         "§6[★] §eВспышка",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.BLINDNESS, 20, 0),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.GLOWING, 240, 0)
         ),
         Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)
      ),
      KILLER(
         13369344,
         "§4[★] §cЗелье Киллера",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.RESISTANCE, 180, 0),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.STRENGTH, 90, 3)
         ),
         Arrays.asList(12582912, 15007744, 14024704)
      ),
      BURP(
         16737792,
         "§c[★] §6Зелье Отрыжки",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.BLINDNESS, 10, 0),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.GLOWING, 180, 0),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.HUNGER, 90, 9),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.SLOWNESS, 180, 2),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.WITHER, 30, 4)
         ),
         Arrays.asList(16727040, 16733184, 16739072)
      ),
      SULFURIC_ACID(
         10092339,
         "§2[★] §aСерная кислота",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.POISON, 50, 1),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.SLOWNESS, 90, 3),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.WEAKNESS, 90, 2),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.WITHER, 30, 4)
         ),
         Arrays.asList(9961472, 10223411, 10027007)
      ),
      MEDIC(
         16711935,
         "§5[★] §dЗелье Медика",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.HEALTH_BOOST, 45, 2),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.REGENERATION, 45, 2)
         ),
         Arrays.asList(16716287, 16707583, 14680063)
      ),
      WINNER(
         65280,
         "§2[★] §aЗелье Победителя",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.HEALTH_BOOST, 180, 1),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.INVISIBILITY, 900, 0),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.REGENERATION, 60, 1),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.RESISTANCE, 60, 0)
         ),
         Arrays.asList(59136, 57088, 63232, 65310, 65350)
      ),
      URINE(
         65280,
         "§3[★] §bМоча Флеша",
         Arrays.asList(
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.JUMP_BOOST, 120, 1),
            new UseTracker.SpecialPotionType.PotionEffectData(StatusEffects.SPEED, 120, 2)
         ),
         Arrays.asList(65535)
      );

      private final Integer baseColor;
      private final String displayName;
      private final List<UseTracker.SpecialPotionType.PotionEffectData> effects;
      private final List<Integer> colorVariations;

      public Integer getBaseColor() {
         return this.baseColor;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public List<UseTracker.SpecialPotionType.PotionEffectData> getEffects() {
         return this.effects;
      }

      public List<Integer> getColorVariations() {
         return this.colorVariations;
      }

      private SpecialPotionType(
         Integer baseColor, String displayName, List<UseTracker.SpecialPotionType.PotionEffectData> effects, List<Integer> colorVariations
      ) {
         this.baseColor = baseColor;
         this.displayName = displayName;
         this.effects = effects;
         this.colorVariations = colorVariations;
      }

      public static class PotionEffectData {
         private final RegistryEntry<StatusEffect> effect;
         private final Integer durationSeconds;
         private final Integer amplifier;

         public Integer getDurationTicks() {
            return this.durationSeconds * 20;
         }

         public RegistryEntry<StatusEffect> getEffect() {
            return this.effect;
         }

         public Integer getDurationSeconds() {
            return this.durationSeconds;
         }

         public Integer getAmplifier() {
            return this.amplifier;
         }

         public PotionEffectData(RegistryEntry<StatusEffect> effect, Integer durationSeconds, Integer amplifier) {
            this.effect = effect;
            this.durationSeconds = durationSeconds;
            this.amplifier = amplifier;
         }
      }
   }
}
