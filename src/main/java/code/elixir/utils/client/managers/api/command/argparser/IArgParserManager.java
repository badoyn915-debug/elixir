package code.elixir.utils.client.managers.api.command.argparser;

import code.elixir.utils.client.managers.api.command.argument.ICommandArgument;
import code.elixir.utils.client.managers.api.command.exception.CommandInvalidTypeException;
import code.elixir.utils.client.managers.api.command.registry.Registry;

public interface IArgParserManager {
   <T> IArgParser.Stateless<T> getParserStateless(Class<T> var1);

   <T, S> IArgParser.Stated<T, S> getParserStated(Class<T> var1, Class<S> var2);

   <T> T parseStateless(Class<T> var1, ICommandArgument var2) throws CommandInvalidTypeException;

   <T, S> T parseStated(Class<T> var1, Class<S> var2, ICommandArgument var3, S var4) throws CommandInvalidTypeException;

   Registry<IArgParser> getRegistry();
}
