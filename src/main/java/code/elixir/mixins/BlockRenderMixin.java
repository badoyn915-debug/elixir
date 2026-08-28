package code.elixir.mixins;

import code.elixir.features.impl.render.NoRender;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SeagrassBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockRenderManager.class})
public class BlockRenderMixin {
   @Inject(
      method = {"renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hideVegetation(
      BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci
   ) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideVegetation() && isVegetationBlock(state)) {
         ci.cancel();
      }
   }

   private static boolean isVegetationBlock(BlockState blockState) {
      if (blockState == null) {
         return false;
      } else {
         Block block = blockState.getBlock();
         return !(block instanceof PlantBlock) && !(block instanceof CropBlock) && !(block instanceof SeagrassBlock) && !(block instanceof TallPlantBlock)
            ? blockState.isIn(BlockTags.LEAVES)
               || blockState.isIn(BlockTags.FLOWERS)
               || blockState.isIn(BlockTags.SAPLINGS)
               || blockState.isIn(BlockTags.CROPS)
            : true;
      }
   }
}
