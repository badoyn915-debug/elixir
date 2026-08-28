package code.elixir.utils.features.aura.rotations.constructor;

import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public abstract class RotateConstructor {
   private final String name;

   protected RotateConstructor(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public Turns limitAngleChange(Turns currentAngle, Turns targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, Vec3d.ZERO, null);
   }

   public abstract Turns limitAngleChange(Turns var1, Turns var2, Vec3d var3, Entity var4);

   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
