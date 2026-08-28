package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class FireworkEvent implements Event {
   public Vec3d vector;

   public FireworkEvent(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
