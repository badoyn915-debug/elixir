package code.elixir.features.impl.misc;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import java.util.Arrays;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoTpAccept extends Module {
   private final String[] teleportMessages = new String[]{
      "has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам", "просит к вам телепортироваться"
   };
   private boolean canAccept;
   private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", "Будет принимать запросы только от друзей").setValue(false);

   public AutoTpAccept() {
      super("AutoTpAccept", "AutoTpAccept", ModuleCategory.MISC);
      this.setup(new Setting[]{this.friendSetting});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof GameMessageS2CPacket m) {
         String message = m.content().getString();
         boolean validPlayer = !this.friendSetting.isValue() || FriendUtils.getFriends().stream().anyMatch(s -> message.contains(s.getName()));
         if (this.isTeleportMessage(message)) {
            this.canAccept = validPlayer;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!Network.isPvp() && this.canAccept) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
         this.canAccept = false;
      }
   }

   private boolean isTeleportMessage(String message) {
      return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
   }
}
