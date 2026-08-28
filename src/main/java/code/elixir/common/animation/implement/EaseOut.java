package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class EaseOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return 1.0 - (1.0 - x) * (1.0 - x);
   }
}
