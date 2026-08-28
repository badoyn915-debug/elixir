package code.elixir.features.impl.movement.autoMystGlider.states;

import code.elixir.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record GlideState(Vec3d vec3d) implements QuickImports {
   public boolean done() {
      if (mc.player == null) {
         return false;
      } else {
         Vec3d curPos = mc.player.getPos();
         Vec3d delta = this.vec3d.subtract(curPos);
         return delta.horizontalLengthSquared() < 3.0;
      }
   }

   public boolean horizontalGlide() {
      if (mc.player == null) {
         return false;
      } else {
         Vec3d curPos = mc.player.getPos();
         Vec3d delta = this.vec3d.subtract(curPos);
         return !this.verticalGlide() && delta.horizontalLengthSquared() > 3.0;
      }
   }

   public boolean verticalGlide() {
      if (mc.player == null) {
         return false;
      } else {
         Vec3d curPos = mc.player.getPos();
         Vec3d delta = this.vec3d.subtract(curPos);
         return delta.getY() > -130.0 && !this.done();
      }
   }

   public boolean solidBlockInRadius(int radius) {
      BlockPos blockPos = BlockPos.ofFloored(mc.player.getPos());

      for (int i = 0; i < radius; i++) {
         BlockPos blockPos1 = blockPos.withY(blockPos.down().getY() - i);
         if (!mc.player.getWorld().getBlockState(blockPos1).isAir()) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldLand() {
      return mc.player != null && mc.world != null ? !this.solidBlockInRadius(2) : false;
   }

   public boolean fullyDone() {
      return this.vec3d.getY() - mc.player.getPos().getY() > 3.0;
   }

   public PlayerInput selectInput() {
      return WalkDirectionUtil.fromVec3d(this.vec3d.subtract(mc.player.getPos()), mc.player.getYaw());
   }
}
