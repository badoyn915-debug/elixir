package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoAuth extends Module {
   private final TextSetting password = new TextSetting("Пароль", "Пароль для автоматической регистрации/входа").setText("bee1892");

   public AutoAuth() {
      super("AutoAuth", "AutoAuth", ModuleCategory.MISC);
      this.setup(new Setting[]{this.password});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.state && mc.player != null && mc.world != null) {
         if (e.getPacket() instanceof GameMessageS2CPacket chatPacket) {
            String chatMessage = chatPacket.content().getString();
            String pass = this.password.getText();
            if ((chatMessage.contains("Войдите") || chatMessage.contains("/login")) && mc.player.networkHandler != null) {
               mc.player.networkHandler.sendChatCommand("login " + pass);
            }

            if ((chatMessage.contains("Зарегистрируйтесь") || chatMessage.contains("/reg"))
               && pass != null
               && pass.length() >= 4
               && mc.player.networkHandler != null) {
               mc.player.networkHandler.sendChatCommand("reg " + pass + " " + pass);
            }
         }
      }
   }
}
