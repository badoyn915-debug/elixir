package code.elixir.common.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ProxyScreens {
   private ProxyScreens() {
   }

   public static void open(Screen parent) {
      try {
         MinecraftClient.getInstance().setScreen(new GuiProxy(parent));
      } catch (Throwable var2) {
         System.err.println("Failed to open proxy screen");
         var2.printStackTrace();
      }
   }
}
