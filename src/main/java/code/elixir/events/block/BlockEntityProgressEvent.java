package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.block.entity.BlockEntity;

public record BlockEntityProgressEvent(BlockEntity blockEntity, BlockEntityProgressEvent.Type type) implements Event {
   public static enum Type {
      ADD,
      REMOVE;
   }
}
