package code.elixir.common.animation;

import code.elixir.utils.math.time.TimerUtil;

public abstract class Animation implements AnimationCalculation {
   protected final TimerUtil counter = new TimerUtil();
   protected int ms;
   protected double value;
   protected Direction direction = Direction.FORWARDS;

   public void reset() {
      this.counter.resetCounter();
   }

   public boolean isDone() {
      return this.counter.isReached(this.ms);
   }

   public boolean isFinished(Direction direction) {
      return this.direction == direction && this.isDone();
   }

   public void setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
         this.adjustTimer();
      }
   }

   public boolean isDirection(Direction direction) {
      return this.direction == direction;
   }

   private void adjustTimer() {
      this.counter.setTime(System.currentTimeMillis() - (this.ms - Math.min((long)this.ms, this.counter.getTime())));
   }

   public Double getOutput() {
      double time = this.calculation(this.ms - this.counter.getTime()) * this.value;
      return this.direction == Direction.FORWARDS ? this.endValue() : (this.isDone() ? 0.0 : time);
   }

   protected double endValue() {
      return this.isDone() ? this.value : this.calculation(this.counter.getTime()) * this.value;
   }

   public Animation setMs(int ms) {
      this.ms = ms;
      return this;
   }

   public Animation setValue(double value) {
      this.value = value;
      return this;
   }
}
