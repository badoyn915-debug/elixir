package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.MotionEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import java.util.Comparator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SnowballItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ProjectHelper extends Module implements QuickImports {
   private static final long HUD_KEEP_MS = 700L;
   private static final int MAX_PREDICT_TICKS = 60;
   private static final int SOLVER_ITERATIONS = 3;
   private static final double BOW_GRAVITY = 0.05;
   private static final double TRIDENT_GRAVITY = 0.05;
   private static final double SNOWBALL_GRAVITY = 0.03;
   private static final double CROSSBOW_GRAVITY = 0.05;
   private final MultiSelectSetting weapons = new MultiSelectSetting("Оружие", "Выбор типов метательного оружия")
      .value("Лук", "Трезубец", "Арбалет", "Снежок")
      .selected("Лук", "Трезубец", "Арбалет");
   private final SliderSettings distance = new SliderSettings("Дистанция", "Максимальная дистанция поиска цели")
      .setValue(50.0F)
      .range(10.0F, 200.0F)
      .step(5.0F);
   private final SliderSettings fov = new SliderSettings("FOV", "Максимальный угол поиска цели").setValue(180.0F).range(10.0F, 180.0F).step(5.0F);
   private final SliderSettings predict = new SliderSettings("Предикт", "Множитель упреждения по движению цели").setValue(1.0F).range(0.0F, 3.0F).step(0.1F);
   private final BooleanSetting moveCorrection = new BooleanSetting("Коррекция движения", "Исправление движения относительно цели").setValue(true);
   private final BooleanSetting throughWalls = new BooleanSetting("Через стены", "Стрелять через блоки").setValue(false);
   private final SelectSetting sorting = new SelectSetting("Сортировка", "Как выбирать цель")
      .value("Смешанная", "По углу", "По дистанции", "По здоровью")
      .selected("Смешанная");
   private LivingEntity currentTarget;
   private LivingEntity hudTarget;
   private long lastHudTargetAt;
   private ProjectHelper.ProjectileProfile lastProjectileProfile;
   private long lastProjectileProfileAt;
   private Turns currentRotation;
   private Turns serverRotation;
   private boolean isAiming;

   public ProjectHelper() {
      super("ProjectHelper", "ProjectHelper", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.weapons, this.distance, this.fov, this.predict, this.moveCorrection, this.throughWalls, this.sorting});
   }

   public static ProjectHelper getInstance() {
      return Instance.get(ProjectHelper.class);
   }

   @Override
   public void deactivate() {
      this.currentTarget = null;
      this.hudTarget = null;
      this.lastHudTargetAt = 0L;
      this.lastProjectileProfile = null;
      this.lastProjectileProfileAt = 0L;
      this.currentRotation = null;
      this.serverRotation = null;
      this.isAiming = false;
      super.deactivate();
   }

   @EventHandler
   public void onInput(InputEvent event) {
   }

   @EventHandler
   public void onMotion(MotionEvent event) {
      if (this.isAiming && this.currentRotation != null && mc.player != null) {
         event.setYaw(this.currentRotation.getYaw());
         event.setPitch(this.currentRotation.getPitch());
         mc.player.headYaw = this.currentRotation.getYaw();
         mc.player.bodyYaw = this.currentRotation.getYaw();
         mc.player.prevHeadYaw = this.currentRotation.getYaw();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (!PlayerInteractionHelper.nullCheck() && !mc.player.isGliding()) {
         ProjectHelper.ProjectileProfile projectileProfile = this.resolveActiveWeapon();
         if (projectileProfile != null && mc.player.isUsingItem()) {
            this.lastProjectileProfile = projectileProfile;
            this.lastProjectileProfileAt = System.currentTimeMillis();
            this.currentTarget = this.findTarget();
            if (this.currentTarget != null) {
               this.hudTarget = this.currentTarget;
               this.lastHudTargetAt = System.currentTimeMillis();
               Turns aimRotation = this.solveAim(this.currentTarget, projectileProfile);
               if (aimRotation != null) {
                  this.currentRotation = aimRotation;
                  this.serverRotation = aimRotation;
                  this.isAiming = true;
               } else {
                  this.isAiming = false;
               }
            } else {
               this.isAiming = false;
            }
         } else {
            this.isAiming = false;
            this.currentRotation = null;
            this.currentTarget = null;
            if (this.hudTarget != null && System.currentTimeMillis() - this.lastHudTargetAt > 700L) {
               this.hudTarget = null;
            }

            if (this.lastProjectileProfile != null && System.currentTimeMillis() - this.lastProjectileProfileAt > 700L) {
               this.lastProjectileProfile = null;
               this.lastProjectileProfileAt = 0L;
            }
         }
      } else {
         this.resetState();
      }
   }

   private void resetState() {
      this.currentTarget = null;
      this.hudTarget = null;
      this.lastHudTargetAt = 0L;
      this.lastProjectileProfile = null;
      this.lastProjectileProfileAt = 0L;
      this.currentRotation = null;
      this.serverRotation = null;
      this.isAiming = false;
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.isSend() && !PlayerInteractionHelper.nullCheck()) {
         if (event.getPacket() instanceof PlayerActionC2SPacket actionPacket) {
            if (actionPacket.getAction() == Action.RELEASE_USE_ITEM) {
               ProjectHelper.ProjectileProfile projectileProfile = this.resolveActiveWeapon();
               if (projectileProfile == null) {
                  projectileProfile = this.lastProjectileProfile;
               }

               if (projectileProfile != null) {
                  LivingEntity target = this.currentTarget != null ? this.currentTarget : this.findTarget();
                  if (target != null) {
                     Turns turns = this.solveAim(target, projectileProfile);
                     if (turns != null) {
                        event.setCancelled(true);
                        this.hudTarget = target;
                        this.currentTarget = target;
                        this.lastHudTargetAt = System.currentTimeMillis();
                        PlayerInteractionHelper.sendPacketWithOutEvent(
                           new Full(
                              mc.player.getX(),
                              mc.player.getY(),
                              mc.player.getZ(),
                              turns.getYaw(),
                              turns.getPitch(),
                              mc.player.isOnGround(),
                              mc.player.horizontalCollision
                           )
                        );
                        PlayerInteractionHelper.sendPacketWithOutEvent(actionPacket);
                        this.lastProjectileProfile = null;
                        this.lastProjectileProfileAt = 0L;
                     }
                  }
               }
            }
         }
      }
   }

   public LivingEntity getHudTarget() {
      if (!this.isState()) {
         return null;
      } else if (this.hudTarget == null) {
         return null;
      } else if (!this.hudTarget.isAlive()) {
         this.hudTarget = null;
         return null;
      } else if (mc.player == null || mc.player.distanceTo(this.hudTarget) > this.distance.getValue() + 5.0F) {
         return null;
      } else if (mc.player.isUsingItem() && this.resolveActiveWeapon() != null) {
         return this.hudTarget;
      } else {
         return System.currentTimeMillis() - this.lastHudTargetAt <= 700L ? this.hudTarget : null;
      }
   }

   private LivingEntity findTarget() {
      return mc.world != null && mc.player != null
         ? mc.world
            .getPlayers()
            .stream()
            .filter(this::isValidTarget)
            .filter(entity -> mc.player.distanceTo(entity) <= this.distance.getValue())
            .filter(arg_0 -> mc.player.canSee(arg_0))
            .filter(this::isInsideFov)
            .min(Comparator.comparingDouble(this::getTargetScore))
            .orElse(null)
         : null;
   }

   private boolean isValidTarget(PlayerEntity entity) {
      if (entity == mc.player || !entity.isAlive() || entity.isSpectator()) {
         return false;
      } else if (FriendUtils.isFriend(entity)) {
         return false;
      } else {
         return !this.throughWalls.isValue() && !this.canSeeTarget(entity) ? false : !AntiBot.isBot(entity);
      }
   }

   private boolean canSeeTarget(LivingEntity entity) {
      if (mc.world != null && mc.player != null) {
         Vec3d playerEye = mc.player.getEyePos();
         RaycastContext context = new RaycastContext(playerEye, entity.getBoundingBox().getCenter(), ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         BlockHitResult result = mc.world.raycast(context);
         return result == null || result.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   private double getTargetScore(LivingEntity entity) {
      Vec3d point = this.getTargetPoint(entity, 0.0);
      Turns angle = MathAngle.calculateAngle(point);
      double angleDiff = Math.hypot(MathHelper.wrapDegrees(angle.getYaw() - mc.player.getYaw()), angle.getPitch() - mc.player.getPitch());
      String var6 = this.sorting.getSelected();

      return switch (var6) {
         case "По углу" -> angleDiff;
         case "По дистанции" -> mc.player.distanceTo(entity);
         case "По здоровью" -> entity.getHealth();
         default -> mc.player.distanceTo(entity) + angleDiff * 0.05;
      };
   }

   private Turns solveAim(LivingEntity target, ProjectHelper.ProjectileProfile projectileProfile) {
      Vec3d baseTargetPoint = this.getTargetPoint(target, 0.0);
      double travelTicks = Math.max(1.0, mc.player.getEyePos().distanceTo(baseTargetPoint) / Math.max(0.1, projectileProfile.speed));
      Vec3d predictedPoint = baseTargetPoint;

      for (int i = 0; i < 3; i++) {
         predictedPoint = this.getTargetPoint(target, travelTicks);
         double horizontalDistance = this.horizontalDistance(predictedPoint);
         if (horizontalDistance < 1.0E-4) {
            return MathAngle.calculateAngle(predictedPoint);
         }

         double pitch = this.solvePitchFor(predictedPoint, projectileProfile);
         double yaw = Math.toDegrees(Math.atan2(predictedPoint.z - mc.player.getEyePos().z, predictedPoint.x - mc.player.getEyePos().x)) - 90.0;
         travelTicks = this.estimateTravelTicks(predictedPoint, pitch, projectileProfile);
      }

      if (predictedPoint == null) {
         return null;
      } else {
         double pitch = this.solvePitchFor(predictedPoint, projectileProfile);
         double yaw = Math.toDegrees(Math.atan2(predictedPoint.z - mc.player.getEyePos().z, predictedPoint.x - mc.player.getEyePos().x)) - 90.0;
         return new Turns((float)yaw, (float)MathHelper.clamp(pitch, -89.0, 89.0));
      }
   }

   private double solvePitchFor(Vec3d targetPoint, ProjectHelper.ProjectileProfile projectileProfile) {
      Vec3d eyePos = mc.player.getEyePos();
      double dx = targetPoint.x - eyePos.x;
      double dz = targetPoint.z - eyePos.z;
      double dy = targetPoint.y - eyePos.y;
      double velocitySquared = projectileProfile.speed * projectileProfile.speed;
      double gravity = projectileProfile.gravity;
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      double discriminant = velocitySquared * velocitySquared - gravity * (gravity * horizontalDistance * horizontalDistance + 2.0 * dy * velocitySquared);
      if (discriminant < 0.0) {
         return -Math.toDegrees(Math.atan2(dy, horizontalDistance));
      } else {
         double angle = Math.atan((velocitySquared - Math.sqrt(discriminant)) / (gravity * horizontalDistance));
         return -Math.toDegrees(angle);
      }
   }

   private double estimateTravelTicks(Vec3d targetPoint, double pitch, ProjectHelper.ProjectileProfile projectileProfile) {
      Vec3d eyePos = mc.player.getEyePos();
      double yaw = Math.toDegrees(Math.atan2(targetPoint.z - eyePos.z, targetPoint.x - eyePos.x)) - 90.0;
      Vec3d velocity = this.createVelocity((float)yaw, (float)pitch, projectileProfile.speed);
      Vec3d pos = eyePos;
      int bestTick = 1;
      double bestDistanceSq = Double.MAX_VALUE;

      for (int tick = 1; tick <= 60; tick++) {
         double distanceSq = (pos = pos.add(velocity)).squaredDistanceTo(targetPoint);
         if (distanceSq < bestDistanceSq) {
            bestDistanceSq = distanceSq;
            bestTick = tick;
         }

         velocity = velocity.multiply(projectileProfile.drag);
         velocity = new Vec3d(velocity.x, velocity.y - projectileProfile.gravity, velocity.z);
      }

      return bestTick;
   }

   private Vec3d createVelocity(float yaw, float pitch, double speed) {
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      double x = -Math.sin(yawRad) * Math.cos(pitchRad) * speed;
      double y = -Math.sin(pitchRad) * speed;
      double z = Math.cos(yawRad) * Math.cos(pitchRad) * speed;
      return new Vec3d(x, y, z);
   }

   private Vec3d getTargetPoint(LivingEntity target, double futureTicks) {
      double scaledTicks = Math.min(futureTicks * this.predict.getValue(), 60.0);
      Vec3d velocity = target.getVelocity().multiply(scaledTicks);
      Vec3d basePos = target.getPos().add(velocity);
      double aimHeight = MathHelper.clamp(target.getHeight() * 0.75, 0.85, 1.6);
      return basePos.add(0.0, aimHeight, 0.0);
   }

   private boolean isInsideFov(LivingEntity entity) {
      Vec3d point = this.getTargetPoint(entity, 0.0);
      Turns angle = MathAngle.calculateAngle(point);
      float yawDiff = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - mc.player.getYaw()));
      return Math.hypot(yawDiff, Math.abs(angle.getPitch() - mc.player.getPitch())) <= this.fov.getValue();
   }

   private double horizontalDistance(Vec3d point) {
      Vec3d eyePos = mc.player.getEyePos();
      double dx = point.x - eyePos.x;
      double dz = point.z - eyePos.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private ProjectHelper.ProjectileProfile resolveActiveWeapon() {
      if (mc.player == null) {
         return null;
      } else {
         ItemStack active = mc.player.getActiveItem();
         ProjectHelper.ProjectileProfile fromActive = ProjectHelper.ProjectileProfile.fromStack(active, mc.player.getItemUseTime(), this.weapons);
         return fromActive != null
            ? fromActive
            : ProjectHelper.ProjectileProfile.fromStack(mc.player.getMainHandStack(), mc.player.getItemUseTime(), this.weapons);
      }
   }

   public MultiSelectSetting getWeapons() {
      return this.weapons;
   }

   public SliderSettings getDistance() {
      return this.distance;
   }

   public SliderSettings getFov() {
      return this.fov;
   }

   public SliderSettings getPredict() {
      return this.predict;
   }

   public BooleanSetting getMoveCorrection() {
      return this.moveCorrection;
   }

   public BooleanSetting getThroughWalls() {
      return this.throughWalls;
   }

   public SelectSetting getSorting() {
      return this.sorting;
   }

   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   public long getLastHudTargetAt() {
      return this.lastHudTargetAt;
   }

   public ProjectHelper.ProjectileProfile getLastProjectileProfile() {
      return this.lastProjectileProfile;
   }

   public long getLastProjectileProfileAt() {
      return this.lastProjectileProfileAt;
   }

   public Turns getCurrentRotation() {
      return this.currentRotation;
   }

   public Turns getServerRotation() {
      return this.serverRotation;
   }

   public boolean isAiming() {
      return this.isAiming;
   }

   private static final class ProjectileProfile {
      final double speed;
      final double drag;
      final double gravity;

      ProjectileProfile(double speed, double drag, double gravity) {
         this.speed = speed;
         this.drag = drag;
         this.gravity = gravity;
      }

      static ProjectHelper.ProjectileProfile fromStack(ItemStack stack, int useTicks, MultiSelectSetting weapons) {
         if (stack == null || stack.isEmpty()) {
            return null;
         } else if (stack.isOf(Items.BOW) && weapons.isSelected("Лук")) {
            float pullProgress = useTicks / 20.0F;
            pullProgress = (pullProgress * pullProgress + pullProgress * 2.0F) / 3.0F;
            float var5;
            return (var5 = Math.min(pullProgress, 1.0F)) <= 0.1F ? null : new ProjectHelper.ProjectileProfile(3.0 * var5, 0.99, 0.05);
         } else if (stack.isOf(Items.TRIDENT) && weapons.isSelected("Трезубец")) {
            return useTicks < 10 ? null : new ProjectHelper.ProjectileProfile(2.5, 0.99, 0.05);
         } else if (stack.isOf(Items.CROSSBOW) && weapons.isSelected("Арбалет")) {
            return !CrossbowItem.isCharged(stack) ? null : new ProjectHelper.ProjectileProfile(3.15, 0.99, 0.05);
         } else {
            return stack.getItem() instanceof SnowballItem && weapons.isSelected("Снежок") ? new ProjectHelper.ProjectileProfile(1.5, 0.99, 0.03) : null;
         }
      }
   }
}
