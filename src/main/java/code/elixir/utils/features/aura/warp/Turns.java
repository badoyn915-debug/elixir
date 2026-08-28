package code.elixir.utils.features.aura.warp;

import code.elixir.main.client.Api;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Turns {
   public static Turns DEFAULT = new Turns(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public static Turns fromTargetHead(Vec3d playerPos, Vec3d targetPos, double targetHeight) {
      double headY = targetPos.y + targetHeight * 0.9;
      double deltaX = targetPos.x - playerPos.x;
      double deltaY = headY - (playerPos.y + 1.5);
      double deltaZ = targetPos.z - playerPos.z;
      float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
      yaw = MathHelper.wrapDegrees(yaw);
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float pitch = (float)Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
      pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      return new Turns(yaw, pitch);
   }

   public static void smoothResetRotation() {
      if (Api.mc.player == null) {
         TurnsConnection.INSTANCE.clear();
         TurnsConnection.INSTANCE.setRotation(null);
      } else {
         Turns currentRotation = TurnsConnection.INSTANCE.getRotation();
         if (currentRotation != null) {
            float playerYaw = Api.mc.player.getYaw();
            float playerPitch = Api.mc.player.getPitch();
            float resetYaw = 0.2F;
            float resetPitch = 0.2F;
            TurnsConnection.INSTANCE.setRotation(new Turns(resetYaw, resetPitch));
         }
      }
   }

   public Turns adjustSensitivity() {
      double gcd = Calculate.computeGcd();
      Turns previousAngle = TurnsConnection.INSTANCE.getServerAngle();
      float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
      float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
      return new Turns(adjustedYaw, MathHelper.clamp(adjustedPitch, -90.0F, 90.0F));
   }

   public Turns random(float f) {
      return new Turns(this.yaw + Calculate.getRandom(-f, f), this.pitch + Calculate.getRandom(-f, f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = axisValue - previousValue;
      return previousValue + (float)Math.round(delta / gcd) * (float)gcd;
   }

   public final Vec3d toVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public Turns addYaw(float yaw) {
      return new Turns(this.yaw + yaw, this.pitch);
   }

   public Turns addPitch(float pitch) {
      this.pitch = MathHelper.clamp(this.pitch + pitch, -90.0F, 90.0F);
      return this;
   }

   public Turns of(Turns angle) {
      return new Turns(angle.getYaw(), angle.getPitch());
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public String toString() {
      return "Turns(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public Turns(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public static class VecRotation {
      private final Turns angle;
      private final Vec3d vec;

      @Override
      public String toString() {
         return "Turns.VecRotation(angle=" + this.getAngle() + ", vec=" + this.getVec() + ")";
      }

      public Turns getAngle() {
         return this.angle;
      }

      public Vec3d getVec() {
         return this.vec;
      }

      public VecRotation(Turns angle, Vec3d vec) {
         this.angle = angle;
         this.vec = vec;
      }
   }
}
