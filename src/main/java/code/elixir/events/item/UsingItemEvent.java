package code.elixir.events.item;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.Hand;

public class UsingItemEvent extends EventCancellable {
   private final byte type;
   private final Hand hand;

   public UsingItemEvent(byte type) {
      this(type, null);
   }

   public UsingItemEvent(byte type, Hand hand) {
      this.type = type;
      this.hand = hand;
   }

   public byte getType() {
      return this.type;
   }

   public Hand getHand() {
      return this.hand;
   }
}
