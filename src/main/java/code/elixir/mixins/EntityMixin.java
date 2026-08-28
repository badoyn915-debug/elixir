package code.elixir.mixins;

import code.elixir.events.player.BoundingBoxControlEvent;
import code.elixir.events.player.EventOnMovePost;
import code.elixir.events.player.PlayerVelocityStrafeEvent;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.client.stealth.MovementMixinGuard;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.AuraMixinHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin implements QuickImports {
   @Shadow
   private Box boundingBox;
   @Shadow
   public float yaw;
   @Unique
   private final MinecraftClient client = MinecraftClient.getInstance();

   @Redirect(
      method = {"updateVelocity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"
      )
   )
   public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
      if ((Object)this == mc.player && MovementMixinGuard.shouldHandleVelocityStrafe()) {
         Vec3d velocity = Entity.movementInputToVelocity(movementInput, speed, yaw);
         PlayerVelocityStrafeEvent event = SafeEvents.dispatch(() -> new PlayerVelocityStrafeEvent(movementInput, speed, yaw, velocity));
         return event != null ? event.getVelocity() : velocity;
      } else {
         return Entity.movementInputToVelocity(movementInput, speed, yaw);
      }
   }

   @Inject(
      method = {"getBoundingBox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
      if (MovementMixinGuard.isHitBoxActive()) {
         BoundingBoxControlEvent event = SafeEvents.dispatch(() -> new BoundingBoxControlEvent(this.boundingBox, (Entity)(Object)this));
         cir.setReturnValue(event != null ? event.getBox() : this.boundingBox);
      }
   }

   @ModifyVariable(
      method = {"getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"},
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private float modifyRotationVectorYaw(float yaw) {
      return (Object)this instanceof ClientPlayerEntity ? AuraMixinHelper.resolveYaw(yaw) : yaw;
   }

   @ModifyVariable(
      method = {"getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float modifyPitch(float pitch) {
      return (Object)this instanceof ClientPlayerEntity ? AuraMixinHelper.resolvePitch(pitch) : pitch;
   }

   @ModifyExpressionValue(
      method = {"move"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"
      )}
   )
   public boolean isControlledByPlayerHook(boolean original) {
      return (Object)this == mc.player && MovementMixinGuard.shouldUseEntityMovementPhysics() ? false : original;
   }

   @Inject(
      method = {"updateVelocity"},
      at = {@At("TAIL")}
   )
   private void onUpdateVelocityPost(float speed, Vec3d movementInput, CallbackInfo ci) {
      Entity me = (Entity)(Object)this;
      if (MinecraftClient.getInstance().player != null && me.getId() == MinecraftClient.getInstance().player.getId()) {
         EventManager.callEvent(new EventOnMovePost(speed, movementInput));
      }
   }
}
