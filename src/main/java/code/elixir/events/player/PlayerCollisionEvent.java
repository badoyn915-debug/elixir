package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.block.Block;

public class PlayerCollisionEvent extends EventCancellable {
   private Block block;

   public void setBlock(Block block) {
      this.block = block;
   }

   public Block getBlock() {
      return this.block;
   }

   public PlayerCollisionEvent(Block block) {
      this.block = block;
   }
}
