package code.elixir.features.impl.player.autoMine.region.states;

import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Comparator;
import net.minecraft.util.math.BlockPos;

public class RegionComparators implements QuickImports {
   public static final Comparator<BlockPos> BY_DISTANCE_COMPARATOR = (a, b) -> Double.compare(
      WorldUtility.horizontalDistance(mc.player.getBlockPos(), a), WorldUtility.horizontalDistance(mc.player.getBlockPos(), b)
   );
   public static final Comparator<BlockPos> BY_PRIORITY_COMPARATOR = (a, b) -> {
      RegionBlockState aState = RegionBlockMaps.IMMUTABLE_BLOCK_MAP.get(WorldUtility.fromBlockPos(a).getBlock());
      RegionBlockState bState = RegionBlockMaps.IMMUTABLE_BLOCK_MAP.get(WorldUtility.fromBlockPos(b).getBlock());
      if (aState == null && bState == null) {
         return 0;
      } else if (aState == null) {
         return 1;
      } else {
         return bState == null ? -1 : Integer.compare(aState.priority(), bState.priority());
      }
   };

   public static double score(BlockPos blockPos) {
      BlockPos playerPos = mc.player.getBlockPos();
      double horizontal = WorldUtility.horizontalDistance(playerPos, blockPos);
      double vertical = Math.abs(WorldUtility.verticalDistance(playerPos, blockPos));
      RegionBlockState state = RegionBlockMaps.IMMUTABLE_BLOCK_MAP.get(WorldUtility.fromBlockPos(blockPos).getBlock());
      int priority = state != null ? state.priority() : 100;
      return horizontal + vertical * 0.5 + priority * 2.0;
   }
}
