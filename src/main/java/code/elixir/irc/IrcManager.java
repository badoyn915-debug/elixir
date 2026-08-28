package code.elixir.irc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.chat.ChatEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.main.listener.Listener;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.interfaces.QuickImports;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class IrcManager implements Listener, QuickImports {
   private static final String RELAY_URL = "wss://Elixir-emotions.onrender.com";
   private static final String MESSAGE_PREFIX = "__irc__:";
   private static final String IRC_SERVER_ID = "irc";
   private static final long SEND_COOLDOWN_MS = 10000L;
   private static final int MAX_HISTORY = 160;
   private static IrcManager instance;
   private final CopyOnWriteArrayList<IrcMessage> messages = new CopyOnWriteArrayList<>();
   private final KeySetView<String, Boolean> knownMessageIds = ConcurrentHashMap.newKeySet();
   private IrcRelayClient relayClient;
   private long nextReconnectAttemptMs;
   private long lastSendAtMs;
   private String selectedNickname = "";
   private String lastPrivateContact = "-";

   public static IrcManager create() {
      instance = new IrcManager();
      return instance;
   }

   public static IrcManager getInstance() {
      return instance;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.ensureConnected();
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      String raw = event.getMessage();
      if (raw != null) {
         String message = raw.trim();
         if (!message.isEmpty() && !message.startsWith("/")) {
            if (message.toLowerCase(Locale.ROOT).startsWith(".irc")) {
               String payload = message.length() > 4 ? message.substring(4).trim() : "";
               if (!payload.isEmpty()) {
                  if (payload.startsWith("@@") && payload.length() > 2) {
                     if (this.replyToLastPrivate(payload.substring(2).trim())) {
                        event.cancel();
                     }
                  } else {
                     int split;
                     if (payload.startsWith("@") && (split = payload.indexOf(32)) > 1 && split < payload.length() - 1) {
                        String target = payload.substring(1, split).trim();
                        if (this.sendPrivateMessage(target, payload.substring(split + 1).trim())) {
                           event.cancel();
                        }
                     } else {
                        if (this.sendGlobalMessage(payload)) {
                           event.cancel();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public List<IrcMessage> getMessages() {
      return List.copyOf(this.messages);
   }

   public boolean sendGlobalMessage(String text) {
      if (this.canSend(text) && !this.isOnCooldown()) {
         String finalText = text.trim();
         JsonObject payload = new JsonObject();
         payload.addProperty("messageId", UUID.randomUUID().toString());
         payload.addProperty("kind", "global");
         payload.addProperty("ircNick", this.getDisplayName());
         payload.addProperty("text", finalText);
         payload.addProperty("timestamp", System.currentTimeMillis());
         if (!this.sendPayload(payload)) {
            return false;
         } else {
            this.lastSendAtMs = System.currentTimeMillis();
            this.appendMessage(IrcMessage.outgoing(payload.get("messageId").getAsString(), this.getDisplayName(), finalText, this.lastSendAtMs, false, false));
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean sendPrivateMessage(String targetName, String text) {
      if (this.canSend(text) && targetName != null && !targetName.isBlank() && !this.isOnCooldown()) {
         String finalText = text.trim();
         String cleanTarget = targetName.trim();
         JsonObject payload = new JsonObject();
         payload.addProperty("messageId", UUID.randomUUID().toString());
         payload.addProperty("kind", "pm");
         payload.addProperty("ircNick", this.getDisplayName());
         payload.addProperty("targetName", cleanTarget);
         payload.addProperty("text", finalText);
         payload.addProperty("timestamp", System.currentTimeMillis());
         if (!this.sendPayload(payload)) {
            return false;
         } else {
            this.lastSendAtMs = System.currentTimeMillis();
            this.lastPrivateContact = cleanTarget;
            this.appendMessage(IrcMessage.outgoing(payload.get("messageId").getAsString(), this.getDisplayName(), finalText, this.lastSendAtMs, true, false));
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean replyToLastPrivate(String text) {
      return this.lastPrivateContact != null && !this.lastPrivateContact.isBlank() && !"-".equals(this.lastPrivateContact)
         ? this.sendPrivateMessage(this.lastPrivateContact, text)
         : false;
   }

   public void setNickname(String nickname) {
      this.selectedNickname = this.sanitizeNickname(nickname);
   }

   public String getSelectedNickname() {
      return this.selectedNickname.isBlank() ? this.getMinecraftName() : this.selectedNickname;
   }

   public String getLastPrivateContact() {
      return this.lastPrivateContact;
   }

   public boolean isConnected() {
      return this.relayClient != null && this.relayClient.isOpen();
   }

   public boolean isConnecting() {
      return this.relayClient != null && this.relayClient.isConnectingState();
   }

   public String getConnectionState() {
      if (this.isConnected()) {
         return "connected";
      } else {
         return this.isConnecting() ? "connecting" : "disconnected";
      }
   }

   public String getRelayUrl() {
      return "wss://Elixir-emotions.onrender.com";
   }

   public long getRemainingCooldownMs() {
      return Math.max(0L, 10000L - (System.currentTimeMillis() - this.lastSendAtMs));
   }

   public boolean isOnCooldown() {
      return this.getRemainingCooldownMs() > 0L;
   }

   public boolean isIrcViewOpen() {
      return false;
   }

   public String buildMentionTemplate(String playerName) {
      return ".irc @" + playerName + " ";
   }

   public String getDisplayName() {
      return this.getMinecraftName();
   }

   private void ensureConnected() {
      if (this.relayClient == null || !this.relayClient.isOpen() && !this.relayClient.isConnectingState()) {
         long now = System.currentTimeMillis();
         if (now >= this.nextReconnectAttemptMs) {
            this.nextReconnectAttemptMs = now + 10000L;

            try {
               this.relayClient = new IrcRelayClient(URI.create("wss://Elixir-emotions.onrender.com"), this::handleIncomingMessage);
               this.relayClient.connect();
            } catch (Exception var4) {
            }
         }
      }
   }

   private boolean canSend(String text) {
      return mc.player != null && text != null && !text.isBlank();
   }

   private boolean sendPayload(JsonObject payload) {
      if (this.relayClient != null && this.relayClient.isOpen() && mc.player != null) {
         String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
         JsonObject packet = new JsonObject();
         packet.addProperty("type", "play_emotion");
         packet.addProperty("playerUuid", mc.player.getUuidAsString());
         packet.addProperty("playerName", mc.player.getName().getString());
         packet.addProperty("emotion", "__irc__:" + encodedPayload);
         packet.addProperty("startedAt", payload.has("timestamp") ? payload.get("timestamp").getAsLong() : System.currentTimeMillis());
         packet.addProperty("serverId", "irc");
         this.relayClient.send(gson.toJson(packet));
         return true;
      } else {
         return false;
      }
   }

   private void handleIncomingMessage(String rawMessage) {
      try {
         JsonObject packet = JsonParser.parseString(rawMessage).getAsJsonObject();
         if (!packet.has("emotion")) {
            return;
         }

         String emotion = packet.get("emotion").getAsString();
         if (!emotion.startsWith("__irc__:")) {
            return;
         }

         String decodedPayload = new String(Base64.getUrlDecoder().decode(emotion.substring("__irc__:".length())), StandardCharsets.UTF_8);
         JsonObject payload = JsonParser.parseString(decodedPayload).getAsJsonObject();
         String kind = payload.has("kind") ? payload.get("kind").getAsString() : "";
         String senderName = payload.has("ircNick")
            ? this.sanitizeNickname(payload.get("ircNick").getAsString())
            : (packet.has("playerName") ? packet.get("playerName").getAsString() : "Unknown");
         String text = payload.has("text") ? payload.get("text").getAsString() : "";
         String messageId = payload.has("messageId") ? payload.get("messageId").getAsString() : UUID.randomUUID().toString();
         long timestamp = payload.has("timestamp") ? payload.get("timestamp").getAsLong() : System.currentTimeMillis();
         boolean mentioned = this.shouldPing(text, senderName);
         if ("global".equals(kind)) {
            this.appendMessage(IrcMessage.incoming(messageId, senderName, text, timestamp, false, mentioned));
            if (mentioned) {
               this.notifyMention(senderName, text);
            }

            return;
         }

         if (!"pm".equals(kind) || mc.player == null || !payload.has("targetName")) {
            return;
         }

         String targetName = payload.get("targetName").getAsString();
         if (!this.matchesSelfIdentity(targetName)) {
            return;
         }

         this.lastPrivateContact = senderName;
         this.appendMessage(IrcMessage.incoming(messageId, senderName, text, timestamp, true, true));
         this.notifyMention(senderName, text);
      } catch (Exception var15) {
      }
   }

   private void appendMessage(IrcMessage message) {
      if (this.knownMessageIds.add(message.id())) {
         this.messages.add(message);

         while (this.messages.size() > 160) {
            IrcMessage removed = this.messages.remove(0);
            this.knownMessageIds.remove(removed.id());
         }

         this.pushToIngameChat(message);
      }
   }

   private void pushToIngameChat(IrcMessage message) {
      MutableText line = message.outgoing()
         ? Text.literal("")
            .append(Text.literal(message.direct() ? "[IRC PM ->] " : "[IRC ->] ").formatted(Formatting.AQUA))
            .append(Text.literal(message.senderName()).formatted(Formatting.WHITE))
            .append(Text.literal(": ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(message.text()).formatted(Formatting.GRAY))
         : Text.literal("")
            .append(Text.literal(message.direct() ? "[IRC PM] " : "[IRC] ").formatted(Formatting.AQUA))
            .append(Text.literal(message.senderName()).formatted(Formatting.WHITE))
            .append(Text.literal(": ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(message.text()).formatted(Formatting.GRAY));
      ChatMessage.send(line);
   }

   private boolean matchesSelfIdentity(String targetName) {
      String displayName = this.getDisplayName();
      String minecraftName = this.getMinecraftName();
      return targetName.equalsIgnoreCase(displayName) || targetName.equalsIgnoreCase(minecraftName);
   }

   private void notifyMention(String senderName, String text) {
      if (!this.isIrcViewOpen()) {
         MutableText notification = Text.literal("")
            .append(Text.literal("IRC").formatted(Formatting.AQUA))
            .append(Text.literal("  " + senderName).formatted(Formatting.WHITE))
            .append(Text.literal("  " + this.preview(text)).formatted(Formatting.GRAY));
         Notifications.getInstance().addList(notification, 4200, SoundManager.NOTIF);
      }
   }

   private String preview(String text) {
      if (text != null && !text.isBlank()) {
         return text.length() > 42 ? text.substring(0, 42) + "..." : text;
      } else {
         return "";
      }
   }

   private boolean shouldPing(String text, String senderName) {
      String selfName = this.getDisplayName();
      if (!selfName.isBlank() && !senderName.equalsIgnoreCase(selfName)) {
         String mention = "@" + selfName.toLowerCase(Locale.ROOT);
         return text.toLowerCase(Locale.ROOT).contains(mention);
      } else {
         return false;
      }
   }

   private String sanitizeNickname(String nickname) {
      if (nickname == null) {
         return "";
      } else {
         String cleaned = nickname.trim().replaceAll("[^\\p{L}\\p{N}_-]", "");
         if (cleaned.isBlank()) {
            return "";
         } else {
            return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
         }
      }
   }

   private String getMinecraftName() {
      return mc.player != null ? mc.player.getName().getString() : "Unknown";
   }
}
