package code.elixir.mixins;

import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.features.impl.misc.Panic;
import code.elixir.utils.client.managers.event.SafeEvents;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Final
   @Shadow
   private MinecraftClient client;

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")}
   )
   private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      if (key != -1 && window == this.client.getWindow().getHandle()) {
         if (key == 344 && action == 1 && this.client.currentScreen == null && this.client.player != null && !Panic.isPanicMode()) {
            MenuScreen.getInstance().openGui();
         }

         SafeEvents.dispatch(() -> new KeyEvent(this.client.currentScreen, Type.KEYSYM, key, action));
      }
   }
}
