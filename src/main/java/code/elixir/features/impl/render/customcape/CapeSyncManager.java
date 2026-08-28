package code.elixir.features.impl.render.customcape;

import code.elixir.emotions.EmotionWheelManager;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.render.CustomCape;
import code.elixir.main.listener.Listener;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class CapeSyncManager implements Listener, QuickImports {
   private static final String CAPE_MARKER = "__custom_cape__";
   private static final String CAPE_PREFIX = "__custom_cape__:";
   private static final long BROADCAST_INTERVAL_MS = 5000L;
   private static final long STATE_TIMEOUT_MS = 35000L;
   private static CapeSyncManager instance;
   private final Map<UUID, CapeSyncManager.TimedRemoteCapeState> remoteStates = new ConcurrentHashMap<>();
   private long nextBroadcastMs;
   private boolean wasConnected;
   private String lastServerId = "";
   private CapeSyncManager.LocalCapeSnapshot lastSentSnapshot;
   private final Consumer<JsonObject> relayListener = this::handleRelayPacket;

   public static CapeSyncManager create() {
      instance = new CapeSyncManager();
      EmotionWheelManager manager = EmotionWheelManager.getInstance();
      if (manager != null) {
         manager.addRelayListener(instance.relayListener);
      }

      return instance;
   }

   public static CapeSyncManager getInstance() {
      return instance;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         EmotionWheelManager manager = EmotionWheelManager.getInstance();
         if (manager == null) {
            this.clearState();
            this.wasConnected = false;
         } else {
            manager.addRelayListener(this.relayListener);
            String currentServerId = manager.getDebugServerId();
            if (!Objects.equals(this.lastServerId, currentServerId)) {
               this.remoteStates.clear();
               this.lastServerId = currentServerId;
               this.nextBroadcastMs = 0L;
               this.lastSentSnapshot = null;
            }

            this.pruneRemoteStates();
            this.broadcastLocalStateIfNeeded(manager);
         }
      } else {
         this.clearState();
         this.wasConnected = false;
      }
   }

   public boolean hasRemoteCape(UUID uuid) {
      CapeSyncManager.TimedRemoteCapeState state = this.remoteStates.get(uuid);
      return state != null && state.enabled();
   }

   private void broadcastLocalStateIfNeeded(EmotionWheelManager manager) {
      boolean connected = manager.isSyncConnected();
      CapeSyncManager.LocalCapeSnapshot snapshot = this.captureLocalSnapshot();
      long now = System.currentTimeMillis();
      if (!connected) {
         this.wasConnected = false;
      } else {
         boolean changed = !snapshot.equals(this.lastSentSnapshot);
         boolean reconnected = !this.wasConnected;
         boolean intervalElapsed = now >= this.nextBroadcastMs;
         if (!changed && !reconnected && !intervalElapsed) {
            this.wasConnected = true;
         } else {
            boolean sent = manager.sendRelayPacket(this.encodeSnapshot(snapshot), now, json -> json.addProperty("customCapeEnabled", snapshot.enabled()));
            if (!sent) {
               this.wasConnected = false;
            } else {
               this.lastSentSnapshot = snapshot;
               this.nextBroadcastMs = now + 5000L;
               this.wasConnected = true;
            }
         }
      }
   }

   private CapeSyncManager.LocalCapeSnapshot captureLocalSnapshot() {
      CustomCape module = CustomCape.getInstance();
      return module != null && module.isState() ? new CapeSyncManager.LocalCapeSnapshot(true) : new CapeSyncManager.LocalCapeSnapshot(false);
   }

   private void pruneRemoteStates() {
      long cutoff = System.currentTimeMillis() - 35000L;
      this.remoteStates.entrySet().removeIf(entry -> entry.getValue().updatedAt() < cutoff);
   }

   private void clearState() {
      this.remoteStates.clear();
      this.lastServerId = "";
      this.nextBroadcastMs = 0L;
      this.lastSentSnapshot = null;
   }

   private void handleRelayPacket(JsonObject json) {
      try {
         if (!json.has("emotion")) {
            return;
         }

         String emotionValue = json.get("emotion").getAsString();
         Boolean enabled = this.decodeSnapshot(emotionValue, json);
         if (enabled == null) {
            return;
         }

         if (!json.has("playerUuid")) {
            return;
         }

         UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
         if (mc.player != null && mc.player.getUuid().equals(uuid)) {
            return;
         }

         this.remoteStates.put(uuid, new CapeSyncManager.TimedRemoteCapeState(enabled, System.currentTimeMillis()));
      } catch (Exception var5) {
      }
   }

   private String encodeSnapshot(CapeSyncManager.LocalCapeSnapshot snapshot) {
      JsonObject payload = new JsonObject();
      payload.addProperty("enabled", snapshot.enabled());
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
      return "__custom_cape__:" + encoded;
   }

   @Nullable
   private Boolean decodeSnapshot(String emotionValue, JsonObject json) {
      if (emotionValue.startsWith("__custom_cape__:")) {
         try {
            String encoded = emotionValue.substring("__custom_cape__:".length());
            String decodedPayload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(decodedPayload).getAsJsonObject();
            return payload.has("enabled") && payload.get("enabled").getAsBoolean();
         } catch (Exception var6) {
            return null;
         }
      } else {
         return !"__custom_cape__".equals(emotionValue) ? null : json.has("customCapeEnabled") && json.get("customCapeEnabled").getAsBoolean();
      }
   }

   private record LocalCapeSnapshot(boolean enabled) {
   }

   private record TimedRemoteCapeState(boolean enabled, long updatedAt) {
   }
}
