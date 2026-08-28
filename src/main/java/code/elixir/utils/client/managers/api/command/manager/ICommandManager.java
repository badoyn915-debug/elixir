package code.elixir.utils.client.managers.api.command.manager;

import code.elixir.utils.client.managers.api.command.ICommand;
import code.elixir.utils.client.managers.api.command.argument.ICommandArgument;
import code.elixir.utils.client.managers.api.command.registry.Registry;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;

public interface ICommandManager {
   Registry<ICommand> getRegistry();

   ICommand getCommand(String var1);

   boolean execute(String var1);

   boolean execute(Pair<String, List<ICommandArgument>> var1);

   Stream<String> tabComplete(Pair<String, List<ICommandArgument>> var1);

   Stream<String> tabComplete(String var1);
}
