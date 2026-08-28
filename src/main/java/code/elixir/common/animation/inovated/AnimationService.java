package code.elixir.common.animation.inovated;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class AnimationService {
   public static double delta;

   public static float animation(float animation, float target, float speedTarget) {
      float dif = (target - animation) / Math.max((float)MinecraftClient.getInstance().getCurrentFps(), 5.0F) * 15.0F;
      if (dif > 0.0F) {
         dif = Math.max(speedTarget, dif);
         dif = Math.min(target - animation, dif);
      } else if (dif < 0.0F) {
         dif = Math.min(-speedTarget, dif);
         dif = Math.max(target - animation, dif);
      }

      return animation + dif;
   }

   public static float animationSpeed(float animation, float target, float speedTarget) {
      float dif = (target - animation) / Math.max((float)MinecraftClient.getInstance().getCurrentFps(), 5.0F) * 15.0F;
      if (dif > 0.0F) {
         dif = Math.min(target - animation, speedTarget);
      } else if (dif < 0.0F) {
         dif = -speedTarget;
         dif = Math.max(target - animation, dif);
      }

      return animation + dif;
   }

   public static float getAnimationState(float animation, float finalState, float speed) {
      float add = (float)(delta * (speed / 1000.0F));
      float var4;
      float var5;
      return animation < finalState
         ? (animation + add < finalState ? (var4 = animation + add) : finalState)
         : (animation - add > finalState ? (var5 = animation - add) : finalState);
   }

   public static double interpolateAnimation(double start, double end, double step) {
      return start + (end - start) * step;
   }

   public static float move(float from, float to, float minstep, float maxstep, float factor) {
      float f = (to - from) * MathHelper.clamp(factor, 0.0F, 1.0F);
      f = f < 0.0F ? MathHelper.clamp(f, -maxstep, -minstep) : MathHelper.clamp(f, minstep, maxstep);
      return Math.abs(f) > Math.abs(to - from) ? to : from + f;
   }

   private AnimationService() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
