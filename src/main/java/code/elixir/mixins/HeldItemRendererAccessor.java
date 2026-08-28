package code.elixir.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
   @Invoker("renderItem")
   void elixir$renderItem(
      LivingEntity var1, ItemStack var2, ModelTransformationMode var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7
   );

   @Invoker("renderArmHoldingItem")
   void invokeRenderArmHoldingItem(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, Arm var6);

   @Invoker("applyEquipOffset")
   void invokeApplyEquipOffset(MatrixStack var1, Arm var2, float var3);

   @Invoker("swingArm")
   void invokeSwingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5);

   @Invoker("renderMapInBothHands")
   void invokeRenderMapInBothHands(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, float var6);

   @Invoker("renderMapInOneHand")
   void invokeRenderMapInOneHand(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

   @Invoker("renderItem")
   void invokeRenderItem(LivingEntity var1, ItemStack var2, ModelTransformationMode var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7);
}
