package code.elixir.utils.features.combat;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.SprintEvent;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.concurrent.ThreadLocalRandom;

public final class TestSprintReset implements QuickImports {
   private static int releaseTicks = 0;
   private static boolean active = false;
   private static long lastResetTime = 0L;

   private TestSprintReset() {
   }

   public static boolean prepareForAttack() {
      if (mc.player == null) {
         return true;
      } else if (!mc.player.isGliding() && !mc.player.isTouchingWater() && !mc.player.isSwimming() && !mc.player.isSubmergedInWater()) {
         boolean isAirCrit = !mc.player.isOnGround() && mc.player.fallDistance > 0.0F && mc.player.getVelocity().y < 0.0;
         if (isAirCrit) {
            return true;
         } else {
            triggerReset();
            return true;
         }
      } else {
         return true;
      }
   }

   public static void onAttack() {
      if (mc.player != null) {
         if (!mc.player.isGliding() && !mc.player.isTouchingWater() && !mc.player.isSwimming() && !mc.player.isSubmergedInWater()) {
            triggerReset();
         }
      }
   }

   private static void triggerReset() {
      long now = System.currentTimeMillis();
      if (now - lastResetTime >= 80L) {
         lastResetTime = now;
         if (mc.options != null && mc.options.sprintKey != null) {
            mc.options.sprintKey.setPressed(false);
         }

         active = true;
         double roll = ThreadLocalRandom.current().nextDouble();
         if (roll < 0.2) {
            releaseTicks = 1;
         } else if (roll < 0.85) {
            releaseTicks = 2;
         } else {
            releaseTicks = 3;
         }
      }
   }

   public static void applyInputReset(InputEvent event) {
      if (active && mc.player != null) {
         if (releaseTicks > 0) {
            event.setForward(0.0F);
            releaseTicks--;
         } else {
            active = false;
         }
      }
   }

   public static void handleSprintEvent(SprintEvent event) {
      if (active && releaseTicks > 0) {
         event.setSprinting(false);
      }
   }

   public static void reset() {
      active = false;
      releaseTicks = 0;
      lastResetTime = 0L;
   }
}
