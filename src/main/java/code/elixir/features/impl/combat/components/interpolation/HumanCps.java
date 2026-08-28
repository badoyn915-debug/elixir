package code.elixir.features.impl.combat.components.interpolation;

import java.util.concurrent.ThreadLocalRandom;

public class HumanCps {
   private static double driftPhase = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
   private static long lastCall = 0L;

   public static long humanize(long baseCooldown) {
      long now = System.currentTimeMillis();
      double elapsed = lastCall == 0L ? 0.0 : (now - lastCall) / 1000.0;
      lastCall = now;
      double drift = Math.sin(driftPhase += elapsed * 1.4) * 60.0 + Math.sin(driftPhase * 0.37 + 1.3) * 35.0;
      double gaussian = ThreadLocalRandom.current().nextGaussian() * 40.0;
      double jitter = ThreadLocalRandom.current().nextDouble(-22.0, 22.0);
      double burst = ThreadLocalRandom.current().nextDouble() < 0.12 ? -ThreadLocalRandom.current().nextDouble(40.0, 90.0) : 0.0;
      long result = baseCooldown + (long)(drift + gaussian + jitter + burst);
      long min = baseCooldown - 150L;
      long max = baseCooldown + 170L;
      if (result < min) {
         result = min;
      }

      if (result > max) {
         result = max;
      }

      if (result < 50L) {
         result = 50L;
      }

      return result;
   }
}
