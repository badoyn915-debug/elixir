package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class CircOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return Math.sqrt(1.0 - Math.pow(x - 1.0, 2.0));
   }
}
