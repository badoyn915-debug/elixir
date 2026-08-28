package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;

public class DeathScreenEvent implements Event {
   private int ticksSinceDeath;

   public int getTicksSinceDeath() {
      return this.ticksSinceDeath;
   }

   public DeathScreenEvent(int ticksSinceDeath) {
      this.ticksSinceDeath = ticksSinceDeath;
   }
}
