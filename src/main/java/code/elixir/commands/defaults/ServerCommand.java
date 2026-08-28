package code.elixir.commands.defaults;

import code.elixir.emotions.EmotionWheelManager;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ServerCommand extends Command {
   protected ServerCommand() {
      super("server");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      EmotionWheelManager manager = EmotionWheelManager.getInstance();
      this.logEmotionStatus(manager);
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Shows emotion sync status";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Shows emotion sync state.", "", "Usage:", "> server - prints emotion sync state");
   }

   private void logEmotionStatus(EmotionWheelManager manager) {
      String status = manager == null
         ? "unavailable"
         : (manager.isSyncConnected() ? "connected" : (manager.isSyncConnecting() ? "connecting" : "disconnected"));
      this.logDirect("Emotion sync: " + status);
   }
}
