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

public class FuntimeTestRotation extends RotationsSystem implements QuickImports {
   private float lastYaw = Float.NaN;
   private float lastPitch = Float.NaN;
   private float velYaw = 0.0F;
   private float velPitch = 0.0F;
   private double organicPhase = 0.0;
   private int lastTargetId = -1;
   private float polarOvershootYaw = 0.0F;
   private float polarOvershootPitch = 0.0F;
   private int ticksOnTarget = 0;
   private FuntimeTestRotation.AimStyle currentStyle = FuntimeTestRotation.AimStyle.AGGRESSIVE;
   private int styleTicksRemaining = 45;

   @Override
   public void reset() {
      super.reset();
      this.lastYaw = Float.NaN;
      this.lastPitch = Float.NaN;
      this.velYaw = 0.0F;
      this.velPitch = 0.0F;
      this.organicPhase = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
      this.lastTargetId = -1;
      this.polarOvershootYaw = 0.0F;
      this.polarOvershootPitch = 0.0F;
      this.ticksOnTarget = 0;
      this.currentStyle = FuntimeTestRotation.AimStyle.AGGRESSIVE;
      this.styleTicksRemaining = 45;
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         this.styleTicksRemaining--;
         if (this.styleTicksRemaining <= 0) {
            FuntimeTestRotation.AimStyle[] styles = FuntimeTestRotation.AimStyle.values();

            FuntimeTestRotation.AimStyle next;
            do {
               next = styles[ThreadLocalRandom.current().nextInt(styles.length)];
            } while (next == this.currentStyle);

            this.currentStyle = next;
            this.styleTicksRemaining = ThreadLocalRandom.current().nextInt(40, 65);
         }

         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         if (Float.isNaN(this.lastYaw)) {
            this.lastYaw = currentYaw;
            this.lastPitch = currentPitch;
            this.lastTargetId = target.getId();
            this.organicPhase = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
         }

         if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.velYaw = 0.0F;
            this.velPitch = 0.0F;
            this.ticksOnTarget = 0;
            this.polarOvershootYaw = 0.0F;
            this.polarOvershootPitch = 0.0F;
         }

         Box box = this.getPredictedBox(target);
         this.organicPhase += 0.035;

         double widthMultiplier = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.32;
            case TRACKING -> 0.18;
            case DISPERSION -> 0.44;
         };

         double heightBase = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.72;
            case TRACKING -> 0.68;
            case DISPERSION -> 0.62;
         };

         double heightAmp = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.08;
            case TRACKING -> 0.05;
            case DISPERSION -> 0.14;
         };
         double widthBound = box.getLengthX() * widthMultiplier;
         double heightBound = box.getLengthY() * heightAmp;
         Vec3d center = box.getCenter();
         double aimX = center.x + Math.sin(this.organicPhase * 1.1) * widthBound;
         double aimY = box.minY + box.getLengthY() * heightBase + Math.sin(this.organicPhase * 1.7 + 0.6) * heightBound;
         double aimZ = center.z + Math.cos(this.organicPhase * 1.1) * widthBound;
         Vec3d aimPoint = new Vec3d(aimX, aimY, aimZ);
         Vec3d eyes = mc.player.getEyePos();
         Vec3d diff = aimPoint.subtract(eyes);
         float rawTargetYaw = (float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
         float rawTargetPitch = (float)(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
         rawTargetPitch = MathHelper.clamp(rawTargetPitch, -89.0F, 89.0F);
         float diffYaw = MathHelper.wrapDegrees(rawTargetYaw - currentYaw);
         float diffPitch = rawTargetPitch - currentPitch;
         float angularDist = (float)Math.hypot(diffYaw, diffPitch);

         float overshootFactor = switch (this.currentStyle) {
            case AGGRESSIVE -> 1.25F;
            case TRACKING -> 0.5F;
            case DISPERSION -> 1.0F;
         };
         if (angularDist > 12.0F && this.ticksOnTarget > 4) {
            float overshootScale = Math.min(angularDist / 30.0F, 1.0F) * overshootFactor;
            this.polarOvershootYaw = (float)Math.sin(this.organicPhase * 2.3) * 2.2F * overshootScale;
            this.polarOvershootPitch = (float)Math.cos(this.organicPhase * 2.3) * 1.2F * overshootScale;
            this.ticksOnTarget = 0;
         }

         if (angularDist < 6.0F) {
            this.polarOvershootYaw *= 0.75F;
            this.polarOvershootPitch *= 0.75F;
            this.ticksOnTarget++;
         }

         diffYaw += this.polarOvershootYaw;
         diffPitch += this.polarOvershootPitch;
         float speedNorm = MathHelper.clamp(angularDist / 32.0F, 0.12F, 1.0F);
         float easedSpeed = HumanAim.bezierEase(speedNorm);

         float baseSpeed = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.38F;
            case TRACKING -> 0.3F;
            case DISPERSION -> 0.34F;
         };

         float maxSpeed = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.88F;
            case TRACKING -> 0.78F;
            case DISPERSION -> 0.84F;
         };
         float speed = MathHelper.clamp(baseSpeed + 0.46F * easedSpeed, 0.22F, maxSpeed);
         float microYaw = (float)(Math.sin(this.organicPhase * 3.4) * 0.18 + Math.sin(this.organicPhase * 5.1) * 0.1);
         float microPitch = (float)(Math.sin(this.organicPhase * 2.9 + 1.0) * 0.12 + Math.sin(this.organicPhase * 4.7) * 0.08);
         float targetVelYaw = diffYaw * speed + microYaw;
         float targetVelPitch = diffPitch * speed + microPitch;

         float dampingFactor = switch (this.currentStyle) {
            case AGGRESSIVE -> 0.45F;
            case TRACKING -> 0.32F;
            case DISPERSION -> 0.38F;
         };
         this.velYaw = this.velYaw + (targetVelYaw - this.velYaw) * dampingFactor;
         this.velPitch = this.velPitch + (targetVelPitch - this.velPitch) * dampingFactor;
         float deltaYaw = GCDUtil.getFixedRotation(this.velYaw);
         float deltaPitch = GCDUtil.getFixedRotation(this.velPitch);
         float outYaw = currentYaw + deltaYaw;
         float outPitch = MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F);
         this.lastYaw = outYaw;
         this.lastPitch = outPitch;
         RotationStorage.update(new Rotation(outYaw, outPitch), 360.0F, 360.0F, 360.0F, 360.0F, 8, 1, false);
      } else {
         this.reset();
      }
   }

   public static enum AimStyle {
      AGGRESSIVE,
      TRACKING,
      DISPERSION;
   }
}
