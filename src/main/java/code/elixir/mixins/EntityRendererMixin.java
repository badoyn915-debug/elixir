package code.elixir.mixins;

import code.elixir.features.impl.render.Esp;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<S extends EntityRenderState> {
   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      Esp esp = Esp.getInstance();
      if (esp != null && esp.isState() && this.canRemove((int)(state.width * 100.0F), esp)) {
         ci.cancel();
      }
   }

   @Unique
   private boolean canRemove(int width, Esp esp) {
      return switch (width) {
         case 60 -> Esp.entityType.isSelected("Player");
         case 98 -> Esp.entityType.isSelected("TNT");
         default -> false;
      };
   }
}
