package code.elixir.events.item;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class HandAnimationEvent extends EventCancellable {
   private MatrixStack matrices;
   private Hand hand;
   private float swingProgress;
   private AbstractClientPlayerEntity player;
   private ItemStack stack;
   private Arm arm;
   private float equipProgress;
   private float tickDelta;

   public HandAnimationEvent(
      MatrixStack matrices, Hand hand, float swingProgress, AbstractClientPlayerEntity player, ItemStack stack, Arm arm, float equipProgress, float tickDelta
   ) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
      this.player = player;
      this.stack = stack;
      this.arm = arm;
      this.equipProgress = equipProgress;
      this.tickDelta = tickDelta;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public Hand getHand() {
      return this.hand;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Arm getArm() {
      return this.arm;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }

   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }

   public void setPlayer(AbstractClientPlayerEntity player) {
      this.player = player;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public void setArm(Arm arm) {
      this.arm = arm;
   }

   public void setEquipProgress(float equipProgress) {
      this.equipProgress = equipProgress;
   }

   public void setTickDelta(float tickDelta) {
      this.tickDelta = tickDelta;
   }
}
