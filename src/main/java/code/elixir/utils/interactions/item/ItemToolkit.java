package code.elixir.utils.interactions.item;

import code.elixir.Elixir;
import code.elixir.events.packet.PacketEvent;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;

public class ItemToolkit implements QuickImports {
   public static final ItemToolkit INSTANCE = new ItemToolkit();
   public boolean useItem;
   public boolean releaseItem = true;

   public ItemToolkit() {
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof PlayerActionC2SPacket player) {
         if (player.getAction().equals(Action.RELEASE_USE_ITEM)) {
            this.releaseItem = true;
         }
      } else if (packet instanceof ClientStatusC2SPacket status) {
         if (status.getMode().equals(Mode.PERFORM_RESPAWN)) {
            this.releaseItem = true;
         }
      } else if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
         this.releaseItem = true;
      }
   }

   public void useHand(Hand hand) {
      if (this.releaseItem) {
         mc.interactionManager.interactItem(mc.player, hand);
         this.releaseItem = false;
      }

      this.useItem = true;
   }

   public void setUseItem(boolean useItem) {
      this.useItem = useItem;
   }

   public void setReleaseItem(boolean releaseItem) {
      this.releaseItem = releaseItem;
   }

   public boolean isUseItem() {
      return this.useItem;
   }

   public boolean isReleaseItem() {
      return this.releaseItem;
   }
}
