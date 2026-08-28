package code.elixir.common.animation.Easy;

public class EaseBackIn extends EasyAnimService {
   private final float easeAmount;

   public EaseBackIn(int ms, double endPoint, float easeAmount) {
      super(ms, endPoint);
      this.easeAmount = easeAmount;
   }

   public EaseBackIn(int ms, double endPoint, float easeAmount, Direction direction) {
      super(ms, endPoint, direction);
      this.easeAmount = easeAmount;
   }

   @Override
   protected boolean correctOutput() {
      return true;
   }

   @Override
   protected double getEquation(double x) {
      float shrink = this.easeAmount + 1.0F;
      return Math.max(0.0, 1.0 + shrink * Math.pow(x - 1.0, 3.0) + this.easeAmount * Math.pow(x - 1.0, 2.0));
   }
}
