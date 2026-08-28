package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleProvider;
import code.elixir.features.module.ModuleRepository;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.KeyDataType;
import code.elixir.utils.client.managers.api.command.datatypes.ModuleDataType;
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

public class BindCommand extends Command {
   private final ModuleProvider moduleProvider;
   private final ModuleRepository moduleRepository;

   public BindCommand(Elixir main) {
      super("bind");
      this.moduleRepository = main.getModuleRepository();
      this.moduleProvider = main.getModuleProvider();
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String var4 = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (var4) {
         case "add":
            this.handleAddBind(args);
            break;
         case "remove":
            this.handleRemoveBind(args);
            break;
         case "list":
            this.handleListBinds(args, label);
            break;
         case "clear":
            this.handleClearBinds(args);
      }
   }

   private void handleAddBind(IArgConsumer args) throws CommandException {
      args.requireMin(2);
      String moduleName = args.getString();
      int key = args.<Entry<String, Integer>, KeyDataType>getDatatypeFor(KeyDataType.INSTANCE).getValue();
      Object module = this.moduleProvider.get(moduleName);
      ((Module)module).setKey(key);
      this.logDirect(
         Formatting.WHITE
            + "Модуль "
            + Formatting.RED
            + moduleName
            + Formatting.WHITE
            + " привязан к кнопке "
            + Formatting.RED
            + StringHelper.getBindName(key).toLowerCase()
      );
   }

   private void handleRemoveBind(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      String moduleName = args.getString();
      Object module = this.moduleProvider.get(moduleName);
      ((Module)module).setKey(-1);
      this.logDirect(Formatting.WHITE + "Бинд для модуля " + Formatting.RED + moduleName + Formatting.WHITE + " был успешно удален!");
   }

   private void handleListBinds(IArgConsumer args, String label) throws CommandException {
      args.requireMax(1);
      List<Module> filtredList = this.moduleRepository.modules().stream().filter(module -> module.getKey() != -1).toList();
      Paginator.paginate(
         args,
         new Paginator<>(filtredList),
         () -> this.logDirect("Список модулей:"),
         module -> {
            String names = module.getName();
            String keys = StringHelper.getBindName(module.getKey()).toLowerCase();
            return Text.literal(Formatting.GRAY + "Название: " + Formatting.WHITE + names)
               .append(Text.literal(Formatting.GRAY + " Клавиша: " + Formatting.WHITE + keys));
         },
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
      );
   }

   private void handleClearBinds(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      this.moduleRepository.modules().forEach(function -> function.setKey(-1));
      this.logDirect("Все бинды модулей были удалены.", Formatting.WHITE);
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasExactlyOne()) {
         return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
      } else {
         String arg = args.getString();
         if (arg.equalsIgnoreCase("add")) {
            if (args.hasExactly(1)) {
               return args.tabCompleteDatatype(ModuleDataType.INSTANCE);
            }

            if (args.hasExactly(2)) {
               return args.tabCompleteDatatype(KeyDataType.INSTANCE);
            }
         }

         return Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Позволяет управлять биндами модулей";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Эта команда позволяет управлять биндами для модулей, которые будут активироваться при нажатии определённых клавиш.",
         "",
         "Использование:",
         "> bind add <module> <key> - Привязывает модуль к указанной клавише.",
         "> bind remove <module> - Удаляет привязку модуля.",
         "> bind list - Показывает список всех текущих биндов модулей.",
         "> bind clear - Удаляет все бинды модулей."
      );
   }
}
