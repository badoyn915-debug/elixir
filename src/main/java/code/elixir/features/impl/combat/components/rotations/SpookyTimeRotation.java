package code.elixir.features.impl.combat.components.rotations;

import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.rotate.Rotation;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SpookyTimeRotation extends RotationsSystem implements QuickImports {
   private float lastYaw = Float.NaN;
   private float lastPitch = Float.NaN;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;
   private double currentOffsetX = 0.0;
   private double currentOffsetY = 0.68;
   private double currentOffsetZ = 0.0;
   private double targetOffsetX = 0.0;
   private double targetOffsetY = 0.68;
   private double targetOffsetZ = 0.0;
   private int driftTimer = 0;
   private int lastTargetId = -1;
   private long lastUpdateMs = 0L;
   private float jitterYaw = 0.0F;
   private float jitterPitch = 0.0F;

   @Override
   public void reset() {
      super.reset();
      this.lastYaw = Float.NaN;
      this.lastPitch = Float.NaN;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.currentOffsetX = 0.0;
      this.currentOffsetY = 0.68;
      this.currentOffsetZ = 0.0;
      this.targetOffsetX = 0.0;
      this.targetOffsetY = 0.68;
      this.targetOffsetZ = 0.0;
      this.driftTimer = 0;
      this.lastTargetId = -1;
      this.lastUpdateMs = 0L;
      this.jitterYaw = 0.0F;
      this.jitterPitch = 0.0F;
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         long now = System.currentTimeMillis();
         float deltaSec = this.lastUpdateMs == 0L ? 0.05F : (float)(now - this.lastUpdateMs) / 1000.0F;
         deltaSec = MathHelper.clamp(deltaSec, 0.001F, 0.1F);
         this.lastUpdateMs = now;
         if (Float.isNaN(this.lastYaw)) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
            this.lastTargetId = target.getId();
         }

         if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.currentOffsetX = 0.0;
            this.currentOffsetY = 0.68;
            this.currentOffsetZ = 0.0;
            this.targetOffsetX = 0.0;
            this.targetOffsetY = 0.68;
            this.targetOffsetZ = 0.0;
            this.driftTimer = 0;
            this.jitterYaw = 0.0F;
            this.jitterPitch = 0.0F;
         }

         Box box = this.getPredictedBox(target);
         Vec3d targetVelVec = target.getVelocity();
         double targetVelocity = targetVelVec != null ? Math.hypot(targetVelVec.x, targetVelVec.z) : 0.0;
         int dynamicDriftMax = targetVelocity > 0.1 ? 8 : 22;
         if (this.driftTimer <= 0) {
            double safeWidth = box.getLengthX() * 0.15;
            this.targetOffsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * safeWidth;
            this.targetOffsetY = 0.6 + ThreadLocalRandom.current().nextDouble() * 0.14;
            this.targetOffsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * safeWidth;
            this.driftTimer = ThreadLocalRandom.current().nextInt(dynamicDriftMax, dynamicDriftMax + 14);
         }

         this.driftTimer--;
         double lerpSpeed = targetVelocity > 0.1 ? 0.12 : 0.08;
         this.currentOffsetX = this.currentOffsetX + (this.targetOffsetX - this.currentOffsetX) * lerpSpeed;
         this.currentOffsetY = this.currentOffsetY + (this.targetOffsetY - this.currentOffsetY) * lerpSpeed;
         this.currentOffsetZ = this.currentOffsetZ + (this.targetOffsetZ - this.currentOffsetZ) * lerpSpeed;
         Vec3d center = box.getCenter();
         double aimY = box.minY + box.getLengthY() * this.currentOffsetY;
         Vec3d aimPoint = new Vec3d(center.x + this.currentOffsetX, aimY, center.z + this.currentOffsetZ);
         Vec3d eyes = mc.player.getEyePos();
         Vec3d diff = aimPoint.subtract(eyes);
         float rawTargetYaw = (float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
         float rawTargetPitch = (float)(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
         rawTargetPitch = MathHelper.clamp(rawTargetPitch, -89.0F, 89.0F);
         float diffYaw = MathHelper.wrapDegrees(rawTargetYaw - this.lastYaw);
         float diffPitch = rawTargetPitch - this.lastPitch;
         float angularDist = (float)Math.hypot(diffYaw, diffPitch);
         float speedNorm = MathHelper.clamp(angularDist / 35.0F, 0.08F, 1.0F);
         float easedSpeed = HumanAim.bezierEase(speedNorm);
         float speedYaw = MathHelper.clamp(0.24F + 0.38F * easedSpeed, 0.16F, 0.65F);
         float speedPitch = speedYaw * 0.55F;
         float targetVelYaw = diffYaw * speedYaw;
         float targetVelPitch = diffPitch * speedPitch;
         float maxAccelYaw = 3.5F;
         float maxAccelPitch = 2.2F;
         float targetAccelYaw = targetVelYaw - this.velYaw;
         float targetAccelPitch = targetVelPitch - this.velPitch;
         this.velYaw = this.velYaw + MathHelper.clamp(targetAccelYaw, -maxAccelYaw, maxAccelYaw);
         this.velPitch = this.velPitch + MathHelper.clamp(targetAccelPitch, -maxAccelPitch, maxAccelPitch);
         this.velYaw = MathHelper.clamp(this.velYaw, -19.0F, 19.0F);
         this.velPitch = MathHelper.clamp(this.velPitch, -10.5F, 10.5F);
         this.jitterYaw = this.jitterYaw + (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.12F;
         this.jitterPitch = this.jitterPitch + (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.12F;
         this.jitterYaw *= 0.7F;
         this.jitterPitch *= 0.7F;
         float effectiveVelYaw = this.velYaw + this.jitterYaw;
         float effectiveVelPitch = this.velPitch + this.jitterPitch;
         float baseDeltaYaw = GCDUtil.getFixedRotation(effectiveVelYaw);
         float baseDeltaPitch = GCDUtil.getFixedRotation(effectiveVelPitch);
         if (ThreadLocalRandom.current().nextDouble() < 0.04 && Math.abs(baseDeltaYaw) > GCDUtil.getGCDValue()) {
            baseDeltaYaw -= Math.signum(baseDeltaYaw) * GCDUtil.getGCDValue();
         }

         float outYaw = this.lastYaw + baseDeltaYaw;
         float outPitch = MathHelper.clamp(this.lastPitch + baseDeltaPitch, -89.0F, 89.0F);
         this.lastYaw = outYaw;
         this.lastPitch = outPitch;
         RotationStorage.update(new Rotation(outYaw, outPitch), 360.0F, 360.0F, 360.0F, 360.0F, 8, 1, false);
      } else {
         this.reset();
      }
   }
}
