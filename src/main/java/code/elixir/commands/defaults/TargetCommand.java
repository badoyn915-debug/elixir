package code.elixir.commands.defaults;

import code.elixir.common.repository.target.TargetRepository;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.TabPlayerDataType;
import code.elixir.utils.client.managers.api.command.datatypes.TargetDataType;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.client.managers.api.command.manager.ICommandManager;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TargetCommand extends Command implements QuickImports {
   private final TargetRepository repository = TargetRepository.getInstance();

   public TargetCommand() {
      super("target");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         this.handleListTargets(label, args);
      } else {
         String var4 = args.peekString().toLowerCase(Locale.US);
         switch (var4) {
            case "add":
               args.get();
               this.handleAddTarget(args);
               break;
            case "remove":
               args.get();
               this.handleRemoveTarget(args);
               break;
            case "clear":
               args.get();
               this.handleClearTargets(args);
               break;
            case "list":
               args.get();
               this.handleListTargets(label, args);
               break;
            default:
               this.handleAddTarget(args);
         }
      }
   }

   private void handleAddTarget(IArgConsumer args) throws CommandException {
      args.requireMin(1);
      String name = args.getString();
      if (!this.repository.isTarget(name)) {
         this.repository.addTarget(name);
         this.logDirect("Игрок " + Formatting.GREEN + name + Formatting.GRAY + " добавлен в приоритетный список.", Formatting.GRAY);
      } else {
         this.logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " уже в приоритетном списке.", Formatting.RED);
      }
   }

   private void handleRemoveTarget(IArgConsumer args) throws CommandException {
      args.requireMin(1);
      String name = args.getString();
      if (this.repository.isTarget(name)) {
         this.repository.removeTarget(name);
         this.logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " удален из приоритетного списка.", Formatting.GRAY);
      } else {
         this.logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " не найден в приоритетном списке.", Formatting.RED);
      }
   }

   private void handleClearTargets(IArgConsumer args) throws CommandException {
      args.requireMax(0);
      this.repository.clearTargets();
      this.logDirect("Приоритетный список целей очищен.", Formatting.GREEN);
   }

   private void handleListTargets(String label, IArgConsumer args) throws CommandException {
      args.requireMax(1);
      Paginator.paginate(
         args,
         new Paginator<>(this.repository.getTargets()),
         () -> this.logDirect("Приоритетный список целей:"),
         targetName -> Text.literal(Formatting.GRAY + "- " + Formatting.WHITE + targetName),
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label + " list"
      );
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasExactlyOne()) {
         return new TabCompleteHelper()
            .prepend("add", "remove", "list", "clear")
            .filterPrefix(args.peekString())
            .addCommands((ICommandManager)mc.getNetworkHandler().getCommandSource())
            .stream();
      } else if (args.hasAtMost(2) && args.peekString(0).equalsIgnoreCase("add")) {
         return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
      } else {
         return args.hasAtMost(2) && args.peekString(0).equalsIgnoreCase("remove") ? args.tabCompleteDatatype(TargetDataType.INSTANCE) : Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Управляет приоритетными целями для Aura.";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Добавляет игроков в приоритетный список для модуля Aura.",
         "",
         "Использование:",
         "> target <ник> - Добавить игрока в приоритет.",
         "> target add <ник> - Добавить игрока в приоритет.",
         "> target remove <ник> - Удалить игрока из приоритета.",
         "> target list - Показать список приоритетных игроков.",
         "> target clear - Очистить список."
      );
   }
}
