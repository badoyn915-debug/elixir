package code.elixir.utils.display.interfaces;

import code.elixir.utils.client.chat.ChatMessage;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface QuickLogger {
   static Text getPrefix() {
      MutableText text = ChatMessage.brandmessage();
      text.setStyle(text.getStyle().withColor(Formatting.GRAY));
      return text;
   }

   default void logDirect(Text... components) {
      MutableText component = Text.literal("");
      component.append(getPrefix());
      component.append(Text.literal(" "));
      Arrays.asList(components).forEach(arg_0 -> component.append(arg_0));
      if (MinecraftClient.getInstance().player != null) {
         MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(component);
      }
   }

   default void logDirect(String message, Formatting color) {
      Stream.of(message.split("\n")).forEach(line -> {
         MutableText component = Text.literal(line.replace("\t", "    "));
         component.setStyle(component.getStyle().withColor(color));
         this.logDirect(component);
      });
   }

   default void logDirect(String message) {
      this.logDirect(message, Formatting.WHITE);
   }
}
