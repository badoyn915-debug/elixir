package code.elixir.features.impl.player.autoMine.region.states;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class RegionBlockMaps {
   public static final Map<Block, RegionBlockState> IMMUTABLE_BLOCK_MAP = new HashMap<Block, RegionBlockState>() {
      {
         this.put(Blocks.DIAMOND_ORE, new RegionBlockState(620822527, 0));
      }
   };
}
