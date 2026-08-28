package code.elixir.utils.features.combat;

import code.elixir.features.impl.combat.CombatModules;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PredictUtils implements QuickImports {
   private static final Map<UUID, PredictUtils.PositionData> positionCache = new ConcurrentHashMap<>();

   public static void updateEntity(LivingEntity entity) {
      PredictUtils.PositionData data = positionCache.computeIfAbsent(entity.getUuid(), k -> new PredictUtils.PositionData());
      data.update(entity.getX(), entity.getY(), entity.getZ());
   }

   public static PredictUtils.PositionData getData(LivingEntity entity) {
      return positionCache.get(entity.getUuid());
   }

   public static Vec3d predict(LivingEntity entity, int ticks, float extraForward, boolean isMeFlying) {
      PredictUtils.PositionData data = getData(entity);
      Vec3d pos = new Vec3d(entity.getX(), entity.getY() + entity.getStandingEyeHeight() / 2.0F, entity.getZ());
      if (data == null) {
         return predictElytraPhysics(entity, pos, ticks);
      } else {
         Vec3d forward = data.getResolvedForward();
         double speed = data.getLastSpeed();
         boolean isHighSpeed = data.isSpeedChanged();
         if (entity.isGliding()) {
            double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
            double verticalSpeed = Math.abs(forward.y) * 20.0;
            if (horizontalSpeed <= 5.0 && verticalSpeed <= 5.0) {
               return pos;
            } else {
               boolean shouldPredict = isMeFlying && entity.isGliding() && isHighSpeed;
               float predictMultiplier = shouldPredict ? ticks + 2 + extraForward : ticks;
               Vec3d linearPredict = pos.add(forward.multiply(predictMultiplier, predictMultiplier, predictMultiplier));
               Vec3d physicsPredict = predictElytraPhysics(entity, pos, ticks);
               double weight = MathHelper.clamp(speed / 50.0, 0.3, 0.9);
               return new Vec3d(
                  MathHelper.lerp(weight, physicsPredict.x, linearPredict.x),
                  MathHelper.lerp(weight, physicsPredict.y, linearPredict.y),
                  MathHelper.lerp(weight, physicsPredict.z, linearPredict.z)
               );
            }
         } else {
            return speed > 1.0 ? pos.add(forward.multiply(ticks, ticks, ticks)) : pos;
         }
      }
   }

   public static Vec3d predict(LivingEntity entity, Vec3d pos, int ticks) {
      PredictUtils.PositionData data = getData(entity);
      if (data != null && entity.isGliding()) {
         Vec3d forward = data.getResolvedForward();
         double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
         double verticalSpeed = Math.abs(forward.y) * 20.0;
         return horizontalSpeed <= 5.0 && verticalSpeed <= 5.0 ? pos : pos.add(forward.multiply(ticks, ticks, ticks));
      } else {
         return predictElytraPhysics(entity, pos, ticks);
      }
   }

   public static Vec3d predictElytraPhysics(LivingEntity entity, Vec3d pos, int ticks) {
      Vec3d velocity = entity.getVelocity();
      if (!entity.isGliding()) {
         return pos.add(velocity.multiply(ticks, ticks, ticks));
      } else {
         double horizontalDelta = Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20.0;
         double verticalDelta = Math.abs(entity.getY() - entity.prevY) * 20.0;
         if (horizontalDelta <= 5.0 && verticalDelta <= 5.0) {
            return pos;
         } else {
            for (int i = 0; i < ticks; i++) {
               Vec3d rotation = entity.getRotationVector();
               float pitchRad = (float)Math.toRadians(entity.getPitch());
               double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
               double velocityLength = velocity.length();
               float cos = MathHelper.cos(pitchRad);
               cos = (float)(cos * cos * Math.min(1.0, rotation.length() / 0.4));
               velocity = velocity.add(0.0, -0.08 * (-1.0 + cos * 0.75), 0.0);
               if (velocity.y < 0.0 && horizontalSpeed > 0.0) {
                  double d5 = velocity.y * -0.1 * cos;
                  velocity = velocity.add(rotation.x * d5 / horizontalSpeed, d5, rotation.z * d5 / horizontalSpeed);
               }

               if (pitchRad < 0.0F && horizontalSpeed > 0.0) {
                  double lift = velocityLength * -MathHelper.sin(pitchRad) * 0.04;
                  velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2, -rotation.z * lift / horizontalSpeed);
               }

               if (horizontalSpeed > 0.0) {
                  velocity = velocity.add(
                     (rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1,
                     0.0,
                     (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1
                  );
               }

               velocity = velocity.multiply(0.99, 0.98, 0.99);
               pos = pos.add(velocity);
            }

            return pos;
         }
      }
   }

   public static Vec3d bypasselytrahacking(LivingEntity target) {
      Vec3d interpolatedRotation = Vec3d.fromPolar(target.getLerpTargetPitch(), target.getLerpTargetYaw());
      Vec3d rotationVector = target.getRotationVector();
      Vec3d relativePos = target.getPos().add(0.0, target.getHeight() * 0.6F, 0.0).subtract(mc.player.getEyePos());
      Vec3d blendedDirection = interpolatedRotation.normalize().lerp(rotationVector, interpolatedRotation.length());
      return relativePos.add(blendedDirection.normalize().multiply(CombatModules.getElytraForward()));
   }

   public static void cleanup() {
      long now = System.currentTimeMillis();
      positionCache.entrySet().removeIf(e -> now - e.getValue().getLastUpdate() > 10000L);
   }

   public static void clear() {
      positionCache.clear();
   }

   public static class PositionData {
      private double serverX;
      private double serverY;
      private double serverZ;
      private double prevServerX;
      private double prevServerY;
      private double prevServerZ;
      private double backUpX;
      private double backUpY;
      private double backUpZ;
      private double lastSpeed;
      private double prevSpeed;
      private long lastUpdate;

      public Vec3d getResolvedPos() {
         return new Vec3d(this.serverX, this.serverY, this.serverZ);
      }

      public Vec3d getResolvedForward() {
         return new Vec3d(this.serverX - this.prevServerX, this.serverY - this.prevServerY, this.serverZ - this.prevServerZ);
      }

      public void update(double x, double y, double z) {
         this.backUpX = this.prevServerX;
         this.backUpY = this.prevServerY;
         this.backUpZ = this.prevServerZ;
         this.prevServerX = this.serverX;
         this.prevServerY = this.serverY;
         this.prevServerZ = this.serverZ;
         this.serverX = x;
         this.serverY = y;
         this.serverZ = z;
         this.prevSpeed = this.lastSpeed;
         this.lastSpeed = this.getResolvedForward().length() * 20.0;
         this.lastUpdate = System.currentTimeMillis();
      }

      public boolean isSpeedChanged() {
         return this.lastSpeed >= 20.0 || this.lastSpeed != this.prevSpeed && this.lastSpeed == 0.0;
      }

      public double getServerX() {
         return this.serverX;
      }

      public double getServerY() {
         return this.serverY;
      }

      public double getServerZ() {
         return this.serverZ;
      }

      public double getPrevServerX() {
         return this.prevServerX;
      }

      public double getPrevServerY() {
         return this.prevServerY;
      }

      public double getPrevServerZ() {
         return this.prevServerZ;
      }

      public double getBackUpX() {
         return this.backUpX;
      }

      public double getBackUpY() {
         return this.backUpY;
      }

      public double getBackUpZ() {
         return this.backUpZ;
      }

      public double getLastSpeed() {
         return this.lastSpeed;
      }

      public double getPrevSpeed() {
         return this.prevSpeed;
      }

      public long getLastUpdate() {
         return this.lastUpdate;
      }
   }
}
