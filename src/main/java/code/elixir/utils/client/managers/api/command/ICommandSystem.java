package code.elixir.utils.client.managers.api.command;

import code.elixir.utils.client.managers.api.command.argparser.IArgParserManager;

public interface ICommandSystem {
   IArgParserManager getParserManager();
}
