package code.elixir.features.impl.render.zenisesp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class StickPhysicsSystem {
   public static final int NODE_COUNT = 10;
   private final Map<Integer, EntityStickState> stickMap = new HashMap<>();

   public static class EntityStickState {
      public final List<StickSegment> nodes = new ArrayList<>(NODE_COUNT);
      public Vec3d lastRootPos = null;
      public float lastYaw = 0.0F;
      public long lastUpdateTime = 0L;
      public boolean initialized = false;

      public EntityStickState(Vec3d rootPos) {
         for (int i = 0; i < NODE_COUNT; i++) {
            this.nodes.add(new StickSegment(rootPos));
         }
         this.lastRootPos = rootPos;
      }
   }

   public EntityStickState getOrCreateState(int entityId, Vec3d rootPos) {
      return this.stickMap.computeIfAbsent(entityId, id -> new EntityStickState(rootPos));
   }

   public void clear() {
      this.stickMap.clear();
   }

   public void remove(int entityId) {
      this.stickMap.remove(entityId);
   }

   public void updatePhysics(
      LivingEntity entity,
      Vec3d rootPos,
      float bodyYaw,
      float bodyPitch,
      String posture,
      float totalLength,
      float stiffness,
      float damping,
      float gravityFactor
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null || entity == null) return;

      EntityStickState state = this.getOrCreateState(entity.getId(), rootPos);
      float segLength = totalLength / (NODE_COUNT - 1);
      boolean isStanding = "Стоячий".equalsIgnoreCase(posture);

      // Compute Rest Direction based on Body Yaw/Pitch and Posture
      double yawRad = Math.toRadians(bodyYaw);
      double pitchRad = Math.toRadians(isStanding ? 35.0 : -78.0);

      double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
      double forwardY = Math.sin(pitchRad);
      double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);
      Vec3d restDir = new Vec3d(forwardX, forwardY, forwardZ).normalize();

      // Initialization on first spawn
      if (!state.initialized) {
         for (int i = 0; i < NODE_COUNT; i++) {
            Vec3d initialPos = rootPos.add(restDir.multiply(i * segLength));
            StickSegment seg = state.nodes.get(i);
            seg.pos = initialPos;
            seg.prevPos = initialPos;
            seg.renderPos = initialPos;
            seg.prevRenderPos = initialPos;
         }
         state.lastRootPos = rootPos;
         state.lastYaw = bodyYaw;
         state.lastUpdateTime = System.currentTimeMillis();
         state.initialized = true;
         return;
      }

      // Delta time in seconds (clamped to prevent physics explosion on lags)
      long now = System.currentTimeMillis();
      float dt = MathHelper.clamp((float)(now - state.lastUpdateTime) / 1000.0F, 0.005F, 0.06F);
      state.lastUpdateTime = now;

      // Sync render poses for interpolation
      for (StickSegment seg : state.nodes) {
         seg.syncRender();
      }

      // Root Anchor movement velocity
      Vec3d rootVelocity = state.lastRootPos != null ? rootPos.subtract(state.lastRootPos).multiply(1.0 / dt) : Vec3d.ZERO;
      state.lastRootPos = rootPos;
      state.lastYaw = bodyYaw;

      // Set root node firmly at rootPos
      state.nodes.get(0).prevPos = state.nodes.get(0).pos;
      state.nodes.get(0).pos = rootPos;

      // 1. Verlet Integration + Forces
      Vec3d gravity = new Vec3d(0, -9.81 * gravityFactor * (isStanding ? 0.35 : 1.0), 0);

      for (int i = 1; i < NODE_COUNT; i++) {
         StickSegment seg = state.nodes.get(i);
         float nodeProgress = (float)i / (NODE_COUNT - 1);

         // Velocity from previous step
         Vec3d velocity = seg.pos.subtract(seg.prevPos).multiply(damping);

         // Spring force pulling towards the rest target orientation
         Vec3d targetNodePos = rootPos.add(restDir.multiply(i * segLength));
         Vec3d springForce = targetNodePos.subtract(seg.pos).multiply(stiffness * (isStanding ? 28.0 : 14.0));

         // Total acceleration
         Vec3d acceleration = gravity.add(springForce);

         seg.prevPos = seg.pos;
         seg.pos = seg.pos.add(velocity).add(acceleration.multiply(dt * dt));
      }

      // 2. Constraint Relaxation + Collision Resolution (Multiple Iterations for Stability)
      int iterations = 8;
      for (int iter = 0; iter < iterations; iter++) {
         // Maintain distance constraints along the chain
         for (int i = 0; i < NODE_COUNT - 1; i++) {
            StickSegment a = state.nodes.get(i);
            StickSegment b = state.nodes.get(i + 1);

            Vec3d delta = b.pos.subtract(a.pos);
            double dist = delta.length();
            if (dist > 0.0001) {
               double diff = (dist - segLength) / dist;
               Vec3d correction = delta.multiply(diff);

               if (i == 0) {
                  // Root is fixed, only move child b
                  b.pos = b.pos.subtract(correction);
               } else {
                  // Move both nodes to satisfy distance
                  a.pos = a.pos.add(correction.multiply(0.45));
                  b.pos = b.pos.subtract(correction.multiply(0.55));
               }
            }
         }

         // Block & Surface Collision with Environment
         resolveBlockCollisions(mc.world, state.nodes, segLength * 0.45F);
      }
   }

   private static void resolveBlockCollisions(World world, List<StickSegment> nodes, float radius) {
      if (world == null) return;

      for (int i = 1; i < nodes.size(); i++) {
         StickSegment seg = nodes.get(i);
         StickSegment prev = nodes.get(i - 1);

         // Raycast test between consecutive nodes
         RaycastContext ctx = new RaycastContext(
            prev.pos,
            seg.pos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            MinecraftClient.getInstance().player
         );

         BlockHitResult hit = world.raycast(ctx);
         if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            Vec3d hitPos = hit.getPos();
            Direction side = hit.getSide();
            Vec3d normal = Vec3d.of(side.getVector());

            // Push node away from the surface along collision normal
            seg.pos = hitPos.add(normal.multiply(radius + 0.02));
         }

         // Voxel Shape penetration box test
         Box nodeBox = Box.of(seg.pos, radius * 2.0, radius * 2.0, radius * 2.0);
         BlockPos min = BlockPos.ofFloored(nodeBox.minX, nodeBox.minY, nodeBox.minZ);
         BlockPos max = BlockPos.ofFloored(nodeBox.maxX, nodeBox.maxY, nodeBox.maxZ);

         for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockState blockState = world.getBlockState(pos);
            if (!blockState.isAir()) {
               VoxelShape shape = blockState.getCollisionShape(world, pos);
               if (!shape.isEmpty()) {
                  for (Box blockBox : shape.getBoundingBoxes()) {
                     Box worldBox = blockBox.offset(pos);
                     if (worldBox.intersects(nodeBox)) {
                        // Calculate minimal push-out vector
                        Vec3d push = calculatePushOut(seg.pos, worldBox, radius);
                        seg.pos = seg.pos.add(push);
                     }
                  }
               }
            }
         }
      }
   }

   private static Vec3d calculatePushOut(Vec3d pos, Box box, float radius) {
      double minX = box.minX - radius;
      double maxX = box.maxX + radius;
      double minY = box.minY - radius;
      double maxY = box.maxY + radius;
      double minZ = box.minZ - radius;
      double maxZ = box.maxZ + radius;

      if (pos.x > minX && pos.x < maxX && pos.y > minY && pos.y < maxY && pos.z > minZ && pos.z < maxZ) {
         double dx1 = pos.x - minX;
         double dx2 = maxX - pos.x;
         double dy1 = pos.y - minY;
         double dy2 = maxY - pos.y;
         double dz1 = pos.z - minZ;
         double dz2 = maxZ - pos.z;

         double min = Math.min(Math.min(Math.min(dx1, dx2), Math.min(dy1, dy2)), Math.min(dz1, dz2));
         if (min == dx1) return new Vec3d(-dx1, 0, 0);
         if (min == dx2) return new Vec3d(dx2, 0, 0);
         if (min == dy1) return new Vec3d(0, -dy1, 0);
         if (min == dy2) return new Vec3d(0, dy2, 0);
         if (min == dz1) return new Vec3d(0, 0, -dz1);
         return new Vec3d(0, 0, dz2);
      }
      return Vec3d.ZERO;
   }
}
