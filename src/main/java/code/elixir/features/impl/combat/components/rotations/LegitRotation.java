package code.elixir.features.impl.combat.components.rotations;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.AimPoint;
import code.elixir.features.impl.combat.components.interpolation.HitChance;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.features.impl.combat.components.interpolation.Interpolator;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.features.rotate.RotationUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class LegitRotation extends RotationsSystem implements QuickImports {
   private static final float FOLLOW_YAW = 0.15F;
   private static final float FOLLOW_PITCH = 0.08F;
   private static final float MAX_ACCEL_YAW = 1.8F;
   private static final float MAX_ACCEL_PITCH = 0.7F;
   private static final float AIM_SMOOTH_YAW = 0.5F;
   private static final float AIM_SMOOTH_PITCH = 0.06F;
   private long lastUpdateMs = 0L;
   private int lastTargetId = -1;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;
   private float commitSmooth = 0.0F;
   private float aimYaw = 0.0F;
   private float aimPitch = 0.0F;
   private boolean haveAim = false;

   @Override
   public void reset() {
      this.lastUpdateMs = 0L;
      this.lastTargetId = -1;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.commitSmooth = 0.0F;
      this.aimYaw = 0.0F;
      this.aimPitch = 0.0F;
      this.haveAim = false;
      HumanAim.reset();
      AimPoint.reset();
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         long now = System.currentTimeMillis();
         float dt = this.lastUpdateMs == 0L ? 1.0F : MathHelper.clamp((float)(now - this.lastUpdateMs) / 50.0F, 0.5F, 2.0F);
         this.lastUpdateMs = now;
         if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.haveAim = false;
         }

         Vec3d aimPoint = this.getPredictedPoint(target, AimPoint.get(target));
         Vec2f desired = RotationUtils.getRotations(aimPoint);
         if (!this.haveAim) {
            this.aimYaw = desired.x;
            this.aimPitch = desired.y;
            this.haveAim = true;
         } else {
            this.aimYaw = this.aimYaw + MathHelper.wrapDegrees(desired.x - this.aimYaw) * MathHelper.clamp(0.5F * dt, 0.0F, 1.0F);
            this.aimPitch = this.aimPitch + MathHelper.wrapDegrees(desired.y - this.aimPitch) * MathHelper.clamp(0.06F * dt, 0.0F, 1.0F);
         }

         this.aimYaw = MathHelper.wrapDegrees(this.aimYaw);
         this.aimPitch = MathHelper.clamp(this.aimPitch, -89.0F, 89.0F);
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         float baseDiffYaw = MathHelper.wrapDegrees(this.aimYaw - currentYaw);
         float baseDiffPitch = MathHelper.wrapDegrees(this.aimPitch - currentPitch);
         float baseDist = (float)Math.hypot(baseDiffYaw, baseDiffPitch);
         boolean commit = AttackAura.getInstance() != null && AttackAura.getInstance().isLegitAimCommit();
         this.commitSmooth = this.commitSmooth + ((commit ? 1.0F : 0.0F) - this.commitSmooth) * MathHelper.clamp(0.15F * dt, 0.0F, 1.0F);
         float hitChance = HitChance.get(target);
         float tremorScale = (1.0F - 0.5F * this.commitSmooth) * (1.0F - 0.4F * hitChance);
         float jitterYaw = HumanAim.noiseYaw() * tremorScale;
         float jitterPitch = HumanAim.noisePitch() * tremorScale * 0.25F;
         float diffYaw = MathHelper.wrapDegrees(this.aimYaw + jitterYaw - currentYaw);
         float diffPitch = MathHelper.wrapDegrees(this.aimPitch + jitterPitch - currentPitch);
         float reaction = HumanAim.reactionFactor(target);
         float fov = HumanAim.fovFactor(baseDiffYaw, baseDiffPitch);
         float speedJitter = HumanAim.speedJitter();
         float gate = reaction * fov * speedJitter;
         float speedFrac = HumanAim.bezierEase((float)Interpolator.EASE_BOTH.interpolate(0.0, 1.0, (double)MathHelper.clamp(baseDist / 60.0F, 0.0F, 1.0F)));
         float maxSpeedYaw = (2.0F + speedFrac * 10.0F) * gate;
         float maxSpeedPitch = (0.6F + speedFrac * 2.5F) * gate;
         float stepYaw = this.follow(diffYaw, true, 0.15F, maxSpeedYaw, 1.8F, dt);
         float stepPitch = this.follow(diffPitch, false, 0.08F, maxSpeedPitch, 0.7F, dt);
         float deltaYaw = GCDUtil.getFixedRotation(stepYaw);
         float deltaPitch = GCDUtil.getFixedRotation(stepPitch);
         float newYaw = currentYaw + deltaYaw;
         float newPitch = MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F);
         AttackAura.otvodkaYaw = 0.0F;
         AttackAura.otvodkaPitch = 0.0F;
         RotationStorage.update(new Rotation(newYaw, newPitch), 360.0F, 360.0F, 40.0F, 35.0F, 1, 1, false);
      }
   }

   private float follow(float error, boolean yawAxis, float followGain, float maxSpeed, float maxAccel, float dt) {
      float vel = yawAxis ? this.velYaw : this.velPitch;
      float desiredVel = MathHelper.clamp(error * followGain, -maxSpeed, maxSpeed);
      float dv = MathHelper.clamp(desiredVel - vel, -maxAccel * dt, maxAccel * dt);
      vel += dv;
      if (yawAxis) {
         this.velYaw = vel;
      } else {
         this.velPitch = vel;
      }

      return vel * dt;
   }
}
