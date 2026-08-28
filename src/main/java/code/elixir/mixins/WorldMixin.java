package code.elixir.mixins;

import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({World.class})
public abstract class WorldMixin implements QuickImports {
   @Inject(
      method = {"onBlockChanged"},
      at = {@At("RETURN")}
   )
   private void onBlockChangedHook(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
      if (mc.world == (Object)this) {
         SafeEvents.dispatch(() -> new BlockUpdateEvent(newBlock, pos.toImmutable(), BlockUpdateEvent.Type.UPDATE));
      }
   }
}
