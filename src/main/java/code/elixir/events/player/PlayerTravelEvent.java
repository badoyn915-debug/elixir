package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.math.Vec3d;

public class PlayerTravelEvent extends EventCancellable {
   private Vec3d motion;
   private final boolean pre;

   public PlayerTravelEvent(Vec3d motion, boolean pre) {
      this.motion = motion;
      this.pre = pre;
   }

   public Vec3d getMotion() {
      return this.motion;
   }

   public void setMotion(Vec3d motion) {
      this.motion = motion;
   }

   public boolean isPre() {
      return this.pre;
   }
}
