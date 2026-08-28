package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.block.Block;

public enum BlockESPDataType implements IDatatypeFor<Block> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> blocks = this.getBlocks().stream().map(b -> b.getName().getString().replace(" ", "_"));
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(blocks).filterPrefix(context).sortAlphabetically().stream();
   }

   public Block get(IDatatypeContext datatypeContext) throws CommandException {
      String text = datatypeContext.getConsumer().getString();
      return this.getBlocks().stream().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst().orElse(null);
   }

   private List<? extends Block> getBlocks() {
      return Elixir.getInstance().getBoxESPRepository().blocks.keySet().stream().toList();
   }
}
