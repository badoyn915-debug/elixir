package code.elixir.utils.features.aura.utils;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.striking.StrikerConstructor;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RaycastAngle implements QuickImports {
   public static BlockHitResult raycast(double range, Turns angle, boolean includeFluids) {
      return raycast(Objects.requireNonNull(mc.player).getCameraPosVec(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3d vec, double range, Turns angle, boolean includeFluids) {
      Entity entity = mc.cameraEntity;
      if (entity == null) {
         return null;
      } else {
         Vec3d rotationVec = angle.toVector();
         Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         ClientWorld world = mc.world;
         if (world == null) {
            return null;
         } else {
            FluidHandling fluidHandling = includeFluids ? FluidHandling.ANY : FluidHandling.NONE;
            RaycastContext context = new RaycastContext(vec, end, ShapeType.OUTLINE, fluidHandling, entity);
            return world.raycast(context);
         }
      }
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      return raycast(start, end, shapeType, mc.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
      return raycast(start, end, shapeType, FluidHandling.NONE, entity);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
      return mc.world.raycast(new RaycastContext(start, end, shapeType, fluidHandling, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Turns angle, Predicate<Entity> filter) {
      ClientPlayerEntity entity = mc.player;
      if (entity == null) {
         return null;
      } else {
         Vec3d cameraVec = entity.getCameraPosVec(1.0F);
         Vec3d rotationVec = angle.toVector();
         Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);
         return ProjectileUtil.raycast(entity, cameraVec, vec3d3, box, e -> !e.isSpectator() && filter.test(e), range * range);
      }
   }

   public static boolean rayTrace(StrikerConstructor.AttackPerpetratorConfigurable config) {
      return rayTrace(TurnsConnection.INSTANCE.getRotation().toVector(), config.getMaximumRange() - 0.25F, config.getBox());
   }

   public static boolean rayTrace(double range, Box box) {
      return rayTrace(TurnsConnection.INSTANCE.getRotation().toVector(), range - 0.25, box);
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      Vec3d cameraVec = Objects.requireNonNull(mc.player).getEyePos();
      return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
   }

   private RaycastAngle() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
