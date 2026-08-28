package code.elixir.mixins;

import code.elixir.features.impl.render.CustomCape;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayerEntity.class})
public class AbstractClientPlayerEntityMixin {
   @Inject(
      method = {"getSkinTextures"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void customCape$getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
      AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)(Object)this;
      CustomCape customCape = CustomCape.getInstance();
      if (customCape != null && customCape.shouldApply(player)) {
         cir.setReturnValue(CustomCape.withCustomCape((SkinTextures)cir.getReturnValue()));
      }
   }
}
