package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;

public class RotatedMovementInputEvent implements Event {
   private float forward;
   private float sideways;

   public RotatedMovementInputEvent(float forward, float sideways) {
      this.forward = forward;
      this.sideways = sideways;
   }

   public float getForward() {
      return this.forward;
   }

   public float getSideways() {
      return this.sideways;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public void setSideways(float sideways) {
      this.sideways = sideways;
   }

   @Override
   public String toString() {
      return "RotatedMovementInputEvent(forward=" + this.getForward() + ", sideways=" + this.getSideways() + ")";
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof RotatedMovementInputEvent other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return Float.compare(this.getForward(), other.getForward()) != 0 ? false : Float.compare(this.getSideways(), other.getSideways()) == 0;
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof RotatedMovementInputEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getForward());
      return result * 59 + Float.floatToIntBits(this.getSideways());
   }
}
