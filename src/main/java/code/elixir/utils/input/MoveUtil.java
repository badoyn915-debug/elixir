package code.elixir.utils.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class MoveUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static double getMotion() {
      if (mc.player == null) {
         return 0.0;
      } else {
         Vec3d velocity = mc.player.getVelocity();
         return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      }
   }

   public static void setMotion(double speed) {
      if (mc.player != null) {
         float forward = mc.player.input.movementForward;
         float sideways = mc.player.input.movementSideways;
         float yaw = mc.player.getYaw();
         if (forward == 0.0F && sideways == 0.0F) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
         } else {
            if (forward != 0.0F) {
               if (sideways > 0.0F) {
                  yaw -= forward > 0.0F ? 45 : -45;
               } else if (sideways < 0.0F) {
                  yaw += forward > 0.0F ? 45 : -45;
               }

               sideways = 0.0F;
               forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double rad = Math.toRadians(yaw + 90.0F);
            double motionX = forward * speed * Math.cos(rad) + sideways * speed * Math.sin(rad);
            double motionZ = forward * speed * Math.sin(rad) - sideways * speed * Math.cos(rad);
            mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
         }
      }
   }

   public static void strafe() {
      strafe(getMotion());
   }

   public static void strafe(double speed) {
      if (mc.player != null) {
         float forward = mc.player.input.movementForward;
         float sideways = mc.player.input.movementSideways;
         float yaw = mc.player.getYaw();
         if (forward == 0.0F && sideways == 0.0F) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
         } else {
            if (forward != 0.0F) {
               if (sideways > 0.0F) {
                  yaw -= forward > 0.0F ? 45 : -45;
               } else if (sideways < 0.0F) {
                  yaw += forward > 0.0F ? 45 : -45;
               }

               sideways = 0.0F;
               forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double rad = Math.toRadians(yaw + 90.0F);
            mc.player
               .setVelocity(
                  forward * speed * Math.cos(rad) + sideways * speed * Math.sin(rad),
                  mc.player.getVelocity().y,
                  forward * speed * Math.sin(rad) - sideways * speed * Math.cos(rad)
               );
         }
      }
   }

   public static boolean isMoving() {
      return mc.player == null ? false : mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
   }

   public static double getDirection() {
      if (mc.player == null) {
         return 0.0;
      } else {
         float forward = mc.player.input.movementForward;
         float sideways = mc.player.input.movementSideways;
         float yaw = mc.player.getYaw();
         if (forward < 0.0F) {
            yaw += 180.0F;
         }

         float strafe = 1.0F;
         if (forward < 0.0F) {
            strafe = -0.5F;
         } else if (forward > 0.0F) {
            strafe = 0.5F;
         }

         if (sideways > 0.0F) {
            yaw -= 90.0F * strafe;
         }

         if (sideways < 0.0F) {
            yaw += 90.0F * strafe;
         }

         return Math.toRadians(yaw);
      }
   }

   public static double getBaseSpeed() {
      if (mc.player == null) {
         return 0.2873;
      } else {
         double baseSpeed = 0.2873;
         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed *= 1.0 - 0.15 * (amplifier + 1);
         }

         return baseSpeed;
      }
   }

   public static void jump() {
      if (mc.player != null) {
         mc.player.jump();
      }
   }

   public static void setSpeed(double speed, float yaw) {
      if (mc.player != null) {
         double rad = Math.toRadians(yaw + 90.0F);
         mc.player.setVelocity(speed * Math.cos(rad), mc.player.getVelocity().y, speed * Math.sin(rad));
      }
   }

   public static void addMotion(double speed) {
      if (mc.player != null) {
         float yaw = mc.player.getYaw();
         double rad = Math.toRadians(yaw + 90.0F);
         Vec3d velocity = mc.player.getVelocity();
         mc.player.setVelocity(velocity.x + speed * Math.cos(rad), velocity.y, velocity.z + speed * Math.sin(rad));
      }
   }

   public static double getSpeedBPS() {
      return getMotion() * 20.0;
   }

   public static boolean isOnGround() {
      return mc.player != null && mc.player.isOnGround();
   }

   public static Vec3d predictPosition(int ticks) {
      if (mc.player == null) {
         return Vec3d.ZERO;
      } else {
         Vec3d pos = mc.player.getPos();
         Vec3d velocity = mc.player.getVelocity();

         for (int i = 0; i < ticks; i++) {
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.91, 0.98, 0.91);
         }

         return pos;
      }
   }

   public static PlayerInput getPlayerInput(float angleDiff) {
      boolean forward = false;
      boolean back = false;
      boolean left = false;
      boolean right = false;
      if (angleDiff >= -22.5 && angleDiff < 22.5) {
         forward = true;
      } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
         forward = true;
         right = true;
      } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
         right = true;
      } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
         back = true;
         right = true;
      } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
         forward = true;
         left = true;
      } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
         left = true;
      } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
         back = true;
         left = true;
      } else {
         back = true;
      }

      return new PlayerInput(forward, back, left, right, false, false, false);
   }

   public static float getdir() {
      if (mc.player == null) {
         return -1.0F;
      } else {
         float forward = mc.player.input.movementForward;
         float sideways = mc.player.input.movementSideways;
         if (forward == 0.0F && sideways == 0.0F) {
            return -1.0F;
         } else {
            float yaw = mc.player.getYaw();
            if (forward < 0.0F) {
               yaw += 180.0F;
            }

            float strafe = 1.0F;
            if (forward < 0.0F) {
               strafe = -0.5F;
            } else if (forward > 0.0F) {
               strafe = 0.5F;
            }

            if (sideways > 0.0F) {
               yaw -= 90.0F * strafe;
            }

            if (sideways < 0.0F) {
               yaw += 90.0F * strafe;
            }

            return yaw;
         }
      }
   }
}
