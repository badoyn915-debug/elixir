package code.elixir.features.impl.player.autoMine.controllers;

import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.player.autoMine.region.states.RegionBlockMaps;
import code.elixir.features.impl.player.autoMine.region.states.RegionComparators;
import code.elixir.features.impl.player.autoMine.region.states.RegionState;
import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.Render3D;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class RegionController implements QuickImports {
   private final RegionState regionState;
   private long lastScanTime = -1L;
   private BlockPos targetBlock = null;
   static final int MAX_REACH_UP = 4;
   static final int MAX_REACH_DOWN = 3;

   public RegionController(RegionState regionState) {
      this.regionState = regionState;
   }

   public static RegionState createRegion(BlockPos min, BlockPos max) {
      return new RegionState(min, max);
   }

   public static boolean isValidBlockState(BlockState blockState) {
      return blockState != null && !blockState.isAir() && RegionBlockMaps.IMMUTABLE_BLOCK_MAP.containsKey(blockState.getBlock());
   }

   public void onTick() {
      this.targetBlock = null;
      BlockPos playerPos = mc.player.getBlockPos();
      if (System.currentTimeMillis() - this.lastScanTime > 100L || this.regionState.getRandStack().isEmpty()) {
         this.regionState.getRandStack().clear();
         List<BlockPos> sorted = this.regionState.findBlocksSortedByDistance(playerPos, RegionController::isValidBlockState);
         sorted.forEach(this.regionState.getRandStack()::add);
         this.lastScanTime = System.currentTimeMillis();
      }

      this.updateTargetBlock();
   }

   public void onWorldRender(WorldRenderEvent worldRenderEvent) {
      Render3D.drawBox(this.regionState.asBox(), 637534207, 2.0F);
      this.drawTargetBlock();
   }

   public void onWorldChange() {
      this.regionState.getRandStack().clear();
      this.targetBlock = null;
   }

   private void drawTargetBlock() {
      BlockState blockState;
      if (this.targetBlock != null
         && (blockState = WorldUtility.fromBlockPos(this.targetBlock)) != null
         && RegionBlockMaps.IMMUTABLE_BLOCK_MAP.containsKey(blockState.getBlock())) {
         Render3D.drawBox(new Box(this.targetBlock), ColorAssist.setAlpha(RegionBlockMaps.IMMUTABLE_BLOCK_MAP.get(blockState.getBlock()).color(), 255), 2.0F);
      }
   }

   private void updateTargetBlock() {
      if (!this.regionState.getRandStack().isEmpty()) {
         BlockPos playerPos = mc.player.getBlockPos();
         List<BlockPos> reachable = this.regionState.getRandStack().stream().filter(pos -> {
            double vertDist = WorldUtility.verticalDistance(playerPos, pos);
            return vertDist <= 4.0 && vertDist >= -3.0;
         }).toList();
         if (!reachable.isEmpty()) {
            this.targetBlock = reachable.stream().min(Comparator.comparingDouble(RegionComparators::score)).<BlockPos>map(BlockPos::toImmutable).orElse(null);
         }
      }
   }

   public RegionState getRegionState() {
      return this.regionState;
   }

   public long getLastScanTime() {
      return this.lastScanTime;
   }

   public BlockPos getTargetBlock() {
      return this.targetBlock;
   }
}
