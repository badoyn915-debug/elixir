package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class ExpoOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
   }
}
