package code.elixir.features.impl.movement.autoMystGlider.handers;

import code.elixir.events.player.MoveEvent;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

final class InternalNoclipFly implements QuickImports {
   private static final float SPEED = 0.45F;
   private static final InternalNoclipFly INSTANCE = new InternalNoclipFly();

   private InternalNoclipFly() {
   }

   static InternalNoclipFly get() {
      return INSTANCE;
   }

   void onTick() {
      if (mc.player != null) {
         mc.player.noClip = true;
         mc.player.fallDistance = 0.0F;
      }
   }

   void onMove(MoveEvent moveEvent) {
      if (mc.player != null) {
         boolean sneak = mc.options.sneakKey.isPressed();
         boolean jump = mc.options.jumpKey.isPressed();
         double y = moveEvent.getMovement().y;
         if (sneak) {
            y = -0.45F;
         } else if (jump) {
            y = 0.45F;
         }

         float yaw = mc.player.getYaw();
         float forward = mc.player.input.movementForward;
         float strafe = mc.player.input.movementSideways;
         double x = 0.0;
         double z = 0.0;
         if (forward != 0.0F || strafe != 0.0F) {
            float yawRad = yaw * (float) (Math.PI / 180.0);
            x = -MathHelper.sin(yawRad) * 0.45F * forward + MathHelper.cos(yawRad) * 0.45F * strafe;
            z = MathHelper.cos(yawRad) * 0.45F * forward + MathHelper.sin(yawRad) * 0.45F * strafe;
         }

         moveEvent.setMovement(new Vec3d(x, y, z));
         mc.player.noClip = true;
         mc.player.fallDistance = 0.0F;
      }
   }

   void reset() {
      if (mc.player != null) {
         mc.player.noClip = false;
      }
   }
}
