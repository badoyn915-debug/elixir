package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class MotionEvent extends EventCancellable {
   double x;
   double y;
   double z;
   float yaw;
   float pitch;
   boolean onGround;

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MotionEvent other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else if (Double.compare(this.getX(), other.getX()) != 0) {
         return false;
      } else if (Double.compare(this.getY(), other.getY()) != 0) {
         return false;
      } else if (Double.compare(this.getZ(), other.getZ()) != 0) {
         return false;
      } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
         return false;
      } else {
         return Float.compare(this.getPitch(), other.getPitch()) != 0 ? false : this.isOnGround() == other.isOnGround();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof MotionEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      long $x = Double.doubleToLongBits(this.getX());
      result = result * 59 + (int)($x >>> 32 ^ $x);
      long $y = Double.doubleToLongBits(this.getY());
      result = result * 59 + (int)($y >>> 32 ^ $y);
      long $z = Double.doubleToLongBits(this.getZ());
      result = result * 59 + (int)($z >>> 32 ^ $z);
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      result = result * 59 + Float.floatToIntBits(this.getPitch());
      return result * 59 + (this.isOnGround() ? 79 : 97);
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   @Override
   public String toString() {
      return "MotionEvent(x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", z="
         + this.getZ()
         + ", yaw="
         + this.getYaw()
         + ", pitch="
         + this.getPitch()
         + ", onGround="
         + this.isOnGround()
         + ")";
   }

   public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.onGround = onGround;
   }
}
