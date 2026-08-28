package code.elixir.features.impl.combat.components.interpolation;

import code.elixir.utils.display.interfaces.QuickImports;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class HumanAim implements QuickImports {
   private static int lastTargetId = -1;
   private static long reactionStart = 0L;
   private static long reactionDelay = 0L;
   private static long rampDuration = 160L;
   private static float speedJitterValue = 1.0F;

   public static float reactionFactor(Entity target) {
      int id = target == null ? -1 : target.getId();
      long now = System.currentTimeMillis();
      if (id != lastTargetId) {
         lastTargetId = id;
         reactionStart = now;
         reactionDelay = ThreadLocalRandom.current().nextLong(90L, 230L);
         rampDuration = ThreadLocalRandom.current().nextLong(120L, 220L);
      }

      long elapsed;
      if ((elapsed = now - reactionStart) <= reactionDelay) {
         return 0.0F;
      } else {
         float t = (float)(elapsed - reactionDelay) / (float)rampDuration;
         return t >= 1.0F ? 1.0F : (float)(1.0 - Math.pow(1.0 - t, 3.0));
      }
   }

   public static void reset() {
      lastTargetId = -1;
      reactionStart = 0L;
      reactionDelay = 0L;
   }

   public static float noiseYaw() {
      double t = System.nanoTime() / 1.0E9;
      return (float)(Math.sin(t * 2.3) * 0.16 + Math.sin(t * 5.7 + 1.1) * 0.05);
   }

   public static float noisePitch() {
      double t = System.nanoTime() / 1.0E9;
      return (float)(Math.sin(t * 1.9 + 0.7) * 0.09 + Math.sin(t * 4.3 + 2.2) * 0.03);
   }

   public static float bezierEase(float t) {
      float c = MathHelper.clamp(t, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   public static float fovFactor(float diffYaw, float diffPitch) {
      float dist = (float)Math.hypot(diffYaw, diffPitch);
      if (dist <= 30.0F) {
         return 1.0F;
      } else {
         return dist >= 120.0F ? 0.55F : 1.0F - (dist - 30.0F) / 90.0F * 0.45F;
      }
   }

   public static float speedJitter() {
      float target = (float)(1.0 + ThreadLocalRandom.current().nextGaussian() * 0.08);
      target = MathHelper.clamp(target, 0.85F, 1.15F);
      speedJitterValue = speedJitterValue + (target - speedJitterValue) * 0.08F;
      return speedJitterValue;
   }
}
