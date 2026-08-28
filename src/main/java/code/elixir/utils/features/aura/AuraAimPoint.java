package code.elixir.utils.features.aura;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.HotbarUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class AuraAimPoint implements QuickImports {
   public static Vec3d aim(Vec3d eye, LivingEntity target, double reach, boolean throughWalls) {
      Box box = target.getBoundingBox();
      boolean mace = isHoldingMace();
      Vec3d aimEye = !mace ? eye : eye.add(mc.player.getRotationVector());
      double mx = (box.minX + box.maxX) * 0.5;
      double mz = (box.minZ + box.maxZ) * 0.5;
      Vec3d targetEye = target.getPos().add(0.0, target.getHeight(), 0.0);
      double distToTargetEye = aimEye.distanceTo(targetEye);
      Vec3d aimOrigin = aimEye;
      if (mace && distToTargetEye > 3.0) {
         aimOrigin = new Vec3d(aimEye.x, targetEye.y, aimEye.z);
      }

      double blendDist = mace ? Math.min(distToTargetEye, 3.0) : distToTargetEye;
      double aimHeight = aimEye.y;
      if (mace && distToTargetEye > 3.0) {
         aimHeight = targetEye.y;
      }

      double ay = lerp(MathHelper.clamp(blendDist / 3.0, 0.0, 1.0), box.minY, MathHelper.clamp(aimHeight, box.minY, box.maxY));
      Vec3d ideal = new Vec3d(mx, ay, mz);
      List<Vec3d> pts = new ArrayList<>();
      pts.add(ideal);
      double[] t = new double[]{0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
      int last = t.length - 1;

      for (int a = 0; a < t.length; a++) {
         for (int b = 0; b < t.length; b++) {
            for (int c = 0; c < t.length; c++) {
               if (a == 0 || a == last || b == 0 || b == last || c == 0 || c == last) {
                  pts.add(new Vec3d(lerp(t[a], box.minX, box.maxX), lerp(t[b], box.minY, box.maxY), lerp(t[c], box.minZ, box.maxZ)));
               }
            }
         }
      }

      for (double pad : new double[]{0.0, 0.20000001551382535}) {
         List<Vec3d> visible = new ArrayList<>();

         for (Vec3d p : pts) {
            Vec3d d = p.subtract(aimOrigin);
            double len = d.distanceTo(Vec3d.ZERO);
            double limit = reach + pad;
            if (mace || len <= limit) {
               float traceDist = (float)(mace ? len + pad + 0.010000001417203743 : limit);
               if (rayHitsBox(
                  aimOrigin,
                  MathHelper.wrapDegrees((float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0)),
                  (float)(-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.z, d.x)))),
                  traceDist,
                  target,
                  false
               )) {
                  visible.add(p);
               }
            }
         }

         if (!visible.isEmpty()) {
            Vec3d centroid = visible.stream().reduce(Vec3d.ZERO, (v0, v1) -> v0.add(v1)).multiply(1.0 / visible.size());
            return visible.stream().min(Comparator.comparingDouble(pt -> pt.squaredDistanceTo(centroid))).get().subtract(aimOrigin);
         }

         if (throughWalls) {
            List<Vec3d> through = new ArrayList<>();

            for (Vec3d p2 : pts) {
               Vec3d d2 = p2.subtract(aimOrigin);
               double len2 = d2.distanceTo(Vec3d.ZERO);
               double limit2 = reach + pad;
               if (mace || len2 <= limit2) {
                  float traceDist2 = (float)(mace ? len2 + pad + 0.010000001417203743 : limit2);
                  if (rayHitsBox(
                     aimOrigin,
                     MathHelper.wrapDegrees((float)(Math.toDegrees(Math.atan2(d2.z, d2.x)) - 90.0)),
                     (float)(-Math.toDegrees(Math.atan2(d2.y, Math.hypot(d2.z, d2.x)))),
                     traceDist2,
                     target,
                     true
                  )) {
                     through.add(p2);
                  }
               }
            }

            if (!through.isEmpty()) {
               Vec3d centroid2 = through.stream().reduce(Vec3d.ZERO, (v0, v1) -> v0.add(v1)).multiply(1.0 / through.size());
               return through.stream().min(Comparator.comparingDouble(pt2 -> pt2.squaredDistanceTo(centroid2))).get().subtract(aimOrigin);
            }
         }
      }

      return Vec3d.ZERO;
   }

   private static boolean rayHitsBox(Vec3d rayOrigin, float yaw, float pitch, double distance, LivingEntity entity, boolean throwWalls) {
      if (mc.player != null && mc.world != null) {
         Vec3d dir = Vec3d.fromPolar(pitch, yaw).multiply(distance);
         Box box = entity.getBoundingBox();
         Optional<Vec3d> hit = box.contains(rayOrigin) ? Optional.of(rayOrigin) : box.raycast(rayOrigin, rayOrigin.add(dir));
         return !hit.isPresent()
            ? false
            : throwWalls || mc.world.raycast(new RaycastContext(rayOrigin, hit.get(), ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static boolean isHoldingMace() {
      return mc.player != null && mc.player.getMainHandStack().isOf(HotbarUtil.getItem("mace"));
   }

   public static boolean maceInInventory() {
      if (mc.player == null) {
         return false;
      } else {
         for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(HotbarUtil.getItem("mace"))) {
               return true;
            }
         }

         return false;
      }
   }

   public static float smoothStep(float start, float end, float amount) {
      float a = MathHelper.clamp(amount, 0.0F, 1.0F);
      float d = MathHelper.wrapDegrees(end - start);
      if (Math.abs(d) < 0.5F) {
         return end;
      } else {
         float stepped = MathHelper.wrapDegrees(start + d * a);
         float patched = latticePatch(start, stepped);
         float remaining = MathHelper.wrapDegrees(end - patched);
         return Math.abs(remaining) < 0.5F ? end : patched;
      }
   }

   private static float latticePatch(float current, float target) {
      double sens = ((Double)mc.options.getMouseSensitivity().getValue()).floatValue() * 0.6000000498956214 + 0.19999998556632664;
      double gcd = sens * sens * sens * 8.0;
      return (float)(current + Math.ceil((target - current) / gcd / 0.15000007F) * gcd * 0.15000007F);
   }

   private static double lerp(double delta, double start, double end) {
      return start + (end - start) * delta;
   }

   private AuraAimPoint() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
