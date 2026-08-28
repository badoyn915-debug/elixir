package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class AspectRatioEvent extends EventCancellable {
   private float ratio;

   public float getRatio() {
      return this.ratio;
   }

   public void setRatio(float ratio) {
      this.ratio = ratio;
   }
}
