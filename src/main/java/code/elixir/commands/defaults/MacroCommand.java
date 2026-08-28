package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.common.repository.macro.MacroRepository;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.KeyDataType;
import code.elixir.utils.client.managers.api.command.datatypes.MacroDataType;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MacroCommand extends Command {
   private final MacroRepository macroRepository;

   public MacroCommand(Elixir main) {
      super("macro", "macros");
      this.macroRepository = main.getMacroRepository();
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String var4 = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (var4) {
         case "add":
            this.handleAddMacro(args);
            break;
         case "remove":
            this.handleRemoveMacro(args);
            break;
         case "list":
            this.handleListMacros(args, label);
            break;
         case "clear":
            this.handleClearMacros(args);
      }
   }

   private void handleAddMacro(IArgConsumer args) throws CommandException {
      args.requireMin(3);
      int key = args.<Entry<String, Integer>, KeyDataType>getDatatypeFor(KeyDataType.INSTANCE).getValue();
      String name = args.getString();
      String command = args.rawRest();
      if (this.macroRepository.hasMacro(name)) {
         this.logDirect("Макрос с таким именем уже есть в списке!", Formatting.RED);
      } else {
         this.macroRepository.addMacro(name, command, key);
         this.logDirect(
            Formatting.WHITE
               + "Добавлен макрос с названием "
               + Formatting.RED
               + name
               + Formatting.WHITE
               + " с кнопкой "
               + Formatting.RED
               + StringHelper.getBindName(key).toLowerCase()
               + Formatting.WHITE
               + " с командой "
               + Formatting.RED
               + command
         );
      }
   }

   private void handleRemoveMacro(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      String name = args.getString();
      if (this.macroRepository.hasMacro(name)) {
         this.macroRepository.deleteMacro(name);
         this.logDirect(Formatting.WHITE + "Макрос " + Formatting.RED + name + Formatting.WHITE + " был успешно удален!");
      } else {
         this.logDirect("Макрос с таким именем не найден!", Formatting.RED);
      }
   }

   private void handleListMacros(IArgConsumer args, String label) throws CommandException {
      args.requireMax(1);
      Paginator.paginate(
         args,
         new Paginator<>(this.macroRepository.macroList),
         () -> this.logDirect("Список макросов:"),
         macro -> {
            String names = macro.name();
            String keys = StringHelper.getBindName(macro.key()).toLowerCase();
            String command = macro.message();
            return Text.literal(Formatting.GRAY + "Название: " + Formatting.WHITE + names)
               .append(Text.literal(Formatting.GRAY + " Клавиша: " + Formatting.WHITE + keys))
               .append(Text.literal(Formatting.GRAY + " Команда: " + Formatting.WHITE + command));
         },
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
      );
   }

   private void handleClearMacros(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      this.macroRepository.clearList();
      this.logDirect("Все макросы были удалены.", Formatting.WHITE);
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny() && args.hasExactlyOne()) {
         return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
      } else {
         if (args.hasAny()) {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("add") && args.hasExactlyOne()) {
               return args.tabCompleteDatatype(KeyDataType.INSTANCE);
            }

            if (arg.equalsIgnoreCase("remove") && args.hasExactlyOne()) {
               return args.tabCompleteDatatype(MacroDataType.INSTANCE);
            }
         }

         return Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Позволяет управлять макросами";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Эта команда позволяет управлять макросами, которые автоматически вводят заданные команды в чат.",
         "",
         "Использование:",
         "> macro add <key> <name> <message> - Добавляет новый макрос, который будет активироваться при нажатии на указанную клавишу и вводить указанное сообщение.",
         "> macro remove <name> - Удаляет макрос с указанным именем.",
         "> macro list - Отображает список всех текущих макросов.",
         "> macro clear - Удаляет все макросы из списка."
      );
   }
}
