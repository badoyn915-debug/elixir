package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.function.Supplier;

public class SliderSettings extends Setting {
   private float value;
   private float min;
   private float max;
   private float step = 0.01F;
   private boolean integer;
   private boolean barOnly;

   public SliderSettings(String name, String description) {
      super(name, description);
   }

   public SliderSettings range(float min, float max) {
      this.min = min;
      this.max = max;
      return this;
   }

   public SliderSettings range(int min, int max) {
      this.min = min;
      this.max = max;
      this.integer = true;
      return this;
   }

   public int getInt() {
      return (int)this.value;
   }

   public SliderSettings visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public SliderSettings step(float step) {
      this.step = step;
      return this;
   }

   public SliderSettings barOnly() {
      this.barOnly = true;
      return this;
   }

   public boolean isBarOnly() {
      return this.barOnly;
   }

   public float getValue() {
      return this.value;
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public float getStep() {
      return this.step;
   }

   public boolean isInteger() {
      return this.integer;
   }

   public SliderSettings setValue(float value) {
      this.value = value;
      return this;
   }

   public SliderSettings setMin(float min) {
      this.min = min;
      return this;
   }

   public SliderSettings setMax(float max) {
      this.max = max;
      return this;
   }

   public SliderSettings setStep(float step) {
      this.step = step;
      return this;
   }

   public SliderSettings setInteger(boolean integer) {
      this.integer = integer;
      return this;
   }

   public SliderSettings setBarOnly(boolean barOnly) {
      this.barOnly = barOnly;
      return this;
   }
}
