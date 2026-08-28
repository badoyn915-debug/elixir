package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockCollisionEvent implements Event {
   private BlockPos blockPos;
   private BlockState state;

   public BlockCollisionEvent(BlockPos blockPos, BlockState state) {
      this.blockPos = blockPos;
      this.state = state;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public BlockState getState() {
      return this.state;
   }

   public void setBlockPos(BlockPos blockPos) {
      this.blockPos = blockPos;
   }

   public void setState(BlockState state) {
      this.state = state;
   }
}
