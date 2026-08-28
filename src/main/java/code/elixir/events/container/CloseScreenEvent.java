package code.elixir.events.container;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.client.gui.screen.Screen;

public class CloseScreenEvent extends EventCancellable {
   private Screen screen;

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof CloseScreenEvent other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else {
         Screen this$screen = this.getScreen();
         Screen other$screen = other.getScreen();
         return this$screen == null ? other$screen == null : this$screen.equals(other$screen);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof CloseScreenEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      Screen $screen = this.getScreen();
      return result * 59 + ($screen == null ? 43 : $screen.hashCode());
   }

   public Screen getScreen() {
      return this.screen;
   }

   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Override
   public String toString() {
      return "CloseScreenEvent(screen=" + this.getScreen() + ")";
   }

   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
