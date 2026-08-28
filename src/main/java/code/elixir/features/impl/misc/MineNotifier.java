package code.elixir.features.impl.misc;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.math.time.StopWatch;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class MineNotifier extends Module {
   private static final String API_URL = "http://85.208.139.128:8000/api/mines/nearest";
   private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final StopWatch updateTimer = new StopWatch();
   private static final long UPDATE_INTERVAL_MS = 5000L;
   private String lastMineInfo = "";

   public MineNotifier() {
      super("MineNotifier", "Mine Notifier", ModuleCategory.MISC);
      this.updateTimer.reset();
   }

   @Override
   public void activate() {
      super.activate();
      ChatMessage.brandmessage("MineNotifier включен. Проверка автошахт каждые 5 секунд...");
      this.lastMineInfo = "";
      this.updateTimer.reset();
      this.fetchNearestMine();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      ChatMessage.brandmessage("MineNotifier выключен.");
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.updateTimer.finished(5000.0)) {
            this.updateTimer.reset();
            this.fetchNearestMine();
         }
      }
   }

   private void fetchNearestMine() {
      try {
         HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://85.208.139.128:8000/api/mines/nearest"))
            .timeout(Duration.ofSeconds(5L))
            .GET()
            .build();
         httpClient.sendAsync(request, BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() == 200) {
               try {
                  String responseBody = response.body();
                  JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                  boolean found = json.get("found").getAsBoolean();
                  String mineInfo;
                  if (found && !(mineInfo = json.get("mine").getAsString()).equals(this.lastMineInfo)) {
                     this.lastMineInfo = mineInfo;
                     mc.execute(() -> ChatMessage.brandmessage(mineInfo));
                  }
               } catch (Exception var6) {
               }
            }
         }).exceptionally(e -> null);
      } catch (Exception var2) {
      }
   }
}
