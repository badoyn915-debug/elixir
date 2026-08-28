package code.elixir.display.hud;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.util.math.MatrixStack;

public final class TimerTextAnimator {
   private static final long ANIM_MS = 170L;
   private static final float SLIDE_PX = 5.0F;
   private static final Map<String, TimerTextAnimator.Entry> ENTRIES = new HashMap<>();
   private static final ThreadLocal<char[]> CHAR_BUF = ThreadLocal.withInitial(() -> new char[1]);

   private TimerTextAnimator() {
   }

   private static String singleCharString(char c) {
      char[] buf = CHAR_BUF.get();
      buf[0] = c;
      return new String(buf);
   }

   public static float getAnimatedWidth(FontRenderer font, String key, String text) {
      TimerTextAnimator.Entry entry = ENTRIES.get(key);
      String previous = entry != null ? entry.previous : text;
      String current = entry != null ? entry.current : text;
      if (previous.length() != current.length()) {
         return font.getStringWidth(current);
      } else {
         int maxLen = Math.max(previous.length(), current.length());
         float width = 0.0F;

         for (int i = 0; i < maxLen; i++) {
            char oldChar = i < previous.length() ? previous.charAt(i) : 0;
            char newChar = i < current.length() ? current.charAt(i) : 0;
            float oldW = oldChar == 0 ? 0.0F : font.getStringWidth(singleCharString(oldChar));
            float newW = newChar == 0 ? 0.0F : font.getStringWidth(singleCharString(newChar));
            width += Math.max(oldW, newW);
         }

         return width;
      }
   }

   public static void draw(FontRenderer font, MatrixStack matrix, String key, String text, float x, float y, int color) {
      long now = System.currentTimeMillis();
      TimerTextAnimator.Entry entry = ENTRIES.computeIfAbsent(key, k -> new TimerTextAnimator.Entry(text));
      if (!text.equals(entry.current)) {
         entry.previous = text.length() != entry.current.length() ? text : entry.current;
         entry.current = text;
         entry.changedAt = now;
      }

      entry.lastUse = now;
      cleanup(now);
      String previous = entry.previous;
      String current = entry.current;
      if (previous.length() != current.length()) {
         font.drawString(matrix, current, x, y, color);
      } else {
         float progress = Math.min(1.0F, (float)(now - entry.changedAt) / 170.0F);
         if (progress >= 1.0F) {
            font.drawString(matrix, current, x, y, color);
         } else {
            int baseAlpha = color >>> 24 & 0xFF;
            if (baseAlpha == 0) {
               baseAlpha = 255;
            }

            int maxLen = Math.max(previous.length(), current.length());
            float cursor = x;

            for (int i = 0; i < maxLen; i++) {
               char oldChar = i < previous.length() ? previous.charAt(i) : 0;
               char newChar = i < current.length() ? current.charAt(i) : 0;
               float oldW = oldChar == 0 ? 0.0F : font.getStringWidth(singleCharString(oldChar));
               float newW = newChar == 0 ? 0.0F : font.getStringWidth(singleCharString(newChar));
               float charW = Math.max(oldW, newW);
               boolean animate = Character.isDigit(oldChar) && Character.isDigit(newChar) && oldChar != newChar && progress < 1.0F;
               if (animate) {
                  int oldAlpha = (int)(baseAlpha * (1.0F - progress));
                  int newAlpha = (int)(baseAlpha * progress);
                  font.drawString(matrix, singleCharString(oldChar), cursor, y - progress * 5.0F, ColorAssist.replAlpha(color, oldAlpha));
                  font.drawString(matrix, singleCharString(newChar), cursor, y + (1.0F - progress) * 5.0F, ColorAssist.replAlpha(color, newAlpha));
               } else if (newChar != 0) {
                  font.drawString(matrix, singleCharString(newChar), cursor, y, color);
               }

               cursor += charW;
            }
         }
      }
   }

   private static void cleanup(long now) {
      if (ENTRIES.size() >= 512) {
         ENTRIES.entrySet().removeIf(e -> now - e.getValue().lastUse > 10000L);
      }
   }

   private static final class Entry {
      private String previous;
      private String current;
      private long changedAt;
      private long lastUse;

      private Entry(String text) {
         this.previous = text;
         this.current = text;
         this.lastUse = this.changedAt = System.currentTimeMillis();
      }
   }
}
