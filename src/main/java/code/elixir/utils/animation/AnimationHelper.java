package code.elixir.utils.animation;

import java.util.HashMap;
import java.util.Map;

public class AnimationHelper {
   private static final Map<String, AnimationHelper.AnimationData> animations = new HashMap<>();

   public static void startAnimation(String key, float startValue, float endValue, long duration, AnimationHelper.EasingType easingType) {
      animations.put(key, new AnimationHelper.AnimationData(startValue, endValue, duration, easingType));
   }

   public static float getAnimationValue(String key, float defaultValue) {
      AnimationHelper.AnimationData data = animations.get(key);
      if (data == null) {
         return defaultValue;
      } else {
         long currentTime = System.currentTimeMillis();
         long elapsed = currentTime - data.startTime;
         if (elapsed >= data.duration) {
            animations.remove(key);
            return data.endValue;
         } else {
            float progress = (float)elapsed / (float)data.duration;
            float easedProgress = applyEasing(progress, data.easingType);
            return data.startValue + (data.endValue - data.startValue) * easedProgress;
         }
      }
   }

   public static boolean isAnimationActive(String key) {
      return animations.containsKey(key);
   }

   public static void stopAnimation(String key) {
      animations.remove(key);
   }

   private static float applyEasing(float t, AnimationHelper.EasingType easingType) {
      switch (easingType) {
         case LINEAR:
            return t;
         case EASE_OUT:
            return 1.0F - (1.0F - t) * (1.0F - t);
         case EASE_IN:
            return t * t;
         case EASE_IN_OUT:
            return t < 0.5F ? 2.0F * t * t : 1.0F - (2.0F - 2.0F * t) * (2.0F - 2.0F * t) / 2.0F;
         case BOUNCE_OUT:
            if (t < 0.36363637F) {
               return 7.5625F * t * t;
            } else if (t < 0.72727275F) {
               float var4;
               return 7.5625F * (var4 = t - 0.54545456F) * var4 + 0.75F;
            } else {
               if (t < 0.90909094F) {
                  float var3;
                  return 7.5625F * (var3 = t - 0.8181818F) * var3 + 0.9375F;
               }

               float var2;
               return 7.5625F * (var2 = t - 0.95454544F) * var2 + 0.984375F;
            }
         default:
            return t;
      }
   }

   public static void fadeIn(String key, long duration) {
      startAnimation(key, 0.0F, 1.0F, duration, AnimationHelper.EasingType.EASE_OUT);
   }

   public static void fadeOut(String key, long duration) {
      startAnimation(key, 1.0F, 0.0F, duration, AnimationHelper.EasingType.EASE_IN);
   }

   public static void slideIn(String key, float startY, float endY, long duration) {
      startAnimation(key, startY, endY, duration, AnimationHelper.EasingType.BOUNCE_OUT);
   }

   public static void colorTransition(String key, int startColor, int endColor, long duration) {
      startAnimation(key, startColor, endColor, duration, AnimationHelper.EasingType.EASE_IN_OUT);
   }

   public static class AnimationData {
      public float startValue;
      public float endValue;
      public long startTime;
      public long duration;
      public AnimationHelper.EasingType easingType;

      public AnimationData(float startValue, float endValue, long duration, AnimationHelper.EasingType easingType) {
         this.startValue = startValue;
         this.endValue = endValue;
         this.startTime = System.currentTimeMillis();
         this.duration = duration;
         this.easingType = easingType;
      }
   }

   public static enum EasingType {
      LINEAR,
      EASE_OUT,
      EASE_IN,
      EASE_IN_OUT,
      BOUNCE_OUT;
   }
}
