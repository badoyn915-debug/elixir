package code.elixir.utils.display.color;

import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

public final class ColorAssist {
   public static final int green = -12517568;
   public static final int yellow = -192;
   public static final int orange = -32736;
   public static final int red = -49088;
   private static final ThreadLocal<ByteBuffer> PIXEL_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(4));
   public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");
   public static Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   public static final int RED = getColor(255, 0, 0);
   public static final int GREEN = getColor(0, 255, 0);
   public static final int BLUE = getColor(0, 0, 255);
   public static final int YELLOW = getColor(255, 255, 0);
   public static final int WHITE = getColor(255);
   public static final int BLACK = getColor(0);
   public static final int HALF_BLACK = getColor(0, 0.5F);
   public static final int LIGHT_RED = getColor(255, 85, 85);

   public static int colorForRectsCustom$() {
      return -10797100;
   }

   public static int colorForRectsBlack$() {
      return -15066598;
   }

   public static int colorForTextWhite$() {
      return -1;
   }

   public static int colorForTextCustom$() {
      return -8231726;
   }

   public static int clampSaturation(int color, float min, float max) {
      float[] hsb = Color.RGBtoHSB(getRed(color), getGreen(color), getBlue(color), new float[3]);
      return Color.HSBtoRGB(hsb[0], Math.clamp(hsb[1], min, max), hsb[2]);
   }

   public static int red(int c) {
      return c >> 16 & 0xFF;
   }

   public static int green(int c) {
      return c >> 8 & 0xFF;
   }

   public static int blue(int c) {
      return c & 0xFF;
   }

   public static int alpha(int c) {
      return c >> 24 & 0xFF;
   }

   public static float redf(int c) {
      return red(c) / 255.0F;
   }

   public static float greenf(int c) {
      return green(c) / 255.0F;
   }

   public static float bluef(int c) {
      return blue(c) / 255.0F;
   }

   public static float alphaf(int c) {
      return alpha(c) / 255.0F;
   }

   public static int[] getRGBA(int c) {
      return new int[]{red(c), green(c), blue(c), alpha(c)};
   }

   public static int[] getRGB(int c) {
      return new int[]{red(c), green(c), blue(c)};
   }

   public static float[] getRGBAf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
   }

   public static float[] getRGBf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c)};
   }

   public static int multSaturation(int color, float saturation) {
      float[] hsb = Color.RGBtoHSB(getRed(color), getGreen(color), getBlue(color), new float[3]);
      return Color.HSBtoRGB(hsb[0], Calculate.clamp(hsb[1] + saturation, 0.0F, 1.0F), hsb[2]);
   }

   public static int getColor(float red, float green, float blue, float alpha) {
      return getColor(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
   }

   public static int getColor(int red, int green, int blue, float alpha) {
      return getColor(red, green, blue, Math.round(alpha * 255.0F));
   }

   public static int getColor(float red, float green, float blue) {
      return getColor(red, green, blue, 1.0F);
   }

   public static int getColor(int brightness, int alpha) {
      return getColor(brightness, brightness, brightness, alpha);
   }

   public static int getColor(int brightness, float alpha) {
      return getColor(brightness, Math.round(alpha * 255.0F));
   }

   public static int getColor(int brightness) {
      return getColor(brightness, brightness, brightness);
   }

   public static int replAlpha(int color, int alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int replAlpha(int color, float alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int multAlpha(int color, float percent01) {
      return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
   }

   public static int applyOpacity(int hex, int percent) {
      return applyOpacity(hex, 2.55F * Math.min(percent, 100));
   }

   public static int applyOpacity(int hex, float opacity) {
      return ColorHelper.getArgb(
         (int)(ColorHelper.getAlpha(hex) * (opacity / 255.0F)), ColorHelper.getRed(hex), ColorHelper.getGreen(hex), ColorHelper.getBlue(hex)
      );
   }

   public static int lerp(float value, int from, int to) {
      return ColorHelper.lerp(value, from, to);
   }

   public static int pixelColor(int x, int y) {
      ByteBuffer byteBuffer = PIXEL_BUFFER.get();
      byteBuffer.clear();
      GL11.glReadPixels(x, y, 1, 1, 6408, 5121, byteBuffer);
      return ColorHelper.getArgb(getRed(byteBuffer.get(0)), getGreen(byteBuffer.get(1)), getBlue(byteBuffer.get(2)));
   }

   public static int calculateHuyDegrees(int divisor, int offset) {
      long currentTime = System.currentTimeMillis();
      long calculatedValue = (currentTime / divisor + offset) % 360L;
      return (int)calculatedValue;
   }

   public static int reAlphaInt(int color, int alpha) {
      return Math.clamp((long)alpha, 0, 255) << 24 | color & 16777215;
   }

   public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
      float hueStep = 90.0F;
      float basaHuy = calculateHuyDegrees(speed, index);
      float huy = (basaHuy + index * hueStep) % 360.0F;
      saturation = Math.clamp(saturation, 0.0F, 1.0F);
      brightness = Math.clamp(brightness, 0.0F, 1.0F);
      float var12;
      int rgb = Color.HSBtoRGB(var12 = huy / 360.0F, saturation, brightness);
      int Ialpha = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
      return reAlphaInt(rgb, Ialpha);
   }

   public static int rgb(int r, int g, int b) {
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static void setAlphaColor(int color, float alpha) {
      float red = (color >> 16 & 0xFF) / 255.0F;
      float green = (color >> 8 & 0xFF) / 255.0F;
      float blue = (color & 0xFF) / 255.0F;
      RenderSystem.setShaderColor(red, green, blue, alpha);
   }

   public static void setColor(int color) {
      setAlphaColor(color, (color >> 24 & 0xFF) / 255.0F);
   }

   public static int toColor(String hexColor) {
      int argb = Integer.parseInt(hexColor.substring(1), 16);
      return setAlpha(argb, 255);
   }

   public static int setAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   public static int gradient(int start, int end, int index, int speed) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      int color = interpolate(start, end, Math.clamp(angle / 180.0F - 1.0F, 0.0F, 1.0F));
      float[] hs = rgba(color);
      float[] hsb = Color.RGBtoHSB((int)(hs[0] * 255.0F), (int)(hs[1] * 255.0F), (int)(hs[2] * 255.0F), null);
      hsb[1] *= 1.5F;
      hsb[1] = Math.min(hsb[1], 1.0F);
      return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
   }

   public static int gradient(int speed, int index, int... colors) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      int colorIndex = (int)(angle / 360.0F * colors.length);
      if (colorIndex == colors.length) {
         colorIndex--;
      }

      int color1 = colors[colorIndex];
      int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
      return interpolate(color1, color2, angle / 360.0F * colors.length - colorIndex);
   }

   public static double interpolateD(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return (int)(oldValue + (newValue - oldValue) * interpolationValue);
   }

   public static int getRed(int hex) {
      return hex >> 16 & 0xFF;
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 0xFF;
   }

   public static int getBlue(int hex) {
      return hex & 0xFF;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 0xFF;
   }

   public static int multColor(int colorStart, int colorEnd, float progress) {
      return getColor(
         Math.round(red(colorStart) * (redf(colorEnd) * progress)),
         Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
         Math.round(blue(colorStart) * (bluef(colorEnd) * progress)),
         Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress))
      );
   }

   public static int multRed(int colorStart, int colorEnd, float progress) {
      return getColor(
         Math.round(red(colorStart) * (redf(colorEnd) * progress)),
         Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
         Math.round(blue(colorStart) * (bluef(colorEnd) * progress)),
         Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress))
      );
   }

   public static int multDark(int color, float percent01) {
      return getColor(Math.round(red(color) * percent01), Math.round(green(color) * percent01), Math.round(blue(color) * percent01), alpha(color));
   }

   public static int multBright(int color, float percent01) {
      return getColor(
         Math.min(255, Math.round(red(color) / percent01)),
         Math.min(255, Math.round(green(color) / percent01)),
         Math.min(255, Math.round(blue(color) / percent01)),
         alpha(color)
      );
   }

   public static int overCol(int color1, int color2, float percent01) {
      float percent = MathHelper.clamp(percent01, 0.0F, 1.0F);
      return getColor(
         MathHelper.lerp(percent, red(color1), red(color2)),
         MathHelper.lerp(percent, green(color1), green(color2)),
         MathHelper.lerp(percent, blue(color1), blue(color2)),
         MathHelper.lerp(percent, alpha(color1), alpha(color2))
      );
   }

   public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
      return new Vector4i(
         multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha)
      );
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(red(color), Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
   }

   public static int multRed(int color, float percent01) {
      return getColor(red(color), Math.min(255, Math.round(green(color) / percent01)), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
   }

   public static int multGreen(int color, float percent01) {
      return getColor(Math.min(255, Math.round(green(color) / percent01)), green(color), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
   }

   public static int rgba(int red, int green, int blue, int alpha) {
      return getColor(red, green, blue, alpha);
   }

   public static int[] genGradientForText(int color1, int color2, int length) {
      int[] gradient = new int[length];

      for (int i = 0; i < length; i++) {
         float pc = (float)i / (length - 1);
         gradient[i] = overCol(color1, color2, pc);
      }

      return gradient;
   }

   public static float[] rgba(int color) {
      return new float[]{(color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F};
   }

   public static int interpolate(int start, int end, float value) {
      float[] startColor = rgba(start);
      float[] endColor = rgba(end);
      return rgba(
         (int)Calculate.interpolate(startColor[0] * 255.0F, endColor[0] * 255.0F, value),
         (int)Calculate.interpolate(startColor[1] * 255.0F, endColor[1] * 255.0F, value),
         (int)Calculate.interpolate(startColor[2] * 255.0F, endColor[2] * 255.0F, value),
         (int)Calculate.interpolate(startColor[3] * 255.0F, endColor[3] * 255.0F, value)
      );
   }

   public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      float hue = angle / 360.0F;
      int color = Color.HSBtoRGB(hue, saturation, brightness);
      return getColor(red(color), green(color), blue(color), Math.round(opacity * 255.0F));
   }

   public static int fade(int speed, int index, int first, int second) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = angle >= 180 ? 360 - angle : angle;
      return overCol(first, second, angle / 180.0F);
   }

   public static int rgbaFloat(float r, float g, float b, float a) {
      return (int)(MathHelper.clamp(a, 0.0F, 1.0F) * 255.0F) << 24
         | (int)(MathHelper.clamp(r, 0.0F, 1.0F) * 255.0F) << 16
         | (int)(MathHelper.clamp(g, 0.0F, 1.0F) * 255.0F) << 8
         | (int)(MathHelper.clamp(b, 0.0F, 1.0F) * 255.0F);
   }

   public static int fade(int index) {
      return fade(8, index, getClientColor(), -1);
   }

   public static int getRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float t = (float)(Math.sin(time / 500.0 * 0.001) + 1.0) / 2.0F;
      return interpolateColor(color1, color2, t);
   }

   public static int getDynamicRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float speed = 0.5F;
      float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0F;
      t = t * t * (3.0F - 2.0F * t);
      return interpolateColor(color1, color2, t);
   }

   public static int getTrueRainbowColor() {
      long time = System.currentTimeMillis();
      float speed = 0.5F;
      float hue = (float)time / speed % 360.0F;
      return Color.HSBtoRGB(hue / 360.0F, 0.8F, 1.0F);
   }

   public static int getGradientRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float speed = 0.5F;
      float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0F;
      float rainbowMod = (float)(Math.sin((float)time / speed * 2.0F) * 0.1F + 0.9F);
      t *= rainbowMod;
      t = MathHelper.clamp(t, 0.0F, 1.0F);
      return interpolateColor(color1, color2, t);
   }

   public static int getSmoothRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float speed = 0.5F;
      float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0F;
      t = t * t * (3.0F - 2.0F * t);
      return interpolateColor(color1, color2, t);
   }

   public static int getSimpleRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float speed = 500.0F;
      float t = (float)(Math.sin((float)time / (speed * 0.1F)) + 1.0) / 2.0F;
      return interpolateColor(color1, color2, t);
   }

   public static int getTrueRainbowColor(int color1, int color2) {
      return fade(5, 0, color1, color2);
   }

   public static int getFastRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float hue = (float)time / 10.0F % 360.0F;
      return Color.HSBtoRGB(hue / 360.0F, 0.8F, 1.0F);
   }

   public static int getDebugRainbowColor(int color1, int color2) {
      long time = System.currentTimeMillis();
      float speed = 0.5F;
      float t = (float)(Math.sin((float)time / (speed * 100.0F)) + 1.0) / 2.0F;
      if (time % 1000L < 50L) {
         System.out.println("Rainbow t: " + t + ", speed: " + speed + ", time: " + time);
      }

      return interpolateColor(color1, color2, t);
   }

   public static int interpolateColor(int colorStart, int colorEnd, float t) {
      float startAlpha = (colorStart >> 24 & 0xFF) / 255.0F;
      float startRed = (colorStart >> 16 & 0xFF) / 255.0F;
      float startGreen = (colorStart >> 8 & 0xFF) / 255.0F;
      float startBlue = (colorStart & 0xFF) / 255.0F;
      float endAlpha = (colorEnd >> 24 & 0xFF) / 255.0F;
      float endRed = (colorEnd >> 16 & 0xFF) / 255.0F;
      float endGreen = (colorEnd >> 8 & 0xFF) / 255.0F;
      float endBlue = (colorEnd & 0xFF) / 255.0F;
      float alpha = startAlpha + t * (endAlpha - startAlpha);
      float red = startRed + t * (endRed - startRed);
      float green = startGreen + t * (endGreen - startGreen);
      float blue = startBlue + t * (endBlue - startBlue);
      return (int)(alpha * 255.0F) << 24 | (int)(red * 255.0F) << 16 | (int)(green * 255.0F) << 8 | (int)(blue * 255.0F);
   }

   public static Vector4i roundClientColor(float alpha) {
      return new Vector4i(multAlpha(fade(270), alpha), multAlpha(fade(0), alpha), multAlpha(fade(180), alpha), multAlpha(fade(90), alpha));
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24
         | MathHelper.clamp(red, 0, 255) << 16
         | MathHelper.clamp(green, 0, 255) << 8
         | MathHelper.clamp(blue, 0, 255);
   }

   public static int getColor(int red, int green, int blue) {
      return getColor(red, green, blue, 255);
   }

   private static int computeColor(int red, int green, int blue, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24
         | MathHelper.clamp(red, 0, 255) << 16
         | MathHelper.clamp(green, 0, 255) << 8
         | MathHelper.clamp(blue, 0, 255);
   }

   private static String generateKey(int red, int green, int blue, int alpha) {
      return red + "," + green + "," + blue + "," + alpha;
   }

   public static String formatting(int color) {
      return "⏏" + color + "⏏";
   }

   public static String removeFormatting(String text) {
      return text != null && !text.isEmpty() ? FORMATTING_CODE_PATTERN.matcher(text).replaceAll("") : null;
   }

   public static int getMainGuiColor() {
      return -15461352;
   }

   public static int getGuiRectColor(float alpha) {
      return multAlpha(-15066593, alpha);
   }

   public static int getGuiRectColor2(float alpha) {
      return multAlpha(-14803418, alpha);
   }

   public static int getRect(float alpha) {
      return multAlpha(-469762048, alpha);
   }

   public static int getRectDarker(float alpha) {
      return multAlpha(-15198178, alpha);
   }

   public static int getText(float alpha) {
      return multAlpha(-1, alpha);
   }

   public static int getText() {
      return -1;
   }

   public static int getClientColor() {
      return ThemeManager.primaryColor.getColor();
   }

   public static int getClientColor2() {
      return ThemeManager.secondaryColor.getColor();
   }

   public static int getClientColor2(float alpha) {
      return multAlpha(ThemeManager.secondaryColor.getColor(), alpha);
   }

   public static int getClientColor(float alpha) {
      return multAlpha(getClientColor(), alpha);
   }

   public static int getHudColor() {
      return ThemeManager.primaryColor.getColor();
   }

   public static int getHudColor(float alpha) {
      return multAlpha(getHudColor(), alpha);
   }

   public static int getHudColor2() {
      return ThemeManager.secondaryColor.getColor();
   }

   public static int getHudColor2(float alpha) {
      return multAlpha(getHudColor2(), alpha);
   }

   public static int getFriendColor() {
      return -11141291;
   }

   public static int getOutline(float alpha, float bright) {
      return multBright(multAlpha(getOutline(), alpha), bright);
   }

   public static int getOutline(float alpha) {
      return multAlpha(getOutline(), alpha);
   }

   public static int getOutline() {
      return -13158586;
   }

   private ColorAssist() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   private static class CacheEntry implements Delayed {
      private final ColorAssist.ColorKey key;
      private final int color;
      private final long expirationTime;

      CacheEntry(ColorAssist.ColorKey key, int color, long ttl) {
         this.key = key;
         this.color = color;
         this.expirationTime = System.currentTimeMillis() + ttl;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         long delay = this.expirationTime - System.currentTimeMillis();
         return unit.convert(delay, TimeUnit.MILLISECONDS);
      }

      public int compareTo(Delayed other) {
         return other instanceof ColorAssist.CacheEntry ? Long.compare(this.expirationTime, ((ColorAssist.CacheEntry)other).expirationTime) : 0;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > this.expirationTime;
      }

      public ColorAssist.ColorKey getKey() {
         return this.key;
      }

      public int getColor() {
         return this.color;
      }

      public long getExpirationTime() {
         return this.expirationTime;
      }
   }

   private static class ColorKey {
      final int red;
      final int green;
      final int blue;
      final int alpha;

      public int getRed() {
         return this.red;
      }

      public int getGreen() {
         return this.green;
      }

      public int getBlue() {
         return this.blue;
      }

      public int getAlpha() {
         return this.alpha;
      }

      public ColorKey(int red, int green, int blue, int alpha) {
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alpha = alpha;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof ColorAssist.ColorKey other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (this.getRed() != other.getRed()) {
            return false;
         } else if (this.getGreen() != other.getGreen()) {
            return false;
         } else {
            return this.getBlue() != other.getBlue() ? false : this.getAlpha() == other.getAlpha();
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof ColorAssist.ColorKey;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         result = result * 59 + this.getRed();
         result = result * 59 + this.getGreen();
         result = result * 59 + this.getBlue();
         return result * 59 + this.getAlpha();
      }
   }

   public static class IntColor {
      public static float[] rgb(int color) {
         return new float[]{(color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F};
      }

      public static int rgba(int r, int g, int b, int a) {
         return a << 24 | r << 16 | g << 8 | b;
      }

      public static int rgb(int r, int g, int b) {
         return 0xFF000000 | r << 16 | g << 8 | b;
      }

      public static int getRed(int hex) {
         return hex >> 16 & 0xFF;
      }

      public static int getGreen(int hex) {
         return hex >> 8 & 0xFF;
      }

      public static int getBlue(int hex) {
         return hex & 0xFF;
      }

      public static int getAlpha(int hex) {
         return hex >> 24 & 0xFF;
      }
   }
}
