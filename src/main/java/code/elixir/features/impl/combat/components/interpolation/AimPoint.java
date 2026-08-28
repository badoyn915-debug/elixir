package code.elixir.features.impl.combat.components.interpolation;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimPoint implements QuickImports {
   private static double lockedYOffset = 0.55;
   private static Vec3d smoothed = null;
   private static int lastId = -1;

   public static Vec3d get(LivingEntity target) {
      if (target == null) {
         return null;
      } else {
         Box box = target.getBoundingBox();
         Vec3d center = box.getCenter();
         double distance = mc.player.getEyePos().distanceTo(center) + 2.0;
         Vec3d chosen = BestPoint.getMultipoint(target, distance);
         if (chosen == null) {
            chosen = center;
         }

         Vec3d visible = new Vec3d(
            MathHelper.clamp(chosen.x, box.minX, box.maxX), MathHelper.clamp(chosen.y, box.minY, box.maxY), MathHelper.clamp(chosen.z, box.minZ, box.maxZ)
         );
         int id = target.getId();
         smoothed = smoothed != null && id == lastId && !(smoothed.distanceTo(visible) > 4.0)
            ? smoothed.add(visible.subtract(smoothed).multiply(0.2))
            : visible;
         lastId = id;
         return smoothed;
      }
   }

   public static void reset() {
      lockedYOffset = 0.55;
      smoothed = null;
      lastId = -1;
   }
}
