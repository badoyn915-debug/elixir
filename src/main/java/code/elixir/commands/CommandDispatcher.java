package code.elixir.commands;

import code.elixir.Elixir;
import code.elixir.commands.argument.ArgConsumer;
import code.elixir.commands.argument.CommandArguments;
import code.elixir.commands.manager.CommandRepository;
import code.elixir.events.chat.ChatEvent;
import code.elixir.events.chat.TabCompleteEvent;
import code.elixir.features.impl.misc.Panic;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.ICommandArgument;
import code.elixir.utils.client.managers.api.command.exception.CommandNotEnoughArgumentsException;
import code.elixir.utils.client.managers.api.command.exception.CommandNotFoundException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.client.managers.api.command.manager.ICommandManager;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickLogger;
import code.elixir.utils.interactions.path.BaritoneHelper;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;

public class CommandDispatcher implements QuickLogger {
   private final ICommandManager manager = Elixir.getInstance().getCommandRepository();
   public static String prefix = ".";

   public CommandDispatcher(EventManager eventManager) {
      EventManager.register(this);
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      String msg = event.getMessage();
      if (Panic.isPanicMode()) {
         if (!msg.equals(Panic.getRecoveryCode())) {
            ;
         }
      } else if (!msg.startsWith("/")) {
         if (BaritoneHelper.handleChatCommand(msg)) {
            event.cancel();
         } else {
            boolean forceRun = msg.startsWith(IBaritoneChatControl.FORCE_COMMAND_PREFIX);
            if (msg.startsWith(prefix) || forceRun) {
               event.cancel();
               String commandStr = msg.substring(forceRun ? IBaritoneChatControl.FORCE_COMMAND_PREFIX.length() : prefix.length());
               if (!this.runCommand(commandStr) && !commandStr.trim().isEmpty()) {
                  new CommandNotFoundException((String)CommandRepository.expand(commandStr).getLeft()).handle(null, null);
               }
            }
         }
      }
   }

   public boolean runCommand(String msg) {
      if (msg.isEmpty()) {
         return this.runCommand("help");
      } else {
         Pair<String, List<ICommandArgument>> pair = CommandRepository.expand(msg);
         String command = (String)pair.getLeft();
         String rest = msg.substring(((String)pair.getLeft()).length());
         new ArgConsumer(this.manager, (List<ICommandArgument>)pair.getRight());
         return this.manager.execute(pair);
      }
   }

   @EventHandler
   public void onTabComplete(TabCompleteEvent event) {
      if (!Panic.isPanicMode()) {
         String eventPrefix = event.prefix;
         if (eventPrefix.startsWith(prefix)) {
            String msg = eventPrefix.substring(prefix.length());
            List<ICommandArgument> args = CommandArguments.from(msg, true);
            Stream<String> stream = this.tabComplete(msg);
            if (args.size() == 1) {
               stream = stream.map(x -> prefix + x);
            }

            event.completions = stream.toArray(String[]::new);
         }
      }
   }

   public Stream<String> tabComplete(String msg) {
      try {
         List<ICommandArgument> args = CommandArguments.from(msg, true);
         ArgConsumer argc = new ArgConsumer(this.manager, args);
         return argc.hasAtMost(2) && argc.hasExactly(1)
            ? new TabCompleteHelper().addCommands(this.manager).filterPrefix(argc.getString()).stream()
            : this.manager.tabComplete(msg);
      } catch (CommandNotEnoughArgumentsException var4) {
         return Stream.empty();
      }
   }
}
