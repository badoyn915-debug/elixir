package code.elixir.utils.display.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public final class LazyWindow {
   private Window delegate() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null ? client.getWindow() : null;
   }

   public int getScaledWidth() {
      Window window = this.delegate();
      return window != null ? window.getScaledWidth() : 0;
   }

   public int getScaledHeight() {
      Window window = this.delegate();
      return window != null ? window.getScaledHeight() : 0;
   }

   public int getWidth() {
      Window window = this.delegate();
      return window != null ? window.getWidth() : 0;
   }

   public int getHeight() {
      Window window = this.delegate();
      return window != null ? window.getHeight() : 0;
   }

   public long getHandle() {
      Window window = this.delegate();
      return window != null ? window.getHandle() : 0L;
   }

   public double getScaleFactor() {
      Window window = this.delegate();
      return window != null ? window.getScaleFactor() : 1.0;
   }

   public boolean isFullscreen() {
      Window window = this.delegate();
      return window != null && window.isFullscreen();
   }

   public void toggleFullscreen() {
      Window window = this.delegate();
      if (window != null) {
         window.toggleFullscreen();
      }
   }

   public void setVsync(boolean enabled) {
      Window window = this.delegate();
      if (window != null) {
         window.setVsync(enabled);
      }
   }
}
