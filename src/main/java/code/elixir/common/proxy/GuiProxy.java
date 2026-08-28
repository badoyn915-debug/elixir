package code.elixir.common.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CheckboxWidget.Builder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuiProxy extends Screen {
   private boolean isSocks4 = false;
   private TextFieldWidget ipPort;
   private TextFieldWidget username;
   private TextFieldWidget password;
   private CheckboxWidget enabledCheck;
   private Screen parentScreen;
   private String msg = "";
   private int[] positionY;
   private int positionX;

   public GuiProxy(Screen parentScreen) {
      super(Text.translatable("PROXY"));
      this.parentScreen = parentScreen;
   }

   private static boolean isValidIpPort(String ipP) {
      if (ipP != null && !ipP.isEmpty()) {
         String[] split = ipP.split(":");
         if (split.length <= 1) {
            return false;
         } else if (!isNumeric(split[1])) {
            return false;
         } else {
            try {
               int port = Integer.parseInt(split[1]);
               return port >= 0 && port <= 65535;
            } catch (NumberFormatException var3) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isNumeric(String value) {
      if (value != null && !value.isEmpty()) {
         for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean checkProxy() {
      if (!isValidIpPort(this.ipPort.getText())) {
         this.msg = Formatting.RED + Text.translatable("ОШИБКА В НАПИСАНИИ АЙПИ АДРЕСА ИЛИ ПОРТА").getString();
         this.ipPort.setFocused(true);
         return false;
      } else {
         return true;
      }
   }

   private void centerButtons(int amount, int buttonLength, int gap) {
      this.positionX = this.width / 2 - buttonLength / 2;
      this.positionY = new int[amount];
      int center = (this.height + amount * gap) / 2;
      int buttonStarts = center - amount * gap;

      for (int i = 0; i != amount; i++) {
         this.positionY[i] = buttonStarts + gap * i;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      super.keyPressed(keyCode, scanCode, modifiers);
      this.msg = "";
      return true;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      if (this.enabledCheck.isChecked() && !isValidIpPort(this.ipPort.getText())) {
         this.enabledCheck.onPress();
      }

      context.drawTextWithShadow(
         this.textRenderer, Text.translatable("Введите айпи адрес и порт. Пример ниже").getString(), this.width / 2 - 106, this.positionY[2] - 30, 10526880
      );
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Айпи:Порт").getString(), this.width / 2 - 24, this.positionY[2] - 14, 10526880);
      this.ipPort.render(context, mouseX, mouseY, delta);
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Никнейм").getString(), this.width / 2 - 20, this.positionY[4] - 14, 10526880);
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Пароль").getString(), this.width / 2 - 20, this.positionY[5] + 3, 10526880);
      this.username.render(context, mouseX, mouseY, delta);
      this.password.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.msg, this.width / 2, this.positionY[6] + 5, 10526880);
   }

   public void init() {
      int buttonLength = 160;
      this.centerButtons(10, buttonLength, 26);
      this.isSocks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4;
      this.ipPort = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[2], buttonLength, 20, Text.literal(""));
      this.ipPort.setText(ProxyServer.proxy.ipPort);
      this.ipPort.setMaxLength(1024);
      this.ipPort.setFocused(true);
      this.addSelectableChild(this.ipPort);
      this.username = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[4], buttonLength, 20, Text.literal(""));
      this.username.setMaxLength(255);
      this.username.setText(ProxyServer.proxy.username);
      this.addSelectableChild(this.username);
      this.password = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[5] + 15, buttonLength, 20, Text.literal(""));
      this.password.setMaxLength(255);
      this.password.setText(ProxyServer.proxy.password);
      this.addSelectableChild(this.password);
      int posXButtons = this.width / 2 - buttonLength / 2 * 3 / 2;
      ButtonWidget apply = ButtonWidget.builder(Text.translatable("Применить"), button -> {
         if (this.checkProxy()) {
            ProxyServer.proxy = new Proxy(this.isSocks4, this.ipPort.getText(), this.username.getText(), this.password.getText());
            ProxyServer.proxyEnabled = this.enabledCheck.isChecked();
            Config.setDefaultProxy(ProxyServer.proxy);
            Config.saveConfig();
            MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
         }
      }).dimensions(posXButtons + (buttonLength / 2 - 65) * 2, this.positionY[8], buttonLength / 2 + 3, 20).build();
      this.addDrawableChild(apply);
      Builder checkboxBuilder = CheckboxWidget.builder(Text.translatable("Включить прокси"), this.textRenderer);
      checkboxBuilder.pos(this.width / 2 - (15 + this.textRenderer.getWidth(Text.translatable("Включить прокси"))) / 2, this.positionY[7]);
      if (ProxyServer.proxyEnabled) {
         checkboxBuilder.checked(ProxyServer.proxyEnabled);
      }

      this.enabledCheck = checkboxBuilder.build();
      this.addDrawableChild(this.enabledCheck);
      ButtonWidget cancel = ButtonWidget.builder(Text.translatable("Отменить"), button -> MinecraftClient.getInstance().setScreen(this.parentScreen))
         .dimensions(posXButtons + (buttonLength / 2 - 16) * 2, this.positionY[8], buttonLength / 2 - 3, 20)
         .build();
      this.addDrawableChild(cancel);
   }

   public void close() {
      this.msg = "";
   }
}
