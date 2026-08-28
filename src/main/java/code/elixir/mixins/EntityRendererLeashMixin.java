package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState.LeashData;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public class EntityRendererLeashMixin {
   @Inject(
      method = {"renderLeash"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hideFishingLeash(MatrixStack matrices, VertexConsumerProvider vertexConsumers, LeashData leashData, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.player.fishHook != null) {
         NoRender noRender = NoRender.getInstance();
         if (noRender != null && noRender.shouldHideFishingRod()) {
            ci.cancel();
         }
      }
   }
}
