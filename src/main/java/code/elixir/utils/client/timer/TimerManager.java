package code.elixir.utils.client.timer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public class TimerManager {
   private static float timerSpeed = 1.0F;

   public static void setTimer(float speed) {
      timerSpeed = Math.max(0.1F, Math.min(speed, 10.0F));
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         RenderTickCounter tickCounter = mc.getRenderTickCounter();
         if (tickCounter instanceof IRenderTickCounterDynamic) {
            ((IRenderTickCounterDynamic)tickCounter).setSpeedMultiplier(speed);
         }
      }
   }

   public static void reset() {
      setTimer(1.0F);
   }

   public static float getTimerSpeed() {
      return timerSpeed;
   }
}
