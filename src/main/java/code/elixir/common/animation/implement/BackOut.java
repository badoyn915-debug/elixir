package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class BackOut extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      double c1 = 1.70158;
      double c3 = c1 + 1.0;
      return 1.0 + c3 * Math.pow(x - 1.0, 3.0) + c1 * Math.pow(x - 1.0, 2.0);
   }
}
