package code.elixir.utils.math;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
   public static float random(float min, float max) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double randA = random.nextDouble();
      double randB = random.nextDouble();
      double randC = random.nextGaussian() * 0.02;
      double smoothFactor = Math.pow(randA, 1.0 + random.nextDouble() * 0.7);
      double mixFactor = (randB * 0.8 + 0.1) * (Math.log1p(randA * 3.0) * 0.5 + 0.5);
      return (float)(min + (max - min) * smoothFactor * mixFactor + randC);
   }

   public static double randomBest(double min, double max) {
      return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
   }

   private MathUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
