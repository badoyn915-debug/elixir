package code.elixir.utils.client.chat;

import code.elixir.mixins.acc.InsertChatMixin;
import code.elixir.utils.client.text.TextHelper;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatMessage {
   public static MutableText brandmessage() {
      MutableText Elixir = (MutableText)TextHelper.applyPredefinedGradient("Elixir", "blue", true);
      MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
      return Elixir.append(arrow);
   }

   public static void send(Text message) {
      if (QuickImports.mc.inGameHud != null && QuickImports.mc.inGameHud.getChatHud() != null) {
         Text prefix = TextHelper.applyPredefinedGradient("Elixir", "blue", true);
         MutableText arrow = Text.literal(" >").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
         MutableText fullMessage = prefix.copy().append(arrow).append(" ").append(message);
         int tick = QuickImports.mc.inGameHud.getTicks();
         ChatHudLine line = new ChatHudLine(tick, fullMessage, null, MessageIndicator.system());
         ChatHud chatHud = QuickImports.mc.inGameHud.getChatHud();
         ((InsertChatMixin)chatHud).invokeAddMessage(line);
         ((InsertChatMixin)chatHud).invokeAddVisibleMessage(line);
      }
   }

   public static void brandmessage(String message) {
      if (MinecraftClient.getInstance().player != null) {
         MutableText Elixir = (MutableText)TextHelper.applyPredefinedGradient("Elixir", "blue", true);
         MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
         MutableText formattedMessage = Elixir.append(arrow).append(Text.literal(message));
         MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
      }
   }

   public static void brandmessage(Text message) {
      if (MinecraftClient.getInstance().player != null) {
         MutableText Elixir = (MutableText)TextHelper.applyPredefinedGradient("Elixir", "blue", true);
         MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
         MutableText formattedMessage = Elixir.append(arrow).append(message);
         MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
      }
   }
}
