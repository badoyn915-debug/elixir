package code.elixir.utils.customization;

import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;

public class CustomizationManager {
   private static final List<ColorSetting> customizationSettings = new ArrayList<>();

   public static List<ColorSetting> getCustomizationSettings() {
      return new ArrayList<>(customizationSettings);
   }

   public static int getSettingsCount() {
      return customizationSettings.size();
   }

   public static boolean hasSettings() {
      return !customizationSettings.isEmpty();
   }

   static {
      customizationSettings.add(ThemeManager.primaryColor);
      customizationSettings.add(ThemeManager.secondaryColor);
      customizationSettings.add(ThemeManager.BackgroundGui);
      customizationSettings.add(ThemeManager.offModuleColor);
      customizationSettings.add(ThemeManager.BackgroundSettings);
      customizationSettings.add(ThemeManager.textColor);
   }
}
