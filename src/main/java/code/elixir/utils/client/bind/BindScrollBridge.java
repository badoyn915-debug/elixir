package code.elixir.utils.client.bind;

import code.elixir.events.keyboard.HotBarScrollEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.util.InputUtil.Type;

public class BindScrollBridge implements QuickImports {
   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent event) {
      if (mc.currentScreen == null && event.getVertical() != 0.0) {
         int code = event.getVertical() > 0.0 ? 998 : 999;
         SafeEvents.dispatch(() -> new KeyEvent(null, Type.KEYSYM, code, 1));
      }
   }
}
