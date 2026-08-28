package code.elixir.utils.client.auth;

import code.elixir.utils.client.logs.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class TelegramAuthService {
   private static final String DEFAULT_BASE_URL = "https://elixir.fun";
   private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();

   private TelegramAuthService() {
   }

   public static TelegramAuthService.SessionInitResponse createSession() throws IOException, InterruptedException {
      return createSession("login");
   }

   public static TelegramAuthService.SessionInitResponse createSession(String mode) throws IOException, InterruptedException {
      JsonObject payload = new JsonObject();
      String deviceId = TelegramAuthManager.getDeviceId();
      String hwid = TelegramAuthManager.getHardwareId();
      String clientName = "Elixir";
      String clientVersion = "2.1.2";
      String authMode = mode != null && !mode.isBlank() ? mode.toLowerCase() : "login";
      payload.addProperty("device_id", deviceId);
      payload.addProperty("hwid", hwid);
      payload.addProperty("auth_mode", authMode);
      payload.addProperty("client_name", clientName);
      payload.addProperty("client_version", clientVersion);
      Logger.info(
         "[TelegramAuthService] createSession baseUrl=" + getBaseUrl() + ", auth_mode=" + authMode + ", client=" + clientName + ", version=" + clientVersion
      );
      String query = "?device_id="
         + encode(deviceId)
         + "&hwid="
         + encode(hwid)
         + "&auth_mode="
         + encode(authMode)
         + "&client_name="
         + encode(clientName)
         + "&client_version="
         + encode(clientVersion);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(getBaseUrl() + "/api/client-auth/session" + query))
         .timeout(Duration.ofSeconds(10L))
         .header("Content-Type", "application/json")
         .header("X-Device-Id", deviceId)
         .header("X-HWID", hwid)
         .POST(BodyPublishers.ofString(payload.toString()))
         .build();
      JsonObject json = sendJson(request);
      String code = getString(json, "code");
      String authUrl = getString(json, "auth_url");
      if (!code.isBlank() && !authUrl.isBlank()) {
         return new TelegramAuthService.SessionInitResponse(code, authUrl, json.has("expires_in") ? json.get("expires_in").getAsLong() : 180L);
      } else {
         throw new IOException("Session response does not contain code/auth_url: " + json);
      }
   }

   public static TelegramAuthService.SessionStatusResponse fetchStatus(String code) throws IOException, InterruptedException {
      Logger.info("[TelegramAuthService] fetchStatus baseUrl=" + getBaseUrl() + ", code=" + code);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(getBaseUrl() + "/api/client-auth/session/" + code))
         .timeout(Duration.ofSeconds(10L))
         .GET()
         .build();
      JsonObject json = sendJson(request);
      JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
      return new TelegramAuthService.SessionStatusResponse(
         getString(json, "status"),
         getString(json, "message"),
         getFirstNonBlank(json, "reason", "status_reason"),
         user.has("telegram_id") ? getString(user, "telegram_id") : null,
         user.has("username") ? getString(user, "username") : null,
         user.has("display_name") ? getString(user, "display_name") : null,
         user.has("role") ? getString(user, "role") : null,
         user.has("subscription_label") ? getString(user, "subscription_label") : null
      );
   }

   public static TelegramAuthService.RegisterResponse registerWithCode(String code, String login, String password) throws IOException, InterruptedException {
      JsonObject payload = new JsonObject();
      payload.addProperty("code", code);
      payload.addProperty("login", login);
      payload.addProperty("password", password);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(getBaseUrl() + "/api/client-auth/register"))
         .timeout(Duration.ofSeconds(10L))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(payload.toString()))
         .build();
      JsonObject json = sendJson(request);
      JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
      return new TelegramAuthService.RegisterResponse(
         getString(json, "status"), getString(json, "message"), user.has("login") ? getString(user, "login") : null
      );
   }

   public static TelegramAuthManager.AuthSession loginWithCredentials(String login, String password) throws IOException, InterruptedException {
      String deviceId = TelegramAuthManager.getDeviceId();
      String hwid = TelegramAuthManager.getHardwareId();
      String safeLogin = login == null ? "" : login.trim();
      String safePassword = password == null ? "" : password;
      JsonObject payload = new JsonObject();
      payload.addProperty("login", safeLogin);
      payload.addProperty("username", safeLogin);
      payload.addProperty("password", safePassword);
      payload.addProperty("device_id", deviceId);
      payload.addProperty("hwid", hwid);
      Logger.info(
         "[TelegramAuthService] loginWithCredentials baseUrl="
            + getBaseUrl()
            + ", loginLen="
            + safeLogin.length()
            + ", passwordLen="
            + safePassword.length()
            + ", deviceId="
            + deviceId
      );
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(getBaseUrl() + "/api/client-auth/login"))
         .timeout(Duration.ofSeconds(10L))
         .header("Content-Type", "application/json")
         .header("X-Device-Id", deviceId)
         .header("X-HWID", hwid)
         .header("X-Login", safeLogin)
         .header("X-Username", safeLogin)
         .header("X-Password", safePassword)
         .POST(BodyPublishers.ofString(payload.toString()))
         .build();
      JsonObject json = sendJson(request);
      String status = getString(json, "status");
      String reason = getFirstNonBlank(json, "reason", "status_reason");
      String message = getString(json, "message");
      if (!"APPROVED".equalsIgnoreCase(status)) {
         throw new IOException(buildDeniedMessage(status, reason, message));
      } else {
         JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
         return new TelegramAuthManager.AuthSession(
            true,
            readUid(user),
            getString(user, "telegram_id"),
            readUsername(user),
            getString(user, "display_name"),
            getString(user, "role"),
            getString(user, "subscription_label"),
            getString(json, "token")
         );
      }
   }

   public static TelegramAuthManager.AuthSession fetchProfile(String token) throws IOException, InterruptedException {
      if (token != null && !token.isBlank()) {
         HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/auth/me"))
            .timeout(Duration.ofSeconds(8L))
            .header("Authorization", "Bearer " + token.trim())
            .GET()
            .build();
         HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
         if (response.statusCode() != 200) {
            return null;
         } else {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : null;
            return user == null
               ? null
               : new TelegramAuthManager.AuthSession(
                  true,
                  readUid(user),
                  readUid(user),
                  readUsername(user),
                  readUsername(user),
                  getString(user, "role"),
                  user.has("hasSubscription") && user.get("hasSubscription").getAsBoolean() ? "FREE" : "NONE",
                  token.trim()
               );
         }
      } else {
         return null;
      }
   }

   private static TelegramAuthManager.AuthSession sessionFromClientAuth(JsonObject json) {
      JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
      return new TelegramAuthManager.AuthSession(
         true,
         readUid(user),
         getString(user, "telegram_id"),
         readUsername(user),
         getString(user, "display_name"),
         getString(user, "role"),
         getString(user, "subscription_label"),
         getString(json, "token")
      );
   }

   private static String readUid(JsonObject user) {
      if (user.has("uid") && !user.get("uid").isJsonNull()) {
         return String.valueOf(user.get("uid").getAsInt());
      } else {
         String telegramId = getString(user, "telegram_id");
         return !telegramId.isBlank() ? telegramId : getString(user, "id");
      }
   }

   private static String readUsername(JsonObject user) {
      String username = getString(user, "username");
      return !username.isBlank() ? username : getString(user, "login");
   }

   public static TelegramAuthManager.AuthSession restoreSession(String deviceId, String hwid) throws IOException, InterruptedException {
      Logger.info("[TelegramAuthService] restoreSession baseUrl=" + getBaseUrl() + ", deviceId=" + deviceId);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(getBaseUrl() + "/api/client-auth/device/" + deviceId + "?hwid=" + encode(hwid)))
         .timeout(Duration.ofSeconds(6L))
         .header("X-HWID", hwid)
         .GET()
         .build();
      JsonObject json = sendJson(request);
      String status = getString(json, "status");
      if (!"APPROVED".equalsIgnoreCase(status)) {
         String reason = getFirstNonBlank(json, "reason", "status_reason");
         String message = getString(json, "message");
         Logger.info("[TelegramAuthService] restoreSession denied: " + buildDeniedMessage(status, reason, message));
         return null;
      } else {
         return sessionFromClientAuth(json);
      }
   }

   public static String getBaseUrl() {
      String configured = System.getProperty("Elixir.auth.baseUrl");
      if (configured == null || configured.isBlank()) {
         configured = System.getenv("Elixir_AUTH_BASE_URL");
      }

      return configured != null && !configured.isBlank() ? configured : "https://elixir.fun";
   }

   private static JsonObject sendJson(HttpRequest request) throws IOException, InterruptedException {
      HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
      Logger.info("[TelegramAuthService] HTTP " + response.statusCode() + " from " + request.uri() + ": " + response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
         JsonElement element = JsonParser.parseString(response.body());
         if (!element.isJsonObject()) {
            throw new IOException("Invalid JSON response");
         } else {
            return element.getAsJsonObject();
         }
      } else {
         throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
      }
   }

   private static String getString(JsonObject json, String key) {
      return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
   }

   private static String getFirstNonBlank(JsonObject json, String... keys) {
      for (String key : keys) {
         String value = getString(json, key);
         if (!value.isBlank()) {
            return value;
         }
      }

      return "";
   }

   private static String buildDeniedMessage(String status, String reason, String message) {
      if (message != null && !message.isBlank()) {
         return message;
      } else {
         String normalizedReason = reason == null ? "" : reason.trim().toUpperCase();
         if ("HWID_MISMATCH".equals(normalizedReason)) {
            return "HWID mismatch. Request HWID reset in Telegram bot.";
         } else if (status != null && !status.isBlank()) {
            return !normalizedReason.isBlank() ? status + ": " + normalizedReason : status;
         } else {
            return !normalizedReason.isBlank() ? normalizedReason : "Login denied";
         }
      }
   }

   private static String encode(String value) {
      return URLEncoder.encode(value, StandardCharsets.UTF_8);
   }

   public record RegisterResponse(String status, String message, String login) {
   }

   public record SessionInitResponse(String code, String authUrl, long expiresInSeconds) {
   }

   public record SessionStatusResponse(
      String status, String message, String reason, String telegramId, String username, String displayName, String role, String subscriptionLabel
   ) {
   }
}
