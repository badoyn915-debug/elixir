package code.elixir.commands.defaults;

import code.elixir.features.impl.combat.Neuro;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class NeuroCommand extends Command {
   public NeuroCommand() {
      super("neuro", "Train or manage Neuro aura", "neuro");
   }

   @Override
   public void execute(String label, IArgConsumer argsObj) throws CommandException {
      if (!argsObj.hasAny()) {
         this.logDirect(Formatting.RED + "Usage: .neuro train <name>, .neuro train stop, .neuro recover <name>");
      } else {
         String action = argsObj.getString().toLowerCase();
         if (action.equals("train")) {
            if (argsObj.hasAny()) {
               String config = argsObj.getString();
               if (config.equalsIgnoreCase("stop")) {
                  Neuro.isTraining = false;
                  this.logDirect(Formatting.WHITE + "Neuro training stopped! Model saved: " + Neuro.currentConfig);
               } else {
                  Neuro.currentConfig = config;
                  Neuro.isTraining = true;
                  Neuro.trainingData.clear();
                  this.logDirect(Formatting.WHITE + "Neuro training started for model: " + config);
               }
            } else {
               this.logDirect(Formatting.RED + "Usage: .neuro train <name> OR .neuro train stop");
            }
         } else if (action.equals("recover")) {
            if (argsObj.hasAny()) {
               String config = argsObj.getString();
               Neuro.currentConfig = config;
               Neuro.isTraining = true;
               this.logDirect(Formatting.WHITE + "Neuro recovering (appending) for model: " + config);
            } else {
               this.logDirect(Formatting.RED + "Usage: .neuro recover <name>");
            }
         } else {
            this.logDirect(Formatting.RED + "Unknown Neuro command.");
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Train or manage Neuro aura";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Обучение Neuro Ауры", ".neuro train <name>", ".neuro train stop", ".neuro recover <name>");
   }
}
