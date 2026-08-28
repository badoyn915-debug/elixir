package code.elixir.features.impl.combat.components.interpolation;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.rotate.RotationUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class HitChance implements QuickImports {
   public static float get(LivingEntity target) {
      if (target != null && mc.player != null) {
         Box box = target.getBoundingBox();
         Vec3d center = box.getCenter();
         Vec2f rot = RotationUtils.getRotations(center);
         float diffYaw = Math.abs(MathHelper.wrapDegrees(rot.x - mc.player.getYaw()));
         float diffPitch = Math.abs(MathHelper.wrapDegrees(rot.y - mc.player.getPitch()));
         float ang = (float)Math.hypot(diffYaw, diffPitch);
         float dist = mc.player.distanceTo(target);
         float angScore = MathHelper.clamp(1.0F - ang / 70.0F, 0.0F, 1.0F);
         float distScore = MathHelper.clamp(1.0F - (dist - 3.0F) / 4.0F, 0.0F, 1.0F);
         return angScore * 0.75F + distScore * 0.25F;
      } else {
         return 0.0F;
      }
   }
}
