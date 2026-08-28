package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class EntityGlowMixin {
   @Inject(
      method = {"isGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)(Object)this;
      NoRender noRender;
      if (self instanceof PlayerEntity && (noRender = NoRender.getInstance()) != null && noRender.shouldHidePlayerGlow()) {
         cir.setReturnValue(false);
      }
   }
}
