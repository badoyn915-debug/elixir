package code.elixir.common.animation.inovated;

public class EasingList {
   public static final double c1 = 1.70158;
   public static final double c2 = 2.5949095;
   public static final double c3 = 2.70158;
   public static final double c4 = Math.PI * 2.0 / 3.0;
   public static final double c5 = Math.PI * 4.0 / 9.0;
   public static final EasingList.Easing QUAD_IN_OUT = powIN_OUT(2.0);
   public static final EasingList.Easing SINE_IN = value -> (float)(1.0 - Math.cos(value * Math.PI / 2.0));
   public static final EasingList.Easing SINE_OUT = value -> (float)Math.sin(value * Math.PI / 2.0);
   public static final EasingList.Easing SINE_BOTH = value -> (float)(-(Math.cos(Math.PI * value) - 1.0) / 2.0);
   public static final EasingList.Easing CIRC_IN = value -> (float)(1.0 - Math.sqrt(1.0 - Math.pow(value, 2.0)));
   public static final EasingList.Easing CIRC_OUT = value -> (float)Math.sqrt(1.0 - Math.pow(value - 1.0, 2.0));
   public static final EasingList.Easing CIRC_BOTH = value -> (float)(
      value < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * value, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * value + 2.0, 2.0)) + 1.0) / 2.0
   );
   public static final EasingList.Easing ELASTIC_IN = value -> value != 0.0 && value != 1.0
      ? (float)(Math.pow(-2.0, 10.0 * value - 10.0) * Math.sin((value * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)))
      : value;
   public static final EasingList.Easing ELASTIC_OUT = value -> value != 0.0 && value != 1.0
      ? (float)(Math.pow(2.0, -10.0 * value) * Math.sin((value * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0)
      : value;
   public static final EasingList.Easing ELASTIC_BOTH = value -> value != 0.0 && value != 1.0
      ? (float)(
         value < 0.5
            ? -(Math.pow(2.0, 20.0 * value - 10.0) * Math.sin((20.0 * value - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
            : Math.pow(2.0, -20.0 * value + 10.0) * Math.sin((20.0 * value - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0
      )
      : value;
   public static final EasingList.Easing EXPO_IN = value -> value != 0.0 ? (float)Math.pow(2.0, 10.0 * value - 10.0) : value;
   public static final EasingList.Easing EXPO_OUT = value -> value != 1.0 ? (float)(1.0 - Math.pow(2.0, -10.0 * value)) : value;
   public static final EasingList.Easing EXPO_BOTH = value -> value != 0.0 && value != 1.0
      ? (float)(value < 0.5 ? Math.pow(2.0, 20.0 * value - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * value + 10.0)) / 2.0)
      : value;
   public static final EasingList.Easing BACK_IN = value -> (float)(2.70158 * Math.pow(value, 3.0) - 1.70158 * Math.pow(value, 2.0));
   public static final EasingList.Easing BACK_OUT = value -> (float)(1.0 + 2.70158 * Math.pow(value - 1.0, 3.0) + 1.70158 * Math.pow(value - 1.0, 2.0));
   public static final EasingList.Easing NONE = value -> value;
   public static final EasingList.Easing BACK_BOTH = value -> (float)(
      value < 0.5
         ? Math.pow(2.0 * value, 2.0) * (7.189819 * value - 2.5949095) / 2.0
         : (Math.pow(2.0 * value - 2.0, 2.0) * (3.5949095 * (value * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0
   );
   public static final EasingList.Easing BOUNCE_OUT = value -> {
      float n1 = 7.5625F;
      float d1 = 2.75F;
      if (value < 1.0 / d1) {
         return (float)(n1 * Math.pow(value, 2.0));
      } else {
         return value < 2.0 / d1
            ? (float)(n1 * Math.pow(value - 1.5 / d1, 2.0) + 0.75)
            : (float)(value < 2.5 / d1 ? n1 * Math.pow(value - 2.25 / d1, 2.0) + 0.9375 : n1 * Math.pow(value - 2.625 / d1, 2.0) + 0.984375);
      }
   };
   public static final EasingList.Easing BOUNCE_IN = value -> (float)(1.0 - BOUNCE_OUT.ease((float)(1.0 - value)));
   public static final EasingList.Easing BOUNCE_BOTH = value -> (float)(
      value < 0.5 ? (1.0 - BOUNCE_OUT.ease((float)(1.0 - 2.0 * value))) / 2.0 : (1.0 + BOUNCE_OUT.ease((float)(2.0 * value - 1.0))) / 2.0
   );
   public static final EasingList.Easing QUINT_IN = x -> x < 0.5 ? 16.0F * x * x * x * x * x : (float)(1.0 - Math.pow(-2.0F * x + 2.0F, 5.0) / 2.0);

   public static EasingList.Easing powIN_OUT(double n) {
      return value -> (float)(value < 0.5 ? Math.pow(2.0, n - 1.0) * Math.pow(value, n) : 1.0 - Math.pow(-2.0 * value + 2.0, n) / 2.0);
   }

   @FunctionalInterface
   public interface Easing {
      float ease(float var1);
   }
}
