package code.elixir.events.block;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class PushEvent extends EventCancellable {
   private PushEvent.Type type;

   public PushEvent.Type getType() {
      return this.type;
   }

   public PushEvent(PushEvent.Type type) {
      this.type = type;
   }

   public static enum Type {
      COLLISION,
      BLOCK,
      WATER;
   }
}
