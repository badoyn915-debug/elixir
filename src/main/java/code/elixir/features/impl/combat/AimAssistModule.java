package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.GameUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.geometry.Render2D;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AimAssistModule extends Module {
   private final SliderSettings distanceSetting = new SliderSettings("Дистанция", "Максимальная дистанция до цели").setValue(4.0F).range(1.0F, 6.0F).step(0.1F);
   private final SliderSettings fovSetting = new SliderSettings("FOV", "Угол обзора для захвата цели").setValue(90.0F).range(20.0F, 360.0F).step(1.0F);
   private final BooleanSetting drawFovRadiusSetting = new BooleanSetting("Рисовать радиус FOV", "Рисует круг радиуса FOV на экране").setValue(false);
   private final SliderSettings speedSetting = new SliderSettings("Скорость", "Базовая скорость наведения").setValue(4.0F).range(0.5F, 10.0F).step(0.1F);
   private final SliderSettings aimHeightSetting = new SliderSettings("Высота прицела", "Точка прицеливания по высоте цели")
      .setValue(0.85F)
      .range(0.0F, 1.0F)
      .step(0.05F);
   private final SelectSetting prioritySetting = new SelectSetting("Приоритет", "По какому параметру выбирать цель")
      .value("Дистанция", "Здоровье")
      .selected("Дистанция");
   private final BooleanSetting onlyWithWeaponSetting = new BooleanSetting("Только с оружием", "Работать только с оружием в руке").setValue(false);
   private final BooleanSetting onlyPlayersSetting = new BooleanSetting("Только игроки", "Целиться только на игроков").setValue(true);
   private final BooleanSetting noAimInInventorySetting = new BooleanSetting("Не целить в инвентаре", "Не наводиться когда открыт инвентарь").setValue(true);
   private final BooleanSetting hitInvisibleSetting = new BooleanSetting("Бить невидимых", "Целиться на невидимых игроков").setValue(false);
   private final BooleanSetting onlyArmoredSetting = new BooleanSetting("Только в броне", "Целиться только на игроков в броне").setValue(false);
   private final BooleanSetting ignoreNakedSetting = new BooleanSetting("Не бить голых", "Игнорировать игроков без брони").setValue(false);
   private final BooleanSetting onlyXSetting = new BooleanSetting("Только по X", "Менять только X").setValue(false);
   private final BooleanSetting multipointSetting = new BooleanSetting("Мультипоинт", "Рандомные точки прицеливания на хитбоксе").setValue(true);
   private final BooleanSetting wallCheckSetting = new BooleanSetting("Проверка стен", "Не наводиться на цели за блоками").setValue(true);
   private final BooleanSetting disableOnWorldChangeSetting = new BooleanSetting("Выкл. при смене мира", "Выключить модуль при телепорте в другой мир")
      .setValue(false);
   private final BooleanSetting microMovementsSetting = new BooleanSetting("Микродвижения", "Рандомный шум и вариация для обхода античита").setValue(true);
   private Entity target;
   private long targetSince;
   private long lastFrameNano;
   private double smoothedYaw;
   private double smoothedPitch;
   private double yawWavePhase;
   private double pitchWavePhase;
   private double yawWaveSpeed;
   private double pitchWaveSpeed;
   private double easePower;
   private double speedVariance;
   private double closeBoost;
   private double heightNoise;
   private long lastHeightNoiseTime;
   private int easeType;
   private double mpCurX;
   private double mpCurY;
   private double mpCurZ;
   private double mpTargetX;
   private double mpTargetY;
   private double mpTargetZ;
   private double mpLerpSpeed;
   private long mpNextTime;
   private RegistryKey<World> lastDimension;
   private static final double FIXED_STEP = 0.004166666666666667;

   public AimAssistModule() {
      super("AimAssist", "Aim Assist", ModuleCategory.COMBAT);
      this.ignoreNakedSetting.visible(() -> !this.onlyArmoredSetting.isValue());
      this.setup(
         new Setting[]{
            this.distanceSetting,
            this.fovSetting,
            this.drawFovRadiusSetting,
            this.speedSetting,
            this.aimHeightSetting,
            this.prioritySetting,
            this.onlyWithWeaponSetting,
            this.onlyPlayersSetting,
            this.noAimInInventorySetting,
            this.hitInvisibleSetting,
            this.onlyArmoredSetting,
            this.ignoreNakedSetting,
            this.onlyXSetting,
            this.multipointSetting,
            this.wallCheckSetting,
            this.disableOnWorldChangeSetting,
            this.microMovementsSetting
         }
      );
   }

   @Override
   public void activate() {
      this.reset();
   }

   @Override
   public void deactivate() {
      this.reset();
   }

   private void reset() {
      this.target = null;
      this.targetSince = 0L;
      this.lastFrameNano = 0L;
      this.smoothedYaw = 0.0;
      this.smoothedPitch = 0.0;
      this.lastDimension = null;
      this.mpCurX = 0.0;
      this.mpCurY = 0.0;
      this.mpCurZ = 0.0;
      this.mpTargetX = 0.0;
      this.mpTargetY = 0.0;
      this.mpTargetZ = 0.0;
      this.mpNextTime = 0L;
      this.randomizeWave();
   }

   private void randomizeWave() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.yawWavePhase = random.nextDouble(Math.PI * 2);
      this.pitchWavePhase = random.nextDouble(Math.PI * 2);
      this.yawWaveSpeed = random.nextDouble(0.08, 0.25);
      this.pitchWaveSpeed = random.nextDouble(0.06, 0.2);
      this.easePower = random.nextDouble(1.8, 3.5);
      this.speedVariance = random.nextDouble(0.75, 1.25);
      this.closeBoost = random.nextDouble(-0.1, 0.2);
      this.easeType = random.nextInt(3);
      this.heightNoise = random.nextDouble(-0.15, 0.1);
      this.lastHeightNoiseTime = System.currentTimeMillis();
      this.mpLerpSpeed = random.nextDouble(0.025, 0.065);
      this.randomizeMultipoint(random);
   }

   private void randomizeMultipoint(ThreadLocalRandom random) {
      this.mpTargetX = random.nextDouble(-0.35, 0.35);
      this.mpTargetY = random.nextDouble(-0.12, 0.12);
      this.mpTargetZ = random.nextDouble(-0.35, 0.35);
      this.mpNextTime = System.currentTimeMillis() + random.nextLong(300L, 900L);
      this.mpLerpSpeed = random.nextDouble(0.02, 0.07);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         RegistryKey dimension = mc.world.getRegistryKey();
         if (this.lastDimension == null) {
            this.lastDimension = dimension;
         } else if (!this.lastDimension.equals(dimension)) {
            if (this.disableOnWorldChangeSetting.isValue()) {
               this.setState(false);
               return;
            }

            this.reset();
            this.lastDimension = dimension;
            return;
         }

         if (!this.canAim()) {
            this.target = null;
         } else {
            Entity found = this.findTarget();
            if (found == null) {
               if (this.target != null) {
                  this.reset();
               }
            } else {
               if (found != this.target) {
                  this.target = found;
                  this.targetSince = System.currentTimeMillis();
                  this.randomizeWave();
               }

               if (this.microMovementsSetting.isValue()) {
                  ThreadLocalRandom random = ThreadLocalRandom.current();
                  long time = System.currentTimeMillis();
                  if (time - this.lastHeightNoiseTime > random.nextLong(180L, 450L)) {
                     this.heightNoise = this.heightNoise + random.nextDouble(-0.06, 0.06);
                     this.heightNoise = MathHelper.clamp(this.heightNoise, -0.2, 0.15);
                     this.lastHeightNoiseTime = time;
                  }

                  if (this.multipointSetting.isValue() && time >= this.mpNextTime) {
                     this.randomizeMultipoint(random);
                  }
               }
            }
         }
      }
   }

   private boolean canAim() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else {
         return this.onlyWithWeaponSetting.isValue() && !this.hasWeapon()
            ? false
            : !this.noAimInInventorySetting.isValue() || !(mc.currentScreen instanceof HandledScreen);
      }
   }

   @EventHandler
   public void onGameUpdate(GameUpdateEvent event) {
      AttackAura aura = AttackAura.getInstance();
      if (aura == null || !aura.isState() || aura.getTarget() == null) {
         if (this.target != null && this.canAim()) {
            Entity class_12972 = this.target;
            LivingEntity living;
            if (class_12972 instanceof LivingEntity && !(living = (LivingEntity)class_12972).isDead() && !(living.getHealth() <= 0.0F)) {
               if (!(mc.player.distanceTo(this.target) > this.distanceSetting.getValue() + 1.0)) {
                  boolean micro = this.microMovementsSetting.isValue();
                  boolean onlyX = this.onlyXSetting.isValue();
                  if (this.multipointSetting.isValue()) {
                     this.mpCurX = this.mpCurX + (this.mpTargetX - this.mpCurX) * this.mpLerpSpeed;
                     this.mpCurY = this.mpCurY + (this.mpTargetY - this.mpCurY) * this.mpLerpSpeed;
                     this.mpCurZ = this.mpCurZ + (this.mpTargetZ - this.mpCurZ) * this.mpLerpSpeed;
                  }

                  double[] angles = this.computeAngles(this.target);
                  float yaw = mc.player.getYaw();
                  float pitch = mc.player.getPitch();
                  double yawDiff = wrap(angles[0] - yaw);
                  double pitchDiff = onlyX ? 0.0 : angles[1] - pitch;
                  double dist = onlyX ? Math.abs(yawDiff) : Math.hypot(yawDiff, pitchDiff);
                  if (!(dist < 0.02)) {
                     double speed = this.speedSetting.getValue();
                     double factor = 1.0 - Math.exp(-speed * 0.004166666666666667);
                     double progress = MathHelper.clamp((System.currentTimeMillis() - this.targetSince) / 200.0, 0.0, 1.0);
                     factor *= 0.4 + 0.6 * progress;
                     factor = MathHelper.clamp(factor, 0.0, 1.0);
                     double yawStep = yawDiff * factor;
                     double pitchStep = pitchDiff * factor;
                     if (micro) {
                        this.yawWavePhase = this.yawWavePhase + this.yawWaveSpeed * 0.004166666666666667 * 60.0;
                        this.pitchWavePhase = this.pitchWavePhase + this.pitchWaveSpeed * 0.004166666666666667 * 60.0;
                        double amp = Math.min(Math.abs(yawDiff) * 0.06, 0.12);
                        yawStep += Math.sin(this.yawWavePhase) * amp;
                        pitchStep += Math.cos(this.pitchWavePhase) * amp * 0.7;
                     }

                     this.smoothedYaw = yawStep;
                     this.smoothedPitch = pitchStep;
                     float newYaw = yaw + (float)yawStep;
                     float newPitch = MathHelper.clamp(pitch + (float)pitchStep, -90.0F, 90.0F);
                     if (!Float.isNaN(newYaw) && !Float.isNaN(newPitch) && !Float.isInfinite(newYaw) && !Float.isInfinite(newPitch)) {
                        mc.player.setYaw(newYaw);
                        mc.player.setPitch(newPitch);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (this.isState() && mc.player != null && mc.world != null) {
         if (this.drawFovRadiusSetting.isValue() && mc.currentScreen == null) {
            DrawContext context = event.getDrawContext();
            int width = context.getScaledWindowWidth();
            int height;
            float radius = this.fovRadius(width, height = context.getScaledWindowHeight());
            if (!(radius < 1.0F)) {
               float centerX = width * 0.5F;
               float centerY = height * 0.5F;
               int color = this.target != null ? -866779307 : -855638017;
               Render2D.drawCircleOutline(context.getMatrices(), centerX, centerY, radius, 1.5F, color);
            }
         }
      }
   }

   private float fovRadius(int width, int height) {
      float max = Math.max(0.0F, Math.min(width, height) * 0.5F - 2.0F);
      if (max <= 0.0F) {
         return 0.0F;
      } else {
         double fov = this.fovSetting.getValue();
         double gameFov;
         if (fov >= (gameFov = MathHelper.clamp(((Integer)mc.options.getFov().getValue()).doubleValue(), 30.0, 170.0))) {
            return max;
         } else {
            double halfFov = MathHelper.clamp(fov * 0.5, 0.1, 89.9);
            double halfGame = MathHelper.clamp(gameFov * 0.5, 1.0, 89.9);
            double ratio = Math.tan(Math.toRadians(halfFov)) / Math.tan(Math.toRadians(halfGame));
            double radius = ratio * (Math.min(width, height) * 0.5);
            return (float)MathHelper.clamp(radius, 0.0, max);
         }
      }
   }

   private Entity findTarget() {
      if (mc.world != null && mc.player != null) {
         double maxDistance = this.distanceSetting.getValue();
         double halfFov = this.fovSetting.getValue() / 2.0;
         boolean byHealth = this.prioritySetting.isSelected("Здоровье");
         Entity best = null;
         double bestScore = Double.MAX_VALUE;
         Vec3d eyes = mc.player.getEyePos();
         Vec3d look = mc.player.getRotationVec(1.0F);

         for (Entity entity : mc.world.getEntities()) {
            PlayerEntity player;
            LivingEntity living;
            if (entity != mc.player
               && entity instanceof LivingEntity
               && !(living = (LivingEntity)entity).isDead()
               && !(living.getHealth() <= 0.0F)
               && (!this.onlyPlayersSetting.isValue() || entity instanceof PlayerEntity)
               && (!(entity instanceof PlayerEntity) || !FriendUtils.isFriend(player = (PlayerEntity)entity))
               && (this.hitInvisibleSetting.isValue() || !living.isInvisible())
               && (!this.onlyArmoredSetting.isValue() ? !this.ignoreNakedSetting.isValue() || this.hasArmor(living) : this.hasArmor(living))) {
               double dist = mc.player.distanceTo(entity);
               if (!(dist > maxDistance)
                  && !(dist < 0.5)
                  && (
                     !this.wallCheckSetting.isValue()
                        || !this.isBlocked(eyes, entity.getPos().add(0.0, entity.getHeight() * this.aimHeightSetting.getValue(), 0.0))
                  )
                  && !(this.angleTo(eyes, look, entity) > halfFov)) {
                  double d = byHealth ? living.getHealth() : dist;
                  if (d < bestScore) {
                     bestScore = d;
                     best = entity;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private double angleTo(Vec3d eyes, Vec3d look, Entity entity) {
      Vec3d point = entity.getPos().add(0.0, entity.getHeight() * this.aimHeightSetting.getValue(), 0.0);
      Vec3d delta = point.subtract(eyes);
      double length = delta.length();
      if (length < 0.001) {
         return 0.0;
      } else {
         delta = delta.multiply(1.0 / length);
         double dot = look.x * delta.x + look.y * delta.y + look.z * delta.z;
         dot = MathHelper.clamp(dot, -1.0, 1.0);
         return Math.toDegrees(Math.acos(dot));
      }
   }

   private double[] computeAngles(Entity entity) {
      Vec3d eyes = mc.player.getEyePos();
      boolean micro = this.microMovementsSetting.isValue();
      double height = this.aimHeightSetting.getValue();
      double heightPoint = micro ? height + this.heightNoise : height;
      double offsetX = 0.0;
      double offsetY = 0.0;
      double offsetZ = 0.0;
      if (this.multipointSetting.isValue()) {
         double halfWidth = entity.getWidth() * 0.5;
         offsetX = this.mpCurX * halfWidth;
         offsetZ = this.mpCurZ * halfWidth;
         offsetY = this.mpCurY;
      }

      double finalHeight = MathHelper.clamp(heightPoint + offsetY, 0.05, 0.95);
      Vec3d point = entity.getPos().add(offsetX, entity.getHeight() * finalHeight, offsetZ);
      double dx = point.x - eyes.x;
      double dy = point.y - eyes.y;
      double dz = point.z - eyes.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      double yaw = horizontal < 0.001 ? mc.player.getYaw() : Math.toDegrees(Math.atan2(-dx, dz));
      double full = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double pitch = full < 0.001 ? mc.player.getPitch() : -Math.toDegrees(Math.asin(MathHelper.clamp(dy / full, -1.0, 1.0)));
      return new double[]{yaw, pitch};
   }

   private boolean hasWeapon() {
      if (mc.player == null) {
         return false;
      } else {
         Item item = mc.player.getMainHandStack().getItem();
         return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
      }
   }

   private boolean hasArmor(LivingEntity living) {
      for (ItemStack stack : living.getArmorItems()) {
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean isBlocked(Vec3d from, Vec3d to) {
      if (mc.world == null) {
         return false;
      } else {
         RaycastContext context = new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         BlockHitResult result = mc.world.raycast(context);
         return result != null && result.getType() == Type.BLOCK;
      }
   }

   private static double wrap(double angle) {
      return MathHelper.wrapDegrees(angle);
   }
}
