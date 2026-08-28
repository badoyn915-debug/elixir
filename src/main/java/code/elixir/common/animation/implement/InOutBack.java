package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class InOutBack extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      double c1 = 1.70158;
      double c2 = c1 * 1.525;
      return x < 0.5
         ? Math.pow(2.0 * x, 2.0) * ((c2 + 1.0) * 2.0 * x - c2) / 2.0
         : (Math.pow(2.0 * x - 2.0, 2.0) * ((c2 + 1.0) * (x * 2.0 - 2.0) + c2) + 2.0) / 2.0;
   }
}
