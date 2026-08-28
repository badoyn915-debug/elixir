package code.elixir.utils.theme;

import code.elixir.Elixir;
import code.elixir.display.hud.Notifications;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.sound.SoundManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class ThemeManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String THEME_EXTENSION = ".json";
   private static final String DEFAULT_THEME_NAME = "default_dark";
   private static final long THEME_TRANSITION_DURATION_MS = 520L;
   private static boolean colorAnimationsEnabled = false;
   private static final String PRIMARY_COLOR_KEY = "Первый цвет";
   private static final String SECONDARY_COLOR_KEY = "Второй цвет";
   private static final String BACKGROUND_GUI_KEY = "Цвет заднего фона";
   private static final String OFF_MODULE_COLOR_KEY = "Цвет выкл. модулей";
   private static final String BACKGROUND_SETTINGS_KEY = "Цвет настроек модуля";
   private static final String TEXT_COLOR_KEY = "Цвет текста";
   private static final String LEGACY_PRIMARY_COLOR_KEY = "РџРµСЂРІС‹Р№ С†РІРµС‚";
   private static final String LEGACY_SECONDARY_COLOR_KEY = "Р’С‚РѕСЂРѕР№ С†РІРµС‚";
   private static final String LEGACY_BACKGROUND_GUI_KEY = "Р¦РІРµС‚ Р·Р°РґРЅРµРіРѕ С„РѕРЅР°";
   private static final String LEGACY_OFF_MODULE_COLOR_KEY = "Р¦РІРµС‚ РІС‹РєР». РјРѕРґСѓР»РµР№";
   private static final String LEGACY_BACKGROUND_SETTINGS_KEY = "Р¦РІРµС‚ РЅР°СЃС‚СЂРѕРµРє РјРѕРґСѓР»СЏ";
   private static final String LEGACY_TEXT_COLOR_KEY = "Р¦РІРµС‚ С‚РµРєСЃС‚Р°";
   private static final String LEGACY2_PRIMARY_COLOR_KEY = "Р СџР ВµРЎР‚Р Р†РЎвЂ№Р в„– РЎвЂ Р Р†Р ВµРЎвЂљ";
   private static final String LEGACY2_SECONDARY_COLOR_KEY = "Р вЂ™РЎвЂљР С•РЎР‚Р С•Р в„– РЎвЂ Р Р†Р ВµРЎвЂљ";
   private static final String LEGACY2_BACKGROUND_GUI_KEY = "Р В¦Р Р†Р ВµРЎвЂљ Р В·Р В°Р Т‘Р Р…Р ВµР С–Р С• РЎвЂћР С•Р Р…Р В°";
   private static final String LEGACY2_OFF_MODULE_COLOR_KEY = "Р В¦Р Р†Р ВµРЎвЂљ Р Р†РЎвЂ№Р С”Р В». Р С\u0098Р С•Р Т‘РЎС“Р В»Р ВµР в„–";
   private static final String LEGACY2_BACKGROUND_SETTINGS_KEY = "Р В¦Р Р†Р ВµРЎвЂљ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р ВµР С” Р С\u0098Р С•Р Т‘РЎС“Р В»РЎРЏ";
   private static final String LEGACY2_TEXT_COLOR_KEY = "Р В¦Р Р†Р ВµРЎвЂљ РЎвЂљР ВµР С”РЎРѓРЎвЂљР В°";
   public static final ColorSetting primaryColor = new ThemeManager.AnimatedColorSetting("Основной цвет", "Основной цвет клиента")
      .setColor(new Color(115, 135, 255, 255).getRGB())
      .presets(new Color(115, 135, 255).getRGB(), new Color(185, 205, 255).getRGB(), new Color(140, 180, 255).getRGB(), new Color(220, 230, 255).getRGB());
   public static final ColorSetting secondaryColor = new ThemeManager.AnimatedColorSetting("Вторичный цвет", "Вторичный цвет клиента")
      .setColor(new Color(185, 205, 255, 255).getRGB())
      .presets(new Color(115, 135, 255).getRGB(), new Color(185, 205, 255).getRGB(), new Color(140, 180, 255).getRGB(), new Color(220, 230, 255).getRGB());
   public static final ColorSetting BackgroundGui = new ThemeManager.AnimatedColorSetting("Цвет заднего фона", "Цвет заднего фона в GUI")
      .setColor(new Color(14, 14, 18, 220).getRGB());
   public static final ColorSetting offModuleColor = new ThemeManager.AnimatedColorSetting("Цвет выкл. модулей", "Цвет выключенных модулей в GUI")
      .setColor(new Color(24, 24, 30, 190).getRGB());
   public static final ColorSetting BackgroundSettings = new ThemeManager.AnimatedColorSetting("Цвет настроек модуля", "Цвет фона настроек модуля")
      .setColor(new Color(18, 18, 22, 230).getRGB());
   public static final ColorSetting textColor = new ThemeManager.AnimatedColorSetting("Цвет текста", "Основной цвет текста в GUI")
      .setColor(new Color(245, 248, 255, 255).getRGB());
   private static String currentThemeName = "default_dark";
   private static boolean liquidGlassCustomMode = false;
   private static float liquidGlassGlobalAlphaMin = 0.85F;
   private static float liquidGlassBaseAlpha = 0.97F;
   private static float liquidGlassFresnelAlpha = 1.0F;
   private static float liquidGlassFresnelMix = 0.08F;
   private static float liquidGlassSmoothness = 0.62F;
   private static float liquidGlassDistortStrength = 0.016F;
   private static float liquidGlassCornerSmoothness = 2.0F;
   public static final SliderSettings liquidGlassGlobalAlphaMinSetting = new SliderSettings(
         "Мин. прозрачность", "Минимальная общая прозрачность жидкого стекла"
      )
      .setValue(liquidGlassGlobalAlphaMin)
      .range(0.5F, 1.0F)
      .step(0.01F);
   public static final SliderSettings liquidGlassBaseAlphaSetting = new SliderSettings("Alpha", "Базовая прозрачность жидкого стекла")
      .setValue(liquidGlassBaseAlpha)
      .range(0.5F, 1.0F)
      .step(0.01F);
   public static final SliderSettings liquidGlassFresnelAlphaSetting = new SliderSettings("Edge Alpha", "Прозрачность блика по краям")
      .setValue(liquidGlassFresnelAlpha)
      .range(0.5F, 1.0F)
      .step(0.01F);
   public static final SliderSettings liquidGlassFresnelMixSetting = new SliderSettings("Edge Mix", "Сила светлого блика по краям")
      .setValue(liquidGlassFresnelMix)
      .range(0.0F, 0.6F)
      .step(0.01F);
   public static final SliderSettings liquidGlassSmoothnessSetting = new SliderSettings("Smooth", "Мягкость краев жидкого стекла")
      .setValue(liquidGlassSmoothness)
      .range(0.1F, 1.0F)
      .step(0.01F);
   public static final SliderSettings liquidGlassDistortStrengthSetting = new SliderSettings("Distort", "Сила искажения жидкого стекла")
      .setValue(liquidGlassDistortStrength)
      .range(0.0F, 0.06F)
      .step(0.001F);
   public static final SliderSettings liquidGlassCornerSmoothnessSetting = new SliderSettings("Corners", "Сглаживание углов жидкого стекла")
      .setValue(liquidGlassCornerSmoothness)
      .range(1.0F, 10.0F)
      .step(0.1F);

   public static File getThemeDir() {
      try {
         File filesDir = Elixir.getInstance().getClientInfoProvider().filesDir();
         File themeDir = new File(filesDir, "themes");
         if (!themeDir.exists()) {
            themeDir.mkdirs();
         }

         return themeDir;
      } catch (Exception var2) {
         return new File("Elixir/files/themes");
      }
   }

   public static boolean isLiquidGlassTheme() {
      code.elixir.features.impl.render.ClickGui clickGui = code.elixir.features.impl.render.ClickGui.getInstance();
      return clickGui != null ? clickGui.shouldRenderLiquidGlass() : true;
   }

   public static String getCurrentThemeName() {
      return currentThemeName;
   }

   private static void initializeDefaultThemes() {
      createDefault_darkTheme();
   }

   private static void createDefault_darkTheme() {
      try {
         JsonObject theme = new JsonObject();
         theme.addProperty("name", "default_dark");
         theme.addProperty("version", "1.0");
         theme.addProperty("timestamp", System.currentTimeMillis());
         JsonObject colors = new JsonObject();
         colors.addProperty("Первый цвет", new Color(115, 135, 255, 255).getRGB());
         colors.addProperty("Второй цвет", new Color(185, 205, 255, 255).getRGB());
         colors.addProperty("Цвет заднего фона", new Color(14, 14, 18, 220).getRGB());
         colors.addProperty("Цвет выкл. модулей", new Color(24, 24, 30, 190).getRGB());
         colors.addProperty("Цвет настроек модуля", new Color(18, 18, 22, 230).getRGB());
         colors.addProperty("Цвет текста", new Color(245, 248, 255, 255).getRGB());
         theme.add("colors", colors);
         theme.add("liquidGlass", createLiquidGlassJson());
         File themeFile = new File(getThemeDir(), "default_dark.json");
         String fileName = themeFile.getAbsolutePath();

         try (FileWriter writer = new FileWriter(fileName)) {
            GSON.toJson(theme, writer);
         }

         System.out.println("Default theme created");
      } catch (IOException var9) {
         System.err.println("Failed to create default theme: " + var9.getMessage());
      }
   }

   private static void loadDefaultTheme() {
      if (themeExists("default")) {
         loadTheme("default");
         System.out.println("Default theme loaded");
      } else if (themeExists("default_dark")) {
         loadTheme("default_dark");
         System.out.println("Default dark theme loaded");
      }
   }

   public static boolean saveTheme(String themeName) {
      try {
         JsonObject theme = new JsonObject();
         theme.addProperty("name", themeName);
         theme.addProperty("version", "1.0");
         theme.addProperty("timestamp", System.currentTimeMillis());
         JsonObject colors = new JsonObject();
         colors.addProperty("Первый цвет", primaryColor.getColor());
         colors.addProperty("Второй цвет", secondaryColor.getColor());
         colors.addProperty("Цвет заднего фона", BackgroundGui.getColor());
         colors.addProperty("Цвет выкл. модулей", offModuleColor.getColor());
         colors.addProperty("Цвет настроек модуля", BackgroundSettings.getColor());
         colors.addProperty("Цвет текста", textColor.getColor());
         theme.add("colors", colors);
         theme.add("liquidGlass", createLiquidGlassJson());
         File themeFile = new File(getThemeDir(), themeName + ".json");
         FileWriter writer = new FileWriter(themeFile);

         try {
            GSON.toJson(theme, writer);
         } catch (Throwable var8) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         writer.close();
         if (!themeName.equals("default")) {
            SoundManager.playSound(SoundManager.DISABLE_MODULE);
            Notifications.getInstance().addList("РўРµРјР° В«" + themeName + "В» СЃРѕС…СЂР°РЅРµРЅР°!", 2000, false);
         }

         return true;
      } catch (IOException var9) {
         System.err.println("Failed to save theme: " + var9.getMessage());
         return false;
      }
   }

   public static boolean loadTheme(String themeName) {
      try {
         String resolvedThemeName = normalizeThemeName(themeName);
         File themeFile = new File(getThemeDir(), resolvedThemeName + ".json");
         if (!themeFile.exists()) {
            System.err.println("Theme file not found: " + resolvedThemeName);
            return false;
         } else {
            String content = Files.readString(themeFile.toPath());
            if (content == null) {
               System.err.println("Theme file is empty: " + resolvedThemeName);
               return false;
            } else {
               String trimmed = content.trim();
               if (!trimmed.isEmpty() && trimmed.startsWith("{")) {
                  JsonObject theme;
                  try {
                     theme = JsonParser.parseString(trimmed).getAsJsonObject();
                  } catch (Exception var7) {
                     System.err.println("Failed to parse theme JSON (" + resolvedThemeName + "): " + var7.getMessage());
                     return false;
                  }

                  if (theme.has("colors")) {
                     JsonObject colors = theme.getAsJsonObject("colors");
                     applyThemeColor(colors, primaryColor, "Первый цвет", "РџРµСЂРІС‹Р№ С†РІРµС‚", "Р СџР ВµРЎР‚Р Р†РЎвЂ№Р в„– РЎвЂ Р Р†Р ВµРЎвЂљ");
                     applyThemeColor(colors, secondaryColor, "Второй цвет", "Р’С‚РѕСЂРѕР№ С†РІРµС‚", "Р вЂ™РЎвЂљР С•РЎР‚Р С•Р в„– РЎвЂ Р Р†Р ВµРЎвЂљ");
                     applyThemeColor(
                        colors,
                        BackgroundGui,
                        "Цвет заднего фона",
                        "Р¦РІРµС‚ Р·Р°РґРЅРµРіРѕ С„РѕРЅР°",
                        "Р В¦Р Р†Р ВµРЎвЂљ Р В·Р В°Р Т‘Р Р…Р ВµР С–Р С• РЎвЂћР С•Р Р…Р В°"
                     );
                     applyThemeColor(
                        colors,
                        offModuleColor,
                        "Цвет выкл. модулей",
                        "Р¦РІРµС‚ РІС‹РєР». РјРѕРґСѓР»РµР№",
                        "Р В¦Р Р†Р ВµРЎвЂљ Р Р†РЎвЂ№Р С”Р В». Р С\u0098Р С•Р Т‘РЎС“Р В»Р ВµР в„–"
                     );
                     applyThemeColor(
                        colors,
                        BackgroundSettings,
                        "Цвет настроек модуля",
                        "Р¦РІРµС‚ РЅР°СЃС‚СЂРѕРµРє РјРѕРґСѓР»СЏ",
                        "Р В¦Р Р†Р ВµРЎвЂљ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р ВµР С” Р С\u0098Р С•Р Т‘РЎС“Р В»РЎРЏ"
                     );
                     applyThemeColor(colors, textColor, "Цвет текста", "Р¦РІРµС‚ С‚РµРєСЃС‚Р°", "Р В¦Р Р†Р ВµРЎвЂљ РЎвЂљР ВµР С”РЎРѓРЎвЂљР В°");
                  }

                  if (theme.has("liquidGlass")) {
                     JsonObject glass = theme.getAsJsonObject("liquidGlass");
                     if (glass.has("customMode")) {
                        liquidGlassCustomMode = glass.get("customMode").getAsBoolean();
                     }

                     if (glass.has("globalAlphaMin")) {
                        liquidGlassGlobalAlphaMin = clampf(glass.get("globalAlphaMin").getAsFloat(), 0.5F, 1.0F);
                     }

                     if (glass.has("baseAlpha")) {
                        liquidGlassBaseAlpha = clampf(glass.get("baseAlpha").getAsFloat(), 0.5F, 1.0F);
                     }

                     if (glass.has("fresnelAlpha")) {
                        liquidGlassFresnelAlpha = clampf(glass.get("fresnelAlpha").getAsFloat(), 0.5F, 1.0F);
                     }

                     if (glass.has("fresnelMix")) {
                        liquidGlassFresnelMix = clampf(glass.get("fresnelMix").getAsFloat(), 0.0F, 0.6F);
                     }

                     if (glass.has("smoothness")) {
                        liquidGlassSmoothness = clampf(glass.get("smoothness").getAsFloat(), 0.1F, 1.0F);
                     }

                     if (glass.has("distortStrength")) {
                        liquidGlassDistortStrength = clampf(glass.get("distortStrength").getAsFloat(), 0.0F, 0.06F);
                     }

                     if (glass.has("cornerSmoothness")) {
                        liquidGlassCornerSmoothness = clampf(glass.get("cornerSmoothness").getAsFloat(), 1.0F, 10.0F);
                     }
                  } else {
                     resetLiquidGlassDefaults();
                  }

                  currentThemeName = resolvedThemeName;
                  return true;
               } else {
                  System.err.println("Theme file is not valid JSON: " + resolvedThemeName);
                  return false;
               }
            }
         }
      } catch (IOException var8) {
         System.err.println("Failed to load theme: " + var8.getMessage());
         return false;
      }
   }

   public static void saveDefaultTheme() {
      saveTheme("default");
   }

   private static void applyThemeColor(JsonObject colors, ColorSetting setting, String... keys) {
      for (String key : keys) {
         JsonElement value = colors.get(key);
         if (value != null && !value.isJsonNull()) {
            setting.setColor(value.getAsInt());
            return;
         }
      }
   }

   public static String[] getAvailableThemes() {
      File themeDir = getThemeDir();
      if (themeDir.exists() && themeDir.isDirectory()) {
         File[] files = themeDir.listFiles((dir, name) -> name.endsWith(".json"));
         if (files == null) {
            return new String[0];
         } else {
            ArrayList<ThemeManager.ThemeInfo> themes = new ArrayList<>();

            for (File file : files) {
               String fileName = file.getName();
               String themeName = fileName.substring(0, fileName.length() - ".json".length());
               if (!themeName.equals("default") && !themeName.equals("default_light") && !themeName.equals("default_liquidglass")) {
                  long timestamp = getThemeTimestamp(file);
                  themes.add(new ThemeManager.ThemeInfo(themeName, timestamp));
               }
            }

            themes.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
            return themes.stream().map(t -> t.name).toArray(String[]::new);
         }
      } else {
         return new String[0];
      }
   }

   private static long getThemeTimestamp(File themeFile) {
      try {
         String content = Files.readString(themeFile.toPath());
         JsonObject theme = JsonParser.parseString(content).getAsJsonObject();
         if (theme.has("timestamp")) {
            return theme.get("timestamp").getAsLong();
         }
      } catch (Exception var3) {
      }

      return themeFile.lastModified();
   }

   public static boolean deleteTheme(String themeName) {
      if (themeName.equals("default_dark")) {
         System.out.println("Cannot delete default theme: " + themeName);
         return false;
      } else {
         try {
            File themeFile = new File(getThemeDir(), themeName + ".json");
            if (themeFile.exists()) {
               boolean deleted = themeFile.delete();
               if (deleted) {
                  System.out.println("Theme deleted: " + themeName);
                  SoundManager.playSound(SoundManager.DISABLE_MODULE);
                  Notifications.getInstance().addList("РўРµРјР° В«" + themeName + "В» СѓРґР°Р»РµРЅР°!", 2000, true);
               }

               return deleted;
            } else {
               return false;
            }
         } catch (Exception var3) {
            System.err.println("Failed to delete theme: " + var3.getMessage());
            return false;
         }
      }
   }

   public static boolean themeExists(String themeName) {
      File themeFile = new File(getThemeDir(), normalizeThemeName(themeName) + ".json");
      return themeFile.exists();
   }

   private static String normalizeThemeName(String themeName) {
      if (themeName == null) {
         return "default_dark";
      } else {
         return !"default_blur".equalsIgnoreCase(themeName)
               && !"default_light".equalsIgnoreCase(themeName)
               && !"default_liquidglass".equalsIgnoreCase(themeName)
            ? themeName
            : "default_dark";
      }
   }

   public static void recreateDefaultThemes() {
      createDefault_darkTheme();
      System.out.println("Default themes recreated with original colors");
   }

   public static boolean isLiquidGlassCustomMode() {
      return liquidGlassCustomMode;
   }

   public static void setLiquidGlassCustomMode(boolean value) {
      liquidGlassCustomMode = value;
   }

   public static float getLiquidGlassGlobalAlphaMin() {
      return liquidGlassGlobalAlphaMinSetting.getValue();
   }

   public static void setLiquidGlassGlobalAlphaMin(float value) {
      liquidGlassGlobalAlphaMin = setSliderValue(liquidGlassGlobalAlphaMinSetting, value, 0.5F, 1.0F);
   }

   public static float getLiquidGlassBaseAlpha() {
      return liquidGlassBaseAlphaSetting.getValue();
   }

   public static void setLiquidGlassBaseAlpha(float value) {
      liquidGlassBaseAlpha = setSliderValue(liquidGlassBaseAlphaSetting, value, 0.5F, 1.0F);
   }

   public static float getLiquidGlassFresnelAlpha() {
      return liquidGlassFresnelAlphaSetting.getValue();
   }

   public static void setLiquidGlassFresnelAlpha(float value) {
      liquidGlassFresnelAlpha = setSliderValue(liquidGlassFresnelAlphaSetting, value, 0.5F, 1.0F);
   }

   public static float getLiquidGlassFresnelMix() {
      return liquidGlassFresnelMixSetting.getValue();
   }

   public static void setLiquidGlassFresnelMix(float value) {
      liquidGlassFresnelMix = setSliderValue(liquidGlassFresnelMixSetting, value, 0.0F, 0.6F);
   }

   public static float getLiquidGlassSmoothness() {
      return liquidGlassSmoothnessSetting.getValue();
   }

   public static void setLiquidGlassSmoothness(float value) {
      liquidGlassSmoothness = setSliderValue(liquidGlassSmoothnessSetting, value, 0.1F, 1.0F);
   }

   public static float getLiquidGlassDistortStrength() {
      return liquidGlassDistortStrengthSetting.getValue();
   }

   public static void setLiquidGlassDistortStrength(float value) {
      liquidGlassDistortStrength = setSliderValue(liquidGlassDistortStrengthSetting, value, 0.0F, 0.06F);
   }

   public static float getLiquidGlassCornerSmoothness() {
      return liquidGlassCornerSmoothnessSetting.getValue();
   }

   public static void setLiquidGlassCornerSmoothness(float value) {
      liquidGlassCornerSmoothness = setSliderValue(liquidGlassCornerSmoothnessSetting, value, 1.0F, 10.0F);
   }

   public static void resetLiquidGlassDefaults() {
      liquidGlassCustomMode = false;
      setLiquidGlassGlobalAlphaMin(0.85F);
      setLiquidGlassBaseAlpha(0.97F);
      setLiquidGlassFresnelAlpha(1.0F);
      setLiquidGlassFresnelMix(0.08F);
      setLiquidGlassSmoothness(0.62F);
      setLiquidGlassDistortStrength(0.016F);
      setLiquidGlassCornerSmoothness(2.0F);
   }

   private static JsonObject createLiquidGlassJson() {
      JsonObject glass = new JsonObject();
      glass.addProperty("customMode", liquidGlassCustomMode);
      glass.addProperty("globalAlphaMin", getLiquidGlassGlobalAlphaMin());
      glass.addProperty("baseAlpha", getLiquidGlassBaseAlpha());
      glass.addProperty("fresnelAlpha", getLiquidGlassFresnelAlpha());
      glass.addProperty("fresnelMix", getLiquidGlassFresnelMix());
      glass.addProperty("smoothness", getLiquidGlassSmoothness());
      glass.addProperty("distortStrength", getLiquidGlassDistortStrength());
      glass.addProperty("cornerSmoothness", getLiquidGlassCornerSmoothness());
      return glass;
   }

   public static float getThemeTransitionProgress() {
      float progress = 1.0F;
      progress = Math.min(progress, getColorTransitionProgress(primaryColor));
      progress = Math.min(progress, getColorTransitionProgress(secondaryColor));
      progress = Math.min(progress, getColorTransitionProgress(BackgroundGui));
      progress = Math.min(progress, getColorTransitionProgress(offModuleColor));
      progress = Math.min(progress, getColorTransitionProgress(BackgroundSettings));
      return Math.min(progress, getColorTransitionProgress(textColor));
   }

   public static boolean isThemeTransitioning() {
      return getThemeTransitionProgress() < 1.0F;
   }

   private static float getColorTransitionProgress(ColorSetting setting) {
      return setting instanceof ThemeManager.AnimatedColorSetting animated ? animated.getTransitionProgress() : 1.0F;
   }

   private static int blendColor(int from, int to, float progress) {
      int a = lerpChannel(from >>> 24 & 0xFF, to >>> 24 & 0xFF, progress);
      int r = lerpChannel(from >>> 16 & 0xFF, to >>> 16 & 0xFF, progress);
      int g = lerpChannel(from >>> 8 & 0xFF, to >>> 8 & 0xFF, progress);
      int b = lerpChannel(from & 0xFF, to & 0xFF, progress);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int lerpChannel(int from, int to, float progress) {
      return Math.round(from + (to - from) * progress);
   }

   private static float easeOutCubic(float progress) {
      float inverted = 1.0F - clampf(progress, 0.0F, 1.0F);
      return 1.0F - inverted * inverted * inverted;
   }

   private static float setSliderValue(SliderSettings setting, float value, float min, float max) {
      float clamped = clampf(value, min, max);
      setting.setValue(clamped);
      return clamped;
   }

   private static float clampf(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   static {
      try {
         Files.createDirectories(getThemeDir().toPath());
         initializeDefaultThemes();
         loadDefaultTheme();
         colorAnimationsEnabled = true;
      } catch (IOException var1) {
         System.err.println("Failed to create theme directory: " + var1.getMessage());
      }
   }

   private static class AnimatedColorSetting extends ColorSetting {
      private int startColor;
      private int targetColor;
      private long transitionStartedAt;

      private AnimatedColorSetting(String name, String description) {
         super(name, description);
      }

      @Override
      public int getColor() {
         return ThemeManager.colorAnimationsEnabled && !(this.getTransitionProgress() >= 1.0F)
            ? ThemeManager.blendColor(this.startColor, this.targetColor, ThemeManager.easeOutCubic(this.getTransitionProgress()))
            : super.getColor();
      }

      @Override
      public ColorSetting setColor(int color) {
         if (!ThemeManager.colorAnimationsEnabled) {
            super.setColor(color);
            this.startColor = color;
            this.targetColor = color;
            this.transitionStartedAt = 0L;
            return this;
         } else {
            this.startColor = this.getColor();
            this.targetColor = color;
            this.transitionStartedAt = System.currentTimeMillis();
            super.setColor(color);
            return this;
         }
      }

      private float getTransitionProgress() {
         return this.transitionStartedAt <= 0L
            ? 1.0F
            : ThemeManager.clampf((float)(System.currentTimeMillis() - this.transitionStartedAt) / 520.0F, 0.0F, 1.0F);
      }
   }

   private static class ThemeInfo {
      String name;
      long timestamp;

      ThemeInfo(String name, long timestamp) {
         this.name = name;
         this.timestamp = timestamp;
      }
   }
}
