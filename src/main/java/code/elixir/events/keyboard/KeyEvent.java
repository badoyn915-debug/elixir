package code.elixir.events.keyboard;

import code.elixir.utils.client.bind.BindCodes;
import code.elixir.utils.client.managers.event.events.Event;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event, QuickImports {
   public boolean isKeyDown(int key) {
      return this.matchesBind(key, mc.currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean screen) {
      return this.matchesBind(key, screen);
   }

   public boolean matchesBind(int bindKey, boolean allowScreen) {
      if (bindKey == 998 || bindKey == 999) {
         return false;
      } else if (this.action != 1 || !allowScreen) {
         return false;
      } else {
         return BindCodes.isMouse(bindKey) ? this.type == Type.MOUSE && this.key == bindKey : this.type == Type.KEYSYM && this.key == bindKey;
      }
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, mc.currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean screen) {
      return this.key == key && this.action == 0 && screen;
   }
}
