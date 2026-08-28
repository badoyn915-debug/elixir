package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class SoundsRemove extends Module {
   public static SoundsRemove getInstance() {
      return Instance.get(SoundsRemove.class);
   }

   public SoundsRemove() {
      super("SoundsRemove", "SoundsRemove", ModuleCategory.MISC);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.isState() && !e.isSend()) {
         if (e.getPacket() instanceof PlaySoundS2CPacket packet) {
            String soundId = packet.getSound().getIdAsString();
            if (soundId != null && soundId.contains("entity.wither")) {
               e.setCancelled(true);
            }
         }
      }
   }
}
