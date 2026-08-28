package code.elixir.features.impl.render;

import code.elixir.emotions.EmotionWheelManager;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.misc.Globals;
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

public class ItemReplacerSyncManager implements Listener, QuickImports {
   private static final String SWORD_MODEL_PREFIX = "__sword_model__:";
   private static final long BROADCAST_INTERVAL_MS = 5000L;
   private static final long STATE_TIMEOUT_MS = 35000L;
   private static ItemReplacerSyncManager instance;
   private final Map<UUID, ItemReplacerSyncManager.TimedRemoteSwordState> remoteStates = new ConcurrentHashMap<>();
   private long nextBroadcastMs;
   private boolean wasConnected;
   private String lastServerId = "";
   private ItemReplacerSyncManager.LocalSwordSnapshot lastSentSnapshot;
   private final Consumer<JsonObject> relayListener = this::handleRelayPacket;

   public static ItemReplacerSyncManager create() {
      instance = new ItemReplacerSyncManager();
      EmotionWheelManager manager = EmotionWheelManager.getInstance();
      if (manager != null) {
         manager.addRelayListener(instance.relayListener);
      }

      return instance;
   }

   public static ItemReplacerSyncManager getInstance() {
      return instance;
   }

   @Nullable
   public String getRemoteModelLabel(UUID uuid) {
      ItemReplacerSyncManager.TimedRemoteSwordState state = this.remoteStates.get(uuid);
      return state != null && state.enabled() ? state.modelLabel() : null;
   }

   public boolean hasRemoteState(UUID uuid) {
      ItemReplacerSyncManager.TimedRemoteSwordState state = this.remoteStates.get(uuid);
      return state != null && state.enabled();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (!Globals.isItemReplacerSyncEnabled()) {
         this.clearState();
         this.wasConnected = false;
      } else if (mc.player != null && mc.world != null) {
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

   private void broadcastLocalStateIfNeeded(EmotionWheelManager manager) {
      boolean connected = manager.isSyncConnected();
      ItemReplacerSyncManager.LocalSwordSnapshot snapshot = this.captureLocalSnapshot();
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
               json.addProperty("swordReplacerEnabled", snapshot.enabled());
               json.addProperty("swordModelLabel", snapshot.modelLabel());
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

   private ItemReplacerSyncManager.LocalSwordSnapshot captureLocalSnapshot() {
      ItemReplacer replacer = Instance.get(ItemReplacer.class);
      return replacer != null && replacer.isState()
         ? new ItemReplacerSyncManager.LocalSwordSnapshot(true, replacer.getSelectedVariant().label)
         : new ItemReplacerSyncManager.LocalSwordSnapshot(false, "");
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
         ItemReplacerSyncManager.DecodedSnapshot decodedSnapshot = this.decodeSnapshot(emotionValue, json);
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
            .put(uuid, new ItemReplacerSyncManager.TimedRemoteSwordState(decodedSnapshot.enabled(), decodedSnapshot.modelLabel(), System.currentTimeMillis()));
      } catch (Exception var5) {
      }
   }

   private String encodeSnapshot(ItemReplacerSyncManager.LocalSwordSnapshot snapshot) {
      JsonObject payload = new JsonObject();
      payload.addProperty("enabled", snapshot.enabled());
      payload.addProperty("modelLabel", snapshot.modelLabel());
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
      return "__sword_model__:" + encoded;
   }

   @Nullable
   private ItemReplacerSyncManager.DecodedSnapshot decodeSnapshot(String emotionValue, JsonObject json) {
      if (emotionValue.startsWith("__sword_model__:")) {
         try {
            String encoded = emotionValue.substring("__sword_model__:".length());
            String decodedPayload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(decodedPayload).getAsJsonObject();
            boolean enabled = payload.has("enabled") && payload.get("enabled").getAsBoolean();
            String modelLabel = payload.has("modelLabel") ? payload.get("modelLabel").getAsString() : "";
            return new ItemReplacerSyncManager.DecodedSnapshot(enabled, modelLabel);
         } catch (Exception var8) {
            return null;
         }
      } else if (!emotionValue.startsWith("__sword_model__")) {
         return null;
      } else {
         boolean enabled = json.has("swordReplacerEnabled") && json.get("swordReplacerEnabled").getAsBoolean();
         String modelLabel = json.has("swordModelLabel") ? json.get("swordModelLabel").getAsString() : "";
         return new ItemReplacerSyncManager.DecodedSnapshot(enabled, modelLabel);
      }
   }

   private record DecodedSnapshot(boolean enabled, String modelLabel) {
   }

   private record LocalSwordSnapshot(boolean enabled, String modelLabel) {
   }

   private record TimedRemoteSwordState(boolean enabled, String modelLabel, long updatedAt) {
   }
}
