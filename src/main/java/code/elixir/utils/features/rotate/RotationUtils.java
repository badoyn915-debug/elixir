package code.elixir.utils.features.rotate;

import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;

public final class RotationUtils implements QuickImports {
   public static HitResult rayTrace(double dst, float yaw, float pitch) {
      Vec3d vec3d = mc.player.getCameraPosVec(1.0F);
      Vec3d vec3d2 = getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.world.raycast(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
   }

   static Vec3d getBestVector(Entity entity) {
      Vec3d eyePos = mc.player.getEyePos();
      Box box = entity.getBoundingBox();
      double step = 0.1;
      Vec3d bestVec = null;
      double closestDistance = Double.MAX_VALUE;

      for (double x = box.minX; x <= box.maxX; x += step) {
         for (double y = box.minY; y <= box.maxY; y += step) {
            for (double z = box.minZ; z <= box.maxZ; z += step) {
               Vec3d sample = new Vec3d(x, y, z);
               double dist = eyePos.distanceTo(sample);
               if (dist < closestDistance) {
                  closestDistance = dist;
                  bestVec = sample;
               }
            }
         }
      }

      return bestVec;
   }

   public static Rotation fromVec3d(Vec3d vector) {
      return new Rotation(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   @NotNull
   public static Vec3d getRotationVector(float yaw, float pitch) {
      return new Vec3d(
         MathHelper.sin(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0)),
         -MathHelper.sin(yaw * (float) (Math.PI / 180.0)),
         MathHelper.cos(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0))
      );
   }

   public static Vec2f getRotations(Entity entity) {
      return getRotations(entity.getX(), entity.getY(), entity.getZ());
   }

   public static Vec2f getRotations(Vec3d vec3d) {
      return getRotations(vec3d.x, vec3d.y, vec3d.z);
   }

   public static Vec2f getRotations(double x, double y, double z) {
      double deltaX = x - mc.player.getX();
      double deltaY = y - mc.player.getEyeY();
      double deltaZ = z - mc.player.getZ();
      double distance = MathHelper.sqrt((float)(deltaX * deltaX + deltaZ * deltaZ));
      float yaw = (float)(MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI) - 90.0);
      float pitch = (float)(-MathHelper.atan2(deltaY, distance) * (180.0 / Math.PI));
      return new Vec2f(yaw, pitch);
   }

   public static float[] getRotations(Direction direction) {
      float[] fArray;
      switch (direction) {
         case DOWN:
            float[] fArray2 = new float[2];
            fArray2[0] = mc.player.getYaw();
            fArray = fArray2;
            fArray2[1] = 90.0F;
            break;
         case UP:
            float[] fArray3 = new float[2];
            fArray3[0] = mc.player.getYaw();
            fArray = fArray3;
            fArray3[1] = -90.0F;
            break;
         case NORTH:
            float[] fArray4 = new float[2];
            fArray4[0] = 180.0F;
            fArray = fArray4;
            fArray4[1] = mc.player.getPitch();
            break;
         case SOUTH:
            float[] fArray5 = new float[2];
            fArray5[0] = 0.0F;
            fArray = fArray5;
            fArray5[1] = mc.player.getPitch();
            break;
         case WEST:
            float[] fArray6 = new float[2];
            fArray6[0] = 90.0F;
            fArray = fArray6;
            fArray6[1] = mc.player.getPitch();
            break;
         case EAST:
            float[] fArray7 = new float[]{-90.0F, 0.0F};
            fArray = fArray7;
            fArray7[1] = mc.player.getPitch();
            break;
         default:
            throw new MatchException(null, null);
      }

      return fArray;
   }

   public static float[] correctRotation(float[] rotations) {
      rotations[0] -= rotations[0] % GCDUtil.getGCDValue();
      rotations[1] -= rotations[1] % GCDUtil.getGCDValue();
      return new float[]{rotations[0], rotations[1]};
   }

   public static float getFixRotate(float rot) {
      return getDeltaMouse(rot) * GCDUtil.getGCDValue();
   }

   public static float getDeltaMouse(float delta) {
      return Math.round(delta / GCDUtil.getGCDValue());
   }

   private RotationUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
