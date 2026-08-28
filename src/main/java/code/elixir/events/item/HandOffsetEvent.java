package code.elixir.events.item;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class HandOffsetEvent implements Event {
   private MatrixStack matrices;
   private ItemStack stack;
   private Hand hand;
   private AbstractClientPlayerEntity player;
   private float swingProgress;
   private float equipProgress;
   private float tickDelta;
   private Arm arm;

   public HandOffsetEvent(
      MatrixStack matrices, ItemStack stack, Hand hand, AbstractClientPlayerEntity player, float swingProgress, float equipProgress, float tickDelta, Arm arm
   ) {
      this.matrices = matrices;
      this.stack = stack;
      this.hand = hand;
      this.player = player;
      this.swingProgress = swingProgress;
      this.equipProgress = equipProgress;
      this.tickDelta = tickDelta;
      this.arm = arm;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public Arm getArm() {
      return this.arm;
   }

   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }

   public void setPlayer(AbstractClientPlayerEntity player) {
      this.player = player;
   }

   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }

   public void setEquipProgress(float equipProgress) {
      this.equipProgress = equipProgress;
   }

   public void setTickDelta(float tickDelta) {
      this.tickDelta = tickDelta;
   }

   public void setArm(Arm arm) {
      this.arm = arm;
   }
}
