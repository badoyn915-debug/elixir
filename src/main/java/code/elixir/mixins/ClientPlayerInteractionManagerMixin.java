package code.elixir.mixins;

import code.elixir.events.block.BlockBreakingEvent;
import code.elixir.events.block.BreakBlockEvent;
import code.elixir.events.item.ClickSlotEvent;
import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.player.AttackEvent;
import code.elixir.utils.client.FakePlayerManager;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.interactions.item.ItemToolkit;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void attackEntityHook(PlayerEntity player, Entity target, CallbackInfo info) {
      if (FakePlayerManager.handleAttack(target)) {
         player.swingHand(Hand.MAIN_HAND);
         info.cancel();
      } else {
         AttackEvent event = SafeEvents.dispatch(() -> new AttackEvent(target));
         if (event != null && event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"interactItem"},
      at = {@At("RETURN")}
   )
   public void interactItemHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      Object object = cir.getReturnValue();
      Success success;
      if (object instanceof Success && !(success = (Success)object).swingSource().equals(SwingSource.CLIENT)) {
         SafeEvents.dispatch(() -> new UsingItemEvent((byte)0));
      }
   }

   @Inject(
      method = {"stopUsingItem"},
      at = {@At("HEAD")}
   )
   public void stopUsingItemHook(CallbackInfo ci) {
      SafeEvents.dispatch(() -> new UsingItemEvent((byte)2));
      if (ItemToolkit.INSTANCE.isUseItem()) {
         ItemToolkit.INSTANCE.setUseItem(false);
      }
   }

   @Inject(
      method = {"interactItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void gameModeHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      UsingItemEvent event = SafeEvents.dispatch(() -> new UsingItemEvent((byte)-1, hand));
      if (event != null && event.isCancelled()) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"clickSlot"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
      ClickSlotEvent event = SafeEvents.dispatch(() -> new ClickSlotEvent(syncId, slotId, button, actionType));
      if (event != null && event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")}
   )
   private void injectBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      SafeEvents.dispatch(() -> new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"breakBlock"},
      at = {@At("RETURN")}
   )
   private void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      SafeEvents.dispatch(() -> new BreakBlockEvent(pos));
   }
}
