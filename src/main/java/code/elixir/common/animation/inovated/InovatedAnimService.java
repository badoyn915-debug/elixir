package code.elixir.common.animation.inovated;

import code.elixir.utils.math.calc.Calculate;

public class InovatedAnimService {
   private float value;
   private float prevValue;
   private float animationSpeed;
   private float fromValue;
   private float toValue;
   private float animationValue;

   public void update(boolean update) {
      this.prevValue = this.value;
      this.value = Calculate.clamp(this.value + (update ? this.animationSpeed : -this.animationSpeed), this.fromValue, this.toValue);
   }

   public void animate(float fromValue, float toValue, float animationSpeed, EasingList.Easing easing, float partialTicks) {
      this.animationSpeed = animationSpeed;
      this.fromValue = fromValue;
      this.toValue = toValue;
      this.animationValue = easing.ease(Calculate.interpolate(this.prevValue, this.value, partialTicks));
   }

   public void setValue(float value) {
      this.value = value;
   }

   public void setPrevValue(float prevValue) {
      this.prevValue = prevValue;
   }

   public float getValue() {
      return this.value;
   }

   public float getPrevValue() {
      return this.prevValue;
   }

   public float getAnimationValue() {
      return this.animationValue;
   }

   public void setAnimationValue(float animationValue) {
      this.animationValue = animationValue;
   }
}
