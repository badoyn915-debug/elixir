package code.elixir.common.animation;

public interface AnimationCalculation {
   default double calculation(double value) {
      return 0.0;
   }
}
