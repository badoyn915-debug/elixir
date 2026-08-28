package code.elixir.features.impl.combat.components.gcd;

import code.elixir.utils.display.interfaces.QuickImports;

public class GCDUtil implements QuickImports {
   public static float getFixedRotation(float rot) {
      return getDeltaMouse(rot) * getGCDValue();
   }

   public static float getGCDValue() {
      return (float)(getGCD() * 0.15);
   }

   public static float getGCD() {
      double sens = 0.5;
      if (mc.options != null) {
         sens = (Double)mc.options.getMouseSensitivity().getValue();
      }

      double f = sens * 0.6F + 0.2F;
      return (float)(f * f * f * 8.0);
   }

   public static float getDeltaMouse(float delta) {
      return Math.round(delta / getGCDValue());
   }
}
