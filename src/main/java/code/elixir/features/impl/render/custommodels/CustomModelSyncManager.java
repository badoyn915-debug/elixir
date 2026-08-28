package code.elixir.features.impl.render.custommodels;

import code.elixir.emotions.EmotionWheelManager;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.render.CustomModelModule;
import code.elixir.main.listener.Listener;
import code.elixir.utils.client.Instance;
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

public class CustomModelSyncManager implements Listener, QuickImports {
   private static final String CUSTOM_MODEL_MARKER = "__custom_model__";
   private static final String CUSTOM_MODEL_PREFIX = "__custom_model__:";
   private static final long BROADCAST_INTERVAL_MS = 5000L;
   private static final long STATE_TIMEOUT_MS = 35000L;
   private static CustomModelSyncManager instance;
   private final Map<UUID, CustomModelSyncManager.TimedRemoteModelState> remoteStates = new ConcurrentHashMap<>();
   private long nextBroadcastMs;
   private boolean wasConnected;
   private String lastServerId = "";
   private CustomModelSyncManager.LocalModelSnapshot lastSentSnapshot;
   private final Consumer<JsonObject> relayListener = this::handleRelayPacket;

   public static CustomModelSyncManager create() {
      instance = new CustomModelSyncManager();
      EmotionWheelManager manager = EmotionWheelManager.getInstance();
      if (manager != null) {
         manager.addRelayListener(instance.relayListener);
      }

      return instance;
   }

   public static CustomModelSyncManager getInstance() {
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

   @Nullable
   public CustomModelSyncManager.RemoteModelState getRemoteState(UUID uuid) {
      CustomModelSyncManager.TimedRemoteModelState state = this.remoteStates.get(uuid);
      return state == null ? null : new CustomModelSyncManager.RemoteModelState(state.enabled(), state.modelType());
   }

   public boolean hasRemoteState(UUID uuid) {
      return this.remoteStates.containsKey(uuid);
   }

   private void broadcastLocalStateIfNeeded(EmotionWheelManager manager) {
      boolean connected = manager.isSyncConnected();
      CustomModelSyncManager.LocalModelSnapshot snapshot = this.captureLocalSnapshot();
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
            boolean sent = manager.sendRelayPacket(this.encodeSnapshot(snapshot), now, json -> {
               json.addProperty("customModelEnabled", snapshot.enabled());
               json.addProperty("customModelType", snapshot.modelType());
            });
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

   private CustomModelSyncManager.LocalModelSnapshot captureLocalSnapshot() {
      CustomModelModule module = Instance.get(CustomModelModule.class);
      return module != null && module.isState()
         ? new CustomModelSyncManager.LocalModelSnapshot(true, module.models.getSelected())
         : new CustomModelSyncManager.LocalModelSnapshot(false, "");
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
         CustomModelSyncManager.DecodedSnapshot decodedSnapshot = this.decodeSnapshot(emotionValue, json);
         if (decodedSnapshot == null) {
            return;
         }

         if (!json.has("playerUuid")) {
            return;
         }

         UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
         if (mc.player != null && mc.player.getUuid().equals(uuid)) {
            return;
         }

         this.remoteStates
            .put(uuid, new CustomModelSyncManager.TimedRemoteModelState(decodedSnapshot.enabled(), decodedSnapshot.modelType(), System.currentTimeMillis()));
      } catch (Exception var5) {
      }
   }

   private String encodeSnapshot(CustomModelSyncManager.LocalModelSnapshot snapshot) {
      JsonObject payload = new JsonObject();
      payload.addProperty("enabled", snapshot.enabled());
      payload.addProperty("modelType", snapshot.modelType());
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
      return "__custom_model__:" + encoded;
   }

   @Nullable
   private CustomModelSyncManager.DecodedSnapshot decodeSnapshot(String emotionValue, JsonObject json) {
      if (emotionValue.startsWith("__custom_model__:")) {
         try {
            String encoded = emotionValue.substring("__custom_model__:".length());
            String decodedPayload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(decodedPayload).getAsJsonObject();
            boolean enabled = payload.has("enabled") && payload.get("enabled").getAsBoolean();
            String modelType = payload.has("modelType") ? payload.get("modelType").getAsString() : "";
            return new CustomModelSyncManager.DecodedSnapshot(enabled, modelType);
         } catch (Exception var8) {
            return null;
         }
      } else if (!"__custom_model__".equals(emotionValue)) {
         return null;
      } else {
         boolean enabled = json.has("customModelEnabled") && json.get("customModelEnabled").getAsBoolean();
         String modelType = json.has("customModelType") ? json.get("customModelType").getAsString() : "";
         return new CustomModelSyncManager.DecodedSnapshot(enabled, modelType);
      }
   }

   private record DecodedSnapshot(boolean enabled, String modelType) {
   }

   private record LocalModelSnapshot(boolean enabled, String modelType) {
   }

   public record RemoteModelState(boolean enabled, String modelType) {
   }

   private record TimedRemoteModelState(boolean enabled, String modelType, long updatedAt) {
   }
}
