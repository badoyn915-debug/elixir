package code.elixir.features.impl.movement.autoMystGlider.parsers;

import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.display.interfaces.QuickImports;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map.Entry;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;

public final class SpookyParser implements QuickImports {
   private static final String API_URL = "http://85.208.139.128:8000/api/events";
   private static final int MIN_SECONDS = 120;
   private static int cachedAnarchy = -1;
   private static long cachedRemainingMs = 2000L;

   public static int parseAnarchy() {
      Scoreboard scoreboard = mc.player.getScoreboard();
      if (scoreboard != null) {
         for (ScoreboardObjective objective : scoreboard.getObjectives()) {
            String string = objective.getDisplayName().getString();
            if (string.contains("Анархия")) {
               String formattedString = string.replaceAll("\\D", "");

               try {
                  return Integer.parseInt(formattedString);
               } catch (NumberFormatException var6) {
                  var6.fillInStackTrace();
               }
            }
         }
      }

      return -1;
   }

   public static boolean isAnarchy(int num) {
      return parseAnarchy() == num;
   }

   public static boolean isAnarchy(String string) {
      int cur = parseAnarchy();
      if (cur == -1) {
         return false;
      } else {
         int parsed;
         try {
            parsed = Integer.parseInt(string);
         } catch (NumberFormatException var4) {
            throw new RuntimeException(var4);
         }

         return cur == parsed;
      }
   }

   public static long parseRemainingTime() {
      BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();
      if (bossBarHud != null) {
         for (Entry entry : bossBarHud.bossBars.entrySet()) {
            ClientBossBar clientBossBar = (ClientBossBar)entry.getValue();
            String time = clientBossBar.getName().getString().replaceAll("\\D", "");
            if (!time.isBlank()) {
               try {
                  int seconds = Integer.parseInt(time);
                  return Math.max((seconds - 2) * 1000L, 1000L);
               } catch (NumberFormatException var6) {
                  var6.fillInStackTrace();
               }
            }
         }
      }

      return 2000L;
   }

   public static void fetchFromApi() {
      try {
         HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://85.208.139.128:8000/api/events")).timeout(Duration.ofSeconds(5L)).GET().build();
         HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
         if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray events = json.getAsJsonArray("events");
            JsonObject bestEvent = null;
            int bestSeconds = Integer.MAX_VALUE;

            for (JsonElement element : events) {
               JsonObject event = element.getAsJsonObject();
               int timeSeconds = event.get("time_seconds").getAsInt();
               if (timeSeconds >= 120 && timeSeconds < bestSeconds) {
                  bestSeconds = timeSeconds;
                  bestEvent = event;
               }
            }

            if (bestEvent != null) {
               cachedAnarchy = bestEvent.get("anarchy_number").getAsInt();
               cachedRemainingMs = Math.max((bestSeconds - 2) * 1000L, 1000L);
               ChatMessage.brandmessage("API: Анархия " + cachedAnarchy + ", ивент через " + bestSeconds + " сек");
            } else {
               cachedAnarchy = -1;
               cachedRemainingMs = 30000L;
               ChatMessage.brandmessage("API: Нет подходящих ивентов (>= 2 мин)");
            }
         }
      } catch (Exception var11) {
         ChatMessage.brandmessage("API: Ошибка загрузки ивентов: " + var11.getMessage());
         cachedAnarchy = -1;
         cachedRemainingMs = 30000L;
      }
   }

   public static int getCachedAnarchy() {
      return cachedAnarchy;
   }

   public static long getCachedRemainingMs() {
      return cachedRemainingMs;
   }

   private SpookyParser() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
