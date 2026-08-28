package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.BlockPos;

public record BreakBlockEvent(BlockPos blockPos) implements Event {
}
