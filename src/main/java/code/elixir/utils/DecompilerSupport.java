package code.elixir.utils;

public final class DecompilerSupport {
   public static int typeSwitch(String ignored, Object[] types, Object value, int start) {
      for (int i = Math.max(0, start); i < types.length; i++) {
         if (types[i] instanceof Class<?> clazz && clazz.isInstance(value)) {
            return i;
         }
      }

      return -1;
   }

   private DecompilerSupport() {
   }
}
