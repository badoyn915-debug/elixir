package code.elixir.features.impl.movement;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Blink extends Module {
   private final BooleanSetting pulse = new BooleanSetting("Пульсация", "Периодически отправлять накопленные пакеты").setValue(false);
   private final SliderSettings pulseDelay = new SliderSettings("Задержка", "Интервал пульсации в мс")
      .setValue(500.0F)
      .range(50.0F, 3000.0F)
      .step(10.0F)
      .visible(this.pulse::isValue);
   private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
   private Box box;
   private Vec3d renderPos;
   private long lastToggleTime;
   private long lastPulseTime;
   private boolean isFrozen = true;

   public Blink() {
      super("Blink", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.pulse, this.pulseDelay});
   }

   @Override
   public void activate() {
      this.start();
   }

   @Override
   public void deactivate() {
      this.end();
   }

   public void start() {
      if (mc.player != null) {
         this.box = mc.player.getBoundingBox();
         this.renderPos = mc.player.getPos();
         this.lastPulseTime = System.currentTimeMillis();
      }
   }

   public void end() {
      this.packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
      this.packets.clear();
      this.box = null;
      this.renderPos = null;
      this.lastPulseTime = -1L;
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.update(this.pulse.isValue(), this.pulseDelay.getValue());
   }

   public void update(boolean pulse, double pulseDelay) {
      if (!PlayerInteractionHelper.nullCheck()) {
         if (pulse) {
            long now = System.currentTimeMillis();
            if (now - this.lastPulseTime >= (long)pulseDelay) {
               this.packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
               this.packets.clear();
               if (mc.player != null) {
                  this.box = mc.player.getBoundingBox();
                  this.renderPos = mc.player.getPos();
               }

               this.lastPulseTime = now;
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.isState()) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
            this.setState(false);
            return;
         }
         if (packet instanceof ClientStatusC2SPacket status) {
            if (status.getMode().equals(Mode.PERFORM_RESPAWN)) {
               this.setState(false);
               return;
            }
         }
         if (e.isSend()) {
            this.packets.add(e.getPacket());
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.renderPos != null && mc.player != null) {
         Box renderBox = mc.player.getBoundingBox().offset(this.renderPos.subtract(mc.player.getPos()));
         Render3D.drawBox(renderBox, ColorAssist.getClientColor(), 1.0F);
      }
   }
}
