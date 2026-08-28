package code.elixir.features.impl.movement;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetStrafe extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public SelectSetting mode = new SelectSetting("Режим", "Тип стрейфа").value("Матрикс", "Грим").selected("Матрикс");
   SliderSettings distance = new SliderSettings("Дистанция", "Дистанция от цели")
      .setValue(0.87F)
      .range(0.0F, 3.0F)
      .step(0.1F)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   MultiSelectSetting setting = new MultiSelectSetting("Настройки", "Позволяет настроить работу стрейфов")
      .value("Авто прыжок", "Только при нажатии", "Перед целью")
      .selected("Авто прыжок");
   SliderSettings radius = new SliderSettings("Радиус", "Радиус обхода вокруг цели")
      .setValue(2.5F)
      .range(0.1F, 7.0F)
      .visible(() -> this.mode.getSelected().equals("Матрикс"));
   SliderSettings speed = new SliderSettings("Скорость", "Скорость стрейфа")
      .setValue(0.3F)
      .range(0.1F, 1.0F)
      .visible(() -> this.mode.getSelected().equals("Матрикс"));
   private int grimPointIndex = 0;

   public TargetStrafe() {
      super("TargetStrafe", "TargetStrafe", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.distance, this.radius, this.speed, this.setting});
   }

   public static TargetStrafe getInstance() {
      return Instance.get(TargetStrafe.class);
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = AttackAura.getInstance().getTarget();
         if (target != null && target.isAlive()) {
            String modeValue = this.mode.getSelected();
            if (modeValue.equals("Грим")) {
               if (!mc.player.isSprinting() && mc.player.isOnGround()) {
                  mc.player.setSprinting(true);
               }

               if (!this.setting.isSelected("Только при нажатии")
                  || mc.options.forwardKey.isPressed()
                  || mc.options.backKey.isPressed()
                  || mc.options.leftKey.isPressed()
                  || mc.options.rightKey.isPressed()) {
                  Vec3d playerPos = mc.player.getPos();
                  Vec3d targetPos = target.getPos();
                  double r = this.distance.getValue();
                  Vec3d nextPoint;
                  if (this.setting.isSelected("Перед целью")) {
                     float targetYaw = target.getYaw();
                     double offset = Math.cos(System.currentTimeMillis() / 500.0) * 1.0;
                     nextPoint = targetPos.add(
                        -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
                        0.0,
                        Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
                     );
                  } else {
                     Vec3d[] class_243Array = new Vec3d[]{
                        new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                        new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                        new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                        new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
                     };
                     nextPoint = class_243Array[this.grimPointIndex];
                     if (playerPos.distanceTo(nextPoint) < 0.5) {
                        this.grimPointIndex = (this.grimPointIndex + 1) % class_243Array.length;
                     }
                  }

                  Vec3d direction = nextPoint.subtract(playerPos).normalize();
                  float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
                  float movementAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
                  float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
                  boolean forward = false;
                  boolean back = false;
                  boolean left = false;
                  boolean right = false;
                  if (angleDiff >= -22.5 && angleDiff < 22.5) {
                     forward = true;
                  } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                     forward = true;
                     right = true;
                  } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                     right = true;
                  } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                     back = true;
                     right = true;
                  } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                     forward = true;
                     left = true;
                  } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                     left = true;
                  } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                     back = true;
                     left = true;
                  } else {
                     back = true;
                  }

                  event.setDirectional(forward, back, left, right);
                  if (this.setting.isSelected("Авто прыжок") && mc.player.isOnGround()) {
                     event.setJumping(true);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = AttackAura.getInstance().getTarget();
         if (target != null && target.isAlive()) {
            String modeValue = this.mode.getSelected();
            if (modeValue.equals("Матрикс")) {
               if (!mc.player.isSprinting() && mc.player.isOnGround()) {
                  mc.player.setSprinting(true);
               }

               Vec3d playerPos = mc.player.getPos();
               Vec3d targetPos = target.getPos();
               double r = this.radius.getValue();
               if (!this.setting.isSelected("Только при нажатии")
                  || mc.options.forwardKey.isPressed()
                  || mc.options.backKey.isPressed()
                  || mc.options.leftKey.isPressed()
                  || mc.options.rightKey.isPressed()) {
                  if (this.setting.isSelected("Авто прыжок") && mc.player.isOnGround()) {
                     mc.player.jump();
                  }

                  if (this.setting.isSelected("Перед целью")) {
                     float targetYaw = target.getYaw();
                     double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r;
                     double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r;
                     float yaw = (float)Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
                     double motionSpeed = this.speed.getValue();
                     mc.player
                        .setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed, mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * motionSpeed);
                  } else {
                     double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
                     double var17;
                     double x = targetPos.x + r * Math.cos(var17 = angle + this.speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r));
                     double z = targetPos.z + r * Math.sin(var17);
                     float yaw = (float)Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
                     double motionSpeed = this.speed.getValue();
                     mc.player
                        .setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed, mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * motionSpeed);
                  }
               }
            }
         }
      }
   }

   @Override
   public void activate() {
      super.activate();
      this.grimPointIndex = 0;
   }
}
