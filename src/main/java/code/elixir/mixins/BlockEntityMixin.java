package code.elixir.mixins;

import code.elixir.events.block.BlockEntityProgressEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockEntity.class})
public class BlockEntityMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void initHook(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
      BlockEntity blockEntity = (BlockEntity)(Object)this;
      if (blockEntity != null) {
         SafeEvents.dispatch(() -> new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.ADD));
      }
   }

   @Inject(
      method = {"markRemoved"},
      at = {@At("HEAD")}
   )
   private void markRemovedHook(CallbackInfo ci) {
      BlockEntity blockEntity = (BlockEntity)(Object)this;
      if (blockEntity != null) {
         SafeEvents.dispatch(() -> new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.REMOVE));
      }
   }
}
