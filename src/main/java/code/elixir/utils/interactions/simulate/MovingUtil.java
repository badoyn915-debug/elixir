package code.elixir.utils.interactions.simulate;

import code.elixir.events.player.InputEvent;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.FreeLookStorage;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MovingUtil implements QuickImports {
   public static boolean hasPlayerMovement() {
      return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
   }

   public static double[] calculateDirection(double distance) {
      float forward = mc.player.input.movementForward;
      float sideways = mc.player.input.movementSideways;
      float yaw = mc.player.getYaw();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians(yaw + 90.0F));
      double cosYaw = Math.cos(Math.toRadians(yaw + 90.0F));
      double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
      double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public static double getSpeedSqrt(Entity entity) {
      double dx = entity.getX() - entity.prevX;
      double dy = entity.getY() - entity.prevY;
      double dz = entity.getZ() - entity.prevZ;
      return Math.sqrt(dx * dx + dz * dz + dy * dy);
   }

   public static void setVelocity(double velocity) {
      double[] direction = calculateDirection(velocity);
      Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
   }

   public static void setVelocity(double velocity, double y) {
      double[] direction = calculateDirection(velocity);
      Objects.requireNonNull(mc.player).setVelocity(direction[0], y, direction[1]);
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians(MathHelper.wrapDegrees(yaw));
      return Math.toDegrees(MathHelper.wrapDegrees(optimalYaw - currentYaw));
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
      boolean forwards = input.forward();
      boolean backwards = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (dgs >= -90.0F + deadAngle && dgs <= 90.0F - deadAngle) {
         forwards = true;
      } else if (dgs < -90.0F - deadAngle || dgs > 90.0F + deadAngle) {
         backwards = true;
      }

      if (dgs >= 0.0F + deadAngle && dgs <= 180.0F - deadAngle) {
         right = true;
      } else if (dgs >= -180.0F + deadAngle && dgs <= 0.0F - deadAngle) {
         left = true;
      }

      return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
   }

   public static void fixMovementFocus(InputEvent event, float yaw) {
      float forward = event.getForward();
      float strafe = event.getStrafe();
      if (forward != 0.0F || strafe != 0.0F) {
         double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));
         float bestForward = 0.0F;
         float bestStrafe = 0.0F;
         float smallestDifference = Float.MAX_VALUE;

         for (float testForward = -1.0F; testForward <= 1.0F; testForward++) {
            for (float testStrafe = -1.0F; testStrafe <= 1.0F; testStrafe++) {
               float difference;
               if ((testForward != 0.0F || testStrafe != 0.0F)
                  && (
                        difference = Math.abs(
                           MathHelper.wrapDegrees((float)(targetAngle - MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)))))
                        )
                     )
                     < smallestDifference) {
                  smallestDifference = difference;
                  bestForward = testForward;
                  bestStrafe = testStrafe;
               }
            }
         }

         event.setForward(bestForward);
         event.setStrafe(bestStrafe);
      }
   }

   public static void fixMovementFree(InputEvent event) {
      float inputYaw = mc.player.isGliding() ? mc.player.getYaw() : MathHelper.wrapDegrees(FreeLookStorage.getFreeYaw());
      fixMovementFree(event, mc.player.getYaw(), inputYaw);
   }

   public static void fixMovementFree(InputEvent event, float moveYaw, float inputYaw) {
      float forward = event.getForward();
      float strafe = event.getStrafe();
      double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(inputYaw, forward, strafe)));
      if (forward != 0.0F || strafe != 0.0F) {
         float closestForward = 0.0F;
         float closestStrafe = 0.0F;
         float closestDifference = Float.MAX_VALUE;

         for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
               float difference;
               if ((predictedStrafe != 0.0F || predictedForward != 0.0F)
                  && (
                        difference = Math.abs(
                           MathHelper.wrapDegrees(
                              (float)(angle - MathHelper.wrapDegrees(Math.toDegrees(direction(moveYaw, predictedForward, predictedStrafe))))
                           )
                        )
                     )
                     < closestDifference) {
                  closestDifference = difference;
                  closestForward = predictedForward;
                  closestStrafe = predictedStrafe;
               }
            }
         }

         event.setForward(closestForward);
         event.setStrafe(closestStrafe);
      }
   }

   public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
      if (moveForward < 0.0F) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0F) {
         forward = -0.5F;
      }

      if (moveForward > 0.0F) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0F) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0F) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   @Generated
   private MovingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
