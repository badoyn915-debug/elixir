package code.elixir.utils.animation.util;

import code.elixir.common.animation.inovated.EasingList;

public class Easings {
   public static final EasingList.Easing LINEAR = EasingList.NONE;
   public static final EasingList.Easing CUBIC_OUT = value -> {
      double x = 1.0 - value;
      return (float)(1.0 - x * x * x);
   };
   public static final EasingList.Easing CUBIC_IN = value -> value * value * value;
   public static final EasingList.Easing CUBIC_IN_OUT = value -> value < 0.5F
      ? (float)(4.0 * value * value * value)
      : (float)(1.0 - Math.pow(-2.0 * value + 2.0, 3.0) / 2.0);
   public static final EasingList.Easing QUAD_OUT = value -> (float)(1.0 - (1.0 - value) * (1.0 - value));
   public static final EasingList.Easing QUAD_IN = value -> value * value;
   public static final EasingList.Easing QUAD_IN_OUT = EasingList.QUAD_IN_OUT;
   public static final EasingList.Easing SINE_OUT = EasingList.SINE_OUT;
   public static final EasingList.Easing SINE_IN = EasingList.SINE_IN;
   public static final EasingList.Easing SINE_IN_OUT = EasingList.SINE_BOTH;
   public static final EasingList.Easing EXPO_OUT = EasingList.EXPO_OUT;
   public static final EasingList.Easing EXPO_IN = EasingList.EXPO_IN;
   public static final EasingList.Easing EXPO_IN_OUT = EasingList.EXPO_BOTH;
   public static final EasingList.Easing BOUNCE_OUT = EasingList.BOUNCE_OUT;
   public static final EasingList.Easing BOUNCE_IN = EasingList.BOUNCE_IN;
   public static final EasingList.Easing BOUNCE_IN_OUT = EasingList.BOUNCE_BOTH;
   public static final EasingList.Easing BACK_OUT = EasingList.BACK_OUT;
   public static final EasingList.Easing BACK_IN = EasingList.BACK_IN;
   public static final EasingList.Easing BACK_IN_OUT = EasingList.BACK_BOTH;
   public static final EasingList.Easing ELASTIC_OUT = EasingList.ELASTIC_OUT;
   public static final EasingList.Easing ELASTIC_IN = EasingList.ELASTIC_IN;
   public static final EasingList.Easing ELASTIC_IN_OUT = EasingList.ELASTIC_BOTH;
}
