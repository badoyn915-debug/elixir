package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventOnMovePost implements Event {
   private float speed;
   private Vec3d movementInput;

   public float getSpeed() {
      return this.speed;
   }

   public Vec3d getMovementInput() {
      return this.movementInput;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public void setMovementInput(Vec3d movementInput) {
      this.movementInput = movementInput;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventOnMovePost other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (Float.compare(this.getSpeed(), other.getSpeed()) != 0) {
         return false;
      } else {
         Vec3d this$movementInput = this.getMovementInput();
         Vec3d other$movementInput = other.getMovementInput();
         return this$movementInput == null ? other$movementInput == null : this$movementInput.equals(other$movementInput);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventOnMovePost;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getSpeed());
      Vec3d $movementInput = this.getMovementInput();
      return result * 59 + ($movementInput == null ? 43 : $movementInput.hashCode());
   }

   @Override
   public String toString() {
      return "EventOnMovePost(speed=" + this.getSpeed() + ", movementInput=" + this.getMovementInput() + ")";
   }

   public EventOnMovePost(float speed, Vec3d movementInput) {
      this.speed = speed;
      this.movementInput = movementInput;
   }
}
