package code.elixir.mixins;

import code.elixir.display.screens.mainmenu.MainMenu;
import code.elixir.utils.client.FirstLaunchManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public class TitleScreenMixin {
   private static boolean ElixirRedirectScheduled = false;

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceTitleScreen(Screen screen, CallbackInfo ci) {
      if (screen instanceof TitleScreen && !this.isPanicModeSafe()) {
         MinecraftClient client = (MinecraftClient)(Object)this;
         FirstLaunchManager.applySavedPreferences();
         ci.cancel();
         if (client.currentScreen instanceof MainMenu) {
            return;
         }

         if (ElixirRedirectScheduled) {
            return;
         }

         ElixirRedirectScheduled = true;
         client.execute(() -> {
            try {
               client.setScreen(MainMenu.INSTANCE);
            } finally {
               ElixirRedirectScheduled = false;
            }
         });
      }
   }

   private boolean isPanicModeSafe() {
      try {
         Class<?> panicClass = Class.forName("code.elixir.features.impl.misc.Panic");
         Object result = panicClass.getMethod("isPanicMode").invoke(null);
         return result instanceof Boolean && (Boolean)result;
      } catch (Throwable var4) {
         return false;
      }
   }
}
