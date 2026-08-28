package code.elixir.utils.client.managers.event.impl;

public class EventLayer {
   protected boolean canceled = false;

   public void cancel() {
      this.canceled = true;
   }

   public void resume() {
      this.canceled = false;
   }

   public boolean isCanceled() {
      return this.canceled;
   }
}
