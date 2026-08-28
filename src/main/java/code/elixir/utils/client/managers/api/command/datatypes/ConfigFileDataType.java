package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public enum ConfigFileDataType implements IDatatypeFor<String> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> friends = this.getConfigs().stream().map(String::toString);
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(friends).filterPrefix(context).sortAlphabetically().stream();
   }

   public String get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return this.getConfigs().stream().filter(s -> s.equalsIgnoreCase(username)).findFirst().orElse(null);
   }

   public List<String> getConfigs() {
      ArrayList<String> configs = new ArrayList<>();
      File[] configFiles = Elixir.getInstance().getClientInfoProvider().configsDir().listFiles();
      if (configFiles != null) {
         for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(".json")) {
               String configName = configFile.getName().replace(".json", "");
               configs.add(configName);
            }
         }
      }

      return configs;
   }
}
