package code.elixir.features.impl.render;

import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.projection.Projection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

final class EspSkeletonRenderer {
   private static final int MAX_BONES_PER_PLAYER = 15;
   private static final Vector3f TRANSFORMED = new Vector3f();
   private static final List<Vec3d[]> BONE_BUFFER = new ArrayList<>(64);

   private EspSkeletonRenderer() {
   }

   static void renderPlayers(MatrixStack matrices, List<PlayerEntity> players, float tickDelta, float lineWidth, int color) {
      if (!players.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         BONE_BUFFER.clear();

         for (PlayerEntity player : players) {
            if (player != null
               && !player.isInvisible()
               && (player != mc.player || !mc.options.getPerspective().isFirstPerson())
               && Projection.isWithinRenderDistance(player)
               && Projection.canSee(player.getBoundingBox())) {
               collectBones(player, tickDelta, BONE_BUFFER);
            }
         }

         if (!BONE_BUFFER.isEmpty()) {
            for (Vec3d[] bone : BONE_BUFFER) {
               Render3D.drawLine(bone[0], bone[1], color, lineWidth, false);
            }
         }
      }
   }

   private static void collectBones(PlayerEntity player, float tickDelta, List<Vec3d[]> out) {
      double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
      double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
      double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
      float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
      float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
      float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
      float swing = player.limbAnimator.getPos(tickDelta);
      float swingAmt = Math.min(player.limbAnimator.getSpeed(tickDelta), 1.0F) * 0.5F;
      float handSwing = player.getHandSwingProgress(tickDelta);
      float height = player.getHeight();
      boolean elytra = player.isGliding();
      boolean sneak = player.isSneaking();
      MatrixStack stack = new MatrixStack();
      stack.translate(x, y, z);
      if (sneak && !elytra) {
         stack.translate(0.0, 0.125, 0.0);
      }

      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
      float bodyPitch = 0.0F;
      if (elytra) {
         bodyPitch = 1.57F + pitch / 57.2958F;
      } else if (sneak) {
         bodyPitch = 0.5F;
      }

      if (elytra || sneak) {
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bodyPitch * 57.2958F));
      }

      if (sneak && !elytra) {
         stack.translate(0.0, -0.13, 0.0);
      }

      stack.push();
      stack.translate(0.0, height * 0.75, 0.0);
      Vec3d neck = getPos(stack);
      stack.push();
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw - headYaw));
      if (!elytra) {
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
      }

      stack.translate(0.0, height * 0.15, 0.0);
      Vec3d head = getPos(stack);
      stack.pop();
      stack.push();
      stack.translate(0.25, 0.0, 0.0);
      Vec3d lShoulder = getPos(stack);
      float lArmRot = elytra ? -0.2F : MathHelper.cos(swing * 0.6662F + (float) Math.PI) * 0.8F * swingAmt;
      if (elytra) {
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-5.0F));
      }

      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lArmRot * 57.2958F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d lElbow = getPos(stack);
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.max(0.0F, lArmRot * 15.0F)));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d lHand = getPos(stack);
      stack.pop();
      stack.push();
      stack.translate(-0.25, 0.0, 0.0);
      Vec3d rShoulder = getPos(stack);
      float rArmRot = elytra ? -0.2F : MathHelper.cos(swing * 0.6662F) * 0.8F * swingAmt;
      if (elytra) {
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(5.0F));
      }

      if (handSwing > 0.0F && !elytra) {
         float swingProgress = 1.0F - handSwing;
         swingProgress *= swingProgress;
         float swingRot = MathHelper.sin(swingProgress * (float) Math.PI);
         float headYawDiff = headYaw - bodyYaw;
         float yawFactor = MathHelper.clamp(headYawDiff / 75.0F, -1.0F, 1.0F);
         stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swingRot * 15.0F * yawFactor));
         rArmRot += -swingRot * 0.8F;
      }

      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rArmRot * 57.2958F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d rElbow = getPos(stack);
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.max(0.0F, rArmRot * 15.0F)));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d rHand = getPos(stack);
      stack.pop();
      stack.pop();
      stack.push();
      stack.translate(0.0, height * 0.5, 0.0);
      Vec3d waist = getPos(stack);
      stack.pop();
      stack.push();
      stack.translate(0.0, height * 0.3, 0.0);
      Vec3d pelvis = getPos(stack);
      stack.push();
      stack.translate(0.125, 0.0, 0.0);
      Vec3d lHip = getPos(stack);
      float lLegRot = elytra ? 0.1F : MathHelper.cos(swing * 0.6662F) * 0.5F * swingAmt;
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lLegRot * 57.2958F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d lKnee = getPos(stack);
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(lLegRot) * 15.0F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d lFoot = getPos(stack);
      stack.pop();
      stack.push();
      stack.translate(-0.125, 0.0, 0.0);
      Vec3d rHip = getPos(stack);
      float rLegRot = elytra ? 0.1F : MathHelper.cos(swing * 0.6662F + (float) Math.PI) * 0.5F * swingAmt;
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rLegRot * 57.2958F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d rKnee = getPos(stack);
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(rLegRot) * 15.0F));
      stack.translate(0.0, -0.25, 0.0);
      Vec3d rFoot = getPos(stack);
      stack.pop();
      stack.pop();
      addBone(out, neck, head);
      addBone(out, neck, waist);
      addBone(out, waist, pelvis);
      addBone(out, neck, lShoulder);
      addBone(out, neck, rShoulder);
      addBone(out, lShoulder, lElbow);
      addBone(out, lElbow, lHand);
      addBone(out, rShoulder, rElbow);
      addBone(out, rElbow, rHand);
      addBone(out, pelvis, lHip);
      addBone(out, pelvis, rHip);
      addBone(out, lHip, lKnee);
      addBone(out, lKnee, lFoot);
      addBone(out, rHip, rKnee);
      addBone(out, rKnee, rFoot);
   }

   private static void addBone(List<Vec3d[]> out, Vec3d start, Vec3d end) {
      if (out.size() < 960) {
         out.add(new Vec3d[]{start, end});
      }
   }

   private static Vec3d getPos(MatrixStack stack) {
      stack.peek().getPositionMatrix().transformPosition(0.0F, 0.0F, 0.0F, TRANSFORMED);
      return new Vec3d(TRANSFORMED.x, TRANSFORMED.y, TRANSFORMED.z);
   }
}
