package code.elixir.events.container;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.client.gui.screen.Screen;

public class SetScreenEvent implements Event {
   public Screen screen;

   public Screen getScreen() {
      return this.screen;
   }

   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   public SetScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
