package code.elixir.display.screens.auth;

import code.elixir.display.screens.mainmenu.MainMenu;
import code.elixir.utils.client.auth.TelegramAuthManager;
import code.elixir.utils.client.auth.TelegramAuthService;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.display.gif.GifTextureRenderer;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class TelegramAuthScreen extends Screen implements QuickImports {
   private static final long POLL_INTERVAL_MS = 2500L;
   private static final long SESSION_RELOAD_DELAY_MS = 900L;
   private static final Identifier BLATNOY_GIF = Identifier.of("minecraft", "gifs/blatnoy.gif");
   private static final Identifier BACKGROUND_IMAGE = Identifier.of("minecraft", "textures/gui/background.png");
   private static final GifTextureRenderer AUTH_GIF_RENDERER = new GifTextureRenderer(BLATNOY_GIF);
   private static String draftLogin = "";
   private static String draftPassword = "";
   private String authCode = "";
   private String authUrl = "";
   private String statusText = "Введите логин и пароль";
   private boolean loading;
   private boolean awaitingApproval;
   private long nextPollAt;
   private TextFieldWidget loginField;
   private TextFieldWidget passwordField;

   public TelegramAuthScreen() {
      super(Text.of("Telegram Auth"));
   }

   protected void init() {
      super.init();
      TelegramAuthManager.ensureInitialized();
      if (draftLogin.isBlank()) {
         draftLogin = TelegramAuthManager.getSavedLogin();
      }

      if (draftPassword.isBlank()) {
         draftPassword = TelegramAuthManager.getSavedPassword();
      }

      this.initTextFields();
      this.debug("Screen initialized");
   }

   private void initTextFields() {
      int fieldWidth = 236;
      int fieldHeight = 18;
      int x = this.width / 2 - fieldWidth / 2;
      int y = this.height / 2 - 24;
      this.loginField = new TextFieldWidget(this.textRenderer, x, y, fieldWidth, fieldHeight, Text.literal("Логин"));
      this.loginField.setPlaceholder(Text.literal("Логин"));
      this.loginField.setMaxLength(32);
      this.loginField.setText(draftLogin);
      this.loginField.setDrawsBackground(false);
      this.loginField.setEditableColor(15790320);
      this.loginField.setUneditableColor(9474192);
      this.loginField.setChangedListener(value -> draftLogin = value == null ? "" : value);
      this.addSelectableChild(this.loginField);
      this.passwordField = new TextFieldWidget(this.textRenderer, x, y + 30, fieldWidth, fieldHeight, Text.literal("Пароль"));
      this.passwordField.setPlaceholder(Text.literal("Пароль"));
      this.passwordField.setMaxLength(64);
      this.passwordField.setText(draftPassword);
      this.passwordField.setDrawsBackground(false);
      this.passwordField.setEditableColor(15790320);
      this.passwordField.setUneditableColor(9474192);
      this.passwordField.setChangedListener(value -> draftPassword = value == null ? "" : value);
      this.addSelectableChild(this.passwordField);
      this.setInitialFocus(this.loginField);
   }

   public void tick() {
      super.tick();
      if (this.awaitingApproval && !this.authCode.isBlank() && !this.loading) {
         if (System.currentTimeMillis() >= this.nextPollAt) {
            this.pollStatus();
         }
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND_IMAGE, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      float panelWidth = 276.0F;
      float panelHeight = 246.0F;
      float panelX = this.width / 2.0F - panelWidth / 2.0F;
      float panelY = this.height / 2.0F - panelHeight / 2.0F;
      int panelColor = new Color(21, 24, 29, 196).getRGB();
      int panelStroke = new Color(63, 68, 78, 220).getRGB();
      int titleColor = new Color(237, 239, 244, 255).getRGB();
      int subtitleColor = new Color(139, 146, 161, 255).getRGB();
      int actionColor = new Color(67, 150, 255, 255).getRGB();
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), panelX, panelY, panelWidth, panelHeight)
            .round(10.0F)
            .thickness(1.0F)
            .outlineColor(panelStroke)
            .color(panelColor)
            .build()
      );
      AUTH_GIF_RENDERER.render(context, panelX + 14.0F, panelY + 14.0F, 19.0F, 19.0F);
      FontRenderer titleFont = Fonts.getSize(16, Fonts.Type.DEFAULT);
      FontRenderer subtitleFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
      titleFont.drawString(context.getMatrices(), "TelegramAuth", panelX + 38.0F, panelY + 17.0F, titleColor);
      subtitleFont.drawString(context.getMatrices(), "Вход в Elixir через Telegram", panelX + 14.0F, panelY + 40.0F, subtitleColor);
      float fieldWidth = panelWidth - 28.0F;
      float fieldHeight = 22.0F;
      float fieldX = panelX + 14.0F;
      float loginFieldY = panelY + 66.0F;
      float passwordFieldY = loginFieldY + 30.0F;
      if (this.loginField != null && this.passwordField != null) {
         this.loginField.setX((int)fieldX);
         this.loginField.setY((int)loginFieldY);
         this.loginField.setWidth((int)fieldWidth);
         this.loginField.setHeight((int)fieldHeight);
         this.passwordField.setX((int)fieldX);
         this.passwordField.setY((int)passwordFieldY);
         this.passwordField.setWidth((int)fieldWidth);
         this.passwordField.setHeight((int)fieldHeight);
         this.drawInputField(context, this.loginField, "Логин", fieldX, loginFieldY, fieldWidth, fieldHeight, false);
         this.drawInputField(context, this.passwordField, "Пароль", fieldX, passwordFieldY, fieldWidth, fieldHeight, true);
      }

      float buttonHeight = 22.0F;
      float loginButtonY = panelY + 128.0F;
      float tgButtonY = loginButtonY + 28.0F;
      this.drawMainButton(
         context,
         fieldX,
         loginButtonY,
         fieldWidth,
         buttonHeight,
         this.loading ? "Проверка..." : "Войти",
         !this.loading,
         mouseX,
         mouseY,
         new Color(61, 137, 240, 255).getRGB(),
         titleColor
      );
      this.drawSecondaryButton(
         context,
         fieldX,
         tgButtonY,
         fieldWidth,
         buttonHeight,
         "Войти через Telegram",
         !this.loading,
         mouseX,
         mouseY,
         new Color(32, 36, 45, 255).getRGB(),
         new Color(59, 66, 81, 255).getRGB(),
         titleColor
      );
      if (this.statusText != null && !this.statusText.isBlank()) {
         FontRenderer statusFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
         String lowered = this.statusText.toLowerCase(Locale.ROOT);
         boolean isError = lowered.contains("ошиб")
            || lowered.contains("denied")
            || lowered.contains("invalid")
            || lowered.contains("mismatch")
            || lowered.contains("failed");
         int statusColor = isError ? new Color(255, 114, 114, 255).getRGB() : subtitleColor;
         statusFont.drawCenteredString(context.getMatrices(), this.trimToWidth(this.statusText, 52), panelX + panelWidth / 2.0F, panelY + 189.0F, statusColor);
      }

      FontRenderer bottomFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
      String noAccountText = "Нет аккаунта? ";
      String registerText = "Регистрация через Telegram";
      float noAccountWidth = bottomFont.getStringWidth(noAccountText);
      float totalWidth = noAccountWidth + bottomFont.getStringWidth(registerText);
      float startX = panelX + (panelWidth - totalWidth) / 2.0F;
      float bottomY = panelY + panelHeight - 18.0F;
      bottomFont.drawString(context.getMatrices(), noAccountText, startX, bottomY, subtitleColor);
      bottomFont.drawString(context.getMatrices(), registerText, startX + noAccountWidth, bottomY, actionColor);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         float panelWidth = 276.0F;
         float panelHeight = 246.0F;
         float panelX = this.width / 2.0F - panelWidth / 2.0F;
         float panelY = this.height / 2.0F - panelHeight / 2.0F;
         float fieldWidth = panelWidth - 28.0F;
         float fieldHeight = 22.0F;
         float fieldX = panelX + 14.0F;
         float loginFieldY = panelY + 66.0F;
         float passwordFieldY = loginFieldY + 30.0F;
         if (this.isHovered(mouseX, mouseY, fieldX, loginFieldY, fieldWidth, fieldHeight)) {
            this.setFocused(this.loginField);
            this.loginField.setFocused(true);
            this.passwordField.setFocused(false);
            return true;
         } else if (this.isHovered(mouseX, mouseY, fieldX, passwordFieldY, fieldWidth, fieldHeight)) {
            this.setFocused(this.passwordField);
            this.passwordField.setFocused(true);
            this.loginField.setFocused(false);
            return true;
         } else if (this.loginField != null && this.loginField.mouseClicked(mouseX, mouseY, button)) {
            return true;
         } else if (this.passwordField != null && this.passwordField.mouseClicked(mouseX, mouseY, button)) {
            return true;
         } else {
            float buttonHeight = 22.0F;
            float loginButtonY = panelY + 128.0F;
            float tgButtonY = loginButtonY + 28.0F;
            if (!this.loading && this.isHovered(mouseX, mouseY, fieldX, loginButtonY, fieldWidth, buttonHeight)) {
               this.loginWithCredentials();
               return true;
            } else if (!this.loading && this.isHovered(mouseX, mouseY, fieldX, tgButtonY, fieldWidth, buttonHeight)) {
               this.beginAuthorization("login");
               return true;
            } else {
               FontRenderer bottomFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
               String noAccountText = "Нет аккаунта? ";
               String registerText = "Регистрация через Telegram";
               float noAccountWidth = bottomFont.getStringWidth(noAccountText);
               float totalWidth = noAccountWidth + bottomFont.getStringWidth(registerText);
               float startX = panelX + (panelWidth - totalWidth) / 2.0F;
               float registerX = startX + noAccountWidth;
               float registerWidth = bottomFont.getStringWidth(registerText);
               float registerY = panelY + panelHeight - 19.0F;
               if (!this.loading && this.isHovered(mouseX, mouseY, registerX, registerY - 1.0F, registerWidth, 11.0)) {
                  this.beginAuthorization("register");
                  return true;
               } else {
                  return super.mouseClicked(mouseX, mouseY, button);
               }
            }
         }
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void beginAuthorization(String mode) {
      this.loading = true;
      this.statusText = "Создаем сессию...";
      CompletableFuture.<TelegramAuthService.SessionInitResponse>supplyAsync(() -> {
         try {
            return TelegramAuthService.createSession(mode);
         } catch (Exception var2) {
            throw new RuntimeException(var2);
         }
      }).orTimeout(12L, TimeUnit.SECONDS).whenComplete((response, throwable) -> mc.execute(() -> {
         this.loading = false;
         if (throwable == null && response != null) {
            this.authCode = response.code();
            this.authUrl = response.authUrl();
            this.awaitingApproval = true;
            this.statusText = "Открываем Telegram...";
            this.nextPollAt = System.currentTimeMillis() + 900L;
            this.openTelegramLink();
         } else {
            this.debugError("Failed to create session", (Throwable)(throwable instanceof Throwable ? throwable : new RuntimeException("Unknown auth error")));
            this.statusText = "Не удалось создать сессию";
            this.awaitingApproval = false;
            this.authCode = "";
            this.authUrl = "";
         }
      }));
   }

   private void pollStatus() {
      this.loading = true;
      CompletableFuture.<TelegramAuthService.SessionStatusResponse>supplyAsync(() -> {
            try {
               return TelegramAuthService.fetchStatus(this.authCode);
            } catch (Exception var2) {
               throw new RuntimeException(var2);
            }
         })
         .orTimeout(10L, TimeUnit.SECONDS)
         .whenComplete(
            (response, throwable) -> mc.execute(
               () -> {
                  this.loading = false;
                  this.nextPollAt = System.currentTimeMillis() + 2500L;
                  if (throwable == null && response != null) {
                     String status = response.status() == null ? "" : response.status().toUpperCase(Locale.ROOT);
                     String reason = response.reason() == null ? "" : response.reason().trim().toUpperCase(Locale.ROOT);
                     String message = response.message() == null ? "" : response.message().trim();
                     switch (status) {
                        case "APPROVED":
                           this.awaitingApproval = false;
                           TelegramAuthManager.authorize(
                              new TelegramAuthManager.AuthSession(
                                 true,
                                 response.telegramId(),
                                 response.telegramId(),
                                 response.username(),
                                 response.displayName(),
                                 response.role(),
                                 response.subscriptionLabel(),
                                 TelegramAuthManager.getAuthToken()
                              )
                           );
                           this.openMainMenuWithTransition();
                           break;
                        case "WAIT_CREDENTIALS":
                           this.statusText = message.isBlank() ? "Подтвердите вход и отправьте логин/пароль в боте" : message;
                           break;
                        case "REGISTERED":
                           this.awaitingApproval = false;
                           this.statusText = message.isBlank() ? "Аккаунт создан. Теперь войдите по логину и паролю" : message;
                           break;
                        case "DENIED":
                           this.awaitingApproval = false;
                           if (message.isBlank() && "HWID_MISMATCH".equals(reason)) {
                              this.statusText = "HWID не совпадает. Запросите сброс HWID у администратора в боте";
                           } else {
                              this.statusText = message.isBlank() ? "Доступ отклонен" : message;
                           }
                           break;
                        case "EXPIRED":
                           this.awaitingApproval = false;
                           this.statusText = message.isBlank() ? "Сессия истекла" : message;
                           break;
                        default:
                           this.statusText = message.isBlank() ? "Ожидаем подтверждение в Telegram..." : message;
                     }
                  } else {
                     this.debugError("Poll failed", (Throwable)(throwable instanceof Throwable ? throwable : new RuntimeException("Unknown poll error")));
                     this.statusText = "Сервер не отвечает";
                  }
               }
            )
         );
   }

   private void loginWithCredentials() {
      String login = this.loginField == null ? "" : this.loginField.getText().trim();
      String password = this.passwordField == null ? "" : this.passwordField.getText();
      if (!login.isBlank() && !password.isBlank()) {
         draftLogin = login;
         draftPassword = password;
         this.loading = true;
         this.statusText = "Проверяем данные...";
         CompletableFuture.<TelegramAuthManager.AuthSession>supplyAsync(() -> {
            try {
               return TelegramAuthService.loginWithCredentials(login, password);
            } catch (Exception var3) {
               throw new RuntimeException(var3);
            }
         }).orTimeout(12L, TimeUnit.SECONDS).whenComplete((session, throwable) -> mc.execute(() -> {
            this.loading = false;
            if (throwable == null && session != null) {
               TelegramAuthManager.setSavedCredentials(login, password);
               TelegramAuthManager.authorize(session);
               this.openMainMenuWithTransition();
            } else {
               this.statusText = this.extractErrorMessage(throwable, "Неверный логин или пароль");
            }
         }));
      } else {
         this.statusText = "Введите логин и пароль";
      }
   }

   private void openMainMenuWithTransition() {
      MainMenu.INSTANCE.startAuthTransition();
      mc.setScreen(MainMenu.INSTANCE);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.loginField != null && this.loginField.charTyped(chr, modifiers)) {
         return true;
      } else {
         return this.passwordField != null && this.passwordField.charTyped(chr, modifiers) ? true : super.charTyped(chr, modifiers);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.loginField != null && this.loginField.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (this.passwordField != null && this.passwordField.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (this.loading || keyCode != 257 && keyCode != 335) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.loginWithCredentials();
         return true;
      }
   }

   private void openTelegramLink() {
      if (this.authUrl != null && !this.authUrl.isBlank()) {
         try {
            if (!this.openViaCommand(this.authUrl)) {
               if (Desktop.isDesktopSupported()) {
                  Desktop.getDesktop().browse(URI.create(this.authUrl));
               } else {
                  Util.getOperatingSystem().open(this.authUrl);
               }
            }
         } catch (Throwable var4) {
            this.debugError("Primary browser open failed: " + var4.getMessage(), var4);

            try {
               if (!this.openViaCommand(this.authUrl)) {
                  Util.getOperatingSystem().open(this.authUrl);
               }
            } catch (Throwable var3) {
               this.debugError("Final browser open failed: " + var3.getMessage(), var3);
               if (!this.openViaCommand(this.authUrl)) {
                  this.statusText = "Не удалось открыть браузер";
               }
            }
         }
      } else {
         this.statusText = "Пустая ссылка авторизации";
      }
   }

   private boolean openViaCommand(String url) {
      try {
         String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
         if (os.contains("win")) {
            try {
               new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
               return true;
            } catch (Exception var6) {
               this.debugError("rundll32 failed: " + var6.getMessage(), var6);

               try {
                  new ProcessBuilder("powershell", "-NoProfile", "-Command", "Start-Process '" + url.replace("'", "''") + "'").start();
                  return true;
               } catch (Exception var5) {
                  this.debugError("PowerShell opener failed: " + var5.getMessage(), var5);
                  new ProcessBuilder("cmd", "/c", "start", "", url).start();
                  return true;
               }
            }
         } else if (os.contains("mac")) {
            new ProcessBuilder("open", url).start();
            return true;
         } else {
            new ProcessBuilder("xdg-open", url).start();
            return true;
         }
      } catch (Exception var7) {
         this.debugError("openViaCommand failed: " + var7.getMessage(), var7);
         return false;
      }
   }

   private void drawInputField(
      DrawContext context, TextFieldWidget field, String placeholder, float x, float y, float width, float height, boolean passwordMode
   ) {
      boolean focused = this.getFocused() == field;
      boolean showCaret = focused && System.currentTimeMillis() / 500L % 2L == 0L;
      int fill = new Color(28, 31, 38, 255).getRGB();
      int stroke = focused ? new Color(86, 154, 248, 255).getRGB() : new Color(52, 58, 71, 255).getRGB();
      int textColor = new Color(232, 235, 241, 255).getRGB();
      int placeholderColor = new Color(112, 120, 135, 255).getRGB();
      rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height).round(6.0F).thickness(1.0F).outlineColor(stroke).color(fill).build());
      String value = field.getText() == null ? "" : field.getText();
      int drawColor;
      Object draw;
      if (value.isBlank() && !focused) {
         draw = placeholder;
         drawColor = placeholderColor;
      } else if (passwordMode) {
         draw = "•".repeat(Math.max(0, value.length()));
         drawColor = textColor;
      } else {
         draw = value;
         drawColor = textColor;
      }

      if (showCaret) {
         draw = (String)draw + "|";
      }

      FontRenderer inputFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
      inputFont.drawString(
         context.getMatrices(), this.trimToWidth((String)draw, 32), x + 9.0F, this.centeredTextY(inputFont, (String)draw, y, height), drawColor
      );
   }

   private void drawMainButton(
      DrawContext context, float x, float y, float width, float height, String label, boolean enabled, int mouseX, int mouseY, int fill, int textColor
   ) {
      boolean hovered = enabled && this.isHovered(mouseX, mouseY, x, y, width, height);
      int buttonColor = enabled ? (hovered ? this.brighten(fill, 14) : fill) : new Color(78, 83, 93, 255).getRGB();
      rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height).round(6.0F).color(buttonColor).build());
      FontRenderer font = Fonts.getSize(11, Fonts.Type.DEFAULT);
      font.drawCenteredString(
         context.getMatrices(),
         label,
         x + width / 2.0F,
         this.centeredTextY(font, label, y, height),
         enabled ? textColor : new Color(166, 170, 177, 255).getRGB()
      );
   }

   private void drawSecondaryButton(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      String label,
      boolean enabled,
      int mouseX,
      int mouseY,
      int fill,
      int stroke,
      int textColor
   ) {
      boolean hovered = enabled && this.isHovered(mouseX, mouseY, x, y, width, height);
      int buttonColor = hovered ? this.brighten(fill, 8) : fill;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), x, y, width, height).round(6.0F).thickness(1.0F).outlineColor(stroke).color(buttonColor).build()
      );
      FontRenderer font = Fonts.getSize(11, Fonts.Type.DEFAULT);
      font.drawCenteredString(context.getMatrices(), label, x + width / 2.0F, this.centeredTextY(font, label, y, height), textColor);
   }

   private float centeredTextY(FontRenderer font, String text, float boxY, float boxHeight) {
      return font.getCenteredY(boxY, boxHeight, text);
   }

   private int brighten(int rgb, int delta) {
      Color color = new Color(rgb, true);
      return new Color(Math.min(255, color.getRed() + delta), Math.min(255, color.getGreen() + delta), Math.min(255, color.getBlue() + delta), color.getAlpha())
         .getRGB();
   }

   private boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private void debug(String message) {
      String line = "[TelegramAuth] " + message;
      Logger.info(line);
      System.out.println(line);
   }

   private void debugError(String message, Throwable throwable) {
      String line = "[TelegramAuth] " + message;
      Logger.error(line, throwable);
      System.err.println(line);
      if (throwable != null) {
         throwable.printStackTrace(System.err);
      }
   }

   private String extractErrorMessage(Throwable throwable, String fallback) {
      String best = "";

      for (Throwable current = throwable; current != null; current = current.getCause()) {
         String message = current.getMessage();
         if (message != null && !message.isBlank()) {
            best = message;
         }
      }

      return best.isBlank() ? fallback : this.trimToWidth(best.replace("java.lang.RuntimeException:", "").replace("java.io.IOException:", "").trim(), 52);
   }

   private String trimToWidth(String value, int limit) {
      if (value != null && value.length() > limit) {
         return value.substring(0, Math.max(0, limit - 3)) + "...";
      } else {
         return value == null ? "" : value;
      }
   }
}
