package code.elixir.commands;

import code.elixir.commands.argparser.ArgParserManager;
import code.elixir.utils.client.managers.api.command.ICommandSystem;
import code.elixir.utils.client.managers.api.command.argparser.IArgParserManager;

public enum CommandSystem implements ICommandSystem {
   INSTANCE;

   @Override
   public IArgParserManager getParserManager() {
      return ArgParserManager.INSTANCE;
   }
}
