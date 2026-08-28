package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;

public class SprintEvent implements Event {
   private boolean sprinting;

   public boolean isSprinting() {
      return this.sprinting;
   }

   public void setSprinting(boolean sprinting) {
      this.sprinting = sprinting;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof SprintEvent other)) {
         return false;
      } else {
         return !other.canEqual(this) ? false : this.isSprinting() == other.isSprinting();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof SprintEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      return result * 59 + (this.isSprinting() ? 79 : 97);
   }

   @Override
   public String toString() {
      return "SprintEvent(sprinting=" + this.isSprinting() + ")";
   }

   public SprintEvent(boolean sprinting) {
      this.sprinting = sprinting;
   }
}
