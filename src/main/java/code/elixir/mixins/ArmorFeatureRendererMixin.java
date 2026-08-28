package code.elixir.mixins;

import code.elixir.features.impl.render.CustomModelModule;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ArmorFeatureRenderer.class})
public abstract class ArmorFeatureRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>>
   implements QuickImports {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void Elixir$hideArmorForCustomModels(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance, CallbackInfo ci
   ) {
      if (state instanceof PlayerEntityRenderState playerState && mc.world != null) {
         if (mc.world.getEntityById(playerState.id) instanceof PlayerEntity player) {
            if (CustomModelModule.getModelType(player) != null) {
               ci.cancel();
            }
         }
      }
   }
}
