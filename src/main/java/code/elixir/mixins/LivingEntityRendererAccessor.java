package code.elixir.mixins;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntityRenderer.class})
public interface LivingEntityRendererAccessor<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Invoker("addFeature")
   boolean callAddFeature(FeatureRenderer<S, M> var1);

   @Accessor("model")
   M elixir$getModel();
}
