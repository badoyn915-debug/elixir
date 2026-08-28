package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
   @Inject(
      method = {"renderFireOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderFireOverlayHook(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState()) {
         if (noRender.modeSetting.isSelected("Огонь")) {
            ci.cancel();
         } else {
            MinecraftClient client;
            if (noRender.shouldHideLavaOverlay()
               && (client = MinecraftClient.getInstance()) != null
               && client.player != null
               && client.player.getWorld().getFluidState(client.player.getBlockPos()).isIn(FluidTags.LAVA)) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"renderInWallOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderInWallOverlayHook(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Оверлей блоков")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderUnderwaterOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderUnderwaterOverlayHook(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && client != null && client.player != null) {
         FluidState fluidState = client.player.getWorld().getFluidState(client.player.getBlockPos().up());
         if (fluidState.isIn(FluidTags.WATER) && noRender.shouldHideWaterOverlay()) {
            ci.cancel();
         } else {
            if (fluidState.isIn(FluidTags.LAVA) && noRender.shouldHideLavaOverlay()) {
               ci.cancel();
            }
         }
      }
   }
}
