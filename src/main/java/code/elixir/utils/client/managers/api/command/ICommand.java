package code.elixir.utils.client.managers.api.command;

import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.display.interfaces.QuickLogger;
import java.util.List;
import java.util.stream.Stream;

public interface ICommand extends QuickLogger {
   void execute(String var1, IArgConsumer var2) throws CommandException;

   Stream<String> tabComplete(String var1, IArgConsumer var2) throws CommandException;

   String getShortDesc();

   List<String> getLongDesc();

   List<String> getNames();

   default boolean hiddenFromHelp() {
      return false;
   }
}
