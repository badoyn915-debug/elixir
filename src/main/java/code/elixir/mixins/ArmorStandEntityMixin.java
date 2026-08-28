package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class ArmorStandEntityMixin {
   @Inject(
      method = {"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRender(
      Entity entity,
      double x,
      double y,
      double z,
      float yaw,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      EntityRenderer<? extends Entity, ?> renderer,
      CallbackInfo ci
   ) {
      NoRender noRender;
      if (entity instanceof ArmorStandEntity && (noRender = NoRender.getInstance()) != null && noRender.shouldHideArmorStands()) {
         ci.cancel();
      }
   }
}
