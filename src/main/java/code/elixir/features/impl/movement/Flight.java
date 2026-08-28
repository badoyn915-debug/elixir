package code.elixir.features.impl.movement;

import code.elixir.events.player.MoveEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
   private static final double GLIDE_FALL = -0.005;
   private final SelectSetting mode = new SelectSetting("Тип", "Режим полёта")
      .value("Скольжение", "Прыжки", "Движение", "Обычный", "ReallyWorld Dragon", "Элитра Y", "Grim Glide");
   private final SliderSettings speedXZ = new SliderSettings("Скорость", "Горизонтальная скорость")
      .setValue(1.5F)
      .range(0.1F, 10.0F)
      .step(0.1F)
      .visible(
         () -> !this.mode.isSelected("Элитра Y")
            && !this.mode.isSelected("Прыжки")
            && !this.mode.isSelected("ReallyWorld Dragon")
            && !this.mode.isSelected("Grim Glide")
      );
   private final SliderSettings speedY = new SliderSettings("Скорость по Y", "Вертикальная скорость")
      .setValue(1.5F)
      .range(0.1F, 10.0F)
      .step(0.1F)
      .visible(
         () -> !this.mode.isSelected("Элитра Y")
            && !this.mode.isSelected("Прыжки")
            && !this.mode.isSelected("ReallyWorld Dragon")
            && !this.mode.isSelected("Grim Glide")
      );
   private final SliderSettings elytraYBoost = new SliderSettings("Скорость по Y", "Вертикальный буст в режиме Элитра Y")
      .setValue(0.1F)
      .range(0.05F, 0.5F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Элитра Y"));

   public Flight() {
      super("Flight", "Flight", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.speedXZ, this.speedY, this.elytraYBoost});
   }

   @EventHandler
   public void onMove(MoveEvent event) {
      if (this.state && !PlayerInteractionHelper.nullCheck()) {
         boolean sneak = mc.options.sneakKey.isPressed();
         boolean jump = mc.options.jumpKey.isPressed();
         float horizontal = this.speedXZ.getValue();
         float vertical = this.speedY.getValue();
         String var6 = this.mode.getSelected();
         switch (var6) {
            case "Скольжение":
               this.applyGlide(sneak, jump, vertical, horizontal);
               break;
            case "Прыжки":
               this.applyJumpGlide(sneak, jump, vertical, horizontal);
               break;
            case "Движение":
               this.applyMovementGlide(sneak, jump, vertical, horizontal);
               break;
            case "Обычный":
               if (jump) {
                  mc.player.jump();
               }
               break;
            case "ReallyWorld Dragon":
               this.applyDragon(sneak, jump);
               break;
            case "Элитра Y":
               this.applyElytraY(jump);
               break;
            case "Grim Glide":
               this.applyGrimGlide(sneak, jump);
         }
      }
   }

   private void applyGlide(boolean sneak, boolean jump, float verticalSpeed, float horizontalSpeed) {
      double y = -0.005;
      if (sneak) {
         y = -verticalSpeed;
      } else if (jump) {
         y = verticalSpeed;
      }

      if (mc.player.isOnGround()) {
         mc.player.jump();
      }

      this.setHorizontalSpeed(horizontalSpeed);
      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(velocity.x, y, velocity.z);
   }

   private void applyJumpGlide(boolean sneak, boolean jump, float verticalSpeed, float horizontalSpeed) {
      double y = 0.0;
      if (sneak) {
         y = -verticalSpeed;
      } else if (jump) {
         y = verticalSpeed;
      }

      if (mc.player.isOnGround()) {
         mc.player.jump();
      }

      this.setHorizontalSpeed(horizontalSpeed);
      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(velocity.x, y, velocity.z);
   }

   private void applyMovementGlide(boolean sneak, boolean jump, float verticalSpeed, float horizontalSpeed) {
      double y = mc.player.getVelocity().y;
      if (sneak) {
         y = -verticalSpeed;
      } else if (jump) {
         y = verticalSpeed;
      }

      this.setHorizontalSpeed(horizontalSpeed);
      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(velocity.x, y, velocity.z);
   }

   private void applyDragon(boolean sneak, boolean jump) {
      if (mc.player.getAbilities().flying) {
         mc.player.setVelocity(0.0, 0.0, 0.0);
         boolean grim = Network.isReallyWorld();
         boolean moving = sneak || jump;
         double y = 0.0;
         if (sneak) {
            y = grim ? -0.08 : -1.0;
         } else if (jump) {
            y = grim ? 0.08 : 1.0;
         }

         if (grim) {
            this.setHorizontalSpeed(moving ? 0.08F : 0.04F);
         }

         Vec3d velocity = mc.player.getVelocity();
         mc.player.setVelocity(velocity.x, y, velocity.z);
      }
   }

   private void applyElytraY(boolean jump) {
      if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && mc.player.isGliding()) {
         double y = mc.player.getVelocity().y + this.elytraYBoost.getValue();
         mc.player.setVelocity(0.0, y, 0.0);
         mc.options.jumpKey.setPressed(false);
      }
   }

   private void applyGrimGlide(boolean sneak, boolean jump) {
      if (mc.player.getAbilities().flying) {
         mc.player.setVelocity(0.0, 0.0, 0.0);
         boolean grim = Network.isReallyWorld();
         boolean moving = sneak || jump;
         double y = 0.0;
         if (sneak) {
            y = grim ? -0.08 : -1.0;
         } else if (jump) {
            y = grim ? 0.08 : 1.0;
         }

         if (grim) {
            this.setHorizontalSpeed(moving ? 0.08F : 0.04F);
         }

         Vec3d velocity = mc.player.getVelocity();
         mc.player.setVelocity(velocity.x, y, velocity.z);
      }
   }

   private void setHorizontalSpeed(float speed) {
      float yaw = mc.player.getYaw();
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      if (forward != 0.0F || strafe != 0.0F) {
         float yawRad = yaw * (float) (Math.PI / 180.0);
         double x = -MathHelper.sin(yawRad) * speed * forward + MathHelper.cos(yawRad) * speed * strafe;
         double z = MathHelper.cos(yawRad) * speed * forward + MathHelper.sin(yawRad) * speed * strafe;
         mc.player.setVelocity(x, mc.player.getVelocity().y, z);
      }
   }
}
