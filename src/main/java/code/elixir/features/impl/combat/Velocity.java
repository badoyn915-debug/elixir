package code.elixir.features.impl.combat;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import java.lang.reflect.Field;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Velocity extends Module {
   private final RadioSetting mode = new RadioSetting(
      "Режим", "Выберите режим уменьшения отдачи", new String[]{"Новый Грим", "Старый Грим", "Матрикс", "Обычный"}, "Новый Грим"
   );
   private boolean flag;
   private int grimTicks;
   private int ccCooldown;

   public static Velocity getInstance() {
      return Instance.get(Velocity.class);
   }

   public Velocity() {
      super("Velocity", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.mode});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.state) {
         if (e.getType() == PacketEvent.Type.RECEIVE) {
            if (mc.player != null && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isInLava()) {
               if (this.ccCooldown > 0) {
                  this.ccCooldown--;
               } else {
                  Packet<?> class_25962 = e.getPacket();
                  EntityVelocityUpdateS2CPacket pac;
                  if (class_25962 instanceof EntityVelocityUpdateS2CPacket
                     && (pac = (EntityVelocityUpdateS2CPacket)class_25962).getEntityId() == mc.player.getId()) {
                     String var6 = this.mode.get();
                     switch (var6) {
                        case "Матрикс":
                           if (!this.flag) {
                              e.setCancelled(true);
                              this.flag = true;
                           } else {
                              this.flag = false;
                              this.setVelocityX(pac, (int)(pac.getVelocityX() * -0.1));
                              this.setVelocityZ(pac, (int)(pac.getVelocityZ() * -0.1));
                           }
                           break;
                        case "Обычный":
                           e.setCancelled(true);
                           break;
                        case "Старый Грим":
                           e.setCancelled(true);
                           this.grimTicks = 6;
                           break;
                        case "Новый Грим":
                           e.setCancelled(true);
                           this.flag = true;
                     }
                  }

                  String modeVal;
                  if ((modeVal = this.mode.get()).equals("Старый Грим") && e.getPacket() instanceof CommonPingS2CPacket && this.grimTicks > 0) {
                     e.setCancelled(true);
                     this.grimTicks--;
                  }

                  if (e.getPacket() instanceof PlayerPositionLookS2CPacket && modeVal.equals("Новый Грим")) {
                     this.ccCooldown = 5;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.state && mc.player != null && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
         String modeVal = this.mode.get();
         if (modeVal.equals("Матрикс") && mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
            double var3 = mc.player.getYaw() * (float) (Math.PI / 180.0);
            double var5 = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            mc.player.setVelocity(-Math.sin(var3) * var5, mc.player.getVelocity().y, Math.cos(var3) * var5);
            mc.player.setSprinting(mc.player.age % 2 != 0);
         }

         if (modeVal.equals("Новый Грим") && this.flag) {
            if (this.ccCooldown <= 0) {
               mc.player
                  .networkHandler
                  .sendPacket(
                     new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false)
                  );
               mc.player
                  .networkHandler
                  .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(mc.player.getPos()), Direction.DOWN));
            }

            this.flag = false;
         }

         if (this.grimTicks > 0) {
            this.grimTicks--;
         }
      }
   }

   @Override
   public void activate() {
      super.activate();
      this.grimTicks = 0;
      this.flag = false;
      this.ccCooldown = 0;
   }

   private void setVelocityX(EntityVelocityUpdateS2CPacket packet, int value) {
      try {
         Field field = EntityVelocityUpdateS2CPacket.class.getDeclaredField("velocityX");
         field.setAccessible(true);
         field.setInt(packet, value);
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   private void setVelocityZ(EntityVelocityUpdateS2CPacket packet, int value) {
      try {
         Field field = EntityVelocityUpdateS2CPacket.class.getDeclaredField("velocityZ");
         field.setAccessible(true);
         field.setInt(packet, value);
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public void setFlag(boolean flag) {
      this.flag = flag;
   }

   public void setGrimTicks(int grimTicks) {
      this.grimTicks = grimTicks;
   }

   public void setCcCooldown(int ccCooldown) {
      this.ccCooldown = ccCooldown;
   }

   public RadioSetting getMode() {
      return this.mode;
   }

   public boolean isFlag() {
      return this.flag;
   }

   public int getGrimTicks() {
      return this.grimTicks;
   }

   public int getCcCooldown() {
      return this.ccCooldown;
   }
}
