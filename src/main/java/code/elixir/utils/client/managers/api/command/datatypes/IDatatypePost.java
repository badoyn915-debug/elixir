package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.utils.client.managers.api.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
   T apply(IDatatypeContext var1, O var2) throws CommandException;
}
