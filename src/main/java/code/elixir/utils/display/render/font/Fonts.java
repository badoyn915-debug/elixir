package code.elixir.utils.display.render.font;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public class Fonts {
   private static final Map<Fonts.FontKey, FontRenderer> fontCache = new HashMap<>();

   public static void init() {
      clearCache();
   }

   public static void clearCache() {
      fontCache.clear();
      MsdfFontLibrary.clear();
   }

   public static FontRenderer getSize(int size) {
      return getSize(size, Fonts.Type.BOLD);
   }

   public static FontRenderer getSize(int size, Fonts.Type type) {
      float guiScale = currentGuiScale();
      return fontCache.computeIfAbsent(new Fonts.FontKey(size, type, guiScale), k -> create(size, type.getType()));
   }

   private static float currentGuiScale() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.getWindow() != null ? Math.max(4.0F, (float)client.getWindow().getScaleFactor()) * 1.5F : 8.0F;
   }

   public static FontRenderer create(float size, String name) {
      return new FontRenderer(name, size / 2.0F);
   }

   private record FontKey(int size, Fonts.Type type, float guiScale) {
   }

   public static enum Type {
      DEFAULT("sf_medium"),
      REGULAR("sf_regular"),
      SEMI("sf_semibold"),
      BOLD("sf_bold"),
      ROCKSTAR_REGULAR("SuisseIntl-Regular"),
      ROCKSTAR_MEDIUM("SuisseIntl-Medium"),
      ROCKSTAR_SEMIBOLD("suisseintl-semibold"),
      ICONS("icons"),
      ICONS2("clienticon1"),
      ICONSTYPENEW("icon2"),
      GUIICONS("guiicons"),
      ELIXIR_ICONS("elixiricons"),
      INTER_MEDIUM("icon"),
      SuisseIntlMedium("SuisseIntl-Medium"),
      SuisseIntlRegular("SuisseIntl-Regular"),
      SuisseIntlSemiBold("suisseintl-semibold"),
      ICONSCATEGORY("categoryicons"),
      WAYPOINT_ICONS("waypoint_icons");

      private final String type;

      public String getType() {
         return this.type;
      }

      private Type(String type) {
         this.type = type;
      }
   }
}
