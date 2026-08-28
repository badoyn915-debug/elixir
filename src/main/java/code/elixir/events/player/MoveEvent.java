package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class MoveEvent implements Event {
   private Vec3d movement;

   public Vec3d getMovement() {
      return this.movement;
   }

   public void setMovement(Vec3d movement) {
      this.movement = movement;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MoveEvent other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         Vec3d this$movement = this.getMovement();
         Vec3d other$movement = other.getMovement();
         return this$movement == null ? other$movement == null : this$movement.equals(other$movement);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof MoveEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Vec3d $movement = this.getMovement();
      return result * 59 + ($movement == null ? 43 : $movement.hashCode());
   }

   @Override
   public String toString() {
      return "MoveEvent(movement=" + this.getMovement() + ")";
   }

   public MoveEvent(Vec3d movement) {
      this.movement = movement;
   }
}
