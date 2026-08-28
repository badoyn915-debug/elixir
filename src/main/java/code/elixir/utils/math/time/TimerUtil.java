package code.elixir.utils.math.time;

public class TimerUtil {
   private long lastMS = System.currentTimeMillis();
   private long time;

   public TimerUtil() {
      this.resetCounter();
   }

   public static TimerUtil create() {
      return new TimerUtil();
   }

   public void resetCounter() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean isReached(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public void setLastMS(long newValue) {
      this.lastMS = System.currentTimeMillis() + newValue;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public long getPassedTimeMs() {
      return this.getMs(System.nanoTime() - this.time);
   }

   public long getMs(long time) {
      return time / 1000000L;
   }

   public boolean passedMs(long ms) {
      return this.getMs(System.nanoTime() - this.time) >= ms;
   }

   public boolean isRunning() {
      return System.currentTimeMillis() - this.lastMS <= 0L;
   }

   public boolean hasTimeElapsed(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public boolean hasTimeElapsed() {
      return this.lastMS < System.currentTimeMillis();
   }

   public long getLastMS() {
      return this.lastMS;
   }
}
