package code.elixir.utils.interactions.path;

import code.elixir.utils.display.interfaces.QuickImports;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class BaritoneHelper implements QuickImports {
   private static final String BARITONE_MOD_ID = "baritone-meteor";
   private static final String DEFAULT_PREFIX = "#";
   private static final String[] SILENT_RENDER_SETTINGS = new String[]{
      "renderPath",
      "renderPathAsLine",
      "renderGoal",
      "renderGoalAnimated",
      "renderGoalXZBeacon",
      "renderGoalIgnoreDepth",
      "renderPathIgnoreDepth",
      "renderSelectionBoxes",
      "renderSelectionBoxesIgnoreDepth",
      "renderSelection",
      "renderSelectionIgnoreDepth",
      "renderSelectionCorners",
      "renderCachedChunks",
      "fadePath",
      "elytraRenderRaytraces",
      "elytraRenderHitboxRaytraces",
      "elytraRenderSimulation"
   };
   private static final String[] SILENT_BEHAVIOR_SETTINGS = new String[]{
      "allowBreak", "allowPlace", "autoTool", "walkWhileBreaking", "rightClickContainerOnArrival", "legitMine", "forceInternalMining"
   };
   private static final String[] SILENT_PROCESS_GETTERS = new String[]{
      "getMineProcess", "getBuilderProcess", "getExploreProcess", "getFarmProcess", "getGetToBlockProcess", "getElytraProcess", "getFollowProcess"
   };
   private static final Map<String, Object> silentModeBackup = new HashMap<>();
   private static boolean suppressChat;
   private static boolean silentModeActive;
   private static final Map<String, Object> mlgSettingsBackup = new HashMap<>();
   private static boolean mlgModeActive;

   public static boolean isBaritoneLoaded() {
      return FabricLoader.getInstance().isModLoaded("baritone-meteor");
   }

   public static boolean isBaritoneMessage(String message) {
      if (message != null && !message.isBlank()) {
         String lower = message.toLowerCase();
         return lower.contains("[baritone]") || lower.contains("baritone:");
      } else {
         return false;
      }
   }

   public static void setSilentMode(boolean enabled) {
      if (enabled) {
         if (!silentModeActive) {
            silentModeActive = true;
            silentModeBackup.clear();
         }

         applySilentMode();
      } else if (silentModeActive) {
         restoreSilentMode();
         silentModeActive = false;
      }
   }

   public static void gotoChestBlock(BlockPos pos) {
      Object baritone = getBaritone();
      if (baritone != null) {
         try {
            cancelAllProcesses(baritone);
            Object goal = Class.forName("baritone.api.pathing.goals.GoalGetToBlock").getConstructor(BlockPos.class).newInstance(pos);
            Object customGoal = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
            customGoal.getClass().getMethod("setGoalAndPath", goalClass).invoke(customGoal, goal);
         } catch (Throwable var5) {
         }
      }
   }

   public static void gotoPos(BlockPos pos) {
      Object baritone = getBaritone();
      if (baritone != null) {
         try {
            Object goal = Class.forName("baritone.api.pathing.goals.GoalBlock")
               .getConstructor(int.class, int.class, int.class)
               .newInstance(pos.getX(), pos.getY(), pos.getZ());
            Object customGoal = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
            customGoal.getClass().getMethod("setGoalAndPath", goalClass).invoke(customGoal, goal);
         } catch (Throwable var5) {
         }
      }
   }

   public static void stop() {
      Object baritone = getBaritone();
      if (baritone != null) {
         try {
            Object pathing = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathing.getClass().getMethod("forceCancel").invoke(pathing);
            cancelAllProcesses(baritone);
            Object input = baritone.getClass().getMethod("getInputOverrideHandler").invoke(baritone);
            input.getClass().getMethod("clearAllKeys").invoke(input);
         } catch (Throwable var3) {
         }
      }
   }

   public static boolean isPathing() {
      Object baritone = getBaritone();
      if (baritone == null) {
         return false;
      } else {
         try {
            Object pathing = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            return (Boolean)pathing.getClass().getMethod("isPathing").invoke(pathing);
         } catch (Throwable var2) {
            return false;
         }
      }
   }

   public static boolean handleChatCommand(String message) {
      if (message != null && !message.isBlank()) {
         String prefix = getCommandPrefix();
         if (!looksLikeBaritoneCommand(message, prefix)) {
            return false;
         } else if (!isBaritoneLoaded()) {
            notifyMissingBaritone();
            return true;
         } else {
            Object baritone = getBaritone();
            if (baritone == null) {
               notifyMissingBaritone();
               return true;
            } else {
               try {
                  Class<?> chatEventClass = Class.forName("baritone.api.event.events.ChatEvent");
                  Object chatEvent = chatEventClass.getConstructor(String.class).newInstance(message);
                  Object eventBus = baritone.getClass().getMethod("getGameEventHandler").invoke(baritone);
                  eventBus.getClass().getMethod("onSendChatMessage", chatEventClass).invoke(eventBus, chatEvent);
                  if ((Boolean)chatEventClass.getMethod("isCancelled").invoke(chatEvent)) {
                     return true;
                  }
               } catch (Throwable var6) {
               }

               executePrefixedCommand(message, prefix);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean looksLikeBaritoneCommand(String message, String prefix) {
      if (message.startsWith(prefix)) {
         return true;
      } else {
         try {
            String forcePrefix = (String)Class.forName("baritone.api.command.IBaritoneChatControl").getField("FORCE_COMMAND_PREFIX").get(null);
            return message.startsWith(forcePrefix);
         } catch (Throwable var3) {
            return message.startsWith("#");
         }
      }
   }

   private static String getCommandPrefix() {
      if (!isBaritoneLoaded()) {
         return "#";
      } else {
         try {
            Object settings = Class.forName("baritone.api.BaritoneAPI").getMethod("getSettings").invoke(null);
            Object prefixSetting = settings.getClass().getField("prefix").get(settings);
            Object value = prefixSetting.getClass().getField("value").get(prefixSetting);
            String prefix;
            if (value instanceof String && !(prefix = (String)value).isEmpty()) {
               return prefix;
            }
         } catch (Throwable var4) {
         }

         return "#";
      }
   }

   private static boolean executePrefixedCommand(String message, String prefix) {
      if (!message.startsWith(prefix)) {
         return false;
      } else {
         Object baritone = getBaritone();
         if (baritone == null) {
            return false;
         } else {
            executeBaritoneCommand(baritone, message.substring(prefix.length()));
            return true;
         }
      }
   }

   private static boolean executeBaritoneCommand(Object baritone, String command) {
      try {
         Object commandManager = baritone.getClass().getMethod("getCommandManager").invoke(baritone);
         return (Boolean)commandManager.getClass().getMethod("execute", String.class).invoke(commandManager, command);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void notifyMissingBaritone() {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(Formatting.RED + "Baritone не найден. Установите baritone-meteor для 1.21.4 в папку mods."), false);
      }
   }

   private static void applySilentMode() {
      if (isBaritoneLoaded()) {
         Object settings = getBaritoneSettings();
         if (settings != null) {
            for (String name : SILENT_RENDER_SETTINGS) {
               saveAndSetBooleanSetting(settings, name, false);
            }

            for (String name : SILENT_BEHAVIOR_SETTINGS) {
               saveAndSetBooleanSetting(settings, name, false);
            }

            saveAndSetBooleanSetting(settings, "freeLook", true);
            saveAndSetBooleanSetting(settings, "blockFreeLook", true);
            saveAndSetBooleanSetting(settings, "elytraFreeLook", true);
            saveAndSetBooleanSetting(settings, "smoothLook", false);
            saveAndSetBooleanSetting(settings, "elytraSmoothLook", false);
            saveAndSetBooleanSetting(settings, "remainWithExistingLookDirection", true);
            saveAndSetDoubleSetting(settings, "randomLooking", 0.0);
            saveAndSetDoubleSetting(settings, "randomLooking113", 0.0);
            protectChestBlocksFromBreaking(settings);
         }
      }
   }

   private static void protectChestBlocksFromBreaking(Object settings) {
      Object setting = getSetting(settings, "blocksToDisallowBreaking");
      if (setting != null) {
         Object current = readSettingValue(setting);
         if (current instanceof List currentList) {
            silentModeBackup.putIfAbsent("blocksToDisallowBreaking", new ArrayList(currentList));
            List blocks = (List)current;
            if (!blocks.contains(Blocks.CHEST)) {
               blocks.add(Blocks.CHEST);
            }

            if (!blocks.contains(Blocks.TRAPPED_CHEST)) {
               blocks.add(Blocks.TRAPPED_CHEST);
            }
         }
      }
   }

   private static void cancelAllProcesses(Object baritone) {
      try {
         Object customGoal = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
         customGoal.getClass().getMethod("onLostControl").invoke(customGoal);
      } catch (Throwable var7) {
      }

      for (String getter : SILENT_PROCESS_GETTERS) {
         try {
            Object process = baritone.getClass().getMethod(getter).invoke(baritone);
            process.getClass().getMethod("onLostControl").invoke(process);
         } catch (Throwable var6) {
         }
      }
   }

   private static void restoreSilentMode() {
      if (!silentModeBackup.isEmpty()) {
         Object settings = getBaritoneSettings();
         if (settings == null) {
            silentModeBackup.clear();
         } else {
            for (Entry<String, Object> entry : silentModeBackup.entrySet()) {
               restoreSetting(settings, entry.getKey(), entry.getValue());
            }

            silentModeBackup.clear();
         }
      }
   }

   private static void restoreListSetting(Object settings, String name, Object backup) {
      Object setting = getSetting(settings, name);
      if (setting != null && backup instanceof List backupList) {
         Object current = readSettingValue(setting);
         if (current instanceof List currentList) {
            List target = (List)current;
            target.clear();
            target.addAll(backupList);
         }
      }
   }

   private static Object getBaritoneSettings() {
      if (!isBaritoneLoaded()) {
         return null;
      } else {
         try {
            return Class.forName("baritone.api.BaritoneAPI").getMethod("getSettings").invoke(null);
         } catch (Throwable var1) {
            return null;
         }
      }
   }

   private static void saveAndSetBooleanSetting(Object settings, String name, boolean value) {
      Object setting = getSetting(settings, name);
      if (setting != null) {
         silentModeBackup.putIfAbsent(name, readSettingValue(setting));
         writeSettingValue(setting, value);
      }
   }

   private static void saveAndSetDoubleSetting(Object settings, String name, double value) {
      Object setting = getSetting(settings, name);
      if (setting != null) {
         silentModeBackup.putIfAbsent(name, readSettingValue(setting));
         writeSettingValue(setting, value);
      }
   }

   private static void restoreSetting(Object settings, String name, Object value) {
      if ("blocksToDisallowBreaking".equals(name)) {
         restoreListSetting(settings, name, value);
      } else {
         Object setting = getSetting(settings, name);
         if (setting != null && value != null) {
            writeSettingValue(setting, value);
         }
      }
   }

   private static Object getSetting(Object settings, String name) {
      try {
         return settings.getClass().getField(name).get(settings);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Object readSettingValue(Object setting) {
      try {
         return setting.getClass().getField("value").get(setting);
      } catch (Throwable var2) {
         return null;
      }
   }

   private static void writeSettingValue(Object setting, Object value) {
      try {
         setting.getClass().getField("value").set(setting, value);
      } catch (Throwable var3) {
      }
   }

   public static void startMlgToBlock(BlockPos groundBlock) {
      if (isBaritoneLoaded() && groundBlock != null) {
         if (!mlgModeActive) {
            mlgModeActive = true;
            mlgSettingsBackup.clear();
         }

         applyMlgSettings();
         gotoPos(groundBlock);
      }
   }

   public static void stopMlg() {
      if (mlgModeActive) {
         stop();
         restoreMlgSettings();
         mlgModeActive = false;
      }
   }

   public static boolean isPathingActive() {
      return isPathing();
   }

   private static void applyMlgSettings() {
      Object settings = getBaritoneSettings();
      if (settings != null) {
         saveAndSetBooleanSettingFor(settings, mlgSettingsBackup, "allowWaterBucketFall", true);
         saveAndSetBooleanSettingFor(settings, mlgSettingsBackup, "allowInventory", true);
         saveAndSetBooleanSettingFor(settings, mlgSettingsBackup, "freeLook", false);
         saveAndSetBooleanSettingFor(settings, mlgSettingsBackup, "smoothLook", true);
      }
   }

   private static void restoreMlgSettings() {
      if (!mlgSettingsBackup.isEmpty()) {
         Object settings = getBaritoneSettings();
         if (settings == null) {
            mlgSettingsBackup.clear();
         } else {
            for (Entry<String, Object> entry : mlgSettingsBackup.entrySet()) {
               restoreSetting(settings, entry.getKey(), entry.getValue());
            }

            mlgSettingsBackup.clear();
         }
      }
   }

   private static void saveAndSetBooleanSettingFor(Object settings, Map<String, Object> backup, String name, boolean value) {
      Object setting = getSetting(settings, name);
      if (setting != null) {
         backup.putIfAbsent(name, readSettingValue(setting));
         writeSettingValue(setting, value);
      }
   }

   private static Object getBaritone() {
      if (isBaritoneLoaded() && mc != null) {
         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object forPlayer;
            if (mc.player != null
               && (forPlayer = provider.getClass().getMethod("getBaritoneForPlayer", mc.player.getClass()).invoke(provider, mc.player)) != null) {
               return forPlayer;
            } else {
               Object forClient = provider.getClass().getMethod("getBaritoneForMinecraft", mc.getClass()).invoke(provider, mc);
               return forClient != null ? forClient : provider.getClass().getMethod("createBaritone", mc.getClass()).invoke(provider, mc);
            }
         } catch (Throwable var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private BaritoneHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static boolean isSuppressChat() {
      return suppressChat;
   }

   public static void setSuppressChat(boolean suppressChat) {
      BaritoneHelper.suppressChat = suppressChat;
   }

   public static boolean isSilentModeActive() {
      return silentModeActive;
   }

   public static boolean isMlgModeActive() {
      return mlgModeActive;
   }
}
