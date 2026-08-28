package code.elixir.mixins;

import code.elixir.events.block.BlockCollisionEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BlockCollisionSpliterator.class})
public abstract class BlockCollisionSpliteratorMixin {
   @Redirect(
      method = {"computeNext"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
      )
   )
   private BlockState computeNext(BlockView instance, BlockPos blockPos) {
      BlockState state = instance.getBlockState(blockPos);
      if (!SafeEvents.hasListeners(BlockCollisionEvent.class)) {
         return state;
      } else {
         BlockCollisionEvent event = SafeEvents.dispatch(BlockCollisionEvent.class, () -> new BlockCollisionEvent(blockPos, state));
         return event != null ? event.getState() : state;
      }
   }
}
