package code.elixir.mixins;

import code.elixir.Elixir;
import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.impl.render.NoRender;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.projection.Projection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin implements QuickImports {
   @ModifyExpressionValue(
      method = {"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;invisible:Z"
      )}
   )
   private boolean renderHitboxHook(boolean original, @Local(ordinal = 0,argsOnly = true) Entity entity) {
      if (entity instanceof ArmorStandEntity) {
         return true;
      } else {
         NoRender noRender;
         PlayerEntity player;
         return entity instanceof PlayerEntity
               && (player = (PlayerEntity)entity) != mc.player
               && (noRender = NoRender.getInstance()) != null
               && noRender.shouldHideFriends()
               && FriendUtils.isFriend(player)
            ? true
            : original;
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderFriendsHook(
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
      PlayerEntity player;
      if (entity instanceof PlayerEntity
         && (player = (PlayerEntity)entity) != mc.player
         && (noRender = NoRender.getInstance()) != null
         && noRender.shouldHideFriends()
         && FriendUtils.isFriend(player)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderHitbox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderHitboxHook(
      MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo ci
   ) {
      if (!Elixir.getInstance().getBoxESPRepository().entities.containsKey(entity.getType())) {
         renderBox(entity);
      }

      ci.cancel();
   }

   @Unique
   private static void renderBox(Entity entity) {
      if (entity != mc.player || !mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
         int color = FriendUtils.isFriend(entity) ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
         Vec3d offset = Calculate.interpolate(entity).subtract(entity.getPos());
         Box box = entity.getBoundingBox().offset(offset);
         if (Projection.canSee(box)) {
            if (entity instanceof LivingEntity living) {
               float width = entity.getWidth();
               Vec3d eyeMin = entity.getEyePos().add(offset).add(-width / 2.0F, 0.0, -width / 2.0F);
               Render3D.drawBox(box, ColorAssist.multRed(color, 1 + living.hurtTime), 2.0F, true, true, true);
               Render3D.drawLine(eyeMin, eyeMin.add(width, 0.0, 0.0), ColorAssist.RED, 2.0F, true);
               Render3D.drawLine(eyeMin.add(width, 0.0, 0.0), eyeMin.add(width, 0.0, width), ColorAssist.RED, 2.0F, true);
               Render3D.drawLine(eyeMin, eyeMin.add(0.0, 0.0, width), ColorAssist.RED, 2.0F, true);
               Render3D.drawLine(eyeMin.add(0.0, 0.0, width), eyeMin.add(width, 0.0, width), ColorAssist.RED, 2.0F, true);
            } else {
               Render3D.drawBox(box, color, 2.0F, true, true, true);
            }
         }
      }
   }
}
