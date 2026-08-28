package code.elixir.events.chat;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class ChatEvent extends EventCancellable {
   private String message;

   public ChatEvent(String message) {
      this.message = message;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}
