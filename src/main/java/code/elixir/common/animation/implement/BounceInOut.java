package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class BounceInOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      double n1 = 7.5625;
      double d1 = 2.75;
      return x < 0.5 ? (1.0 - this.bounceOut(1.0 - 2.0 * x)) / 2.0 : (1.0 + this.bounceOut(2.0 * x - 1.0)) / 2.0;
   }

   private double bounceOut(double x) {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (x < 1.0 / d1) {
         return n1 * x * x;
      } else if (x < 2.0 / d1) {
         double var9;
         return n1 * (var9 = x - 1.5 / d1) * var9 + 0.75;
      } else {
         double var7;
         double var8;
         return x < 2.5 / d1 ? n1 * (var7 = x - 2.25 / d1) * var7 + 0.9375 : n1 * (var8 = x - 2.625 / d1) * var8 + 0.984375;
      }
   }
}
