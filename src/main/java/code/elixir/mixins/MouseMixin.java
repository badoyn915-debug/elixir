package code.elixir.mixins;

import code.elixir.events.keyboard.HotBarScrollEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.keyboard.MouseRotationEvent;
import code.elixir.events.render.FovEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseMixin {
   @Final
   @Shadow
   private MinecraftClient client;
   @Shadow
   public double cursorDeltaX;
   @Shadow
   public double cursorDeltaY;

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   public void onMouseButtonHook(long window, int button, int action, int mods, CallbackInfo ci) {
      if (button != -1 && window == this.client.getWindow().getHandle()) {
         SafeEvents.dispatch(() -> new KeyEvent(this.client.currentScreen, Type.MOUSE, button, action));
      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
      )},
      cancellable = true
   )
   public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      HotBarScrollEvent event = SafeEvents.dispatch(() -> new HotBarScrollEvent(horizontal, vertical));
      if (event != null && event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateMouse"},
      at = {@At("HEAD")}
   )
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      FovEvent event = SafeEvents.dispatch(FovEvent::new);
      if (event != null && event.isCancelled()) {
         double slowdown = (double)event.getFov() / ((Integer)(Object)this.client.options.getFov().getValue()).intValue();
         this.cursorDeltaX *= slowdown;
         this.cursorDeltaY *= slowdown;
      }
   }

   @WrapWithCondition(
      method = {"updateMouse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      )},
      require = 1,
      allow = 1
   )
   private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      MouseRotationEvent event = SafeEvents.dispatch(() -> new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY));
      if (event == null) {
         instance.changeLookDirection(cursorDeltaX, cursorDeltaY);
         return false;
      } else if (event.isCancelled()) {
         return false;
      } else {
         instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
         return false;
      }
   }
}
