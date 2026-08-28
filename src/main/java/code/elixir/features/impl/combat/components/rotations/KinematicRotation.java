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

public class KinematicRotation extends RotationsSystem implements QuickImports {
   private float lastYaw = Float.NaN;
   private float lastPitch = Float.NaN;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;
   private double smoothAimX = 0.0;
   private double smoothAimY = 0.0;
   private double smoothAimZ = 0.0;
   private double targetAimX = 0.0;
   private double targetAimY = 0.0;
   private double targetAimZ = 0.0;
   private int aimPointTimer = 0;
   private float tremorPhase = 0.0F;

   @Override
   public void reset() {
      super.reset();
      this.lastYaw = Float.NaN;
      this.lastPitch = Float.NaN;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.smoothAimX = 0.0;
      this.smoothAimY = 0.0;
      this.smoothAimZ = 0.0;
      this.targetAimX = 0.0;
      this.targetAimY = 0.0;
      this.targetAimZ = 0.0;
      this.aimPointTimer = 0;
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         if (Float.isNaN(this.lastYaw)) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
            this.tremorPhase = (float)(ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0);
         }

         Box box = this.getPredictedBox(target);
         if (this.aimPointTimer <= 0) {
            double halfW = box.getLengthX() * 0.32;
            double halfH = box.getLengthY() * 0.32;
            this.targetAimX = MathUtils.random((float)(-halfW), (float)halfW);
            this.targetAimY = MathUtils.random((float)(-halfH), (float)halfH);
            this.targetAimZ = MathUtils.random((float)(-halfW), (float)halfW);
            this.aimPointTimer = ThreadLocalRandom.current().nextInt(14, 28);
         }

         this.aimPointTimer--;
         this.smoothAimX = this.smoothAimX + (this.targetAimX - this.smoothAimX) * 0.12;
         this.smoothAimY = this.smoothAimY + (this.targetAimY - this.smoothAimY) * 0.12;
         this.smoothAimZ = this.smoothAimZ + (this.targetAimZ - this.smoothAimZ) * 0.12;
         Vec3d center = box.getCenter();
         Vec3d aimPoint = center.add(this.smoothAimX, this.smoothAimY, this.smoothAimZ);
         Vec3d eyes = mc.player.getEyePos();
         Vec3d diff = aimPoint.subtract(eyes);
         float rawTargetYaw = (float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
         float rawTargetPitch = (float)(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
         rawTargetPitch = MathHelper.clamp(rawTargetPitch, -89.0F, 89.0F);
         float diffYaw = MathHelper.wrapDegrees(rawTargetYaw - this.lastYaw);
         float diffPitch = rawTargetPitch - this.lastPitch;
         float angularDist = (float)Math.hypot(diffYaw, diffPitch);
         float speedNorm = MathHelper.clamp(angularDist / 35.0F, 0.12F, 1.0F);
         float easedSpeed = HumanAim.bezierEase(speedNorm);
         float speedJitter = HumanAim.speedJitter();
         float speed = MathHelper.clamp(0.28F + 0.58F * easedSpeed * speedJitter, 0.22F, 0.92F);
         float noiseY = (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.25F;
         float noiseP = (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.25F;
         float targetVelYaw = diffYaw * speed + noiseY;
         float targetVelPitch = diffPitch * speed + noiseP;
         this.velYaw = this.velYaw + (targetVelYaw - this.velYaw) * 0.85F;
         this.velPitch = this.velPitch + (targetVelPitch - this.velPitch) * 0.85F;
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
}
