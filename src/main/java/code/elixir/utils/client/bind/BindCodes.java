package code.elixir.utils.client.bind;

import code.elixir.utils.client.chat.StringHelper;

public final class BindCodes {
   public static final int SCROLL_UP = 998;
   public static final int SCROLL_DOWN = 999;

   public static boolean isUnset(int key) {
      return key == -1 || key < 0;
   }

   public static boolean isMouse(int key) {
      return key >= 0 && key <= 7;
   }

   public static boolean isScroll(int key) {
      return key == 998 || key == 999;
   }

   public static boolean canBind(int key) {
      return key != 256 && !isUnset(key) ? key != -1 : false;
   }

   public static String getDisplayName(int key) {
      if (isUnset(key)) {
         return "Не задано";
      } else if (key == 998) {
         return "Колёсико ↑";
      } else {
         return key == 999 ? "Колёсико ↓" : StringHelper.getBindName(key);
      }
   }

   private BindCodes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
