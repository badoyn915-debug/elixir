package code.elixir.features.impl.combat.components.rotations;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.BestPoint;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.rotate.MultipointUtils;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.features.rotate.RotationUtils;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class SlothACRotation extends RotationsSystem implements QuickImports {
   private long lastUpdateMs = 0L;
   private int lastTargetId = -1;
   private float currentVelYaw = 0.0F;
   private float currentVelPitch = 0.0F;
   private double chestOffsetRatio = 0.58;
   private int swayTimer = 0;

   @Override
   public void reset() {
      this.lastUpdateMs = 0L;
      this.lastTargetId = -1;
      this.currentVelYaw = 0.0F;
      this.currentVelPitch = 0.0F;
      this.chestOffsetRatio = 0.58;
      this.swayTimer = 0;
      HumanAim.reset();
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (target != null && mc.player != null) {
         long now = System.currentTimeMillis();
         float dt = this.lastUpdateMs == 0L ? 1.0F : MathHelper.clamp((float)(now - this.lastUpdateMs) / 50.0F, 0.5F, 2.0F);
         this.lastUpdateMs = now;

         if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.currentVelYaw = 0.0F;
            this.currentVelPitch = 0.0F;
            this.chestOffsetRatio = 0.58;
            this.swayTimer = 0;
         }

         // Target the nearest point inside the bounding box with subtle human chest sway
         if (this.swayTimer <= 0) {
            this.chestOffsetRatio = 0.52 + ThreadLocalRandom.current().nextDouble() * 0.16;
            this.swayTimer = 8 + ThreadLocalRandom.current().nextInt(16);
         }
         this.swayTimer--;

         Box box = target.getBoundingBox();
         Vec3d center = box.getCenter();
         Vec3d targetPoint = new Vec3d(center.x, box.minY + box.getLengthY() * this.chestOffsetRatio, center.z);
         Vec3d nearest = BestPoint.getNearestPoint(target);
         if (nearest == null) {
            nearest = MultipointUtils.getClosestPoint(target);
         }
         if (nearest != null) {
            targetPoint = targetPoint.multiply(0.3).add(nearest.multiply(0.7));
         }

         Vec2f desired = RotationUtils.getRotations(targetPoint);
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();

         float diffYaw = MathHelper.wrapDegrees(desired.x - currentYaw);
         float diffPitch = desired.y - currentPitch;
         float dist = (float)Math.hypot(diffYaw, diffPitch);

         // Dynamic human tracking speed: fast when acquiring, precise when on target
         float speedProgress = MathHelper.clamp(dist / 35.0F, 0.15F, 1.0F);
         float easedGain = 0.35F + 0.45F * (speedProgress * speedProgress * (3.0F - 2.0F * speedProgress));

         // Micro-hand tremor
         float tremor = (dist < 12.0F) ? (HumanAim.noiseYaw() * 0.12F) : 0.0F;
         float targetVelYaw = (diffYaw + tremor) * easedGain;
         float targetVelPitch = diffPitch * easedGain * 0.90F;

         // Smooth acceleration limits to prevent snappy machine-detection
         float maxAccelYaw = 14.0F * dt;
         float maxAccelPitch = 8.0F * dt;
         this.currentVelYaw += MathHelper.clamp(targetVelYaw - this.currentVelYaw, -maxAccelYaw, maxAccelYaw);
         this.currentVelPitch += MathHelper.clamp(targetVelPitch - this.currentVelPitch, -maxAccelPitch, maxAccelPitch);

         // Damping
         this.currentVelYaw *= 0.88F;
         this.currentVelPitch *= 0.88F;

         // Minecraft mouse sensitivity GCD quantization
         float stepYaw = this.currentVelYaw * dt;
         float stepPitch = this.currentVelPitch * dt;
         float deltaYaw = GCDUtil.getFixedRotation(stepYaw);
         float deltaPitch = GCDUtil.getFixedRotation(stepPitch);

         if (deltaYaw == 0.0F && Math.abs(diffYaw) > 0.4F) {
            deltaYaw = diffYaw > 0.0F ? GCDUtil.getGCDValue() : -GCDUtil.getGCDValue();
         }
         if (deltaPitch == 0.0F && Math.abs(diffPitch) > 0.4F) {
            deltaPitch = diffPitch > 0.0F ? GCDUtil.getGCDValue() : -GCDUtil.getGCDValue();
         }

         float newYaw = currentYaw + deltaYaw;
         float newPitch = MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F);

         AttackAura.otvodkaYaw = 0.0F;
         AttackAura.otvodkaPitch = 0.0F;
         RotationStorage.update(new Rotation(newYaw, newPitch), 360.0F, 360.0F, 40.0F, 35.0F, 1, 1, false);
      }
   }
}
