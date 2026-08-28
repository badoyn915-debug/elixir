package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.ICommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {
   public static List<ICommand> createAll() {
      Elixir main = Elixir.getInstance();
      ArrayList<Command> commands = new ArrayList<>(
         Arrays.asList(
            new ConfigCommand(main),
            new ThemeCommand(main),
            new MacroCommand(main),
            new BindCommand(main),
            new WayCommand(main),
            new FriendCommand(),
            new PrefixCommand(),
            new TargetCommand(),
            new StaffCommand(),
            new BlockESPCommand(),
            new RCTCommand(main),
            new DragCommand(),
            new FakePlayerCommand(),
            new VClipCommand(),
            new ServerCommand(),
            new IrcCommand(),
            new HelpCommand(main),
            new PanicCommand(),
            new NeuroCommand()
         )
      );
      return Collections.unmodifiableList(commands);
   }
}
