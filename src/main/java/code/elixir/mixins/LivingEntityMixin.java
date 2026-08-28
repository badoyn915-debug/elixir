package code.elixir.mixins;

import code.elixir.events.block.PushEvent;
import code.elixir.events.item.SwingDurationEvent;
import code.elixir.events.player.EntityDeathEvent;
import code.elixir.events.player.EventOnTravelPost;
import code.elixir.events.player.JumpEvent;
import code.elixir.features.impl.render.NoRender;
import code.elixir.features.impl.render.SwingAnimations;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.aura.warp.TurnsConstructor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
   @Shadow
   public float bodyYaw;
   @Shadow
   private int handSwingTicks;
   @Shadow
   protected boolean handSwinging;
   @Unique
   private final MinecraftClient client = MinecraftClient.getInstance();

   @Shadow
   public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   @Nullable
   public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> var1);

   @Inject(
      method = {"hasStatusEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
      if (effect != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.player != null && (Object)this == client.player) {
            NoRender noRender = NoRender.getInstance();
            if (noRender != null && noRender.shouldHideBadEffects()) {
               StatusEffect status = (StatusEffect)effect.value();
               if (status == StatusEffects.NAUSEA || status == StatusEffects.BLINDNESS) {
                  cir.setReturnValue(false);
               }
            }
         }
      }
   }

   @Inject(
      method = {"getStuckArrowCount"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetStuckArrowCount(CallbackInfoReturnable<Integer> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideStuckArrows()) {
         cir.setReturnValue(0);
      }
   }

   @Inject(
      method = {"getStingerCount"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetStingerCount(CallbackInfoReturnable<Integer> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideStuckArrows()) {
         cir.setReturnValue(0);
      }
   }

   @Shadow
   public abstract boolean isInSwimmingPose();

   @Inject(
      method = {"isPushable"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
      PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.COLLISION));
      if (event != null && event.isCancelled()) {
         infoReturnable.setReturnValue(false);
      }
   }

   @Redirect(
      method = {"calcGlidingVelocity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"
      )
   )
   private float hookModifyFallFlyingPitch(LivingEntity instance) {
      if ((Object)this != MinecraftClient.getInstance().player) {
         return instance.getPitch();
      } else {
         TurnsConnection rotationManager = TurnsConnection.INSTANCE;
         Turns rotation = rotationManager.getRotation();
         TurnsConstructor configurable = rotationManager.getCurrentRotationPlan();
         return rotation != null && configurable != null && configurable.isMoveCorrection() && !configurable.isChangeLook()
            ? rotation.getPitch()
            : instance.getPitch();
      }
   }

   @Redirect(
      method = {"calcGlidingVelocity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )
   )
   private Vec3d hookModifyFallFlyingRotationVector(LivingEntity original) {
      if ((Object)this != MinecraftClient.getInstance().player) {
         return original.getRotationVector();
      } else {
         TurnsConnection rotationManager = TurnsConnection.INSTANCE;
         Turns rotation = rotationManager.getRotation();
         TurnsConstructor configurable = rotationManager.getCurrentRotationPlan();
         return rotation != null && configurable != null && configurable.isMoveCorrection() && !configurable.isChangeLook()
            ? rotation.toVector()
            : original.getRotationVector();
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void jump(CallbackInfo info) {
      JumpEvent event;
      if ((Object)this instanceof ClientPlayerEntity player) {
         event = SafeEvents.dispatch(() -> createJumpEvent(player));
      } else {
         event = null;
      }

      if (event != null && event.isCancelled()) {
         info.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"jump"},
      at = {@At(
         value = "NEW",
         target = "(DDD)Lnet/minecraft/util/math/Vec3d;"
      )}
   )
   private Vec3d hookFixRotation(Vec3d original) {
      if (this.client.player != null && (Object)this == (Object)this.client.player) {
         float yaw = TurnsConnection.INSTANCE.getMoveRotation().getYaw() * (float) (Math.PI / 180.0);
         return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"calcGlidingVelocity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"
      )}
   )
   private float hookModifyFallFlyingPitch(float original) {
      return this.client.player != null && (Object)this == (Object)this.client.player ? TurnsConnection.INSTANCE.getMoveRotation().getPitch() : original;
   }

   @ModifyExpressionValue(
      method = {"calcGlidingVelocity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )}
   )
   private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
      return this.client.player != null && (Object)this == (Object)this.client.player ? TurnsConnection.INSTANCE.getMoveRotation().toVector() : original;
   }

   @Inject(
      method = {"calcGlidingVelocity"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onCalcGlidingVelocityReturn(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
      if (this.client.player != null && (Object)this == (Object)this.client.player) {
         if (EventManager.hasListeners(EventOnTravelPost.class)) {
            EventOnTravelPost event = SafeEvents.dispatch(() -> new EventOnTravelPost((Vec3d)cir.getReturnValue()));
            if (event != null) {
               cir.setReturnValue(event.getOldVelocity());
            }
         }
      }
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == (Object)this.client.player) {
         SwingDurationEvent event = SafeEvents.dispatch(() -> new SwingDurationEvent());
         if (event != null && event.isCancelled()) {
            cir.setReturnValue((int)event.getAnimation());
         }
      }
   }

   @Inject(
      method = {"swingHand(Lnet/minecraft/util/Hand;)V"},
      at = {@At("HEAD")}
   )
   private void forceSwingRestartOnRetrigger(Hand hand, CallbackInfo ci) {
      this.forceSwingRestartOnRetrigger();
   }

   @Inject(
      method = {"swingHand(Lnet/minecraft/util/Hand;Z)V"},
      at = {@At("HEAD")}
   )
   private void forceSwingRestartOnRetrigger(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
      this.forceSwingRestartOnRetrigger();
   }

   @Unique
   private void forceSwingRestartOnRetrigger() {
      if (this.client.player != null && (Object)this == (Object)this.client.player) {
         SwingAnimations swings = SwingAnimations.getInstance();
         if (swings != null && swings.shouldRestartSwingOnRetrigger()) {
            if (this.handSwinging) {
               this.handSwingTicks = -1;
            }
         }
      }
   }

   @Inject(
      method = {"onDeath"},
      at = {@At("HEAD")}
   )
   private void onDeath(DamageSource source, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity)(Object)this;
      SafeEvents.dispatch(() -> new EntityDeathEvent(entity, source));
   }

   @Inject(
      method = {"handleStatus"},
      at = {@At("HEAD")}
   )
   private void handleStatus(byte status, CallbackInfo ci) {
      if (status == 3) {
         LivingEntity entity = (LivingEntity)(Object)this;
         SafeEvents.dispatch(() -> new EntityDeathEvent(entity, null));
      }
   }

   private static JumpEvent createJumpEvent(ClientPlayerEntity player) {
      return new JumpEvent(player);
   }
}
