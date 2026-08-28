package code.elixir.mixins;

import code.elixir.events.block.PushEvent;
import code.elixir.events.player.PlayerTravelEvent;
import code.elixir.events.player.SwimmingEvent;
import code.elixir.features.impl.render.CustomCape;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.AuraMixinHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public abstract class PlayerEntityMixin implements QuickImports {
   @Inject(
      method = {"isPushedByFluids"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
      PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.WATER));
      if (event != null && event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"isPartVisible"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void customCape$isPartVisible(PlayerModelPart modelPart, CallbackInfoReturnable<Boolean> cir) {
      if (modelPart == PlayerModelPart.CAPE) {
         PlayerEntity player = (PlayerEntity)(Object)this;
         if (player instanceof AbstractClientPlayerEntity clientPlayer) {
            CustomCape customCape = CustomCape.getInstance();
            if (customCape != null && customCape.shouldApply(clientPlayer)) {
               cir.setReturnValue(true);
            }
         }
      }
   }

   @ModifyExpressionValue(
      method = {"attack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
      )}
   )
   private float hookFixRotation(float original) {
      return AuraMixinHelper.resolveYaw(original);
   }

   @ModifyExpressionValue(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )}
   )
   public Vec3d travelHook(Vec3d vec3d) {
      SwimmingEvent event = SafeEvents.dispatch(() -> new SwimmingEvent(vec3d));
      return event != null ? event.getVector() : vec3d;
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
      if (mc.player != null) {
         PlayerTravelEvent event = SafeEvents.dispatch(() -> new PlayerTravelEvent(movementInput, true));
         if (event != null && event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
      if (mc.player != null) {
         PlayerTravelEvent event = SafeEvents.dispatch(() -> new PlayerTravelEvent(movementInput, false));
         if (event != null && event.isCancelled()) {
         }
      }
   }
}
