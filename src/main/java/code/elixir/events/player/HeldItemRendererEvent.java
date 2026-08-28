package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class HeldItemRendererEvent implements Event {
   public Hand hand;
   public MatrixStack matrix;

   public HeldItemRendererEvent(Hand hand, MatrixStack matrix) {
      this.hand = hand;
      this.matrix = matrix;
   }

   public Hand getHand() {
      return this.hand;
   }

   public MatrixStack getMatrix() {
      return this.matrix;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }

   public void setMatrix(MatrixStack matrix) {
      this.matrix = matrix;
   }
}
