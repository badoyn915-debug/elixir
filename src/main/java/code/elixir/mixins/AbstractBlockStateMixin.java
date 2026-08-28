package code.elixir.mixins;

import code.elixir.events.player.PlayerCollisionEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractBlockState.class})
public abstract class AbstractBlockStateMixin implements QuickImports {
   @Shadow
   public abstract Block getBlock();

   @Inject(
      method = {"onEntityCollision"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onEntityCollision(World world, BlockPos pos, Entity entity, CallbackInfo ci) {
      if (entity == mc.player) {
         PlayerCollisionEvent event = SafeEvents.dispatch(() -> new PlayerCollisionEvent(this.getBlock()));
         if (event != null && event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
