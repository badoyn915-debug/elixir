package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class FovEvent extends EventCancellable {
   private int fov;

   public int getFov() {
      return this.fov;
   }

   public void setFov(int fov) {
      this.fov = fov;
   }
}
