package code.elixir.features.impl.combat.components.rotations;

import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.math.MathUtils;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TestRotation extends RotationsSystem implements QuickImports {
   private float lastYaw = Float.NaN;
   private float lastPitch = Float.NaN;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;
   private double currentOffsetX = 0.0;
   private double currentOffsetY = 0.72;
   private double currentOffsetZ = 0.0;
   private double targetOffsetX = 0.0;
   private double targetOffsetY = 0.72;
   private double targetOffsetZ = 0.0;
   private int driftTimer = 0;
   private int lastTargetId = -1;
   private float overshootYaw = 0.0F;
   private float overshootPitch = 0.0F;
   private int ticksOnTarget = 0;
   private double tremorTime = 0.0;

   @Override
   public void reset() {
      super.reset();
      this.lastYaw = Float.NaN;
      this.lastPitch = Float.NaN;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.currentOffsetX = 0.0;
      this.currentOffsetY = 0.72;
      this.currentOffsetZ = 0.0;
      this.targetOffsetX = 0.0;
      this.targetOffsetY = 0.72;
      this.targetOffsetZ = 0.0;
      this.driftTimer = 0;
      this.lastTargetId = -1;
      this.overshootYaw = 0.0F;
      this.overshootPitch = 0.0F;
      this.ticksOnTarget = 0;
      this.tremorTime = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         if (Float.isNaN(this.lastYaw)) {
            this.lastYaw = currentYaw;
            this.lastPitch = currentPitch;
            this.lastTargetId = target.getId();
            this.tremorTime = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
         }

         Box box = this.getPredictedBox(target);
         if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.ticksOnTarget = 0;
            this.overshootYaw = 0.0F;
            this.overshootPitch = 0.0F;
            this.currentOffsetX = 0.0;
            this.currentOffsetY = 0.7;
            this.currentOffsetZ = 0.0;
            this.targetOffsetX = 0.0;
            this.targetOffsetY = 0.7;
            this.targetOffsetZ = 0.0;
            this.driftTimer = 0;
         }

         if (this.driftTimer <= 0) {
            double widthBound = box.getLengthX() * 0.28;
            this.targetOffsetX = MathUtils.random((float)(-widthBound), (float)widthBound);
            this.targetOffsetY = MathUtils.random(0.64F, 0.78F);
            this.targetOffsetZ = MathUtils.random((float)(-widthBound), (float)widthBound);
            this.driftTimer = ThreadLocalRandom.current().nextInt(14, 28);
         }

         this.driftTimer--;
         this.currentOffsetX = this.currentOffsetX + (this.targetOffsetX - this.currentOffsetX) * 0.1;
         this.currentOffsetY = this.currentOffsetY + (this.targetOffsetY - this.currentOffsetY) * 0.1;
         this.currentOffsetZ = this.currentOffsetZ + (this.targetOffsetZ - this.currentOffsetZ) * 0.1;
         Vec3d center = box.getCenter();
         double aimY = box.minY + box.getLengthY() * this.currentOffsetY;
         Vec3d aimPoint = new Vec3d(center.x + this.currentOffsetX, aimY, center.z + this.currentOffsetZ);
         Vec3d eyes = mc.player.getEyePos();
         Vec3d diff = aimPoint.subtract(eyes);
         float rawTargetYaw = (float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
         float rawTargetPitch = (float)(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
         rawTargetPitch = MathHelper.clamp(rawTargetPitch, -89.0F, 89.0F);
         float diffYaw = MathHelper.wrapDegrees(rawTargetYaw - currentYaw);
         float diffPitch = rawTargetPitch - currentPitch;
         float angularDist = (float)Math.hypot(diffYaw, diffPitch);
         if (angularDist > 22.0F && this.ticksOnTarget > 6) {
            float overshootScale = Math.min(angularDist / 40.0F, 1.2F);
            this.overshootYaw = MathUtils.random(-2.5F, 2.5F) * overshootScale;
            this.overshootPitch = MathUtils.random(-1.5F, 1.5F) * overshootScale;
            this.ticksOnTarget = 0;
         }

         if (angularDist < 6.0F) {
            this.overshootYaw *= 0.6F;
            this.overshootPitch *= 0.6F;
            this.ticksOnTarget++;
         }

         diffYaw += this.overshootYaw;
         diffPitch += this.overshootPitch;
         float speedNorm = MathHelper.clamp(angularDist / 45.0F, 0.08F, 1.0F);
         float easedSpeed = HumanAim.bezierEase(speedNorm);
         float speedJitter = HumanAim.speedJitter();
         float speed = MathHelper.clamp(0.24F + 0.54F * easedSpeed * speedJitter, 0.18F, 0.88F);
         this.tremorTime += 0.05;
         float tremorYaw = (float)(Math.sin(this.tremorTime * 8.5) * 0.028 + Math.sin(this.tremorTime * 11.2 + 0.9) * 0.016);
         float tremorPitch = (float)(Math.sin(this.tremorTime * 7.8 + 0.4) * 0.018 + Math.sin(this.tremorTime * 10.5 + 1.6) * 0.012);
         float targetVelYaw = diffYaw * speed + tremorYaw;
         float targetVelPitch = diffPitch * speed + tremorPitch;
         this.velYaw = this.velYaw + (targetVelYaw - this.velYaw) * 0.78F;
         this.velPitch = this.velPitch + (targetVelPitch - this.velPitch) * 0.78F;
         float deltaYaw = GCDUtil.getFixedRotation(this.velYaw);
         float deltaPitch = GCDUtil.getFixedRotation(this.velPitch);
         float outYaw = currentYaw + deltaYaw;
         float outPitch = MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F);
         this.lastYaw = outYaw;
         this.lastPitch = outPitch;
         RotationStorage.update(new Rotation(outYaw, outPitch), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, false);
      } else {
         this.reset();
      }
   }
}
