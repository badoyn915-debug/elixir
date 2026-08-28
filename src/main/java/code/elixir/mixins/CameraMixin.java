package code.elixir.mixins;

import code.elixir.events.render.CameraEvent;
import code.elixir.events.render.CameraPositionEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3d pos;
   @Shadow
   @Final
   private Mutable blockPos;
   @Shadow
   private float yaw;
   @Shadow
   private float pitch;

   @Shadow
   public abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Inject(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      CameraEvent event = SafeEvents.dispatch(() -> new CameraEvent(false, 4.0F, new Turns(this.yaw, this.pitch)));
      if (event != null) {
         Turns angle = event.getAngle();
         ClientPlayerEntity player;
         if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity && !(player = (ClientPlayerEntity)focusedEntity).isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (inverseView ? 180 : 0);
            float distance = event.getDistance();
            this.setRotation(yaw, pitch);
            this.moveBy(event.isCameraClip() ? -distance : -this.clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void redirectSetRotation(
      Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta
   ) {
      CameraEvent event = SafeEvents.dispatch(() -> new CameraEvent(false, 4.0F, new Turns(yaw, pitch)));
      float newYaw = yaw;
      float newPitch = pitch;
      if (event != null && event.isCancelled() && event.getAngle() != null) {
         newYaw = event.getAngle().getYaw();
         newPitch = event.getAngle().getPitch();
      }

      if (thirdPerson && inverseView) {
         newYaw += 180.0F;
         newPitch = -newPitch;
      }

      instance.setRotation(newYaw, newPitch);
   }

   @Inject(
      method = {"setPos(Lnet/minecraft/util/math/Vec3d;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void posHook(Vec3d pos, CallbackInfo ci) {
      CameraPositionEvent event = SafeEvents.dispatch(() -> new CameraPositionEvent(pos));
      Vec3d targetPos = event != null ? event.getPos() : pos;
      this.pos = targetPos;
      this.blockPos.set(targetPos.x, targetPos.y, targetPos.z);
      ci.cancel();
   }
}
