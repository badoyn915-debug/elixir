package code.elixir.common.animation.implement;

import code.elixir.common.animation.Animation;

public class InOutCirc extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0;
   }
}
