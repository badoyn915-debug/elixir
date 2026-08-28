package code.elixir.utils.client.window;

import code.elixir.features.impl.misc.Panic;

public class WindowTitleAnimation {
   private static WindowTitleAnimation INSTANCE;
   private static final String TITLE = "Elixir Beta v1.1";
   private static final String PANIC_TITLE = "Minecraft 1.21.4";

   private WindowTitleAnimation() {
   }

   public static WindowTitleAnimation getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new WindowTitleAnimation();
      }

      return INSTANCE;
   }

   public void updateTitle() {
   }

   public String getCurrentTitle() {
      return Panic.isPanicMode() ? "Minecraft 1.21.4" : "Elixir Beta v1.1";
   }
}
