package code.elixir.commands.defaults;

import code.elixir.utils.client.FakePlayerManager;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class FakePlayerCommand extends Command {
   public FakePlayerCommand() {
      super("fakeplayer", "fakeplyer");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String var4 = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "add";
      switch (var4) {
         case "add":
         case "spawn":
            String name = args.hasAny() ? args.getString() : "FakePlayer";
            if (FakePlayerManager.add(name)) {
               this.logDirect("FakePlayer spawned. He is invulnerable.", Formatting.GREEN);
            } else {
               this.logDirect("Join a world first.", Formatting.RED);
            }
            break;
         case "del":
         case "delete":
         case "remove":
            if (FakePlayerManager.remove()) {
               this.logDirect("FakePlayer removed.", Formatting.GREEN);
            } else {
               this.logDirect("FakePlayer is not spawned.", Formatting.YELLOW);
            }
            break;
         default:
            this.logDirect("Usage: ." + label + " add [name]", Formatting.GRAY);
            this.logDirect("Usage: ." + label + " del", Formatting.GRAY);
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!args.hasAny()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "del").filterPrefix(arg).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Spawn a client-side fake player target";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Spawns a client-side invulnerable fake player for training.", "", "Usage:", "> fakeplyer add [name]", "> fakeplyer del");
   }
}
