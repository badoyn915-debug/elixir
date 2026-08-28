package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
   Stream<String> tabComplete(IDatatypeContext var1) throws CommandException;
}
