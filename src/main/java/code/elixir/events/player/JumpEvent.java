package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.player.PlayerEntity;

public class JumpEvent extends EventCancellable {
   private PlayerEntity player;

   public PlayerEntity getPlayer() {
      return this.player;
   }

   public JumpEvent(PlayerEntity player) {
      this.player = player;
   }
}
