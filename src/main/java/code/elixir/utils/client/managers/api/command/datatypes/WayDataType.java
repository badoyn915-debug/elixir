package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.Elixir;
import code.elixir.common.repository.way.Way;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;

public enum WayDataType implements IDatatypeFor<Way> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> ways = this.getWay().stream().map(Way::name);
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public Way get(IDatatypeContext datatypeContext) throws CommandException {
      String text = datatypeContext.getConsumer().getString();
      return this.getWay().stream().filter(s -> s.name().equalsIgnoreCase(text)).findFirst().orElse(null);
   }

   private List<? extends Way> getWay() {
      return Elixir.getInstance().getWayRepository().wayList;
   }
}
