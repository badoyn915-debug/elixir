package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.commands.manager.CommandRepository;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.ICommand;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.exception.CommandNotFoundException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class HelpCommand extends Command {
   Elixir main;

   protected HelpCommand(Elixir main) {
      super("help");
      this.main = main;
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      args.requireMax(1);
      CommandRepository commandRepository = this.main.getCommandRepository();
      if (args.hasAny() && !args.is(Integer.class)) {
         String commandName = args.getString().toLowerCase();
         ICommand command2 = commandRepository.getCommand(commandName);
         if (command2 == null) {
            throw new CommandNotFoundException(commandName);
         }

         this.logDirect("");
         command2.getLongDesc().forEach(this::logDirect);
         this.logDirect("");
         MutableText returnComponent = Text.literal("Нажмите что бы вернуться обратно в меню");
         returnComponent.setStyle(
            returnComponent.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label))
         );
         this.logDirect(new Text[]{returnComponent});
      } else {
         Paginator.paginate(
            args,
            new Paginator<>(commandRepository.getRegistry().descendingStream().filter(command -> !command.hiddenFromHelp()).collect(Collectors.toList())),
            () -> this.logDirect("Доступные команды:"),
            command -> {
               String names = String.join("/", command.getNames());
               String name = command.getNames().get(0);
               MutableText shortDescComponent = Text.literal(" - " + command.getShortDesc());
               shortDescComponent.setStyle(shortDescComponent.getStyle().withColor(Formatting.WHITE));
               MutableText namesComponent = Text.literal(names);
               namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
               MutableText hoverComponent = Text.literal("");
               hoverComponent.setStyle(hoverComponent.getStyle().withColor(Formatting.GRAY));
               hoverComponent.append(namesComponent);
               hoverComponent.append("\n" + command.getShortDesc());
               hoverComponent.append("\n\nНажмите, чтобы просмотреть полную справку о команде");
               String clickCommand = IBaritoneChatControl.FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.getNames().get(0));
               MutableText component = Text.literal(name);
               component.setStyle(component.getStyle().withColor(Formatting.WHITE));
               component.append(shortDescComponent);
               component.setStyle(
                  component.getStyle()
                     .withHoverEvent(new HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, hoverComponent))
                     .withClickEvent(new ClickEvent(Action.RUN_COMMAND, clickCommand))
               );
               return component;
            },
            IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
         );
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return args.hasExactlyOne()
         ? new TabCompleteHelper().addCommands(Elixir.getInstance().getCommandRepository()).filterPrefix(args.getString()).stream()
         : Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Просмотр всех доступных команд";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно просмотреть подробную справочную информацию о том, как использовать определенные команды",
         "",
         "Использование:",
         "> help - Перечисляет все команды и их краткие описания.",
         "> help <command> - Отображение справочной информации по конкретной команде."
      );
   }
}
