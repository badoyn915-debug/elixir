package code.elixir.features.impl.player.autoMine.mine;

import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MineMovementBrainer implements QuickImports {
   private BlockPos obstacleBlock;
   private BlockPos moveTarget;
   private boolean shouldDigObstacle;
   private boolean pathClear;
   private Vec3d lastPos;
   private long lastMovedAt;
   private boolean stuck;
   static final double STUCK_MOVE_THRESHOLD = 0.08;
   static final long STUCK_TIME_MS = 1200L;

   public MineMovementBrainer() {
      this.reset();
   }

   public void reset() {
      this.obstacleBlock = null;
      this.moveTarget = null;
      this.shouldDigObstacle = false;
      this.pathClear = true;
      this.lastPos = null;
      this.lastMovedAt = System.currentTimeMillis();
      this.stuck = false;
   }

   public void update(BlockPos targetBlock) {
      if (targetBlock != null && mc.player != null) {
         this.updateStuck();
         BlockPos playerFeet = mc.player.getBlockPos();
         int dx = Integer.compare(targetBlock.getX(), playerFeet.getX());
         int dz = Integer.compare(targetBlock.getZ(), playerFeet.getZ());
         int dy = Integer.compare(targetBlock.getY(), playerFeet.getY());
         double horizontalDist = WorldUtility.horizontalDistance(playerFeet, targetBlock);
         double verticalDist = WorldUtility.verticalDistance(playerFeet, targetBlock);
         if (horizontalDist < 4.5 && Math.abs(verticalDist) < 4.5) {
            BlockPos blockingBlock = this.findBlockBetween(playerFeet, targetBlock);
            if (blockingBlock != null) {
               this.obstacleBlock = blockingBlock;
               this.shouldDigObstacle = this.isInDigRange(blockingBlock);
               this.pathClear = false;
               this.moveTarget = this.shouldDigObstacle ? null : blockingBlock;
            } else {
               this.obstacleBlock = null;
               this.moveTarget = targetBlock;
               this.shouldDigObstacle = false;
               this.pathClear = true;
            }
         } else {
            BlockPos obstacle = this.findObstacleOnPath(playerFeet, dx, dy, dz, targetBlock);
            if (obstacle != null) {
               this.obstacleBlock = obstacle;
               this.shouldDigObstacle = this.isInDigRange(obstacle);
               this.pathClear = false;
               this.moveTarget = this.shouldDigObstacle ? null : this.obstacleBlock;
            } else {
               this.obstacleBlock = null;
               this.shouldDigObstacle = false;
               this.pathClear = true;
               this.moveTarget = targetBlock;
            }

            BlockPos forceObstacle;
            if (this.stuck && this.pathClear && (forceObstacle = this.findImmediateObstacle(playerFeet, dx, dz)) != null) {
               this.obstacleBlock = forceObstacle;
               this.shouldDigObstacle = true;
               this.pathClear = false;
            }
         }
      } else {
         this.obstacleBlock = null;
         this.moveTarget = null;
         this.shouldDigObstacle = false;
         this.pathClear = true;
         this.stuck = false;
      }
   }

   private BlockPos findBlockBetween(BlockPos playerFeet, BlockPos target) {
      int dx = Integer.signum(target.getX() - playerFeet.getX());
      int dy = Integer.signum(target.getY() - playerFeet.getY());
      int dz = Integer.signum(target.getZ() - playerFeet.getZ());
      BlockPos current = playerFeet;
      int maxSteps = 6;

      for (int step = 0; step < maxSteps; step++) {
         BlockPos next = dy < 0 ? current.down() : (dy > 0 ? current.up().up() : current.add(dx, 0, dz));
         if (next.equals(target)) {
            return null;
         }

         if (this.isSolid(next) && !next.equals(target)) {
            return next;
         }

         current = next;
         if (dy < 0 && next.getY() <= target.getY()
            || dy > 0 && next.getY() >= target.getY()
            || dy == 0 && next.getX() == target.getX() && next.getZ() == target.getZ()) {
            break;
         }
      }

      return null;
   }

   private BlockPos findObstacleOnPath(BlockPos playerFeet, int dx, int dy, int dz, BlockPos target) {
      BlockPos current = playerFeet;
      int maxSteps = 10;

      for (int step = 0; step < maxSteps; step++) {
         BlockPos next = dx == 0 && dz == 0 ? current.add(0, dy, 0) : current.add(dx, 0, dz);
         if (this.isSolid(next)) {
            return next;
         }

         if (this.isSolid(next.up())) {
            return next.up();
         }

         BlockPos upNext;
         if (dy > 0 && this.isSolid(upNext = next.up().up())) {
            return upNext;
         }

         if (dy < 0) {
            upNext = current.add(dx, -1, dz);
         }

         current = next;
         if (Math.abs(next.getX() - target.getX()) <= 1 && Math.abs(next.getZ() - target.getZ()) <= 1) {
            if (dy != 0) {
               BlockPos vertCheck = next.add(0, dy, 0);
               if (this.isSolid(vertCheck)) {
                  return vertCheck;
               }

               if (this.isSolid(vertCheck.up())) {
                  return vertCheck.up();
               }
            }
            break;
         }
      }

      return null;
   }

   private BlockPos findImmediateObstacle(BlockPos feet, int dx, int dz) {
      if (dx != 0 || dz != 0) {
         BlockPos front = feet.add(dx, 0, dz);
         if (this.isSolid(front)) {
            return front;
         }

         if (this.isSolid(front.up())) {
            return front.up();
         }
      }

      if (dx != 0) {
         BlockPos frontX = feet.add(dx, 0, 0);
         if (this.isSolid(frontX)) {
            return frontX;
         }

         if (this.isSolid(frontX.up())) {
            return frontX.up();
         }
      }

      if (dz != 0) {
         BlockPos frontZ = feet.add(0, 0, dz);
         if (this.isSolid(frontZ)) {
            return frontZ;
         }

         if (this.isSolid(frontZ.up())) {
            return frontZ.up();
         }
      }

      if (this.isSolid(feet.up().up())) {
         return feet.up().up();
      } else {
         return this.isSolid(feet.down()) ? feet.down() : null;
      }
   }

   private boolean isSolid(BlockPos pos) {
      BlockState state = WorldUtility.fromBlockPos(pos);
      if (state == null || state.isAir()) {
         return false;
      } else {
         return !state.blocksMovement()
            ? false
            : !state.isOf(Blocks.BEDROCK)
               && !state.isOf(Blocks.BARRIER)
               && !state.isOf(Blocks.COMMAND_BLOCK)
               && !state.isOf(Blocks.STRUCTURE_BLOCK)
               && !state.isOf(Blocks.END_PORTAL_FRAME)
               && !state.isOf(Blocks.REINFORCED_DEEPSLATE);
      }
   }

   private boolean isInDigRange(BlockPos block) {
      Vec3d eyePos = mc.player.getEyePos();
      return eyePos.distanceTo(Vec3d.ofCenter(block)) <= 5.0;
   }

   private void updateStuck() {
      Vec3d pos = mc.player.getPos();
      long now = System.currentTimeMillis();
      if (this.lastPos != null && pos.distanceTo(this.lastPos) > 0.08) {
         this.lastMovedAt = now;
         this.stuck = false;
      } else if (now - this.lastMovedAt > 1200L) {
         this.stuck = true;
      }

      this.lastPos = pos;
   }

   public BlockPos getObstacleBlock() {
      return this.obstacleBlock;
   }

   public BlockPos getMoveTarget() {
      return this.moveTarget;
   }

   public boolean isShouldDigObstacle() {
      return this.shouldDigObstacle;
   }

   public boolean isPathClear() {
      return this.pathClear;
   }

   public Vec3d getLastPos() {
      return this.lastPos;
   }

   public long getLastMovedAt() {
      return this.lastMovedAt;
   }

   public boolean isStuck() {
      return this.stuck;
   }
}
