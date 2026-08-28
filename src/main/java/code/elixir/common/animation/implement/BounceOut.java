package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class BounceOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      double n1 = 7.5625;
      double d1 = 2.75;
      if (x < 1.0 / d1) {
         return n1 * x * x;
      } else if (x < 2.0 / d1) {
         double var11;
         return n1 * (var11 = x - 1.5 / d1) * var11 + 0.75;
      } else {
         double var9;
         double var10;
         return x < 2.5 / d1 ? n1 * (var9 = x - 2.25 / d1) * var9 + 0.9375 : n1 * (var10 = x - 2.625 / d1) * var10 + 0.984375;
      }
   }
}
