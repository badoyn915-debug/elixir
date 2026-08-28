package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class ItemRendererEvent implements Event {
   private AbstractClientPlayerEntity player;
   private ItemStack stack;
   private Hand hand;

   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public void setPlayer(AbstractClientPlayerEntity player) {
      this.player = player;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }

   public ItemRendererEvent(AbstractClientPlayerEntity player, ItemStack stack, Hand hand) {
      this.player = player;
      this.stack = stack;
      this.hand = hand;
   }
}
