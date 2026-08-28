package code.elixir.utils.features.aura.point;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.utils.RaycastAngle;
import code.elixir.utils.features.aura.warp.Turns;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;

public class MultiPoint implements QuickImports {
   private Vec3d offset = Vec3d.ZERO;

   public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Turns initialAngle, Vec3d velocity, boolean ignoreWalls) {
      Pair<List<Vec3d>, Box> candidatePoints = this.generateCandidatePoints(entity, maxDistance, ignoreWalls);
      Vec3d bestVector = this.findBestVector((List<Vec3d>)candidatePoints.getLeft(), initialAngle);
      this.updateOffset(velocity);
      return new Pair((bestVector == null ? entity.getEyePos() : bestVector).add(this.offset), (Box)candidatePoints.getRight());
   }

   public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box entityBox = entity.getBoundingBox();
      double stepY = entityBox.getLengthY() / 10.0;
      double centerX = entityBox.getCenter().x;
      double centerZ = entityBox.getCenter().z;
      Vec3d eyePos = mc.player.getEyePos();
      List<Vec3d> list = new ArrayList<>(11);

      for (double y = entityBox.minY; y <= entityBox.maxY; y += stepY) {
         Vec3d point = new Vec3d(centerX, y, centerZ);
         if (this.isValidPoint(eyePos, point, maxDistance, ignoreWalls)) {
            list.add(point);
         }
      }

      return new Pair(list, entityBox);
   }

   public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box entityBox = entity.getBoundingBox();
      double stepY = entityBox.getLengthY() / 10.0;
      double centerX = entityBox.getCenter().x;
      double centerZ = entityBox.getCenter().z;
      Vec3d eyePos = mc.player.getEyePos();

      for (double y = entityBox.minY; y < entityBox.maxY; y += stepY) {
         Vec3d point = new Vec3d(centerX, y, centerZ);
         if (this.isValidPoint(eyePos, point, maxDistance, ignoreWalls)) {
            return true;
         }
      }

      return false;
   }

   private boolean isValidPoint(Vec3d startPoint, Vec3d endPoint, float maxDistance, boolean ignoreWalls) {
      return startPoint.distanceTo(endPoint) <= maxDistance
         && (ignoreWalls || !RaycastAngle.raycast(startPoint, endPoint, ShapeType.COLLIDER).getType().equals(Type.BLOCK));
   }

   private Vec3d findBestVector(List<Vec3d> candidatePoints, Turns initialAngle) {
      if (candidatePoints != null && !candidatePoints.isEmpty()) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d bestPoint = null;
         double bestDiff = Double.MAX_VALUE;

         for (int i = 0; i < candidatePoints.size(); i++) {
            Vec3d point = candidatePoints.get(i);
            double diff = this.calculateRotationDifference(eyePos, point, initialAngle);
            if (diff < bestDiff) {
               bestDiff = diff;
               bestPoint = point;
            }
         }

         return bestPoint;
      } else {
         return null;
      }
   }

   private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Turns initialAngle) {
      Turns targetAngle = MathAngle.fromVec3d(endPoint.subtract(startPoint));
      Turns delta = MathAngle.calculateDelta(initialAngle, targetAngle);
      return Math.hypot(delta.getYaw(), delta.getPitch());
   }

   private void updateOffset(Vec3d velocity) {
      ThreadLocalRandom rand = ThreadLocalRandom.current();
      this.offset = this.offset.add(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).multiply(velocity);
   }

   public Vec3d getOffset() {
      return this.offset;
   }
}
