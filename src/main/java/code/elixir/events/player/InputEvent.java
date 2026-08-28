package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

public class InputEvent extends EventCancellable {
   private PlayerInput input;

   public void setJumping(boolean jump) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), jump, this.input.sneak(), this.input.sprint()
      );
   }

   public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
      this.input = new PlayerInput(forward, backward, left, right, this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void setSneak(boolean sneak) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), sneak, this.input.sprint()
      );
   }

   public void inputNone() {
      this.input = new PlayerInput(false, false, false, false, false, false, false);
   }

   public int forward() {
      return this.input.forward() ? 1 : (this.input.backward() ? -1 : 0);
   }

   public float sideways() {
      return this.input.left() ? 1.0F : (this.input.right() ? -1.0F : 0.0F);
   }

   public float getForward() {
      return this.input.forward() ? 1.0F : (this.input.backward() ? -1.0F : 0.0F);
   }

   public float getStrafe() {
      return this.input.left() ? 1.0F : (this.input.right() ? -1.0F : 0.0F);
   }

   public boolean isJump() {
      return this.input.jump();
   }

   public boolean isSneak() {
      return this.input.sneak();
   }

   public void setForward(float forward) {
      boolean forwardKey = forward > 0.0F;
      boolean backwardKey = forward < 0.0F;
      this.input = new PlayerInput(forwardKey, backwardKey, this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void setStrafe(float strafe) {
      boolean leftKey = strafe > 0.0F;
      boolean rightKey = strafe < 0.0F;
      this.input = new PlayerInput(this.input.forward(), this.input.backward(), leftKey, rightKey, this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void setJump(boolean jump) {
      this.setJumping(jump);
   }

   public void correctMovement(float correctionYaw, float directionYaw) {
      float forward = this.getForward();
      float sideways = this.getStrafe();
      if (forward != 0.0F || sideways != 0.0F) {
         double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(directionYaw, forward, sideways)));
         float closestForward = 0.0F;
         float closestSideways = 0.0F;
         float closestDifference = Float.MAX_VALUE;

         for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedSideways = -1.0F; predictedSideways <= 1.0F; predictedSideways++) {
               float difference;
               if ((predictedForward != 0.0F || predictedSideways != 0.0F)
                  && (
                        difference = Math.abs(
                           MathHelper.wrapDegrees(
                              (float)(angle - MathHelper.wrapDegrees(Math.toDegrees(direction(correctionYaw, predictedForward, predictedSideways))))
                           )
                        )
                     )
                     < closestDifference) {
                  closestDifference = difference;
                  closestForward = predictedForward;
                  closestSideways = predictedSideways;
               }
            }
         }

         this.setForward(closestForward);
         this.setStrafe(closestSideways);
      }
   }

   private static double direction(float yaw, float movementForward, float movementSideways) {
      if (movementForward < 0.0F) {
         yaw += 180.0F;
      }

      float forward = 1.0F;
      if (movementForward < 0.0F) {
         forward = -0.5F;
      } else if (movementForward > 0.0F) {
         forward = 0.5F;
      }

      if (movementSideways > 0.0F) {
         yaw -= 90.0F * forward;
      }

      if (movementSideways < 0.0F) {
         yaw += 90.0F * forward;
      }

      return Math.toRadians(yaw);
   }

   public PlayerInput getInput() {
      return this.input;
   }

   public void setInput(PlayerInput input) {
      this.input = input;
   }

   public InputEvent(PlayerInput input) {
      this.input = input;
   }
}
