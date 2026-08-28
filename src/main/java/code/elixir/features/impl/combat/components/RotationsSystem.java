package code.elixir.features.impl.combat.components;

import code.elixir.features.impl.combat.CombatModules;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.combat.PredictUtils;
import code.elixir.utils.features.rotate.Rotation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public abstract class RotationsSystem implements QuickImports {
   public Vec2f rotate = Vec2f.ZERO;
   protected float currentReach = 3.0F;
   protected boolean allowThroughWalls = true;
   protected float smoothYawState = Float.NaN;
   protected float smoothPitchState = Float.NaN;

   public abstract void updateRotations(LivingEntity var1);

   public void configureAim(float reach, boolean throughWalls) {
      this.currentReach = reach;
      this.allowThroughWalls = throughWalls;
   }

   protected void applyRotation(float targetYaw, float targetPitch, int timeout, int priority) {
      this.applyRotation(targetYaw, targetPitch, 0.2F, timeout, priority);
   }

   protected void applyRotation(float targetYaw, float targetPitch, float smoothFactor, int timeout, int priority) {
      if (mc.player != null) {
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         if (Float.isNaN(this.smoothYawState)
            || Float.isNaN(this.smoothPitchState)
            || Math.abs(MathHelper.wrapDegrees(targetYaw - this.smoothYawState)) > 90.0F) {
            this.smoothYawState = currentYaw;
            this.smoothPitchState = currentPitch;
         }

         float yawDelta = MathHelper.wrapDegrees(targetYaw - this.smoothYawState) * smoothFactor;
         float pitchDelta = MathHelper.wrapDegrees(targetPitch - this.smoothPitchState) * smoothFactor;
         this.smoothYawState += yawDelta;
         this.smoothPitchState += pitchDelta;
         float finalYaw = latticeSnap(currentYaw, this.smoothYawState);
         float finalPitch = MathHelper.clamp(latticeSnap(currentPitch, this.smoothPitchState), -90.0F, 90.0F);
         RotationStorage.update(new Rotation(finalYaw, finalPitch), 180.0F, 180.0F, 6.0F, 5.0F, timeout, priority, false);
      }
   }

   public void reset() {
      this.smoothYawState = Float.NaN;
      this.smoothPitchState = Float.NaN;
   }

   protected static float latticeSnap(float current, float target) {
      return current + GCDUtil.getFixedRotation(target - current);
   }

   public static Vec2f correctRotation(float yaw, float pitch) {
      if (mc.player == null) {
         return new Vec2f(yaw, pitch);
      } else {
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         float deltaYaw = GCDUtil.getFixedRotation(yaw - currentYaw);
         float deltaPitch = GCDUtil.getFixedRotation(pitch - currentPitch);
         return new Vec2f(currentYaw + deltaYaw, MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F));
      }
   }

   protected boolean shouldUseElytraPredict(LivingEntity target) {
      return mc.player != null && target != null && mc.player.isGliding() && target.isGliding() && CombatModules.isElytraPredictActive();
   }

   protected int getElytraPredictTicks() {
      return CombatModules.getElytraForwardTicks();
   }

   protected Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
      return !this.shouldUseElytraPredict(target) ? point : PredictUtils.bypasselytrahacking(target);
   }

   protected Box getPredictedBox(LivingEntity target) {
      Box box = target.getBoundingBox();
      if (!this.shouldUseElytraPredict(target)) {
         return box;
      } else {
         Vec3d currentCenter = box.getCenter();
         Vec3d predictedCenter = this.getPredictedPoint(target, currentCenter);
         return box.offset(predictedCenter.subtract(currentCenter));
      }
   }
}
