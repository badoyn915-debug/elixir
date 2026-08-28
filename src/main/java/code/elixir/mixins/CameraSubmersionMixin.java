package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class CameraSubmersionMixin {
   @Inject(
      method = {"getSubmersionType"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState()) {
         CameraSubmersionType type = (CameraSubmersionType)cir.getReturnValue();
         if (type != null) {
            if (type == CameraSubmersionType.WATER && noRender.shouldHideWaterOverlay()) {
               cir.setReturnValue(CameraSubmersionType.NONE);
            } else {
               if (type == CameraSubmersionType.LAVA && noRender.shouldHideLavaOverlay()) {
                  cir.setReturnValue(CameraSubmersionType.NONE);
               }
            }
         }
      }
   }
}
