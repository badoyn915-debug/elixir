package code.elixir.mixins;

import code.elixir.events.render.EntityColorEvent;
import code.elixir.features.impl.render.Chams;
import code.elixir.features.impl.render.CustomModelModule;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin implements QuickImports {
   @Shadow
   protected EntityModel<?> model;

   @Shadow
   @Nullable
   protected abstract RenderLayer getRenderLayer(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
      )
   )
   private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      EntityColorEvent event;
      if (!translucent
         && state.width == 0.6F
         && SafeEvents.hasListeners(EntityColorEvent.class)
         && (event = SafeEvents.dispatch(EntityColorEvent.class, () -> new EntityColorEvent(-1))) != null
         && event.isCancelled()) {
         translucent = true;
      }

      return this.getRenderLayer(state, showBody, translucent, showOutline);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )}
   )
   private void Elixir$hidePlayerModel(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      if (state instanceof PlayerEntityRenderState playerState && this.model instanceof PlayerEntityModel playerModel) {
         if ((mc.world == null ? null : mc.world.getEntityById(playerState.id)) instanceof PlayerEntity player) {
            if (CustomModelModule.getModelType(player) != null) {
               this.hidePlayerModel(playerModel);
            }
         }
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderModelHook(
      EntityModel<?> instance,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int i,
      int j,
      int l,
      @Local(ordinal = 0,argsOnly = true) LivingEntityRenderState renderState
   ) {
      Entity entity;
      if (renderState instanceof PlayerEntityRenderState playerState
         && instance instanceof PlayerEntityModel playerModel
         && mc.world != null
         && (entity = mc.world.getEntityById(playerState.id)) instanceof PlayerEntity) {
         PlayerEntity player = (PlayerEntity)entity;
         Chams chams = Chams.getInstance();
         if (chams != null && chams.shouldRender(player) && CustomModelModule.getModelType(player) == null) {
            chams.renderPlayer(matrixStack, player, playerModel);
            return;
         }
      }

      int color = l;
      EntityColorEvent event;
      if (renderState.invisibleToPlayer
         && SafeEvents.hasListeners(EntityColorEvent.class)
         && (event = SafeEvents.dispatch(EntityColorEvent.class, () -> new EntityColorEvent(l))) != null) {
         color = event.getColor();
      }

      instance.render(matrixStack, vertexConsumer, i, j, color);
   }

   private void hidePlayerModel(PlayerEntityModel playerModel) {
      playerModel.head.visible = false;
      playerModel.hat.visible = false;
      playerModel.body.visible = false;
      playerModel.rightArm.visible = false;
      playerModel.leftArm.visible = false;
      playerModel.rightLeg.visible = false;
      playerModel.leftLeg.visible = false;
      playerModel.jacket.visible = false;
      playerModel.leftSleeve.visible = false;
      playerModel.rightSleeve.visible = false;
      playerModel.leftPants.visible = false;
      playerModel.rightPants.visible = false;
   }

   @ModifyExpressionValue(
      method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"
      )}
   )
   private float lerpAngleDegreesHook(float original, @Local(ordinal = 0,argsOnly = true) LivingEntity entity, @Local(ordinal = 0,argsOnly = true) float delta) {
      if (mc.player == null) {
         return original;
      } else {
         TurnsConnection controller = TurnsConnection.INSTANCE;
         return entity.equals(mc.player) && controller.getPreviousRotation().getYaw() != mc.player.getYaw() && !(mc.currentScreen instanceof HandledScreen)
            ? MathHelper.lerpAngleDegrees(delta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw())
            : original;
      }
   }

   @ModifyExpressionValue(
      method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"
      )}
   )
   private float getLerpedPitchHook(float original, @Local(ordinal = 0,argsOnly = true) LivingEntity entity, @Local(ordinal = 0,argsOnly = true) float delta) {
      if (mc.player == null) {
         return original;
      } else {
         TurnsConnection controller = TurnsConnection.INSTANCE;
         return entity.equals(mc.player) && controller.getPreviousRotation().getPitch() != mc.player.getPitch() && !(mc.currentScreen instanceof HandledScreen)
            ? MathHelper.lerp(delta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch())
            : original;
      }
   }
}
