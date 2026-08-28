package code.elixir.mixins;

import code.elixir.features.impl.render.ItemReplacer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemRenderState.class})
public class ItemRenderStateMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void elixir$itemReplacerRender(MatrixStack matrixStack, VertexConsumerProvider buffer, int combinedLight, int combinedOverlay, CallbackInfo ci) {
      if (ItemReplacer.renderPending(matrixStack, buffer, combinedLight, combinedOverlay)) {
         ci.cancel();
      }
   }
}
