package code.elixir.features.impl.misc.creeperFarm;

import code.elixir.utils.input.MoveUtil;
import lombok.NonNull;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class WalkDirectionUtil {
   public static PlayerInput fromVec3d(Vec3d vec3d, float yaw) {
      Vec3d direction = vec3d.normalize();
      float movementAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
      return MoveUtil.getPlayerInput(angleDiff);
   }

   public static PlayerInput fromEntity(@NonNull Entity entity, float yaw) {
      if (entity == null) {
         throw new NullPointerException("entity is marked non-null but is null");
      } else {
         return fromVec3d(entity.getPos(), yaw);
      }
   }
}
