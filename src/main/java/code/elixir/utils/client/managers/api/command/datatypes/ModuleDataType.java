package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.Elixir;
import code.elixir.features.module.Module;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;

public enum ModuleDataType implements IDatatypeFor<Module> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> source = this.getModules().stream().map(Module::getName);
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(source).filterPrefix(context).sortAlphabetically().stream();
   }

   public Module get(IDatatypeContext datatypeContext) throws CommandException {
      String name = datatypeContext.getConsumer().getString();
      return this.getModules().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   private List<? extends Module> getModules() {
      return Elixir.getInstance().getModuleRepository().modules();
   }
}
