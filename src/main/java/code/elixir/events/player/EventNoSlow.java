package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class EventNoSlow extends EventCancellable {
   public boolean start;

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventNoSlow other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return !super.equals(o) ? false : this.isStart() == other.isStart();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventNoSlow;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      return result * 59 + (this.isStart() ? 79 : 97);
   }

   public boolean isStart() {
      return this.start;
   }

   @Override
   public String toString() {
      return "EventNoSlow(start=" + this.isStart() + ")";
   }

   public EventNoSlow(boolean start) {
      this.start = start;
   }

   public void setStart(boolean start) {
      this.start = start;
   }
}
