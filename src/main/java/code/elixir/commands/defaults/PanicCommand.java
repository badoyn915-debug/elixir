package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.features.impl.misc.Panic;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class PanicCommand extends Command {
   public PanicCommand() {
      super("panic");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      Panic panicModule = Elixir.getInstance()
         .getModuleRepository()
         .modules()
         .stream()
         .filter(module -> module instanceof Panic)
         .map(module -> (Panic)module)
         .findFirst()
         .orElse(null);
      if (panicModule != null) {
         panicModule.setState(true);
         this.logDirect(Formatting.GREEN + "Panic mode activated! Client is now hidden.");
      } else {
         this.logDirect(Formatting.RED + "Panic module not found!");
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Активирует режим паники - полностью скрывает клиент";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда активирует режим паники, который полностью скрывает клиент.",
         "",
         "При активации:",
         "- Показывается код восстановления (6 цифр)",
         "- Идет обратный отсчет 5-4-3-2-1",
         "- Все модули отключаются",
         "- Чат очищается",
         "- Клиент полностью скрывается",
         "",
         "Для восстановления введите код в чат.",
         "",
         "Использование:",
         "> panic - Активировать режим паники"
      );
   }
}
