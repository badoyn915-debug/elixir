package code.elixir.mixins;

import code.elixir.features.impl.render.CustomCape;
import code.elixir.features.impl.render.NameTags;
import code.elixir.features.impl.render.custommodels.CustomModelFeatureRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerEntityRenderer.class})
public class PlayerEntityRendererMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void Elixir$addCustomModelFeature(Context context, boolean slim, CallbackInfo ci) {
      ((LivingEntityRendererAccessor)(Object)this).callAddFeature(new CustomModelFeatureRenderer((PlayerEntityRenderer)(Object)this));
   }

   @Inject(
      method = {"renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(
      PlayerEntityRenderState playerEntityRenderState,
      Text text,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int i,
      CallbackInfo ci
   ) {
      NameTags nameTags = NameTags.getInstance();
      if (nameTags != null && nameTags.state) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("RETURN")}
   )
   private void customCape$updateRenderState(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
      CustomCape customCape = CustomCape.getInstance();
      if (customCape != null && customCape.shouldApply(player)) {
         state.skinTextures = CustomCape.withCustomCape(player.getSkinTextures());
         state.capeVisible = true;
      }
   }
}
