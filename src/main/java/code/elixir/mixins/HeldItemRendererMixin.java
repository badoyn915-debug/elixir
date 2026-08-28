package code.elixir.mixins;

import code.elixir.events.item.HandAnimationEvent;
import code.elixir.events.item.HandOffsetEvent;
import code.elixir.events.render.ItemRendererEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
         shift = Shift.AFTER
      )}
   )
   private void renderFirstPersonItemHook(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
      SafeEvents.dispatch(() -> new HandOffsetEvent(matrices, stack, hand, player, swingProgress, equipProgress, tickDelta, arm));
   }

   @WrapOperation(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )}
   )
   private void itemRenderHook(
      HeldItemRenderer instance,
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      Operation<Void> original
   ) {
      ItemRendererEvent event = SafeEvents.dispatch(() -> new ItemRendererEvent(player, item, hand));
      original.call(
         new Object[]{
            instance,
            event != null ? event.getPlayer() : player,
            tickDelta,
            pitch,
            event != null ? event.getHand() : hand,
            swingProgress,
            event != null ? event.getStack() : item,
            equipProgress,
            matrices,
            vertexConsumers,
            light
         }
      );
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
         ordinal = 2
      )}
   )
   private void handAnimationHook(
      HeldItemRenderer instance,
      float swingProgress,
      float equipProgress,
      MatrixStack matrices,
      int armX,
      Arm arm,
      Operation<Void> original,
      @Local(ordinal = 0,argsOnly = true) AbstractClientPlayerEntity player,
      @Local(ordinal = 0,argsOnly = true) float tickDelta,
      @Local(ordinal = 0,argsOnly = true) Hand hand,
      @Local(ordinal = 0,argsOnly = true) ItemStack stack
   ) {
      HandAnimationEvent event = SafeEvents.dispatch(() -> new HandAnimationEvent(matrices, hand, swingProgress, player, stack, arm, equipProgress, tickDelta));
      if (event == null || !event.isCancelled()) {
         original.call(new Object[]{instance, swingProgress, equipProgress, matrices, armX, arm});
      }
   }
}
