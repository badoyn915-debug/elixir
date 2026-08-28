package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class CameraPositionEvent implements Event {
   private Vec3d pos;

   public Vec3d getPos() {
      return this.pos;
   }

   public void setPos(Vec3d pos) {
      this.pos = pos;
   }

   public CameraPositionEvent(Vec3d pos) {
      this.pos = pos;
   }
}
