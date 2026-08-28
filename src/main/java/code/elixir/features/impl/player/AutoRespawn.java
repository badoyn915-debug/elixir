package code.elixir.features.impl.player;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.DeathScreenEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;

public class AutoRespawn extends Module {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите, что будет использоваться").value("ФанТайм возврат", "Обычный");

   public AutoRespawn() {
      super("AutoRespawn", "AutoRespawn", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.modeSetting});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof DeathMessageS2CPacket message) {
         if (Network.getWorldType().equals("lobby") && this.modeSetting.isSelected("ФанТайм возврат") && mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(1448.0, 1337.0, 228.0, false, false));
            mc.player.requestRespawn();
            mc.player.closeScreen();
         }
      }
   }

   @EventHandler
   public void onDeathScreen(DeathScreenEvent e) {
      if (this.modeSetting.isSelected("Обычный")) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
   }
}
