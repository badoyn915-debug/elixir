package code.elixir.features.impl.player.autoMine.region.states;

import code.elixir.features.impl.player.autoMine.region.utils.RandStack;
import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RegionState implements QuickImports {
   private final BlockPos min;
   private final BlockPos max;
   private final RandStack<BlockPos> randStack = new RandStack<>();

   public RegionState(BlockPos min, BlockPos max) {
      this.min = min;
      this.max = max;
   }

   public void eachBlocks(Predicate<BlockState> predicate, Consumer<BlockPos> consumer) {
      for (BlockPos blockPos : BlockPos.iterate(this.min, this.max)) {
         BlockState blockState = WorldUtility.fromBlockPos(blockPos);
         if (predicate.test(blockState)) {
            consumer.accept(blockPos.toImmutable());
         }
      }
   }

   public void eachBlocksFromPlayer(BlockPos playerPos, Predicate<BlockState> predicate, Consumer<BlockPos> consumer) {
      int maxRadius = Math.max(
            Math.max(Math.abs(this.max.getX() - this.min.getX()), Math.abs(this.max.getY() - this.min.getY())), Math.abs(this.max.getZ() - this.min.getZ())
         )
         + 1;

      for (int radius = 0; radius <= maxRadius; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  if (Math.abs(dx) == radius || Math.abs(dy) == radius || Math.abs(dz) == radius) {
                     int x = playerPos.getX() + dx;
                     int y = playerPos.getY() + dy;
                     int z = playerPos.getZ() + dz;
                     BlockPos blockPos;
                     if (x >= this.min.getX()
                        && x <= this.max.getX()
                        && y >= this.min.getY()
                        && y <= this.max.getY()
                        && z >= this.min.getZ()
                        && z <= this.max.getZ()
                        && predicate.test(WorldUtility.fromBlockPos(blockPos = new BlockPos(x, y, z)))) {
                        consumer.accept(blockPos.toImmutable());
                     }
                  }
               }
            }
         }
      }
   }

   public List<BlockPos> findBlocksSortedByDistance(BlockPos playerPos, Predicate<BlockState> predicate) {
      ArrayList<BlockPos> blocks = new ArrayList<>();

      for (BlockPos blockPos : BlockPos.iterate(this.min, this.max)) {
         BlockState blockState = WorldUtility.fromBlockPos(blockPos);
         if (predicate.test(blockState)) {
            blocks.add(blockPos.toImmutable());
         }
      }

      blocks.sort(Comparator.comparingDouble(arg_0 -> playerPos.getSquaredDistance(arg_0)));
      return blocks;
   }

   public boolean inState(BlockPos blockPos) {
      return blockPos.getX() >= this.min.getX()
         && blockPos.getX() <= this.max.getX()
         && blockPos.getY() >= this.min.getY()
         && blockPos.getY() <= this.max.getY()
         && blockPos.getZ() >= this.min.getZ()
         && blockPos.getZ() <= this.max.getZ();
   }

   public Box asBox() {
      return new Box(new Vec3d(this.min.getX(), this.min.getY(), this.min.getZ()), new Vec3d(this.max.getX(), this.max.getY(), this.max.getZ()));
   }

   public BlockPos getCenter() {
      return new BlockPos((this.min.getX() + this.max.getX()) / 2, (this.min.getY() + this.max.getY()) / 2, (this.min.getZ() + this.max.getZ()) / 2);
   }

   public Vec3d getCenterVec() {
      return new Vec3d((this.min.getX() + this.max.getX()) / 2.0, (this.min.getY() + this.max.getY()) / 2.0, (this.min.getZ() + this.max.getZ()) / 2.0);
   }

   public BlockPos getMin() {
      return this.min;
   }

   public BlockPos getMax() {
      return this.max;
   }

   public RandStack<BlockPos> getRandStack() {
      return this.randStack;
   }
}
