package code.elixir.events.render;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;

public class FogEvent extends EventCancellable {
   private float distance;
   private float density;
   private int color;

   public float getDistance() {
      return this.distance;
   }

   public float getDensity() {
      return this.density;
   }

   public int getColor() {
      return this.color;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public void setDensity(float density) {
      this.density = density;
   }

   public void setColor(int color) {
      this.color = color;
   }
}
