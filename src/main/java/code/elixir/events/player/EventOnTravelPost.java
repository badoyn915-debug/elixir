package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventOnTravelPost implements Event {
   private Vec3d oldVelocity;

   public Vec3d getOldVelocity() {
      return this.oldVelocity;
   }

   public void setOldVelocity(Vec3d oldVelocity) {
      this.oldVelocity = oldVelocity;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventOnTravelPost other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         Vec3d this$oldVelocity = this.getOldVelocity();
         Vec3d other$oldVelocity = other.getOldVelocity();
         return this$oldVelocity == null ? other$oldVelocity == null : this$oldVelocity.equals(other$oldVelocity);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventOnTravelPost;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Vec3d $oldVelocity = this.getOldVelocity();
      return result * 59 + ($oldVelocity == null ? 43 : $oldVelocity.hashCode());
   }

   @Override
   public String toString() {
      return "EventOnTravelPost(oldVelocity=" + this.getOldVelocity() + ")";
   }

   public EventOnTravelPost(Vec3d oldVelocity) {
      this.oldVelocity = oldVelocity;
   }
}
