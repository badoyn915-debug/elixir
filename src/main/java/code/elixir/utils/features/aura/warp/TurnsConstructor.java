package code.elixir.utils.features.aura.warp;

import code.elixir.features.impl.player.AutoPilot;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.rotations.constructor.RotateConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class TurnsConstructor implements QuickImports {
   private final Turns angle;
   private final Vec3d vec3d;
   private final Entity entity;
   private final RotateConstructor angleSmooth;
   private final int ticksUntilReset;
   private final float resetThreshold;
   public final boolean moveCorrection;
   public final boolean freeCorrection;
   public final boolean changeLook = AutoPilot.getInstance() != null && AutoPilot.getInstance().isState() && AutoPilot.getInstance().target != null;

   public Turns nextRotation(Turns fromAngle, boolean isResetting) {
      return isResetting
         ? this.angleSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(mc.player.getRotationClient()))
         : this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
   }

   public Turns getAngle() {
      return this.angle;
   }

   public Vec3d getVec3d() {
      return this.vec3d;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public RotateConstructor getAngleSmooth() {
      return this.angleSmooth;
   }

   public int getTicksUntilReset() {
      return this.ticksUntilReset;
   }

   public float getResetThreshold() {
      return this.resetThreshold;
   }

   public boolean isMoveCorrection() {
      return this.moveCorrection;
   }

   public boolean isChangeLook() {
      return this.changeLook;
   }

   public TurnsConstructor(
      Turns angle,
      Vec3d vec3d,
      Entity entity,
      RotateConstructor angleSmooth,
      int ticksUntilReset,
      float resetThreshold,
      boolean moveCorrection,
      boolean freeCorrection
   ) {
      this.angle = angle;
      this.vec3d = vec3d;
      this.entity = entity;
      this.angleSmooth = angleSmooth;
      this.ticksUntilReset = ticksUntilReset;
      this.resetThreshold = resetThreshold;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
   }

   public boolean isFreeCorrection() {
      return this.freeCorrection;
   }
}
