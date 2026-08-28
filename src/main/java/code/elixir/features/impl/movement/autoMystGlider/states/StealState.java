package code.elixir.features.impl.movement.autoMystGlider.states;

import net.minecraft.util.math.BlockPos;

public class StealState {
   private final BlockPos blockPos;
   private boolean empty = false;

   public StealState(BlockPos blockPos) {
      this.blockPos = blockPos;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public boolean isEmpty() {
      return this.empty;
   }

   public void setEmpty(boolean empty) {
      this.empty = empty;
   }
}
