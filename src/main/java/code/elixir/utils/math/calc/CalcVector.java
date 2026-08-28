package code.elixir.utils.math.calc;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CalcVector {
   public static Vec3d lerpPosition(Entity entity) {
      float tickDelta = QuickImports.mc.getRenderTickCounter().getTickDelta(true);
      return new Vec3d(
         entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
         entity.prevY + (entity.getY() - entity.prevY) * tickDelta,
         entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
      );
   }
}
