package code.elixir.utils.features.aura.warp;

import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.rotations.constructor.RotateConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class TurnsConfig {
   public static TurnsConfig DEFAULT = new TurnsConfig(new LinearConstructor(), true, true);
   public static boolean moveCorrection;
   public static boolean freeCorrection;
   private final RotateConstructor angleSmooth;
   private final int resetThreshold = 1;

   public TurnsConfig(boolean moveCorrection, boolean freeCorrection) {
      this(new LinearConstructor(), moveCorrection, freeCorrection);
   }

   public TurnsConfig(boolean moveCorrection) {
      this(new LinearConstructor(), moveCorrection, true);
   }

   public TurnsConfig(RotateConstructor angleSmooth, boolean moveCorrection, boolean freeCorrection) {
      this.angleSmooth = angleSmooth;
      TurnsConfig.moveCorrection = moveCorrection;
      TurnsConfig.freeCorrection = freeCorrection;
   }

   public TurnsConstructor createRotationPlan(Turns angle, Vec3d vec, Entity entity, int reset) {
      return new TurnsConstructor(angle, vec, entity, this.angleSmooth, reset, 1.0F, moveCorrection, freeCorrection);
   }

   public TurnsConstructor createRotationPlan(Turns angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
      return new TurnsConstructor(angle, vec, entity, this.angleSmooth, 1, 1.0F, moveCorrection, freeCorrection);
   }
}
