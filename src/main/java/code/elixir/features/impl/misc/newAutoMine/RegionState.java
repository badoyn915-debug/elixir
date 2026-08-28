package code.elixir.features.impl.misc.newAutoMine;

import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class RegionState {
   final BlockPos min;
   final BlockPos max;

   public RegionState(BlockPos min, BlockPos max) {
      this.min = min;
      this.max = max;
   }

   public Box getBox() {
      return new Box(this.min.getX(), this.min.getY(), this.min.getZ(), this.max.getX(), this.max.getY(), this.max.getZ());
   }

   public boolean inRegion(BlockPos blockPos) {
      float minX = this.min.getX();
      float minY = this.min.getY();
      float minZ = this.min.getZ();
      float maxX = this.max.getX();
      float maxY = this.max.getY();
      float maxZ = this.max.getZ();
      return blockPos.getX() >= minX
         && blockPos.getY() >= minY
         && blockPos.getZ() >= minZ
         && blockPos.getX() <= maxX
         && blockPos.getY() <= maxY
         && blockPos.getZ() <= maxZ;
   }

   public List<BlockPos> eachBlocks(Predicate<BlockPos> predicate) {
      ArrayList<BlockPos> list = new ArrayList<>();
      BlockPos.iterate(this.min, this.max).forEach(blockPos -> {
         if (predicate.test(blockPos)) {
            list.add(blockPos);
         }
      });
      return list;
   }

   public double distanceTo(BlockPos blockPos) {
      return Math.min(WorldUtility.horizontalDistance(blockPos, this.min), WorldUtility.horizontalDistance(blockPos, this.max));
   }
}
