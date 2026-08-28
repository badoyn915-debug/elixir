package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;

public class DeathCoords extends Module {
   public DeathCoords() {
      super("DeathCoords", ModuleCategory.MISC);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof DeathMessageS2CPacket) {
         String text = String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
         ChatMessage.send(Text.literal(text).styled(style -> style.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, text))));
      }
   }
}
