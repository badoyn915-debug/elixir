package code.elixir.commands.defaults;

import code.elixir.commands.CommandDispatcher;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class PrefixCommand extends Command implements QuickImports {
   protected PrefixCommand() {
      super("prefix");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      if (arg.equals("set")) {
         args.requireMin(1);
         CommandDispatcher.prefix = args.getString();
         this.logDirect("Установлен префикс '" + Formatting.RED + CommandDispatcher.prefix + Formatting.GRAY + "'", Formatting.GRAY);
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         return arg.equalsIgnoreCase("set")
            ? new TabCompleteHelper().sortAlphabetically().prepend("name").stream()
            : new TabCompleteHelper().sortAlphabetically().prepend("set").filterPrefix(arg).stream();
      } else {
         return Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Позволяет менять префикс команд в моде";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно изменить префикс команд в моде", "", "Использование:", "> prefix set <name> - устанавливает префикс команд"
      );
   }
}
