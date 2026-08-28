package code.elixir.mixins;

import code.elixir.events.block.PushEvent;
import code.elixir.events.container.CloseScreenEvent;
import code.elixir.events.player.EventNoSlow;
import code.elixir.events.player.MotionEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.player.PlayerTravelEvent;
import code.elixir.events.player.PostMotionEvent;
import code.elixir.events.player.PostTickEvent;
import code.elixir.events.player.SprintEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.movement.AutoSprint;
import code.elixir.features.impl.movement.NoSlow;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.client.stealth.MovementMixinGuard;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.FreeLookStorage;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTick;
import code.elixir.utils.interactions.simulate.Simulations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Final
   @Shadow
   protected MinecraftClient client;
   @Shadow
   public Input input;
   @Unique
   private double silentBodyPrevX = 0.0;
   @Unique
   private double silentBodyPrevZ = 0.0;
   private float prevBodyYaw = 0.0F;
   private boolean initialized = false;

   @Shadow
   public abstract float getPitch(float tickDelta);

   @Shadow
   public abstract float getYaw(float tickDelta);

   @Shadow
   protected abstract void autoJump(float var1, float var2);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo info) {
      if (this.client.player != null && this.client.world != null) {
         SafeEvents.dispatch(() -> new TickEvent());
      }

      InventoryTick.update();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void onTick(CallbackInfo ci) {
      if (!this.initialized && QuickImports.mc.player != null) {
         this.silentBodyPrevX = QuickImports.mc.player.getX();
         this.silentBodyPrevZ = QuickImports.mc.player.getZ();
         this.prevBodyYaw = QuickImports.mc.player.getBodyYaw();
         this.initialized = true;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
         shift = Shift.AFTER
      )}
   )
   public void postTick(CallbackInfo callbackInfo) {
      if (this.client.player != null && this.client.world != null) {
         SafeEvents.dispatch(() -> new PostTickEvent());
      }

      if (this.shouldSyncRotation()) {
         float targetYaw = this.getYaw();
         float headBlend = 0.25F;
         this.headYaw = MathHelper.lerpAngleDegrees(headBlend, this.headYaw, targetYaw);
      }
   }

   @Unique
   private boolean shouldSyncRotation() {
      RotationStorage storage = RotationStorage.instance;
      return storage != null && storage.isRotating() ? FreeLookStorage.isActive() : false;
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/input/Input;tick()V",
         shift = Shift.AFTER
      )}
   )
   private void onInputTick(CallbackInfo ci) {
      if (QuickImports.mc.player != null) {
         SafeEvents.dispatch(() -> new PlayerTravelEvent(Vec3d.ZERO, false));
      }
   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets", "tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
      )}
   )
   private float hookSilentRotationYaw(float original) {
      if (QuickImports.mc.player != null && TurnsConnection.INSTANCE.getCurrentAngle() != null) {
         float currentYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
         float newBodyYaw;
         this.prevBodyYaw = newBodyYaw = Simulations.calculateBodyYaw(
            currentYaw,
            this.prevBodyYaw,
            this.silentBodyPrevX,
            this.silentBodyPrevZ,
            QuickImports.mc.player.getX(),
            QuickImports.mc.player.getZ(),
            QuickImports.mc.player.handSwingProgress
         );
         this.silentBodyPrevX = QuickImports.mc.player.getX();
         this.silentBodyPrevZ = QuickImports.mc.player.getZ();
         QuickImports.mc.player.setBodyYaw(newBodyYaw);
         return currentYaw;
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets", "tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
      )}
   )
   private float hookSilentRotationPitch(float original) {
      return QuickImports.mc.player != null && TurnsConnection.INSTANCE.getCurrentAngle() != null
         ? TurnsConnection.INSTANCE.getRotation().getPitch()
         : original;
   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo info) {
      CloseScreenEvent event = SafeEvents.dispatch(() -> new CloseScreenEvent(this.client.currentScreen));
      if (event != null && event.isCancelled()) {
         info.cancel();
      }
   }

   @Redirect(
      method = {"tickMovement"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      ),
      require = 0
   )
   private boolean tickMovementHook(ClientPlayerEntity player) {
      if (!MovementMixinGuard.shouldHandleNoSlowRedirect()) {
         return player.isUsingItem();
      } else {
         EventNoSlow a = SafeEvents.dispatch(() -> new EventNoSlow(false));
         return a != null && a.isCancelled() ? false : player.isUsingItem();
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preMotion(CallbackInfo ci) {
      if (MovementMixinGuard.shouldHandleMotionEvent()) {
         MotionEvent event = SafeEvents.dispatch(
            () -> new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(1.0F), this.getPitch(1.0F), this.isOnGround())
         );
         if (event != null) {
            if (event.isCancelled()) {
               ci.cancel();
            } else {
               this.setPosition(event.getX(), event.getY(), event.getZ());
               this.setYaw(event.getYaw());
               this.setPitch(event.getPitch());
            }
         }
      }
   }

   @Inject(
      method = {"move"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
      )},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      if (MovementMixinGuard.shouldHandleMoveEvent()) {
         MoveEvent event = SafeEvents.dispatch(() -> new MoveEvent(movement));
         double d = this.getX();
         double e = this.getZ();
         super.move(movementType, event != null ? event.getMovement() : movement);
         this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void postMotion(CallbackInfo ci) {
      PostMotionEvent postMotionEvent;
      if (MovementMixinGuard.shouldHandlePostMotionEvent()
         && (postMotionEvent = SafeEvents.dispatch(() -> new PostMotionEvent())) != null
         && postMotionEvent.isCancelled()) {
         ci.cancel();
      }

      InventoryFlowManager.postMotion();
   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.BLOCK));
      if (event != null && event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"shouldStopSprinting"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      )},
      cancellable = true
   )
   public void shouldStopSprintingHook(CallbackInfoReturnable<Boolean> cir) {
      AutoSprint autoSprint = AutoSprint.getInstance();
      NoSlow noSlow = NoSlow.getInstance();
      if (autoSprint != null && noSlow != null && autoSprint.isState() && noSlow.isState()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"canStartSprinting"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      )},
      cancellable = true
   )
   public void canStartSprintingHook(CallbackInfoReturnable<Boolean> cir) {
      AutoSprint autoSprint = AutoSprint.getInstance();
      NoSlow noSlow = NoSlow.getInstance();
      if (autoSprint != null && noSlow != null && autoSprint.isState() && noSlow.isState()) {
         cir.setReturnValue(true);
      }
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"
      )}
   )
   private boolean hookCanSprint(boolean original) {
      SprintEvent event = SafeEvents.dispatch(() -> new SprintEvent(original));
      return event != null ? event.isSprinting() : original;
   }
}
