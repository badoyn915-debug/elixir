package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.math.Vec3d;

public class SwimmingEvent extends EventCancellable {
   Vec3d vector;

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public SwimmingEvent(Vec3d vector) {
      this.vector = vector;
   }
}
