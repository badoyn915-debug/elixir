package code.elixir.utils.client.stealth;

import code.elixir.features.impl.combat.HitBox;
import code.elixir.features.impl.combat.ProjectHelper;
import code.elixir.features.impl.combat.TargetPearl;
import code.elixir.features.impl.misc.FreeCam;
import code.elixir.features.impl.movement.AirStuck;
import code.elixir.features.impl.movement.ElytraMotion;
import code.elixir.features.impl.movement.Flight;
import code.elixir.features.impl.movement.NoSlow;
import code.elixir.features.impl.movement.Speed;
import code.elixir.features.impl.movement.Spider;
import code.elixir.features.impl.movement.Strafe;
import code.elixir.features.impl.movement.TargetStrafe;
import code.elixir.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.elixir.features.module.Module;
import code.elixir.utils.client.Instance;
import code.elixir.utils.features.aura.AuraMixinHelper;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.aura.warp.TurnsConstructor;

public final class MovementMixinGuard {
   public static boolean shouldUseEntityMovementPhysics() {
      return AuraMixinHelper.isCombatActive()
         || isActive(Flight.class)
         || isActive(Speed.class)
         || isActive(Spider.class)
         || isActive(TargetStrafe.class)
         || isActive(Strafe.class)
         || isActive(ElytraMotion.class)
         || isActive(AirStuck.class)
         || isActive(FreeCam.class);
   }

   public static boolean shouldHandleVelocityStrafe() {
      if (AuraMixinHelper.isCombatActive()) {
         return true;
      } else {
         TurnsConstructor rotationPlan = TurnsConnection.INSTANCE.getCurrentRotationPlan();
         return rotationPlan != null && rotationPlan.isMoveCorrection();
      }
   }

   public static boolean isHitBoxActive() {
      return isActive(HitBox.class);
   }

   public static boolean shouldHandleMoveEvent() {
      return isActive(Flight.class) || isActive(AirStuck.class) || isActive(FreeCam.class) || isActive(AutoMystGliderModule.class);
   }

   public static boolean shouldHandleMotionEvent() {
      return isActive(NoSlow.class) || isActive(AirStuck.class) || isActive(ProjectHelper.class);
   }

   public static boolean shouldHandlePostMotionEvent() {
      return true;
   }

   public static boolean shouldHandleNoSlowRedirect() {
      return isActive(NoSlow.class);
   }

   private static boolean isActive(Class<? extends Module> clazz) {
      Module module = Instance.get(clazz);
      return module != null && module.isState();
   }

   private MovementMixinGuard() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
