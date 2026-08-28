package code.elixir.utils.features.rotate;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class MultipointUtils implements QuickImports {
   public static Vec3d getClosestPoint(Entity entity) {
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

   private MultipointUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
