package code.elixir.utils.client.auth;

import code.elixir.Elixir;
import code.elixir.utils.client.logs.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public final class TelegramAuthManager {
   private static final String FILE_NAME = "TelegramAuth.json";
   private static volatile TelegramAuthManager.AuthSession activeSession;
   private static volatile String deviceId;
   private static volatile String hardwareId;
   private static volatile String savedLogin;
   private static volatile String savedPassword;
   private static volatile String authToken;
   private static volatile boolean initialized;

   private TelegramAuthManager() {
   }

   public static synchronized void ensureInitialized() {
      if (!initialized) {
         loadState();
         if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
         }

         if ((hardwareId = HardwareFingerprint.computeStableHwid()) == null || hardwareId.isBlank()) {
            hardwareId = UUID.nameUUIDFromBytes(("Elixir-HWID:" + deviceId).getBytes(StandardCharsets.UTF_8)).toString();
         }

         savedLogin = normalize(savedLogin);
         savedPassword = normalize(savedPassword);
         authToken = normalize(authToken);
         initialized = true;
         if (!restorePersistedSession()) {
            activeSession = TelegramAuthManager.AuthSession.guest();
         }

         saveState();
      }
   }

   public static synchronized boolean restorePersistedSession() {
      ensureInitialized();

      try {
         TelegramAuthManager.AuthSession fromToken;
         if (!authToken.isBlank() && (fromToken = TelegramAuthService.fetchProfile(authToken)) != null && fromToken.authorized()) {
            activeSession = fromToken;
            saveState();
            return true;
         }

         TelegramAuthManager.AuthSession restored = TelegramAuthService.restoreSession(deviceId, hardwareId);
         if (restored != null && restored.authorized()) {
            activeSession = restored;
            if (restored.token() != null && !restored.token().isBlank()) {
               authToken = restored.token();
            }

            saveState();
            return true;
         }
      } catch (Exception var2) {
         Logger.warn("[TelegramAuthManager] restore failed: " + var2.getMessage());
      }

      activeSession = TelegramAuthManager.AuthSession.guest();
      saveState();
      return false;
   }

   public static boolean isAuthorized() {
      ensureInitialized();
      return activeSession != null && activeSession.authorized();
   }

   public static TelegramAuthManager.AuthSession getActiveSession() {
      ensureInitialized();
      return activeSession;
   }

   public static synchronized void authorize(TelegramAuthManager.AuthSession session) {
      ensureInitialized();
      activeSession = session == null ? TelegramAuthManager.AuthSession.guest() : session;
      if (activeSession.token() != null && !activeSession.token().isBlank()) {
         authToken = activeSession.token();
      }

      saveState();
      Elixir.getInstance().syncSiteIdentity();
   }

   public static synchronized void clear() {
      ensureInitialized();
      activeSession = TelegramAuthManager.AuthSession.guest();
      authToken = "";
      saveState();
      Elixir.getInstance().syncSiteIdentity();
   }

   public static synchronized void setSavedCredentials(String login, String password) {
      ensureInitialized();
      savedLogin = normalize(login);
      savedPassword = normalize(password);
      saveState();
   }

   public static String getSavedLogin() {
      ensureInitialized();
      return savedLogin == null ? "" : savedLogin;
   }

   public static String getSavedPassword() {
      ensureInitialized();
      return savedPassword == null ? "" : savedPassword;
   }

   public static String getAuthToken() {
      ensureInitialized();
      return authToken == null ? "" : authToken;
   }

   public static String getDeviceId() {
      ensureInitialized();
      return deviceId;
   }

   public static String getHardwareId() {
      ensureInitialized();
      return hardwareId;
   }

   public static String getDisplayNameOrFallback(String fallback) {
      TelegramAuthManager.AuthSession session = getActiveSession();
      if (session != null && session.authorized()) {
         if (session.username() != null && !session.username().isBlank()) {
            return session.username();
         }

         if (session.displayName() != null && !session.displayName().isBlank()) {
            return session.displayName();
         }
      }

      return fallback;
   }

   public static String getRoleOrFallback(String fallback) {
      TelegramAuthManager.AuthSession session = getActiveSession();
      return session != null && session.authorized() && session.role() != null && !session.role().isBlank() ? session.role() : fallback;
   }

   public static String getUidOrFallback(String fallback) {
      TelegramAuthManager.AuthSession session = getActiveSession();
      if (session != null && session.authorized()) {
         if (session.uid() != null && !session.uid().isBlank()) {
            return session.uid();
         }

         if (session.telegramId() != null && !session.telegramId().isBlank()) {
            return session.telegramId();
         }
      }

      return fallback;
   }

   public static String getIdentifierOrFallback(String fallback) {
      return getUidOrFallback(fallback);
   }

   public static String getSubscriptionLabel() {
      TelegramAuthManager.AuthSession session = getActiveSession();
      if (session != null && session.authorized() && session.subscriptionLabel() != null && !session.subscriptionLabel().isBlank()) {
         return session.subscriptionLabel();
      } else {
         return isAuthorized() ? "FREE" : "NONE";
      }
   }

   public static boolean isBetaUser() {
      TelegramAuthManager.AuthSession session = getActiveSession();
      if (session != null && session.authorized()) {
         String role = session.role() == null ? "" : session.role().trim().toUpperCase();
         return role.contains("BETA") || role.contains("ADMIN") || role.contains("OWNER");
      } else {
         return false;
      }
   }

   private static File getStateFile() {
      try {
         File filesDir = Elixir.getInstance().getClientInfoProvider().filesDir();
         if (!filesDir.exists()) {
            filesDir.mkdirs();
         }

         return new File(filesDir, "TelegramAuth.json");
      } catch (Exception var2) {
         File fallbackDir = new File("Elixir/files");
         if (!fallbackDir.exists()) {
            fallbackDir.mkdirs();
         }

         return new File(fallbackDir, "TelegramAuth.json");
      }
   }

   private static void saveState() {
      try {
         File file = getStateFile();
         JsonObject json = new JsonObject();
         json.addProperty("deviceId", normalize(deviceId));
         json.addProperty("savedLogin", normalize(savedLogin));
         json.addProperty("savedPassword", normalize(savedPassword));
         json.addProperty("authToken", normalize(authToken));
         Files.writeString(file.toPath(), json.toString(), StandardCharsets.UTF_8);
      } catch (Exception var2) {
         Logger.warn("[TelegramAuthManager] Failed to save state: " + var2.getMessage());
      }
   }

   private static void loadState() {
      savedLogin = "";
      savedPassword = "";
      authToken = "";
      deviceId = null;
      hardwareId = null;
      activeSession = TelegramAuthManager.AuthSession.guest();
      File file = getStateFile();
      if (file.exists()) {
         try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            deviceId = readString(json, "deviceId");
            savedLogin = normalize(readString(json, "savedLogin"));
            savedPassword = normalize(readString(json, "savedPassword"));
            authToken = normalize(readString(json, "authToken"));
         } catch (IOException var3) {
            Logger.warn("[TelegramAuthManager] Failed to read state: " + var3.getMessage());
         } catch (Exception var4) {
            Logger.warn("[TelegramAuthManager] Invalid state file format: " + var4.getMessage());
         }
      }
   }

   private static String readString(JsonObject json, String key) {
      return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim();
   }

   public record AuthSession(
      boolean authorized, String uid, String telegramId, String username, String displayName, String role, String subscriptionLabel, String token
   ) {
      public static TelegramAuthManager.AuthSession guest() {
         return new TelegramAuthManager.AuthSession(false, "", "", "", "", "Guest", "NONE", "");
      }
   }
}
