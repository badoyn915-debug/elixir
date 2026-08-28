package code.elixir.utils.display.render.font;

import code.elixir.utils.display.atlasfont.msdf.MsdfFont;
import java.util.HashMap;
import java.util.Map;

public final class MsdfFontLibrary {
   private static final Map<String, MsdfFont> CACHE = new HashMap<>();

   private MsdfFontLibrary() {
   }

   public static MsdfFont get(String resourceName) {
      return CACHE.computeIfAbsent(resourceName, MsdfFontLibrary::load);
   }

   public static MsdfFont resolveType(String typeName) {
      String msdfName = mapTypeToMsdf(typeName);

      try {
         return get(msdfName);
      } catch (RuntimeException var9) {
         for (String fallback : new String[]{"medium", "regular", "bold"}) {
            if (!fallback.equals(msdfName)) {
               try {
                  return get(fallback);
               } catch (RuntimeException var8) {
               }
            }
         }

         throw var9;
      }
   }

   private static String mapTypeToMsdf(String typeName) {
      return switch (typeName) {
         case "sf_medium" -> "medium";
         case "sf_regular" -> "regular";
         case "sf_bold" -> "bold";
         case "sf_semibold" -> "regular_semibold";
         case "SuisseIntl-Medium" -> "suisseintlmedium";
         case "SuisseIntl-Regular" -> "suisseintlregular";
         case "suisseintl-semibold", "suisseintlsemibold" -> "suisseintlsemibold";
         case "icons" -> "icons";
         case "clienticon1" -> "clienticon1";
         case "icon2" -> "icolist";
         case "guiicons" -> "clickgui";
         case "elixiricons" -> "elixiricons";
         case "icon" -> "iconmain";
         case "categoryicons" -> "icolist";
         case "waypoint_icons" -> "xentrixicon1";
         default -> typeName;
      };
   }

   private static MsdfFont load(String name) {
      return MsdfFont.builder().name(name).data(name).atlas(name).build();
   }

   public static void clear() {
      CACHE.clear();
   }
}
