package code.elixir.utils.client;

import code.elixir.Elixir;
import code.elixir.display.screens.mainmenu.MainMenu;
import code.elixir.mixins.IMinecraftClient;
import code.elixir.utils.client.managers.file.impl.AccountFile;
import code.elixir.utils.client.managers.file.impl.account.Account;
import code.elixir.utils.client.managers.file.impl.account.AccountRepository;
import code.elixir.utils.theme.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.UserApiService;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;

public final class FirstLaunchManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String FILE_NAME = "AuthInfo.json";

   private FirstLaunchManager() {
   }

   public static File getAuthInfoFile() {
      try {
         File filesDir = Elixir.getInstance().getClientInfoProvider().filesDir();
         if (!filesDir.exists()) {
            filesDir.mkdirs();
         }

         return new File(filesDir, "AuthInfo.json");
      } catch (Exception var2) {
         File fallbackDir = new File("Elixir/files");
         if (!fallbackDir.exists()) {
            fallbackDir.mkdirs();
         }

         return new File(fallbackDir, "AuthInfo.json");
      }
   }

   public static boolean shouldShowFirstLaunchAnimation() {
      return !getAuthInfoFile().exists();
   }

   public static FirstLaunchManager.AuthInfo loadAuthInfo() {
      File file = getAuthInfoFile();
      if (!file.exists()) {
         return null;
      } else {
         try {
            FirstLaunchManager.AuthInfo var2;
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
               var2 = (FirstLaunchManager.AuthInfo)GSON.fromJson(reader, FirstLaunchManager.AuthInfo.class);
            }

            return var2;
         } catch (Exception var6) {
            return null;
         }
      }
   }

   public static void completeFirstLaunch(String nickname, boolean darkTheme) {
      String normalizedNickname = sanitizeNickname(nickname);
      String themeName = "default_dark";
      applyTheme(themeName);
      setDefaultGuiScaleIfNeeded();
      saveAuthInfo(normalizedNickname, themeName);
      registerOfflineAccount(normalizedNickname);
      applySession(normalizedNickname);
   }

   public static void applySavedPreferences() {
      FirstLaunchManager.AuthInfo authInfo = loadAuthInfo();
      if (authInfo != null) {
         String nickname = sanitizeNickname(authInfo.nickname);
         applyTheme("default_dark");
         registerOfflineAccount(nickname);
         Account active = findActiveAccount();
         if (active != null) {
            applySession(active.name, active.uuid);
         } else {
            applySession(nickname);
         }
      }
   }

   private static Account findActiveAccount() {
      try {
         AccountRepository repository = Elixir.getInstance().getAccountRepository();
         if (repository == null) {
            return null;
         }

         for (Account account : repository.accountList) {
            if (account != null && account.active) {
               return account;
            }
         }
      } catch (Exception var3) {
      }

      return null;
   }

   private static void saveAuthInfo(String nickname, String themeName) {
      JsonObject json = new JsonObject();
      json.addProperty("nickname", nickname);
      json.addProperty("theme", themeName);
      json.addProperty("completedAt", System.currentTimeMillis());
      File file = getAuthInfoFile();

      try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
         GSON.toJson(json, writer);
      } catch (Exception var9) {
      }
   }

   private static void applyTheme(String themeName) {
      ThemeManager.loadTheme(themeName);
      MainMenu.INSTANCE.isDarkMode = true;
      MainMenu.INSTANCE.themeIcon = "I";
   }

   private static void setDefaultGuiScaleIfNeeded() {
      try {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.options != null && (Integer)client.options.getGuiScale().getValue() == 0) {
            client.options.getGuiScale().setValue(2);
         }
      } catch (Exception var1) {
      }
   }

   private static void registerOfflineAccount(String nickname) {
      try {
         AccountRepository repository = Elixir.getInstance().getAccountRepository();
         boolean exists = repository.accountList.stream().anyMatch(account -> account.name.equalsIgnoreCase(nickname));
         if (!exists) {
            String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8)).toString();
            repository.accountList.add(0, new Account(nickname, true, false, null, offlineUuid, "0"));
         }

         if (Elixir.getInstance().getFileController() != null) {
            Elixir.getInstance().getFileController().saveFile(AccountFile.class);
         }
      } catch (Exception var4) {
      }
   }

   private static void applySession(String nickname) {
      String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8)).toString();
      applySession(nickname, offlineUuid);
   }

   private static void applySession(String nickname, String uuid) {
      try {
         UUID parsedUuid;
         try {
            parsedUuid = UUID.fromString(uuid);
         } catch (Exception var6) {
            parsedUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
         }

         Session session = new Session(nickname, parsedUuid, "0", Optional.empty(), Optional.empty(), AccountType.MOJANG);
         IMinecraftClient client = (IMinecraftClient)MinecraftClient.getInstance();
         client.setSessionT(session);
         MinecraftClient.getInstance().getGameProfile().getProperties().clear();
         UserApiService apiService = UserApiService.OFFLINE;
         client.setUserApiService(apiService);
         client.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
         client.setProfileKeys(ProfileKeys.create(apiService, session, MinecraftClient.getInstance().runDirectory.toPath()));
         client.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
      } catch (Exception var7) {
      }
   }

   private static String sanitizeNickname(String nickname) {
      if (nickname == null) {
         return "Player";
      } else {
         String cleaned = nickname.replaceAll("[^A-Za-z0-9_]", "");
         if (cleaned.length() < 3) {
            cleaned = "Player";
         }

         if (cleaned.length() > 16) {
            cleaned = cleaned.substring(0, 16);
         }

         return cleaned;
      }
   }

   public static class AuthInfo {
      public String nickname;
      public String theme;
      public long completedAt;
   }
}
