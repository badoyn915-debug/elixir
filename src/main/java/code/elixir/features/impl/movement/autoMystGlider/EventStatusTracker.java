package code.elixir.features.impl.movement.autoMystGlider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventStatusTracker {
   private static volatile long eventStartTime = -1L;
   private static volatile String eventStatus = null;
   private static volatile boolean eventInProgress = false;
   private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*(?:мин|минут|m|min)\\s*(?:и|и)?\\s*(\\d+)?\\s*(?:сек|секунд|s|sec)?");
   private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(?:сек|секунд|s|sec)");

   public static void updateEventStatus(String message) {
      if (message != null && !message.isEmpty()) {
         String lower = message.toLowerCase();
         if (!lower.contains("ивент уже начался") && !lower.contains("event already started")) {
            long delayMs;
            if ((lower.contains("через") || lower.contains("in")) && (delayMs = parseDelay(message)) > 0L) {
               eventInProgress = false;
               eventStartTime = System.currentTimeMillis() + delayMs;
               eventStatus = message;
            }
         } else {
            eventInProgress = true;
            eventStatus = "Ивент уже идет";
            eventStartTime = -1L;
         }
      }
   }

   private static long parseDelay(String message) {
      Matcher matcher = TIME_PATTERN.matcher(message);
      if (matcher.find()) {
         int minutes = Integer.parseInt(matcher.group(1));
         int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
         return (minutes * 60L + seconds) * 1000L;
      } else {
         matcher = SECONDS_PATTERN.matcher(message);
         if (matcher.find()) {
            int seconds = Integer.parseInt(matcher.group(1));
            return seconds * 1000L;
         } else {
            return -1L;
         }
      }
   }

   public static String getDisplayStatus() {
      if (eventInProgress) {
         return "Ивент уже идет";
      } else if (eventStartTime > 0L) {
         long remaining = eventStartTime - System.currentTimeMillis();
         if (remaining <= 0L) {
            eventInProgress = true;
            eventStartTime = -1L;
            return "Ивент уже идет";
         } else {
            long seconds = remaining / 1000L;
            long minutes = seconds / 60L;
            long secs = seconds % 60L;
            return minutes > 0L ? String.format("Ивент через %d:%02d", minutes, secs) : String.format("Ивент через %ds", secs);
         }
      } else {
         return null;
      }
   }

   public static void reset() {
      eventStartTime = -1L;
      eventStatus = null;
      eventInProgress = false;
   }

   public static boolean hasActiveStatus() {
      if (eventInProgress) {
         return true;
      } else if (eventStartTime > 0L) {
         long remaining = eventStartTime - System.currentTimeMillis();
         return remaining > 0L;
      } else {
         return false;
      }
   }
}
