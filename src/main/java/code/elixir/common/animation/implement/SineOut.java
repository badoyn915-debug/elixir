package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class SineOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return Math.sin(x * Math.PI / 2.0);
   }
}
