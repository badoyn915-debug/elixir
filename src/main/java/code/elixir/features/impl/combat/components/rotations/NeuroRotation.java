package code.elixir.features.impl.combat.components.rotations;

import code.elixir.features.impl.combat.Neuro;
import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.rotate.Rotation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NeuroRotation extends RotationsSystem implements QuickImports {
   private int playIndex = 0;
   private float lastYaw = Float.NaN;
   private float lastPitch = Float.NaN;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;

   @Override
   public void reset() {
      super.reset();
      this.playIndex = 0;
      this.lastYaw = Float.NaN;
      this.lastPitch = Float.NaN;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         if (Float.isNaN(this.lastYaw)) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
         }

         double[] angles = this.computeAngles(target);
         float targetYaw = (float)angles[0];
         float targetPitch = (float)angles[1];
         float yawNoise = 0.0F;
         float pitchNoise = 0.0F;
         if (!Neuro.trainingData.isEmpty()) {
            if (this.playIndex >= Neuro.trainingData.size()) {
               this.playIndex = 0;
            }

            Neuro.Record r = Neuro.trainingData.get(this.playIndex);
            yawNoise = r.yaw % 360.0F / 180.0F * 0.6F;
            pitchNoise = r.pitch % 180.0F / 90.0F * 0.4F;
            this.playIndex++;
         } else {
            yawNoise = (float)(Math.random() - 0.5) * 0.5F;
            pitchNoise = (float)(Math.random() - 0.5) * 0.35F;
         }

         float diffYaw = MathHelper.wrapDegrees(targetYaw + yawNoise - this.lastYaw);
         float diffPitch = targetPitch + pitchNoise - this.lastPitch;
         float angularDist = (float)Math.hypot(diffYaw, diffPitch);
         float speedNorm = MathHelper.clamp(angularDist / 50.0F, 0.1F, 1.0F);
         float speed = MathHelper.clamp(0.25F + 0.55F * HumanAim.bezierEase(speedNorm) * HumanAim.speedJitter(), 0.18F, 0.85F);
         float targetVelYaw = diffYaw * speed;
         float targetVelPitch = diffPitch * speed;
         this.velYaw = this.velYaw + (targetVelYaw - this.velYaw) * 0.7F;
         this.velPitch = this.velPitch + (targetVelPitch - this.velPitch) * 0.7F;
         float deltaYaw = GCDUtil.getFixedRotation(this.velYaw);
         float deltaPitch = GCDUtil.getFixedRotation(this.velPitch);
         float outYaw = this.lastYaw + deltaYaw;
         float outPitch = MathHelper.clamp(this.lastPitch + deltaPitch, -89.0F, 89.0F);
         this.lastYaw = outYaw;
         this.lastPitch = outPitch;
         RotationStorage.update(new Rotation(outYaw, outPitch), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, false);
      } else {
         this.reset();
      }
   }

   private double[] computeAngles(LivingEntity entity) {
      Vec3d eyes = mc.player.getEyePos();
      Box box = this.getPredictedBox(entity);
      Vec3d point = box.getCenter();
      double dx = point.x - eyes.x;
      double dy = point.y - eyes.y;
      double dz = point.z - eyes.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      double yaw = horizontal < 0.001 ? mc.player.getYaw() : Math.toDegrees(Math.atan2(-dx, dz));
      double full = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double pitch = full < 0.001 ? mc.player.getPitch() : -Math.toDegrees(Math.asin(MathHelper.clamp(dy / full, -1.0, 1.0)));
      return new double[]{yaw, pitch};
   }
}
