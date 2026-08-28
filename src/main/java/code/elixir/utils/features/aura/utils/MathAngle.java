package code.elixir.utils.features.aura.utils;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;

public final class MathAngle implements QuickImports {
   public static Turns fromVec2f(Vec2f vector2f) {
      return new Turns(vector2f.y, vector2f.x);
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   public static Turns fromVec3d(Vec3d vector) {
      return new Turns(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static Turns calculateDelta(Turns start, Turns end) {
      float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
      float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
      return new Turns(deltaYaw, deltaPitch);
   }

   public static Turns calculateAngle(Vec3d to) {
      return mc.player == null ? new Turns(0.0F, 0.0F) : fromVec3d(to.subtract(mc.player.getEyePos()));
   }

   public static Turns pitch(float pitch) {
      return mc.player == null ? new Turns(0.0F, pitch) : new Turns(mc.player.getYaw(), pitch);
   }

   public static Turns cameraAngle() {
      return mc.player == null ? new Turns(0.0F, 0.0F) : new Turns(mc.player.getYaw(), mc.player.getPitch());
   }

   public static boolean rayTrace(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
      if (mc.player != null && mc.world != null) {
         HitResult result = rayTrace(distance, yaw, pitch);
         Vec3d startPoint = mc.player.getPos().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
         double distancePow2 = Math.pow(distance, 2.0);
         AttackAura attackAuraModule1 = AttackAura.getInstance();
         if (result != null) {
            distancePow2 = startPoint.squaredDistanceTo(result.getPos());
         }

         Vec3d rotationVector;
         EntityHitResult ehr;
         if ((
               ehr = ProjectileUtil.raycast(
                  mc.player,
                  startPoint,
                  startPoint.add(rotationVector = getRotationVector(pitch, yaw).multiply(distance)),
                  mc.player.getBoundingBox().stretch(rotationVector).expand(1.0, 1.0, 1.0),
                  e -> !e.isSpectator() && e.canHit() && e == entity,
                  Math.max(distancePow2, Math.pow(wallDistance, 2.0))
               )
            )
            != null) {
            boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(wallDistance, 2.0);
            boolean wallMissing = result == null;
            boolean var10000 = startPoint.squaredDistanceTo(ehr.getPos()) < distancePow2;
            if (startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(distance, 2.0) && ehr.getPos().y >= entity.getY() + entity.getHeight() * 0.3) {
               return ehr.getEntity() == entity;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static float getGCDValue() {
      double sensitivity = (Double)mc.options.getMouseSensitivity().getValue();
      double value = sensitivity * 0.6 + 0.2;
      double result = Math.pow(value, 3.0) * 0.8;
      return (float)result * 0.15F;
   }

   public static HitResult rayTrace(double dst, float yaw, float pitch) {
      if (mc.player != null && mc.world != null) {
         Vec3d vec3d = mc.player.getCameraPosVec(1.0F);
         Vec3d vec3d2 = getRotationVector(pitch, yaw);
         Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
         return mc.world.raycast(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      } else {
         return null;
      }
   }

   @NotNull
   public static Vec3d getRotationVector(float yaw, float pitch) {
      return new Vec3d(
         MathHelper.sin(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0)),
         -MathHelper.sin(yaw * (float) (Math.PI / 180.0)),
         MathHelper.cos(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0))
      );
   }

   public static Vec3d getClosestVec(Entity entity) {
      if (mc.player != null && entity != null) {
         Vec3d eyePosVec = mc.player.getEyePos();
         Box boundingBox = entity.getBoundingBox();
         return new Vec3d(
            MathHelper.clamp(eyePosVec.x, boundingBox.minX, boundingBox.maxX),
            MathHelper.clamp(eyePosVec.y, boundingBox.minY, boundingBox.maxY),
            MathHelper.clamp(eyePosVec.z, boundingBox.minZ, boundingBox.maxZ)
         );
      } else {
         return Vec3d.ZERO;
      }
   }

   public static double getDistanceEyePos(Entity target) {
      if (mc.player != null && target != null) {
         Vec3d closestHitboxPoint = getClosestVec(target);
         return mc.player.getEyePos().distanceTo(closestHitboxPoint);
      } else {
         return 0.0;
      }
   }

   public static double getStrictDistance(Entity entity) {
      return getDistanceEyePos(entity);
   }

   private MathAngle() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
