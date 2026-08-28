package code.elixir.features.impl.player;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.math.time.TimerUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

public class FakePing extends Module {
   SliderSettings slid = new SliderSettings("Значение пинга", "Устанавливает задержку на сервер").setValue(1000.0F).range(0.0F, 10000.0F);
   long id = -1L;
   TimerUtil time = new TimerUtil();

   public FakePing() {
      super("FakePing", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.slid});
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      Packet<?> class_25962;
      if (!event.isSend() && (class_25962 = event.getPacket()) instanceof KeepAliveS2CPacket) {
         KeepAliveS2CPacket p = (KeepAliveS2CPacket)class_25962;
         this.id = p.getId();
         this.time.resetCounter();
         event.cancel();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.time.hasTimeElapsed((long)this.slid.getValue()) && this.id != -1L) {
         mc.player.networkHandler.getConnection().send(new KeepAliveC2SPacket(this.id));
         this.id = -1L;
      }
   }
}
