package code.elixir.features.impl.movement;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.MotionEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class AirStuck extends Module {
   private final RadioSetting mode = new RadioSetting(
      "Режим", "Выбор режима обхода античита", new String[]{"Grim", "Vulcan", "SlothAC", "Packet Cancel", "Blink"}, "Grim"
   );
   private final BooleanSetting allowRotate = new BooleanSetting("Свободный обзор", "Позволяет осматриваться и атаковать во время зависания").setValue(true);
   private final BooleanSetting freezeVelocity = new BooleanSetting("Сброс скорости", "Обнуляет скорость движения и гравитацию").setValue(true);
   private final BooleanSetting antiVoid = new BooleanSetting("Анти-бездна", "Предотвращает зависание над пустотой ниже мира").setValue(false);
   private final SliderSettings spoofDelay = new SliderSettings("Интервал пакетов", "Интервал синхронизации в мс")
      .setValue(150.0F)
      .range(50.0F, 1000.0F)
      .step(25.0F)
      .visible(() -> this.mode.get().equals("Vulcan") || this.mode.get().equals("SlothAC") || this.mode.get().equals("Blink"));

   private Vec3d frozenPosition = null;
   private float frozenYaw = 0.0F;
   private float frozenPitch = 0.0F;
   private boolean frozenOnGround = false;
   private long lastPacketTime = 0L;
   private final List<Packet<?>> bufferedPackets = new CopyOnWriteArrayList<>();

   public AirStuck() {
      super("AirStuck", "AirStuck", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.allowRotate, this.freezeVelocity, this.antiVoid, this.spoofDelay});
   }

   @Override
   public void activate() {
      super.activate();
      if (mc.player != null) {
         this.frozenPosition = mc.player.getPos();
         this.frozenYaw = mc.player.getYaw();
         this.frozenPitch = mc.player.getPitch();
         this.frozenOnGround = mc.player.isOnGround();
         this.lastPacketTime = System.currentTimeMillis();
         this.bufferedPackets.clear();
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      if (this.mode.get().equals("Blink") && !this.bufferedPackets.isEmpty()) {
         this.bufferedPackets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
      }
      this.bufferedPackets.clear();
      this.frozenPosition = null;
      this.frozenOnGround = false;
      this.lastPacketTime = 0L;
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player == null || mc.world == null || this.frozenPosition == null) return;

      // Anti-Void check
      if (this.antiVoid.isValue() && mc.player.getY() < mc.world.getBottomY() + 2) {
         this.setState(false);
         return;
      }

      // Freeze player movement & fall distance
      if (this.freezeVelocity.isValue()) {
         mc.player.setVelocity(Vec3d.ZERO);
         mc.player.velocityDirty = true;
      }
      mc.player.fallDistance = 0.0F;

      // Client position snapping
      if (mc.player.getPos().squaredDistanceTo(this.frozenPosition) > 1.0E-4) {
         mc.player.setPosition(this.frozenPosition.x, this.frozenPosition.y, this.frozenPosition.z);
      }
      mc.player.prevX = this.frozenPosition.x;
      mc.player.prevY = this.frozenPosition.y;
      mc.player.prevZ = this.frozenPosition.z;

      // Periodic keepalive for Vulcan and SlothAC
      String currentMode = this.mode.get();
      if (currentMode.equals("Vulcan") || currentMode.equals("SlothAC")) {
         long now = System.currentTimeMillis();
         if (now - this.lastPacketTime >= (long)this.spoofDelay.getValue()) {
            this.sendRotationPacket(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
            this.lastPacketTime = now;
         }
      }
   }

   @EventHandler
   public void onMotion(MotionEvent e) {
      if (mc.player != null && mc.world != null && this.frozenPosition != null) {
         e.setX(this.frozenPosition.x);
         e.setY(this.frozenPosition.y);
         e.setZ(this.frozenPosition.z);
         // Accurate onGround status prevents Grim/SlothAC GroundSpoof flags
         e.setOnGround(mc.player.isOnGround());
      }
   }

   @EventHandler
   public void onMove(MoveEvent e) {
      if (mc.player != null && mc.world != null && this.frozenPosition != null) {
         e.setMovement(Vec3d.ZERO);
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.player == null || mc.world == null || this.frozenPosition == null) return;

      if (e.getType() == PacketEvent.Type.RECEIVE) {
         // Server setback handling (e.g. Grim / Vulcan / SlothAC teleport correction)
         if (e.getPacket() instanceof PlayerPositionLookS2CPacket s2c) {
            this.frozenPosition = s2c.change().position();
         }
      } else if (e.getType() == PacketEvent.Type.SEND) {
         Packet<?> packet = e.getPacket();

         if (packet instanceof PlayerMoveC2SPacket movePacket) {
            String currentMode = this.mode.get();

            switch (currentMode) {
               case "Grim":
                  e.cancel();
                  if (this.allowRotate.isValue()) {
                     float yaw = mc.player.getYaw();
                     float pitch = mc.player.getPitch();
                     // Only send rotation packet when the view angle actually changes
                     if (Math.abs(yaw - this.frozenYaw) > 0.05F || Math.abs(pitch - this.frozenPitch) > 0.05F) {
                        this.sendRotationPacket(yaw, pitch, mc.player.isOnGround());
                        this.frozenYaw = yaw;
                        this.frozenPitch = pitch;
                     }
                  }
                  break;

               case "SlothAC":
               case "Vulcan":
                  e.cancel();
                  if (this.allowRotate.isValue()) {
                     this.sendRotationPacket(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
                  }
                  break;

               case "Blink":
                  e.cancel();
                  this.bufferedPackets.add(packet);
                  break;

               case "Packet Cancel":
               default:
                  e.cancel();
                  if (this.allowRotate.isValue()) {
                     this.sendRotationPacket(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
                  }
                  break;
            }
         }
      }
   }

   private void sendRotationPacket(float yaw, float pitch, boolean onGround) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         PlayerInteractionHelper.sendPacketWithOutEvent(
            new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, false)
         );
      }
   }

   public Vec3d getFrozenPosition() {
      return this.frozenPosition;
   }
}
