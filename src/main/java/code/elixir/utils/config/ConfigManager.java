package code.elixir.utils.config;

import code.elixir.Elixir;
import code.elixir.display.hud.Notifications;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import code.elixir.utils.client.sound.SoundManager;
import java.io.File;
import java.util.ArrayList;

public class ConfigManager {
   private static final String CONFIG_EXTENSION = ".json";

   public static boolean saveConfig(String configName) {
      try {
         Elixir.getInstance().getFileController().saveFile(configName + ".json");
         System.out.println("Config saved: " + configName);
         if (!configName.equals("default")) {
            Hud hud = Hud.getInstance();
            if (hud != null) {
               SoundManager.playSound(SoundManager.ENABLE_MODULE);
            }

            Notifications.getInstance().addList("Конфиг «" + configName + "» сохранен!", 2000, false);
         }

         return true;
      } catch (FileSaveException var2) {
         System.err.println("Failed to save config: " + var2.getMessage());
         return false;
      }
   }

   public static boolean loadConfig(String configName) {
      try {
         Elixir.getInstance().getFileController().loadFile(configName + ".json");
         return true;
      } catch (FileLoadException var2) {
         System.err.println("Failed to load config: " + var2.getMessage());
         return false;
      }
   }

   public static void saveDefaultConfig() {
      try {
         Elixir.getInstance().getFileController().saveFile("default.json");
      } catch (FileSaveException var1) {
         System.err.println("Failed to auto-save default config: " + var1.getMessage());
      }
   }

   public static String[] getAvailableConfigs() {
      File[] configFiles = Elixir.getInstance().getClientInfoProvider().configsDir().listFiles();
      if (configFiles == null) {
         return new String[0];
      } else {
         ArrayList<String> configs = new ArrayList<>();

         for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(".json")) {
               String configName = configFile.getName().replace(".json", "");
               configs.add(configName);
            }
         }

         return configs.toArray(new String[0]);
      }
   }

   public static boolean deleteConfig(String configName) {
      try {
         File configFile = new File(Elixir.getInstance().getClientInfoProvider().configsDir(), configName + ".json");
         if (configFile.exists()) {
            boolean deleted = configFile.delete();
            if (deleted) {
               Hud hud = Hud.getInstance();
               if (hud != null) {
                  SoundManager.playSound(SoundManager.DISABLE_MODULE);
               }

               Notifications.getInstance().addList("Конфиг «" + configName + "» удален!", 2000, true);
            }

            return deleted;
         } else {
            return false;
         }
      } catch (Exception var4) {
         System.err.println("Failed to delete config: " + var4.getMessage());
         return false;
      }
   }

   public static boolean configExists(String configName) {
      File configFile = new File(Elixir.getInstance().getClientInfoProvider().configsDir(), configName + ".json");
      return configFile.exists();
   }

   public static void loadDefaultConfig() {
      if (configExists("default")) {
         loadConfig("default");
         System.out.println("Default config loaded");
      } else {
         System.out.println("Default config not found, creating new one");
         saveDefaultConfig();
      }
   }

   public static boolean resetConfig() {
      try {
         Elixir.getInstance().getFileController().resetConfig();
         return true;
      } catch (FileSaveException | FileLoadException var1) {
         System.err.println("Failed to reset config: " + var1.getMessage());
         return false;
      }
   }
}
