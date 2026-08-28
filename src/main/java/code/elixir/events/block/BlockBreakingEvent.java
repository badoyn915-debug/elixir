package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {
}
