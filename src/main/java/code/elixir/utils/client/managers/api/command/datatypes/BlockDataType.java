package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

public enum BlockDataType implements IDatatypeFor<Block> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> ways = this.streamBlocks().map(s -> s.getName().getString().replace(" ", "_"));
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public Block get(IDatatypeContext datatypeContext) throws CommandException {
      return this.findBlock(datatypeContext.getConsumer().getString()).orElse(null);
   }

   public Optional<Block> findBlock(String text) {
      return this.streamBlocks().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst();
   }

   public Stream<Block> streamBlocks() {
      return Registries.BLOCK.stream().filter(this::blackList);
   }

   public boolean blackList(Block block) {
      return !List.of(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER).contains(block);
   }
}
