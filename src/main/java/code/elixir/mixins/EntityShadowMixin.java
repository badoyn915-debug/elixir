package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public class EntityShadowMixin {
   @Inject(
      method = {"getShadowRadius"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetShadowRadius(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideShadows()) {
         cir.setReturnValue(0.0F);
      }
   }
}
