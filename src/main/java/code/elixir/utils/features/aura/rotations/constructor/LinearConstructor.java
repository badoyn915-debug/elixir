package code.elixir.utils.features.aura.rotations.constructor;

import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LinearConstructor extends RotateConstructor {
   public LinearConstructor() {
      super("Linear");
   }

   @Override
   public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
      Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawStep = MathHelper.clamp(delta.getYaw(), -180.0F, 180.0F);
      float pitchStep = MathHelper.clamp(delta.getPitch(), -90.0F, 90.0F);
      return new Turns(currentAngle.getYaw() + yawStep, currentAngle.getPitch() + pitchStep);
   }
}
