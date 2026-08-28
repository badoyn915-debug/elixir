package code.elixir.features.impl.combat.components.interpolation;

import code.elixir.features.impl.combat.CombatModules;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.combat.PredictUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public final class PredictRots implements QuickImports {
   public static Vec2f rotating(Vec2f rotation, LivingEntity target) {
      Vec3d vec = calcPointed(target);
      float rawYaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
      float rawPitch = (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
      float yawDelta = MathHelper.wrapDegrees(rawYaw - rotation.x);
      float pitchDelta = MathHelper.wrapDegrees(rawPitch - rotation.y);
      if (Math.abs(yawDelta) > 180.0F) {
         yawDelta -= Math.signum(yawDelta) * 360.0F;
      }

      float additionYaw = MathHelper.clamp(yawDelta, -180.0F, 180.0F);
      float additionPitch = MathHelper.clamp(pitchDelta, -90.0F, 90.0F);
      float yaw = rotation.x + additionYaw;
      float pitch = rotation.y + additionPitch;
      float yawFinal = GCDUtil.getFixedRotation(yaw);
      float pitchFinal = GCDUtil.getFixedRotation(pitch);
      return new Vec2f(yawFinal, pitchFinal);
   }

   public static Vec3d calcPointed(LivingEntity target) {
      if (target != null) {
         Vec3d vecPosition = getPredictedPoint(target, target.getBoundingBox().getCenter());
         return new Vec3d(vecPosition.getX() - mc.player.getX(), vecPosition.getY() - mc.player.getY(), vecPosition.getZ() - mc.player.getZ());
      } else {
         return Vec3d.ZERO;
      }
   }

   public static Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
      return mc.player != null && target != null && mc.player.isGliding() && target.isGliding() && CombatModules.isElytraPredictActive()
         ? PredictUtils.bypasselytrahacking(target)
         : point;
   }

   private PredictRots() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
