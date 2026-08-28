package code.elixir.utils.math.projection;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;

public final class Projection implements QuickImports {
   private static final Vector4f TRANSFORMED = new Vector4f();
   private static final Vector3f PROJECTED = new Vector3f();
   private static final Matrix4f PROJ_MATRIX = new Matrix4f();
   private static final int[] VIEWPORT = new int[4];
   private static final double[] SCREEN_SCRATCH = new double[3];

   @NotNull
   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      return worldSpaceToScreenSpace(pos.x, pos.y, pos.z);
   }

   @NotNull
   public static Vec3d worldSpaceToScreenSpace(double x, double y, double z) {
      return worldSpaceToScreenSpaceInto(x, y, z, SCREEN_SCRATCH)
         ? new Vec3d(SCREEN_SCRATCH[0], SCREEN_SCRATCH[1], SCREEN_SCRATCH[2])
         : new Vec3d(0.0, 0.0, -1.0);
   }

   public static boolean worldSpaceToScreenSpaceInto(double x, double y, double z, double[] out) {
      TRANSFORMED.set((float)x, (float)y, (float)z, 1.0F).mul(Render3D.lastWorldSpaceMatrix.getPositionMatrix());
      PROJ_MATRIX.set(Render3D.lastProjMat);
      VIEWPORT[0] = 0;
      VIEWPORT[1] = 0;
      VIEWPORT[2] = mc.getWindow().getFramebufferWidth();
      VIEWPORT[3] = mc.getWindow().getFramebufferHeight();
      PROJ_MATRIX.project(TRANSFORMED.x(), TRANSFORMED.y(), TRANSFORMED.z(), VIEWPORT, PROJECTED);
      double depth = PROJECTED.z;
      if (depth <= 0.0) {
         return false;
      } else {
         double scale = mc.getWindow().getScaleFactor();
         out[0] = PROJECTED.x / scale;
         out[1] = (mc.getWindow().getHeight() - PROJECTED.y) / scale;
         out[2] = depth;
         return true;
      }
   }

   public static double getDistanceToGround() {
      for (double y = mc.player.getY(); y > 0.0; y -= 0.1) {
         if (!mc.world.getBlockState(mc.player.getBlockPos().down((int)(mc.player.getY() - y + 1.0))).isAir()) {
            return mc.player.getY() - y;
         }
      }

      return 256.0;
   }

   public static double getMaxRenderDistanceSq() {
      if (mc.options == null) {
         return 65536.0;
      } else {
         double max = ((Integer)mc.options.getViewDistance().getValue()).intValue() * 16.0;
         return max * max;
      }
   }

   public static boolean isWithinRenderDistance(Entity ent) {
      return ent != null && mc.player != null ? ent.squaredDistanceTo(mc.player) <= getMaxRenderDistanceSq() : false;
   }

   public static Vector4d getVector4D(Entity ent) {
      Vector4d out = new Vector4d();
      return fillVector4D(ent, out) ? out : null;
   }

   public static boolean fillVector4D(Entity ent, Vector4d out) {
      if (ent != null && isWithinRenderDistance(ent)) {
         Vec3d pos = Calculate.interpolate(ent);
         Box bb = ent.getBoundingBox();
         double dx = pos.x - ent.getX();
         double dy = pos.y - ent.getY();
         double dz = pos.z - ent.getZ();
         double minX = bb.minX + dx - 0.1;
         double minY = bb.minY + dy - 0.1;
         double minZ = bb.minZ + dz - 0.1;
         double maxX = bb.maxX + dx + 0.1;
         double maxY = bb.maxY + dy + 0.1;
         double maxZ = bb.maxZ + dz + 0.1;
         boolean visible = false;
         visible |= mergeCornerInto(out, visible, minX, minY, minZ);
         visible |= mergeCornerInto(out, visible, minX, maxY, minZ);
         visible |= mergeCornerInto(out, visible, maxX, minY, minZ);
         visible |= mergeCornerInto(out, visible, maxX, maxY, minZ);
         visible |= mergeCornerInto(out, visible, minX, minY, maxZ);
         visible |= mergeCornerInto(out, visible, minX, maxY, maxZ);
         visible |= mergeCornerInto(out, visible, maxX, minY, maxZ);
         return visible | mergeCornerInto(out, visible, maxX, maxY, maxZ) && !cantSee(out);
      } else {
         return false;
      }
   }

   private static boolean mergeCornerInto(Vector4d position, boolean hasCorner, double x, double y, double z) {
      if (!worldSpaceToScreenSpaceInto(x, y, z, SCREEN_SCRATCH)) {
         return hasCorner;
      } else {
         double screenX = SCREEN_SCRATCH[0];
         double screenY = SCREEN_SCRATCH[1];
         if (!hasCorner) {
            position.x = screenX;
            position.y = screenY;
            position.z = screenX;
            position.w = screenY;
            return true;
         } else {
            position.x = Math.min(screenX, position.x);
            position.y = Math.min(screenY, position.y);
            position.z = Math.max(screenX, position.z);
            position.w = Math.max(screenY, position.w);
            return true;
         }
      }
   }

   @Deprecated
   @NotNull
   public static Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(
         axisAlignedBB2.minX - ent.getX() + pos.x - 0.1F,
         axisAlignedBB2.minY - ent.getY() + pos.y - 0.1F,
         axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.1F,
         axisAlignedBB2.maxX - ent.getX() + pos.x + 0.1F,
         axisAlignedBB2.maxY - ent.getY() + pos.y + 0.1F,
         axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.1F
      );
      return new Vec3d[]{
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
      };
   }

   public static boolean isInFrontOfCamera(Vec3d worldPos) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      if (camera == null) {
         return false;
      } else {
         Vec3d delta = worldPos.subtract(camera.getPos());
         if (delta.lengthSquared() < 1.0E-6) {
            return false;
         } else {
            Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            return look.dotProduct(delta) > 0.0;
         }
      }
   }

   public static boolean canSee(Vec3d vec3d) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Turns angle = MathAngle.fromVec3d(vec3d.subtract(camera.getPos()));
      return Math.abs(MathHelper.wrapDegrees(angle.getYaw() - camera.getYaw())) < 90.0F
            && Math.abs(MathHelper.wrapDegrees(angle.getPitch() - camera.getPitch())) < 60.0F
         || canSee(new Box(BlockPos.ofFloored(vec3d)));
   }

   public static boolean canSee(Box box) {
      Frustum frustum = mc.worldRenderer.frustum;
      return frustum == null ? true : box != null && frustum.isVisible(box);
   }

   public static boolean cantSee(Vector4d vec) {
      return vec == null || vec.x < 0.0 && vec.z < 1.0 || vec.y < 0.0 && vec.w < 1.0;
   }

   public static double centerX(Vector4d vec) {
      return vec.x + (vec.z - vec.x) / 2.0;
   }

   private Projection() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
