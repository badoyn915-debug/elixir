package code.elixir.mixins;

import code.elixir.features.impl.render.SwingAnimations;
import code.elixir.features.impl.render.ViewModel;
import code.elixir.utils.client.Instance;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererHmiMixin {
   private boolean repPower = false;
   private float prevAge = 0.0F;
   private double previousRotation = 0.0;
   private float swingAngleY = 0.0F;
   private float swingAngleX = 0.0F;
   private float swingVelocityY = 0.0F;
   private float swingVelocityX = 0.0F;
   private float swingVelocityZ = 0.0F;
   private static final float GRAVITY = 0.1F;
   private static final float DAMPING = 0.88F;
   private static final float SENSITIVITY = 0.015F;
   private float vertAngleY = 0.0F;
   private float vertVelocityY = 0.0F;
   private float vertVelocityYSlime = 0.0F;
   private float vertAngleYSlime = 0.0F;
   private float riptideCounter = 0.0F;
   private float netherCounter = 0.0F;
   @Shadow
   private ItemStack mainHand;
   @Shadow
   @Final
   private MinecraftClient client;
   private float fallCounter = 0.0F;
   private float inWaterCounter = 0.0F;
   private float inspect = 0.0F;
   private float tilt = 0.0F;
   private float freezeCounter = 0.0F;
   private float clCount = 0.0F;
   private float crawlCount = 0.0F;
   private float directionalCrawlCount = 0.0F;
   private float climbCount = 0.0F;
   private float mouseHolding = 1.0F;
   private boolean isSwinging = false;
   private float swingProgress = 0.0F;
   private boolean isForward = false;
   private boolean isAttacking = false;
   private boolean left = false;
   private long alphaLastFrameNano = 0L;
   private double frameDelta = 0.0;
   @Shadow
   private float equipProgressMainHand;
   @Shadow
   private float prevEquipProgressMainHand;
   @Shadow
   private float prevEquipProgressOffHand;
   @Shadow
   private float equipProgressOffHand;
   @Shadow
   private ItemStack offHand;

   private float easeInOutBack(float x) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      return (float)(
         x < 0.5
            ? Math.pow(2.0F * x, 2.0) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0
            : (Math.pow(2.0F * x - 2.0F, 2.0) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0
      );
   }

   private float[] resolveHmiSwingValues(SwingAnimations config, float swingProgress) {
      float progress = config.remapHmiSwingProgress(swingProgress);
      if (progress <= 0.0F) {
         return new float[]{0.0F, 0.0F};
      } else {
         float targetSwingRot = progress < 0.6
            ? MathHelper.sin(MathHelper.clamp(progress, 0.0F, 0.12506F) * 12.56F)
            : MathHelper.sin(MathHelper.clamp(progress, 0.62532F, 0.75038F) * 12.56F);
         float targetSwing = this.easeInOutBack(MathHelper.sin(progress * (float) Math.PI));
         return new float[]{targetSwing, targetSwingRot};
      }
   }

   private float getAttackDamage(ItemStack stack) {
      AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0F;
      } else {
         float totalDamage = 0.0F;

         for (Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
               totalDamage += (float)entry.modifier().value();
            }
         }

         return totalDamage;
      }
   }

   private boolean shouldRenderItemStackModel(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (stack.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
         return true;
      } else if (stack.contains(DataComponentTypes.ITEM_MODEL)) {
         return true;
      } else if (stack.contains(DataComponentTypes.PROFILE)) {
         return true;
      } else if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
         return true;
      } else {
         return stack.contains(DataComponentTypes.DYED_COLOR) ? true : stack.contains(DataComponentTypes.POTION_CONTENTS);
      }
   }

   private boolean isSkullItem(ItemStack stack) {
      return stack.isOf(Items.PLAYER_HEAD)
         || stack.isOf(Items.ZOMBIE_HEAD)
         || stack.isOf(Items.SKELETON_SKULL)
         || stack.isOf(Items.WITHER_SKELETON_SKULL)
         || stack.isOf(Items.CREEPER_HEAD)
         || stack.isOf(Items.DRAGON_HEAD)
         || stack.isOf(Items.PIGLIN_HEAD);
   }

   private void applyTotemLikeHandPose(MatrixStack matrices, int side) {
      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * side));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * side));
      matrices.translate(0.0F, -0.05F, -0.1F);
      matrices.scale(0.7F, 0.7F, 0.7F);
   }

   private void renderSkullInHand(
      AbstractClientPlayerEntity player, ItemStack item, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int side, boolean rightHand
   ) {
      matrices.push();
      if (!rightHand) {
         matrices.translate(-0.4F, 0.0F, 0.0F);
      }

      matrices.scale(0.59F, 0.59F, 0.59F);
      matrices.translate(-0.5 * side, -0.25, 0.1);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F * side));
      HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
      if (this.shouldRenderItemStackModel(item)) {
         acc.invokeRenderItem(player, item, ModelTransformationMode.FIXED, !rightHand, matrices, vertexConsumers, light);
      } else if (item.getItem() instanceof BlockItem blockItem) {
         MinecraftClient.getInstance()
            .getBlockRenderManager()
            .renderBlockAsEntity(blockItem.getBlock().getDefaultState(), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
      }

      matrices.pop();
   }

   private boolean isSharpAnimation(SwingAnimations config) {
      return config != null && config.isSharpAnimation();
   }

   private void altSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin(swingProgress * 3.14F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * 0.0F)));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      SwingAnimations swings = SwingAnimations.getInstance();
      if (swings != null && swings.isState() && swings.isHmiMode()) {
         boolean isMainHand = hand == Hand.MAIN_HAND;
         Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         float sideFactor = isMainHand ? 1.0F : -1.0F;
         this.renderCustomFirstPersonItem(player, tickDelta, pitch, hand, arm, sideFactor, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         ci.cancel();
      }
   }

   private void renderCustomFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      Arm arm,
      float sideFactor,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light
   ) {
      SwingAnimations swings = SwingAnimations.getInstance();
      if (swings != null && swings.isState() && swings.isHmiMode() && !player.isUsingSpyglass()) {
         SwingAnimations config = SwingAnimations.getInstance();
         float yaw = player.getYaw();
         double radians = Math.toRadians(yaw);
         double forwardX = -Math.sin(radians);
         double forwardZ = Math.cos(radians);
         Vec3d horizontalVelocity = player.getVelocity();
         double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
         double crossProduct = player.getVelocity().getHorizontal().x * forwardZ - horizontalVelocity.z * forwardX;
         float al = player.getPitch() != 0.0F ? 90.0F / player.getPitch() / 10.0F : 1.0F;
         if (al > 1.0F) {
            al = 1.0F;
         }

         if (al < 0.0F) {
            al = 1.0F;
         }

         boolean bl = hand == Hand.MAIN_HAND;
         matrices.push();
         matrices.push();
         ViewModel viewModel = Instance.get(ViewModel.class);
         if (viewModel != null && viewModel.isState()) {
            viewModel.applyHandPosition(matrices, arm);
         }

         long alphaNow = System.nanoTime();
         this.frameDelta = this.alphaLastFrameNano == 0L ? 0.0 : Math.min(0.05, Math.max(0.0, (alphaNow - this.alphaLastFrameNano) / 1.0E9));
         this.alphaLastFrameNano = alphaNow;
         double tt = this.frameDelta * 30.0;
         float[] swingValues = this.resolveHmiSwingValues(config, swingProgress);
         float swing = swingValues[0];
         float swing_rot = swingValues[1];
         boolean sharpSword = item.isIn(ItemTags.SWORDS) && this.isSharpAnimation(config);
         if ((
               item.isOf(Items.EXPERIENCE_BOTTLE)
                  || item.isOf(Items.WIND_CHARGE)
                  || item.isOf(Items.EGG)
                  || item.isOf(Items.ENDER_EYE)
                  || item.isOf(Items.SNOWBALL)
                  || item.getItem() instanceof SplashPotionItem
                  || item.getItem() instanceof LingeringPotionItem
            )
            && player.getOffHandStack().isEmpty()
            && item.getUseAction() != UseAction.SPEAR
            && !item.isOf(Items.FIRE_CHARGE)
            && !player.isSwimming()
            && !player.isCrawling()
            && !player.isClimbing()) {
            if (player.getMainArm() == Arm.LEFT) {
               bl = !bl;
            }

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * sideFactor));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * sideFactor * swing));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
            matrices.translate(-0.15 * sideFactor, 0.1, 0.1);
            matrices.translate(0.0, -0.55 * swing, 0.4 * swing * 3.14F);
            HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
            acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.getOpposite());
            matrices.pop();
         }

         if (this.client.options.attackKey.isPressed() && !this.isAttacking && swingProgress == 0.0 && config.shouldAlternateClassicSwordSwing()) {
            boolean var60 = this.left = !this.left;
         }

         if (!item.isEmpty()) {
            if (player.getMainArm() == Arm.LEFT) {
               bl = !bl;
            }

            boolean useLeftSwing = this.left;
            if (item.isIn(ItemTags.SWORDS) && !sharpSword) {
               useLeftSwing = config.resolveClassicSwordLeft(this.left);
            }

            if ((useLeftSwing || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK)
               && !item.isIn(ItemTags.SHOVELS)) {
               if (sharpSword) {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else if (item.isIn(ItemTags.SWORDS) || item.isIn(ItemTags.AXES)) {
                  matrices.translate(0.8 * sideFactor * swing_rot, 0.3 * swing_rot, -0.5 * swing);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * sideFactor));
                  if (item.isIn(ItemTags.SWORDS)) {
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  } else {
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                  }
               } else if (item.getUseAction() == UseAction.SPEAR) {
                  matrices.translate(0.0, 0.0, 0.45 * swing_rot);
                  matrices.translate(-0.25 * sideFactor * swing, -0.35 * swing_rot, -0.6 * swing);
                  matrices.translate(0.0, 0.1 * swing, 0.0);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * sideFactor));
               } else if (item.isIn(ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(ItemTags.SHOVELS)) {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else if (item.getUseAction() != UseAction.BLOCK) {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
               } else {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
               }
            } else if (!item.isIn(ItemTags.SHOVELS)) {
               if (sharpSword) {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else if (item.isIn(ItemTags.SWORDS)) {
                  matrices.translate(-0.55 * sideFactor * swing_rot, -0.8 * swing_rot, -0.77 * swing);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
               } else if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.SHOVELS)) {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else {
                  matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
               }
            } else if (item.isIn(ItemTags.SHOVELS)) {
               matrices.translate(0.0, 0.15 * swing_rot, -0.25 * swing_rot);
               matrices.translate(0.0, 0.0, -0.2 * swing);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
            }
         } else if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR
            && (
               !item.isIn(ConventionalItemTags.TOOLS)
                  || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                  || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                  || item.getUseAction() == UseAction.EAT
                  || !item.isEnchantable()
            )
            && item.getUseAction() != UseAction.BOW
            && item.getUseAction() != UseAction.SPYGLASS
            && this.getAttackDamage(item) == 0.0F
            && item.getUseAction() != UseAction.BLOCK
            && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
            && !item.isOf(Items.CARROT_ON_A_STICK)
            && !item.isOf(Items.FISHING_ROD)
            && !item.isOf(Items.SHEARS)) {
            if ((swingProgress = (float)(swingProgress * 1.2)) > 1.0F) {
               swingProgress = 0.0F;
            }
         } else if (!item.isIn(ItemTags.SHOVELS) && (swingProgress = (float)(swingProgress * 1.5)) > 1.0F) {
            swingProgress = 0.0F;
         }

         if (player.getVelocity().length() >= 0.08) {
            this.crawlCount = (float)(this.crawlCount + 0.1 * player.getVelocity().length() * 2.0 * tt);
            this.directionalCrawlCount = (float)(this.directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
            this.directionalCrawlCount = (float)(
               this.directionalCrawlCount + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt)
            );
         }

         if (player.getVelocity().getY() > 0.0) {
            this.climbCount = (float)(this.climbCount + 0.1 * tt);
         }

         if (player.getVelocity().getY() < 0.0) {
            this.climbCount = (float)(this.climbCount - 0.1 * tt);
         }

         if ((
               player.isCrawling() && config.isClimbAndCrawl()
                  || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().getY()) > 0.0 && config.isClimbAndCrawl()
            )
            && !player.isUsingItem()
            && swingProgress == 0.0F) {
            this.clCount = (float)(this.clCount + 0.1 * tt);
            if (this.clCount > 1.0F) {
               this.clCount = 1.0F;
            }

            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * this.clCount));
            }
         } else {
            this.clCount = (float)(this.clCount * Math.pow(0.88F, tt));
         }

         if (swingProgress == 0.0F) {
            matrices.translate(bl ? player.getPitch() / 650.0F * this.clCount * -1.0F : player.getPitch() / 650.0F * this.clCount, 0.0F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * this.clCount));
         }

         if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
            matrices.translate(0.0F, 0.0F, player.getPitch() / 120.0F * this.clCount);
         } else if (swingProgress == 0.0F) {
            matrices.translate(0.0F, 0.0F, player.getPitch() / 80.0F * this.clCount);
         }

         if (player.isClimbing()
            && config.isClimbAndCrawl()
            && !player.isOnGround()
            && !item.isOf(Items.LANTERN)
            && !item.isOf(Items.SOUL_LANTERN)
            && !player.isUsingItem()) {
            matrices.translate(0.0, 0.1, -0.2);
         }

         if ((player.isInFluid() || player.inPowderSnow) && !player.isSwimming() && !player.isSubmergedInWater()) {
            this.inWaterCounter = (float)(this.inWaterCounter + 0.1 * tt);
            if (this.inWaterCounter >= 1.0F) {
               this.inWaterCounter = 1.0F;
            }
         } else {
            this.inWaterCounter = (float)(this.inWaterCounter * Math.pow(0.88F, tt));
         }

         this.freezeCounter = player.inPowderSnow && player.getFreezingScale() > 0.1
            ? (float)(this.freezeCounter + 0.1 * tt)
            : (float)(this.freezeCounter * Math.pow(0.88F, tt));
         matrices.translate(0.0, 0.02 * this.inWaterCounter, 0.0);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * sideFactor * this.inWaterCounter));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(this.freezeCounter * 5.0F)));
         if (player.getVelocity().getY() < -0.85 && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
            this.fallCounter = (float)(this.fallCounter + 0.1 * tt);
            if (this.fallCounter >= 1.0F) {
               this.fallCounter = 1.0F;
            }
         } else {
            this.fallCounter = (float)(this.fallCounter * Math.pow(0.88F, tt));
         }

         if (bl) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * this.fallCounter));
            matrices.translate(0.0, -0.2 * this.fallCounter, 0.0);
         }

         this.vertAngleY = (float)(this.vertAngleY + player.getVelocity().getY() * 0.015F * tt);
         this.vertAngleY = (float)(this.vertAngleY - 0.1F * this.vertAngleY * tt);
         this.vertAngleY = (float)(this.vertAngleY * Math.pow(0.88F, tt));
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + player.getVelocity().getY() * 0.015F * tt);
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime - 0.1F * this.vertAngleYSlime * tt);
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime * Math.pow(0.88F, tt));
         this.vertAngleYSlime = (float)(this.vertAngleYSlime + this.vertVelocityYSlime * tt);
         matrices.translate(0.0F, this.vertAngleY * -1.0F, 0.0F);
         matrices.translate(0.0, Math.sin(player.age * 0.1) * 0.007 * sideFactor, 0.0);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin(player.age * 0.15F) * sideFactor));
         if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
            if (player.getMainArm() == Arm.LEFT) {
               bl = !bl;
            }

            if (item.getUseAction() == UseAction.BLOCK) {
               matrices.translate(0.0F, 0.0F, 0.0F);
            } else {
               matrices.translate(0.0, -0.1, 0.1);
            }
         }

         if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
            matrices.translate(0.0, 0.1, 0.0);
            if (player.isSwimming()) {
               matrices.translate(0.0, -0.1, 0.1);
            }
         }

         if (player.isSwimming() && swingProgress == 0.0F && config.isSwimmingAnimation()) {
            double distance = this.crawlCount;
            double swingAmplitude = 1.5;
            double frequency = 2.0;
            double s = distance * frequency;
            double handRotation = Math.sin(s) * swingAmplitude;
            double smoothRotation = handRotation * 0.8 + this.previousRotation * 0.2;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
            matrices.translate(0.0, 0.0, smoothRotation * 0.2F);
            double k = this.crawlCount * 2.0F;
            double a;
            double b = a = Math.cos(k);
            if (a <= 0.0) {
               b = a * 0.5;
            }

            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl ? b * 30.0 : b * 30.0 * -1.0)));
            matrices.translate(0.0, 0.0, a * 0.2F);
            if (item.isEmpty() && !bl && !player.isInvisible()) {
               matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               float c = MathHelper.sin(equipProgress * 3.14F);
               matrices.scale(0.9F, 0.9F, 0.9F);
               HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
               acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }

            this.previousRotation = smoothRotation;
         }

         if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
            double sx = this.climbCount;
            float v = (float)player.getVelocity().getY();
            float ax = MathHelper.cos((float)sx * 2.0F);
            if (player.isClimbing()) {
               if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * ax * sideFactor));
               } else {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * ax * sideFactor));
               }
            }

            if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
               float crawlProgress = MathHelper.sin(this.directionalCrawlCount * 4.0F * this.mouseHolding);
               float upAndDown = MathHelper.cos(this.directionalCrawlCount * 4.0F * this.mouseHolding);
               if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
                  crawlProgress *= 0.14F;
                  upAndDown *= 0.14F;
               }

               matrices.translate(0.2 * crawlProgress, 0.3 * crawlProgress * sideFactor, -0.2 * crawlProgress * sideFactor * al);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
            }

            if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
               matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               matrices.scale(0.9F, 0.9F, 0.9F);
               HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
               acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }
         }

         if (item.isEmpty()) {
            if (!player.isInvisible()) {
               if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                  if (player.getMainArm() == Arm.LEFT) {
                     bl = !bl;
                  }

                  matrices.translate(0.0, 0.2 * swing_rot, 0.15 * swing_rot);
                  matrices.translate(0.1 * sideFactor * swing, 0.15 * swing, -0.45 * swing);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                  HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
                  acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               } else {
                  matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                  this.altSwing(matrices, arm, swingProgress, item);
                  float c = MathHelper.sin(equipProgress * 3.14F);
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
                  acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               }
            }
         } else if (item.contains(DataComponentTypes.MAP_ID)) {
            if (bl && this.mainHand.isEmpty()) {
               matrices.translate(0.0, 0.1, 0.0);
               HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
               acc.invokeRenderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
            } else {
               matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0);
               HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
               acc.invokeRenderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
            }
         } else if (item.getUseAction() == UseAction.CROSSBOW) {
            matrices.push();
            boolean bl22 = CrossbowItem.isCharged(item);
            boolean bl3 = arm == Arm.RIGHT;
            int i = bl3 ? 1 : -1;
            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
               HeldItemRendererAccessor acc = (HeldItemRendererAccessor)(Object)this;
               acc.invokeApplyEquipOffset(matrices, arm, equipProgress);
               matrices.translate(i * -0.4785682F, -0.24387F, 0.05731531F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 9.785F));
               float f = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
               float g = f / CrossbowItem.getPullTime(item, player);
               if (g > 1.0F) {
                  g = 1.0F;
               }

               if (g > 0.1F) {
                  float h = MathHelper.sin((f - 0.1F) * 1.3F);
                  float j = g - 0.1F;
                  float kx = h * j;
                  matrices.translate(kx * 0.0F, kx * 0.004F, kx * 0.0F);
               }

               matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
               matrices.scale(1.0F, 1.0F, 1.0F);
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
            } else {
               ((HeldItemRendererAccessor)this).invokeSwingArm(swingProgress, equipProgress, matrices, i, arm);
               if (bl22 && swingProgress < 0.001F && bl) {
                  matrices.translate(i * -0.341864F, 0.0F, 0.0F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
               }
            }

            matrices.translate(0.0F, 0.0F, -1.0F);
            matrices.translate(-0.45 * i, 0.45, 1.7);
            matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            this.altSwing(matrices, arm, swingProgress, item);
            float c = MathHelper.sin(equipProgress * 3.14F);
            matrices.scale(0.9F, 0.9F, 0.9F);
            HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
            accx.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            matrices.translate(-0.25 * i, 1.25, 0.05);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(85 * i));
            matrices.scale(1.2F, 1.2F, 1.2F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
            matrices.translate(0.0, -0.15, 0.15);
            accx.invokeRenderItem(
               player,
               item,
               bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
               !bl3,
               matrices,
               vertexConsumers,
               light
            );
            matrices.pop();
            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
               float fx = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
               float gx = fx / CrossbowItem.getPullTime(item, player);
               if (gx > 1.0F) {
                  gx = 1.0F;
               }

               if (gx > 0.1F) {
                  float h = MathHelper.sin((fx - 0.1F) * 1.3F);
                  float j = gx - 0.1F;
                  float kx = h * j;
                  matrices.translate(kx * 0.0F, kx * 0.004F, kx * 0.0F);
               }

               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(gx <= 0.2 ? 75.0F * gx * 5.0F * i : 75 * i));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * gx * 1.5F));
               matrices.translate(-0.37 * i, 0.0, 0.6);
               matrices.translate(0.15 * gx * i, 0.0, 0.0);
               accx.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
            }
         } else {
            boolean bl23 = arm == Arm.RIGHT;
            int l = bl23 ? 1 : -1;
            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
               switch (item.getUseAction()) {
                  case NONE:
                     HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
                     accx.invokeApplyEquipOffset(matrices, arm, equipProgress);
                     break;
                  case EAT:
                  case DRINK:
                     float u = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float y = u / 5.0F;
                     if (y > 1.0F) {
                        y = 1.0F;
                     }

                     float q = MathHelper.sin(u / 2.0F * 3.14F);
                     matrices.translate(1 * l, 0.1, 0.3);
                     matrices.translate(0.2 * l * y, -0.7 * y, -0.2 * y);
                     float var108;
                     matrices.translate(0.0, -0.2 * (var108 = q / 10.0F), -0.2 * var108);
                     matrices.translate(0.0, 0.1 * this.easeInOutBack(MathHelper.sin(y * 3.14F)), 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     float c = MathHelper.sin(equipProgress * 3.14F);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * y * l));
                     HeldItemRendererAccessor acc4 = (HeldItemRendererAccessor)(Object)this;
                     acc4.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                     break;
                  case BLOCK:
                     float kx = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float sxx = kx / 4.0F;
                     float s2 = kx / 6.0F;
                     if (sxx > 1.0F) {
                        sxx = 1.0F;
                     }

                     if (s2 > 1.0F) {
                        s2 = 1.0F;
                     }

                     matrices.translate(0.0, -0.2, 0.0);
                     matrices.translate(1 * l, 0.0, 0.3);
                     matrices.translate(0.7 * sxx * l, 0.0, -1.3 * sxx);
                     matrices.translate(-0.2 * l * s2, 0.0, 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0 * Math.sin(s2 * 3.14))));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * sxx * l));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5 * l * sxx));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * sxx));
                     matrices.translate(0.0, 0.0, -0.2 * sxx);
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     HeldItemRendererAccessor acc5 = (HeldItemRendererAccessor)(Object)this;
                     acc5.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                     matrices.translate(0.35 * l, -0.13, -0.12);
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * l));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                     matrices.translate(-0.2 * l, -0.04, 0.15);
                     matrices.scale(1.0F, 1.0F, 1.0F);
                     break;
                  case BOW:
                     matrices.push();
                     if (player.getMainArm() == Arm.LEFT) {
                        bl = !bl;
                     }

                     float m1 = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float f1 = m1 / 20.0F;
                     float fxxx = (f1 * f1 + f1 * 2.0F) / 3.0F;
                     if (f1 > 1.0F) {
                        f1 = 1.0F;
                     }

                     if (f1 > 0.1F) {
                        float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                        float j1 = g1 * f1;
                        matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                     }

                     matrices.translate(bl ? -0.1 : 0.1, 0.0, f1 * 0.15);
                     HeldItemRendererAccessor acc1 = (HeldItemRendererAccessor)(Object)this;
                     acc1.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     matrices.pop();
                     matrices.translate(bl ? -0.5 : 0.5, -0.45, 0.1);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                     if (bl) {
                        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                     } else {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                     }

                     acc1.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                     if (bl) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                     } else {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                     }

                     matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                     if (f1 > 1.0F) {
                        f1 = 1.0F;
                     }

                     matrices.pop();
                     if (config.isMb3DCompat()) {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10 * l));
                     }

                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                     matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-15 * l));
                     matrices.translate(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);
                     if (fxxx > 0.1F) {
                        float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                        float h1 = f1 - 0.1F;
                        float j1 = g1 * h1;
                        matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                     }

                     matrices.push();
                     break;
                  case SPEAR:
                     float m = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float fxx = m / 10.0F;
                     if (fxx > 1.0F) {
                        fxx = 1.0F;
                     }

                     if (fxx > 0.1F) {
                        float gxx = MathHelper.sin((m - 0.1F) * 1.3F);
                        float h = fxx - 0.1F;
                        float j = gxx * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
                     matrices.translate(0.2 * l, 0.0, 0.8);
                     HeldItemRendererAccessor acc0 = (HeldItemRendererAccessor)(Object)this;
                     acc0.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
                     matrices.translate(0.65F * l, -1.0, -0.6);
                     break;
                  case BRUSH:
                     float f5 = player.getItemUseTimeLeft() % 10;
                     float g5 = f5 - tickDelta + 1.0F;
                     float h5 = 1.0F - g5 / 10.0F;
                     float n2 = -15.0F + 75.0F * MathHelper.cos(h5 * 2.0F * (float) Math.PI);
                     float z = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float x = z / 4.0F;
                     if (x > 1.0F) {
                        x = 1.0F;
                     }

                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l * x));
                     matrices.translate(0.3F * l * x, 0.3 * x, 0.1 * x);
                     if (x == 1.0F) {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n2 / 20.0F));
                     }

                     HeldItemRendererAccessor acc78 = (HeldItemRendererAccessor)(Object)this;
                     acc78.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     break;
                  case BUNDLE:
                     matrices.translate(1 * l, 0.0 - equipProgress * 0.3, 0.3);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     HeldItemRendererAccessor acc67 = (HeldItemRendererAccessor)(Object)this;
                     acc67.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               }
            } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
               this.riptideCounter = (float)(this.riptideCounter + 0.15 * tt);
               float mx = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
               float fxxxx = mx / 10.0F;
               if (fxxxx > 1.0F) {
                  fxxxx = 1.0F;
               }

               if (fxxxx > 0.1F) {
                  float gxx = MathHelper.sin((mx - 0.1F) * 1.3F);
                  float h = fxxxx - 0.1F;
                  float j = gxx * h;
                  matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
               }

               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - this.riptideCounter * 2.0F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
               matrices.translate(0.2 * l, 0.0, 0.75);
               matrices.translate(0.0, 0.0, 0.01 * MathHelper.sin(this.riptideCounter * 6.28F));
               HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
               accx.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
               matrices.translate(0.65F * l, -1.0, -0.6);
            } else {
               this.riptideCounter = 0.0F;
               if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                  if (item.getUseAction() == UseAction.BLOCK) {
                     matrices.translate(0.0, -0.2, 0.0);
                  }
               } else {
                  matrices.translate(0.1 * l, 0.0, -0.1);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
               }

               matrices.translate(1 * l, 0.0 - equipProgress * 0.3, 0.3);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               matrices.scale(0.9F, 0.9F, 0.9F);
               HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
               accx.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }

            matrices.translate(-0.3 * l, 0.65, -0.1);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
            if (item.isIn(ItemTags.WOOL_CARPETS)) {
               matrices.translate(0.2 * l, -0.1, 0.0);
            }

            if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isIn(ConventionalItemTags.BUCKETS)) {
               if (item.getName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                  matrices.scale(1.5F, 1.5F, 1.5F);
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                  matrices.translate(0.2 * l, 0.2, 0.05);
               } else if ((
                     item.isOf(Items.STRING)
                        || item.isOf(Items.REDSTONE)
                        || item.isOf(Items.LEVER)
                        || item.isOf(Items.TRIPWIRE_HOOK)
                        || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                        || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                        || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                        || item.isIn(ItemTags.DOORS)
                  )
                  && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                  && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                  && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                  matrices.translate(0.0, 0.0, -0.1);
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
               } else if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                  matrices.translate(0.2 * l, 0.2, 0.05);
                  if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                     matrices.translate(-0.2 * l, 0.0, 0.0);
                     matrices.scale(1.1F, 1.1F, 1.1F);
                  }
               } else {
                  float dt = (float)(this.frameDelta * 30.0);
                  float yawDelta = player.prevHeadYaw - player.getHeadYaw();
                  float pitchDelta = player.prevPitch - player.getPitch();
                  this.swingVelocityY += yawDelta * 0.015F * dt;
                  this.swingVelocityY += swingProgress * 2.0F * dt;
                  this.swingVelocityX += pitchDelta * 0.015F * dt;
                  this.swingVelocityY = this.swingVelocityY - 0.1F * this.swingAngleY * dt;
                  this.swingVelocityX = this.swingVelocityX - 0.1F * this.swingAngleX * dt;
                  this.swingVelocityY = (float)(this.swingVelocityY * Math.pow(0.88F, dt));
                  this.swingVelocityX = (float)(this.swingVelocityX * Math.pow(0.88F, dt));
                  this.swingAngleY = this.swingAngleY + this.swingVelocityY * dt;
                  this.swingAngleX = this.swingAngleX + this.swingVelocityX * dt;
                  double currentSpeed = player.getVelocity().length();
                  this.swingVelocityZ = (float)(
                     this.swingVelocityZ
                        + (bl ? (currentSpeed * -1.0 * 15.0 - this.swingVelocityZ) * 0.1F * dt : (currentSpeed * 15.0 - this.swingVelocityZ) * 0.1F * dt)
                  );
                  if ((currentSpeed > 0.09 && player.isOnGround() || player.isClimbing() && !player.isOnGround())
                     && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                     boolean randomBoolean = ThreadLocalRandom.current().nextBoolean();
                     this.swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * dt : 5.5 * currentSpeed * dt);
                  }

                  matrices.translate(0.0, 0.0, -0.1);
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35 * l + this.swingAngleY));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.swingAngleX));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l + this.swingVelocityZ));
                  if (item.isIn(ItemTags.HANGING_SIGNS)) {
                     matrices.translate(0.0, -0.1, 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * l));
                  }

                  matrices.translate(0.3 * l, -0.35, 0.0);
                  matrices.translate(0.0, 0.0, 0.1);
                  matrices.scale(1.5F, 1.5F, 1.5F);
               }
            } else if (!this.isSkullItem(item)) {
               if ((
                     !item.isIn(ConventionalItemTags.TOOLS)
                        || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                        || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                        || item.getUseAction() == UseAction.EAT
                        || !item.isEnchantable()
                  )
                  && item.getUseAction() != UseAction.BOW
                  && item.getUseAction() != UseAction.SPYGLASS
                  && this.getAttackDamage(item) == 0.0F
                  && item.getUseAction() != UseAction.BLOCK
                  && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                  && !item.isOf(Items.CARROT_ON_A_STICK)
                  && !item.isOf(Items.FISHING_ROD)
                  && !item.isOf(Items.SHEARS)
                  && !item.isIn(ItemTags.HOES)
                  && !config.isMb3DCompat()) {
                  if (item.getUseAction() == UseAction.BRUSH) {
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                     matrices.translate(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                     if (!bl) {
                        matrices.scale(0.75F, 0.75F, 0.75F);
                     }

                     matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-75 * l));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                     matrices.translate(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                  } else {
                     this.applyTotemLikeHandPose(matrices, l);
                  }

                  if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                     this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * this.frameDelta * 30.0);
                     if ((
                           player.getVelocity().length() > 0.09 && player.isOnGround()
                              || player.isSwimming()
                              || player.isCrawling()
                              || player.isClimbing() && !player.isOnGround()
                        )
                        && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                        boolean randomBoolean = ThreadLocalRandom.current().nextBoolean();
                        this.vertVelocityYSlime = this.vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * this.frameDelta * 30.0);
                     }

                     matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                  }
               } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160 * l));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                  matrices.scale(0.75F, 0.75F, 0.75F);
                  matrices.translate(0.15 * l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                  matrices.translate(0.17 * l, 0.0, 0.3);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * l));
               } else if (item.getUseAction() == UseAction.SPEAR) {
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                  matrices.translate(-0.3F * l, 0.0F, 0.0F);
               } else if (item.getUseAction() != UseAction.SPEAR) {
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
               }

               if (item.getUseAction() != UseAction.BLOCK) {
                  matrices.scale(1.2F, 1.2F, 1.2F);
               }

               if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
                  matrices.translate(-0.1 * l, -0.2, 0.0);
               }

               if (item.isOf(Items.MACE)) {
                  if (config.isMb3DCompat()) {
                     matrices.translate(-0.08, 0.17, 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F));
                  }

                  matrices.translate(0.1 * l, 0.0, 0.0);
                  matrices.scale(0.9F, 0.9F, 0.9F);
               }
            }

            if (this.isSkullItem(item)) {
               this.renderSkullInHand(player, item, matrices, vertexConsumers, light, l, bl23);
            } else if (item.getItem() instanceof BlockItem
               && (
                  !item.isIn(ConventionalItemTags.BUCKETS)
                        && item.getUseAction() != UseAction.EAT
                        && !item.isIn(ItemTags.BANNERS)
                        && !item.isOf(Items.STRING)
                        && !item.isOf(Items.REDSTONE)
                        && !item.isOf(Items.LEVER)
                        && !item.isOf(Items.TRIPWIRE_HOOK)
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                        && !item.isIn(ItemTags.DOORS)
                     || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
               )
               && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
               BlockItem blockItem = (BlockItem)item.getItem();
               BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
               blockRenderManager.getModel(blockItem.getBlock().getDefaultState());
               matrices.push();
               if (!bl23) {
                  matrices.translate(-0.4F, 0.0F, 0.0F);
               }

               matrices.scale(0.59F, 0.59F, 0.59F);
               matrices.translate(-0.5 * l, -0.25, 0.1);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F * l));
               if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) {
                  matrices.translate(0.2 * l, -0.15, -0.2);
               }

               if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) {
                  matrices.translate(0.0, 0.1, 0.0);
               }

               if (item.isOf(Items.SLIME_BLOCK)
                  || item.isOf(Items.HONEY_BLOCK)
                  || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS)
                  || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                  || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS)
                  || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
                  this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * this.frameDelta * 30.0);
                  if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isClimbing() && !player.isOnGround())
                     && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                     boolean randomBoolean = ThreadLocalRandom.current().nextBoolean();
                     this.vertVelocityYSlime = this.vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * this.frameDelta * 30.0);
                  }

                  matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
               }

               BlockState blockState = blockItem.getBlock().getDefaultState();
               if (player.age - this.prevAge >= 100.0F) {
                  this.repPower = !this.repPower;
                  this.prevAge = player.age;
               }

               if (blockItem.getBlock() == Blocks.REPEATER && this.repPower) {
                  blockState = (BlockState)blockState.with(RepeaterBlock.POWERED, true);
               }

               if (blockItem.getBlock() == Blocks.COMPARATOR && this.repPower) {
                  blockState = (BlockState)blockState.with(ComparatorBlock.POWERED, true);
               }

               if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
                  blockState = (BlockState)blockState.with(RedstoneTorchBlock.LIT, false);
               }

               if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
                  blockState = (BlockState)blockState.with(CampfireBlock.LIT, false);
               }

               if (item.isIn(ItemTags.BEDS)) {
                  if (bl) {
                     matrices.translate(0.9, 0.0, 0.8);
                  }

                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
               }

               if (this.shouldRenderItemStackModel(item)) {
                  HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
                  accx.invokeRenderItem(player, item, ModelTransformationMode.FIXED, !bl23, matrices, vertexConsumers, light);
               } else {
                  blockRenderManager.renderBlockAsEntity(blockState, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
               }

               matrices.pop();
            } else {
               if (!this.isSkullItem(item)
                  && (
                     item.isIn(ConventionalItemTags.TOOLS)
                           && !item.isIn(ItemTags.TRIMMABLE_ARMOR)
                           && !item.isIn(ItemTags.BOOKSHELF_BOOKS)
                           && item.getUseAction() != UseAction.EAT
                           && item.isEnchantable()
                        || item.getUseAction() == UseAction.BOW
                        || item.getUseAction() == UseAction.SPYGLASS
                        || this.getAttackDamage(item) != 0.0F
                        || item.getUseAction() == UseAction.BLOCK
                        || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                        || item.isOf(Items.CARROT_ON_A_STICK)
                        || item.isOf(Items.FISHING_ROD)
                        || item.isOf(Items.SHEARS)
                  )) {
                  if (item.isIn(ItemTags.SWORDS) && !sharpSword) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                     matrices.translate(0.0, 0.1 * swing, -0.1 * swing);
                  }

                  if (item.isIn(ItemTags.SHOVELS)) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                  } else if (item.getUseAction() == UseAction.SPEAR) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                     matrices.translate(0.0, 0.1 * swing_rot, -0.1 * swing_rot);
                  } else if (item.getUseAction() != UseAction.BLOCK) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                     matrices.translate(0.0, 0.05 * swing, -0.05 * swing);
                  }
               }

               if (item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL) && config.isMb3DCompat()) {
                  this.netherCounter = (float)(this.netherCounter + 0.9 * tt);
                  matrices.translate(0.0, 0.25 + 0.02 * MathHelper.sin(this.netherCounter * 0.1F), 0.0);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(this.netherCounter * 0.2F)));
                  matrices.scale(
                     1.0F + 0.01F * MathHelper.sin(this.netherCounter),
                     1.0F + 0.01F * MathHelper.sin(this.netherCounter),
                     1.0F + 0.01F * MathHelper.sin(this.netherCounter)
                  );
               } else {
                  this.netherCounter = 0.0F;
               }

               if (config.isMb3DCompat()) {
                  if (item.isIn(ItemTags.SWORDS)) {
                     matrices.translate(0.0, 0.2, 0.0);
                  }

                  if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                     this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * this.frameDelta * 30.0);
                     if ((
                           player.getVelocity().length() > 0.09 && player.isOnGround()
                              || player.isSwimming()
                              || player.isCrawling()
                              || player.isClimbing() && !player.isOnGround()
                        )
                        && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                        boolean randomBoolean = ThreadLocalRandom.current().nextBoolean();
                        this.vertVelocityYSlime = this.vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * this.frameDelta * 30.0);
                     }

                     matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                  }
               }

               if (item.isIn(ItemTags.SHOVELS)) {
                  matrices.translate(0.07 * l, 0.0, 0.05);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
               }

               if (item.isOf(Items.TORCH)) {
                  player.getWorld()
                     .addParticle(ParticleTypes.ITEM_SLIME, player.getPos().getX(), player.getPos().getY(), player.getPos().getZ(), 0.1, 0.1, 0.1);
               }

               HeldItemRendererAccessor accx = (HeldItemRendererAccessor)(Object)this;
               accx.invokeRenderItem(
                  player,
                  item,
                  bl23 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                  !bl23,
                  matrices,
                  vertexConsumers,
                  light
               );
            }
         }

         matrices.pop();
         matrices.pop();
         this.isAttacking = this.client.options.attackKey.isPressed();
      }
   }

   @Shadow
   protected abstract void renderFirstPersonItem(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      VertexConsumerProvider var9,
      int var10
   );

   @Shadow
   protected abstract void swingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5);
}
