package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import java.util.function.Supplier;

public class ColorSetting extends Setting {
   private float hue = 0.0F;
   private float saturation = 1.0F;
   private float brightness = 1.0F;
   private float alpha = 1.0F;
   private int[] presets = new int[0];

   public ColorSetting(String name, String description) {
      super(name, description);
   }

   public ColorSetting value(int value) {
      this.setColor(value);
      return this;
   }

   public ColorSetting presets(int... presets) {
      this.presets = presets;
      return this;
   }

   public ColorSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public int getColor() {
      return this.getColorWithAlpha() & 16777215 | Math.round(this.alpha * 255.0F) << 24;
   }

   public int getColorWithAlpha() {
      return Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
   }

   public ColorSetting setColor(int color) {
      float[] hsb = Color.RGBtoHSB(Calculate.getRed(color), Calculate.getGreen(color), Calculate.getBlue(color), null);
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alpha = Calculate.getAlpha(color) / 255.0F;
      return this;
   }

   public void setHue(float hue) {
      this.hue = hue;
   }

   public void setSaturation(float saturation) {
      this.saturation = saturation;
   }

   public void setBrightness(float brightness) {
      this.brightness = brightness;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public float getHue() {
      return this.hue;
   }

   public float getSaturation() {
      return this.saturation;
   }

   public float getBrightness() {
      return this.brightness;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public int[] getPresets() {
      return this.presets;
   }

   public void setPresets(int[] presets) {
      this.presets = presets;
   }
}
