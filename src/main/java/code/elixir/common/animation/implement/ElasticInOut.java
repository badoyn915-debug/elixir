package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class ElasticInOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      if (x != 0.0 && x != 1.0) {
         double c5 = Math.PI * 4.0 / 9.0;
         return x < 0.5
            ? -(Math.pow(2.0, 20.0 * x - 10.0) * Math.sin((20.0 * x - 11.125) * c5)) / 2.0
            : Math.pow(2.0, -20.0 * x + 10.0) * Math.sin((20.0 * x - 11.125) * c5) / 2.0 + 1.0;
      } else {
         return x;
      }
   }
}
