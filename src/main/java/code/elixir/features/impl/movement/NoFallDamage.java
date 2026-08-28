package code.elixir.features.impl.movement;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.math.projection.Projection;

public class NoFallDamage extends Module {
   private final SelectSetting mode = new SelectSetting("Режим", "Выберите тип").value("SpookyTime").selected("SpookyTime");

   public NoFallDamage() {
      super("NoFallDamage", "NoFallDamage", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.fallDistance > 0.0F && Projection.getDistanceToGround() > 4.0) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
         }
      }
   }

   public SelectSetting getMode() {
      return this.mode;
   }
}
