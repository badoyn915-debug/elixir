package code.elixir.commands.manager;

import code.elixir.Elixir;
import code.elixir.commands.argument.ArgConsumer;
import code.elixir.commands.argument.CommandArguments;
import code.elixir.commands.defaults.DefaultCommands;
import code.elixir.utils.client.managers.api.command.ICommand;
import code.elixir.utils.client.managers.api.command.argument.ICommandArgument;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.exception.CommandUnhandledException;
import code.elixir.utils.client.managers.api.command.exception.ICommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.client.managers.api.command.manager.ICommandManager;
import code.elixir.utils.client.managers.api.command.registry.Registry;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Pair;

public class CommandRepository implements ICommandManager {
   private final Registry<ICommand> registry = new Registry<>();

   public CommandRepository() {
      DefaultCommands.createAll().forEach(this.registry::register);
   }

   @Override
   public Registry<ICommand> getRegistry() {
      return this.registry;
   }

   @Override
   public ICommand getCommand(String name) {
      for (ICommand command : this.registry.entries) {
         if (command.getNames().contains(name.toLowerCase(Locale.US))) {
            return command;
         }
      }

      return null;
   }

   @Override
   public boolean execute(String string) {
      return this.execute(expand(string));
   }

   @Override
   public boolean execute(Pair<String, List<ICommandArgument>> expanded) {
      CommandRepository.ExecutionWrapper execution = this.from(expanded);
      if (execution != null) {
         execution.execute();
      }

      return execution != null;
   }

   @Override
   public Stream<String> tabComplete(Pair<String, List<ICommandArgument>> expanded) {
      CommandRepository.ExecutionWrapper execution = this.from(expanded);
      return execution == null ? Stream.empty() : execution.tabComplete();
   }

   @Override
   public Stream<String> tabComplete(String prefix) {
      Pair<String, List<ICommandArgument>> pair = expand(prefix, true);
      String label = (String)pair.getLeft();
      List args = (List)pair.getRight();
      return args.isEmpty()
         ? new TabCompleteHelper().addCommands(Elixir.getInstance().getCommandRepository()).filterPrefix(label).stream()
         : this.tabComplete(pair);
   }

   private CommandRepository.ExecutionWrapper from(Pair<String, List<ICommandArgument>> expanded) {
      String label = (String)expanded.getLeft();
      ArgConsumer args = new ArgConsumer(this, (List<ICommandArgument>)expanded.getRight());
      ICommand command = this.getCommand(label);
      return command == null ? null : new CommandRepository.ExecutionWrapper(command, label, args);
   }

   private static Pair<String, List<ICommandArgument>> expand(String string, boolean preserveEmptyLast) {
      String label = string.split("\\s", 2)[0];
      List<ICommandArgument> args = CommandArguments.from(string.substring(label.length()), preserveEmptyLast);
      return new Pair(label, args);
   }

   public static Pair<String, List<ICommandArgument>> expand(String string) {
      return expand(string, false);
   }

   private static final class ExecutionWrapper {
      private ICommand command;
      private String label;
      private ArgConsumer args;

      private ExecutionWrapper(ICommand command, String label, ArgConsumer args) {
         this.command = command;
         this.label = label;
         this.args = args;
      }

      private void execute() {
         try {
            this.command.execute(this.label, this.args);
         } catch (Throwable var3) {
            ICommandException exception = (ICommandException)(var3 instanceof ICommandException ? (ICommandException)var3 : new CommandUnhandledException(var3));
            exception.handle(this.command, this.args.getArgs());
         }
      }

      private Stream<String> tabComplete() {
         try {
            return this.command.tabComplete(this.label, this.args);
         } catch (CommandException var2) {
         } catch (Throwable var3) {
            var3.printStackTrace();
         }

         return Stream.empty();
      }
   }
}
