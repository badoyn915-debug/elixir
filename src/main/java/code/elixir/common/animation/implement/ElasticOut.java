package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class ElasticOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      if (x == 0.0) {
         return 0.0;
      } else if (x == 1.0) {
         return 1.0;
      } else {
         double c4 = Math.PI * 2.0 / 3.0;
         return Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * c4) + 1.0;
      }
   }
}
