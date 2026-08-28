package code.elixir.display.screens.clickgui.animation;

import net.minecraft.util.math.MathHelper;

public class CloseAnimationController {
   public static final long DEFAULT_DURATION = 220L;
   private boolean closing = false;
   private long startTime = 0L;
   private long duration = DEFAULT_DURATION;
   private float progress = 1.0F;
   private float scale = 1.0F;
   private float alpha = 1.0F;
   private float offsetX = 0.0F;
   private float offsetY = 0.0F;

   public void reset() {
      this.closing = false;
      this.startTime = 0L;
      this.duration = DEFAULT_DURATION;
      this.progress = 1.0F;
      this.scale = 1.0F;
      this.alpha = 1.0F;
      this.offsetX = 0.0F;
      this.offsetY = 0.0F;
   }

   public void startClosing(float initialYaw, float initialPitch) {
      this.startClosing(DEFAULT_DURATION);
   }

   public void startClosing(long durationMs) {
      this.closing = true;
      this.duration = Math.max(50L, durationMs);
      this.startTime = System.currentTimeMillis();
      this.progress = 1.0F;
      this.scale = 1.0F;
      this.alpha = 1.0F;
      this.offsetX = 0.0F;
      this.offsetY = 0.0F;
   }

   public void updateCamera(float currentYaw, float currentPitch) {
      // Kept for backward compatibility if called
   }

   public void update() {
      if (this.closing) {
         long currentTime = System.currentTimeMillis();
         long elapsed = currentTime - this.startTime;
         float rawProgress = MathHelper.clamp(1.0F - (float)elapsed / (float)this.duration, 0.0F, 1.0F);
         
         // Smooth cubic easing curve: reverse cubic-out / smooth deceleration
         float eased = (float)Math.pow(rawProgress, 2.2);
         this.progress = eased;
         this.alpha = MathHelper.clamp(eased, 0.0F, 1.0F);
         // Scale smoothly from 100% down to 95%
         this.scale = 0.95F + 0.05F * eased;
         this.offsetX = 0.0F;
         this.offsetY = 0.0F;

         if (elapsed >= this.duration) {
            this.closing = false;
            this.alpha = 0.0F;
            this.scale = 0.95F;
            this.progress = 0.0F;
         }
      }
   }

   public boolean isFinished() {
      return !this.closing || this.progress <= 0.001F || this.alpha <= 0.001F;
   }

   public boolean isClosing() {
      return this.closing;
   }

   public float getProgress() {
      return this.progress;
   }

   public float getOffsetX() {
      return this.offsetX;
   }

   public float getOffsetY() {
      return this.offsetY;
   }

   public float getScale() {
      return this.scale;
   }

   public float getAlpha() {
      return this.alpha;
   }
}

