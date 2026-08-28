package code.elixir.utils.client.managers.api.command.exception;

import code.elixir.utils.client.managers.api.command.ICommand;
import code.elixir.utils.client.managers.api.command.argument.ICommandArgument;
import code.elixir.utils.display.interfaces.QuickLogger;
import java.util.List;
import net.minecraft.util.Formatting;

public interface ICommandException extends QuickLogger {
   String getMessage();

   default void handle(ICommand command, List<ICommandArgument> args) {
      this.logDirect(this.getMessage(), Formatting.RED);
   }
}
