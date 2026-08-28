package code.elixir.utils.display.render;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class UseIndicatorRenderer implements QuickImports {
   private static final double[] SCREEN = new double[3];
   private static final float BACKGROUND_RADIUS = 12.0F;
   private static final float ITEM_SCALE = 0.7F;
   private static final int BACKGROUND_COLOR = -871362544;

   private UseIndicatorRenderer() {
   }

   public static void render(DrawContext context, LivingEntity entity, ItemStack stack, Camera camera) {
      if (context != null && stack != null && !stack.isEmpty()) {
         Vec3d position = Calculate.interpolate(entity).add(0.0, getBodyOffset(entity), 0.0);
         if (Projection.isInFrontOfCamera(position = offsetTowardCamera(entity, position, camera))) {
            if (Projection.worldSpaceToScreenSpaceInto(position.x, position.y, position.z, SCREEN)) {
               float screenX = (float)SCREEN[0];
               float screenY = (float)SCREEN[1];
               float entityScale = entity.isBaby() ? 0.75F : 1.0F;
               float radius = 12.0F * entityScale;
               float itemScale = 0.7F * entityScale;
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               MatrixStack matrices = context.getMatrices();
               matrices.push();
               matrices.translate(screenX, screenY, 0.0F);
               float diameter = radius * 2.0F;
               rectangle.render(ShapeProperties.create(matrices, -radius, -radius, diameter, diameter).round(radius).color(-871362544).build());
               float itemSize = 16.0F * itemScale;
               matrices.push();
               matrices.translate(-itemSize / 2.0F, -itemSize / 2.0F, 0.0F);
               matrices.scale(itemScale, itemScale, 1.0F);
               context.drawItem(stack, 0, 0);
               matrices.pop();
               matrices.pop();
               RenderSystem.depthMask(true);
               RenderSystem.enableDepthTest();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private static Vec3d offsetTowardCamera(LivingEntity entity, Vec3d position, Camera camera) {
      if (mc.player == entity && mc.options.getPerspective().isFirstPerson()) {
         return position;
      } else {
         Vec3d toCamera = camera.getPos().subtract(position);
         double lengthSq = toCamera.lengthSquared();
         return lengthSq < 1.0E-6 ? position : position.add(toCamera.normalize().multiply(0.55));
      }
   }

   private static float getBodyOffset(LivingEntity entity) {
      float offset = entity.getHeight() * 0.55F;
      if (entity.isInSneakingPose()) {
         offset -= 0.18F;
      }

      if (entity.isSwimming()) {
         offset = entity.getHeight() * 0.35F;
      }

      PlayerEntity player;
      if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity).isGliding()) {
         offset = entity.getHeight() * 0.42F;
      }

      return offset;
   }
}
