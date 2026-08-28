package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.render.CameraPositionEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Vec3d;

public class FreeCam extends Module {
   private final SliderSettings speedSetting = new SliderSettings("Скорость", "Выберите скорость камеры отладки").setValue(2.0F).range(0.5F, 5.0F);
   private final BooleanSetting freezeSetting = new BooleanSetting("Отменять пакет", "Вы замораживаетесь на месте").setValue(false);
   public Vec3d pos;
   public Vec3d prevPos;

   public static FreeCam getInstance() {
      return Instance.get(FreeCam.class);
   }

   public FreeCam() {
      super("FreeCam", "FreeCam", ModuleCategory.MISC);
      this.setup(new Setting[]{this.speedSetting, this.freezeSetting});
   }

   @Override
   public void activate() {
      this.prevPos = this.pos = new Vec3d(mc.getEntityRenderDispatcher().camera.getPos().toVector3f());
      super.activate();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof PlayerMoveC2SPacket && this.freezeSetting.isValue()) {
         e.cancel();
      } else if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
         this.setState(false);
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      Render3D.drawBox(mc.player.getBoundingBox().offset(Calculate.interpolate(mc.player).subtract(mc.player.getPos())), ColorAssist.getClientColor(), 1.0F);
   }

   @EventHandler
   public void onMove(MoveEvent e) {
      if (this.freezeSetting.isValue()) {
         e.setMovement(Vec3d.ZERO);
      }
   }

   @EventHandler
   public void onInput(InputEvent e) {
      float speed = this.speedSetting.getValue();
      double[] motion = Simulations.calculateDirection(e.forward(), e.sideways(), speed);
      this.prevPos = this.pos;
      this.pos = this.pos.add(motion[0], e.getInput().jump() ? speed : (e.getInput().sneak() ? -speed : 0.0), motion[1]);
      e.inputNone();
   }

   @EventHandler
   public void onCameraPosition(CameraPositionEvent e) {
      e.setPos(Calculate.interpolate(this.prevPos, this.pos));
      mc.options.setPerspective(Perspective.FIRST_PERSON);
   }
}
