package code.elixir.commands.defaults;

import code.elixir.irc.IrcManager;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class IrcCommand extends Command {
   protected IrcCommand() {
      super("irc");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      IrcManager manager = IrcManager.getInstance();
      if (manager == null) {
         this.logDirect("IRC manager is not ready yet.");
      } else if (!args.hasAny()) {
         this.logUsage(manager);
      } else {
         String var5 = args.peekString().toLowerCase(Locale.ROOT);
         switch (var5) {
            case "status":
               args.getString();
               this.logDirect("IRC: " + manager.getConnectionState());
               this.logDirect("Relay: " + manager.getRelayUrl());
               this.logDirect("Nick: " + manager.getSelectedNickname());
               this.logDirect("Reply target: " + manager.getLastPrivateContact());
               break;
            case "nick":
               args.getString();
               args.requireMin(1);
               manager.setNickname(args.rawRest().trim());
               this.logDirect("IRC nick set to " + manager.getSelectedNickname());
               break;
            case "pm":
            case "msg":
            case "tell":
            case "w":
               args.getString();
               args.requireMin(2);
               String target = args.getString();
               if (!manager.sendPrivateMessage(target, args.rawRest().trim())) {
                  this.logSendFailure(manager);
               }
               break;
            case "reply":
            case "r":
               args.getString();
               args.requireMin(1);
               if (!manager.replyToLastPrivate(args.rawRest().trim())) {
                  if ("-".equals(manager.getLastPrivateContact())) {
                     this.logDirect("Nobody to reply to yet.");
                  } else {
                     this.logSendFailure(manager);
                  }
               }
               break;
            case "help":
               this.logUsage(manager);
               break;
            default:
               if (!manager.sendGlobalMessage(args.rawRest().trim())) {
                  this.logSendFailure(manager);
               }
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         return Stream.empty();
      } else {
         return args.hasExactlyOne()
            ? new TabCompleteHelper().append("status", "nick", "pm", "reply", "help").filterPrefix(args.peekString()).stream()
            : Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Relay IRC chat with nicknames and private replies";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Sends messages into the built-in IRC relay.",
         "",
         "Usage:",
         "> irc <message> - send to the global IRC chat",
         "> irc pm <nick> <message> - send a private message",
         "> irc reply <message> - reply to the last private contact",
         "> irc nick <name> - set your IRC nickname",
         "> irc status - show relay state"
      );
   }

   private void logUsage(IrcManager manager) {
      this.logDirect("IRC: " + manager.getConnectionState() + " | nick=" + manager.getSelectedNickname());
      this.logDirect(".irc <message>");
      this.logDirect(".irc pm <nick> <message>");
      this.logDirect(".irc reply <message>");
      this.logDirect(".irc nick <name>");
      this.logDirect(".irc status");
   }

   private void logSendFailure(IrcManager manager) {
      if (manager.isOnCooldown()) {
         double seconds = Math.ceil(manager.getRemainingCooldownMs() / 1000.0);
         this.logDirect("IRC cooldown: wait " + (int)seconds + "s");
      } else {
         this.logDirect("IRC relay is not connected yet.");
      }
   }
}
