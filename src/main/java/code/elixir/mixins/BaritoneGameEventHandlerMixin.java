package code.elixir.mixins;

import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.event.events.BlockInteractEvent.Type;
import baritone.api.utils.Rotation;
import code.elixir.utils.interactions.path.BaritoneHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"baritone.event.GameEventHandler"},
   remap = false
)
public class BaritoneGameEventHandlerMixin {
   @Inject(
      method = {"onRenderPass"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void elixir$suppressRender(RenderEvent event, CallbackInfo ci) {
      if (BaritoneHelper.isSilentModeActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onPlayerRotationMove"},
      at = {@At("TAIL")},
      remap = false
   )
   private void elixir$suppressRotationMove(RotationMoveEvent event, CallbackInfo ci) {
      if (BaritoneHelper.isSilentModeActive()) {
         Rotation original = event.getOriginal();
         event.setYaw(original.getYaw());
         event.setPitch(original.getPitch());
      }
   }

   @Inject(
      method = {"onBlockInteract"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void elixir$preventBlockBreak(BlockInteractEvent event, CallbackInfo ci) {
      if (BaritoneHelper.isSilentModeActive() && event.getType() == Type.START_BREAK) {
         ci.cancel();
      }
   }
}
