package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class EntityColorEvent extends EventCancellable {
   private int color;

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public EntityColorEvent(int color) {
      this.color = color;
   }
}
