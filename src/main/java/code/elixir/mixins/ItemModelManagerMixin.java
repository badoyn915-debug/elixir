package code.elixir.mixins;

import code.elixir.features.impl.render.ItemReplacer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemModelManager.class})
public class ItemModelManagerMixin {
   @Inject(
      method = {"update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V"},
      at = {@At("RETURN")}
   )
   private void elixir$itemReplacerCapture(
      ItemRenderState state, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci
   ) {
      ItemReplacer.captureForRender(stack, mode, entity);
   }

   @Inject(
      method = {"update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V"},
      at = {@At("RETURN")}
   )
   private void elixir$itemReplacerCaptureWithGlint(
      ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean glint, World world, LivingEntity entity, int seed, CallbackInfo ci
   ) {
      ItemReplacer.captureForRender(stack, mode, entity);
   }
}
