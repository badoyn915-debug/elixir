package code.elixir.common.logger.implement;

import code.elixir.common.logger.Logger;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class MinecraftLogger implements Logger, QuickImports {
   @Override
   public void log(Object message) {
   }

   @Override
   public void minecraftLog(Text... components) {
      if (mc.player != null) {
         MutableText component = Text.literal("");
         Arrays.asList(components).forEach(arg_0 -> component.append(arg_0));
         mc.inGameHud.getChatHud().addMessage(component);
      }
   }
}
