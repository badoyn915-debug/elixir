package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public record BlockUpdateEvent(BlockState state, BlockPos pos, BlockUpdateEvent.Type type) implements Event {
   public static enum Type {
      LOAD,
      UNLOAD,
      UPDATE;
   }
}
