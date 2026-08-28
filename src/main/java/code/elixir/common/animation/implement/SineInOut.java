package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class SineInOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return -(Math.cos(Math.PI * x) - 1.0) / 2.0;
   }
}
