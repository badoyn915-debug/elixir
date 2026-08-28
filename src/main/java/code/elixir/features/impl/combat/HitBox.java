package code.elixir.features.impl.combat;

import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.BoundingBoxControlEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.features.aura.rotations.constructor.RotateConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.combat.TargetFinder;
import code.elixir.utils.math.task.TaskPriority;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class HitBox extends Module {
   private final SliderSettings attackRange = new SliderSettings("Дистанция", "Дистанция поиска целей").setValue(3.5F).range(1.0F, 6.0F);
   private final SliderSettings expandSize = new SliderSettings("Расширение", "Размер расширения хитбоксов").setValue(0.5F).range(0.1F, 2.0F);
   private final SelectSetting hitboxMode = new SelectSetting("Режим хитбокса", "Тип расширения хитбокса").value("Умная", "Стандартная").selected("Умная");
   private final SliderSettings standardExpandXZ = new SliderSettings("Расширение XZ", "Расширение хитбокса по X и Z")
      .setValue(0.5F)
      .range(0.1F, 2.0F)
      .visible(() -> this.hitboxMode.isSelected("Стандартная"));
   private final SliderSettings standardExpandY = new SliderSettings("Расширение Y", "Расширение хитбокса по Y")
      .setValue(0.5F)
      .range(0.1F, 2.0F)
      .visible(() -> this.hitboxMode.isSelected("Стандартная"));
   private final SelectSetting rotationMode = new SelectSetting("Режим ротации", "Тип наведения на хитбокс").value("Плавная", "Резкая").selected("Плавная");
   private final MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует цели по типу")
      .value("Игроки", "Мобы", "Животные", "Друзья")
      .selected("Игроки", "Мобы");
   private final BooleanSetting ignoreWalls = new BooleanSetting("Сквозь стены", "Позволяет атаковать через стены").setValue(true);
   private final BooleanSetting antiReach = new BooleanSetting("Анти-Рич", "Отменяет атаку если слишком далеко от оригинального хитбокса").setValue(false);
   private final SliderSettings antiReachDistance = new SliderSettings("Макс. дистанция", "Максимальная дистанция для Анти-Рич")
      .setValue(3.0F)
      .range(1.0F, 6.0F)
      .visible(() -> this.antiReach.isValue());
   private final ColorSetting hitboxColor = new ColorSetting("Цвет хитбокса", "Цвет расширенного хитбокса").value(1684313855);
   private final BooleanSetting renderHitbox = new BooleanSetting("Рендер", "Отображает расширенные хитбоксы").setValue(true);
   private final TargetFinder targetSelector = new TargetFinder();
   private final Random random = new Random();
   private final TurnsConfig rotationConfig = new TurnsConfig(new RotateConstructor("HitBox") {
      @Override
      public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
         return targetAngle;
      }

      @Override
      public Vec3d randomValue() {
         return new Vec3d(0.0, 0.0, 0.0);
      }
   }, true, true);
   private Vec3d mpCurrent = Vec3d.ZERO;
   private float mpOrbit = 0.0F;
   private long mpLastUpdate = 0L;
   private int mpLastTargetId = -1;
   private float smoothedYaw;
   private float smoothedPitch;
   private int smoothTargetId = -1;
   private int snapTargetId = -1;
   private long snapEndTime = 0L;
   private int hitFlashTargetId = -1;
   private long hitFlashEndTime = 0L;

   public static HitBox getInstance() {
      return Instance.get(HitBox.class);
   }

   public HitBox() {
      super("HitBox", ModuleCategory.COMBAT);
      this.setup(
         new Setting[]{
            this.attackRange,
            this.expandSize,
            this.hitboxMode,
            this.standardExpandXZ,
            this.standardExpandY,
            this.rotationMode,
            this.targetType,
            this.ignoreWalls,
            this.antiReach,
            this.antiReachDistance,
            this.hitboxColor,
            this.renderHitbox
         }
      );
   }

   @Override
   public void deactivate() {
      this.resetRotation();
      super.deactivate();
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (this.state) {
         if (e.getType() == 0 && mc.player != null && mc.world != null) {
            this.targetSelector.searchTargets(mc.world.getEntities(), this.attackRange.getValue(), 360.0F, this.ignoreWalls.isValue());
            TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getSelected());
            this.targetSelector.validateTarget(filter::isValid);
            LivingEntity target = this.targetSelector.getCurrentTarget();
            if (target == null || !this.isLookingAtExpandedHitbox(target) || this.isAuraEnabled()) {
               this.resetRotation();
            } else if (this.isSharpMode()) {
               if (this.shouldApplySharpRotation(target)) {
                  this.rotateSharp(target);
               } else {
                  this.resetRotation();
               }
            } else {
               this.rotateSmooth(target);
            }
         }
      }
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      if (this.state) {
         if (e.getEntity() instanceof LivingEntity living && mc.player != null) {
            if (this.isSharpMode()) {
               this.snapTargetId = living.getId();
               this.snapEndTime = System.currentTimeMillis() + 180L;
            }

            if (!this.isAuraEnabled() && this.isLookingAtExpandedHitbox(living)) {
               this.hitFlashTargetId = living.getId();
               this.hitFlashEndTime = System.currentTimeMillis() + 220L;
            }

            if (this.antiReach.isValue()) {
               Box originalBox = living.getType().getDimensions().getBoxAt(living.getPos());
               Vec3d eyes = mc.player.getEyePos();
               if (eyes.distanceTo(this.getClosestPoint(eyes, originalBox)) > this.antiReachDistance.getValue()) {
                  e.setCancelled(true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onBoundingBox(BoundingBoxControlEvent e) {
      if (this.state && this.isStandardHitboxMode()) {
         Entity class_12972 = e.getEntity();
         LivingEntity living;
         if (class_12972 instanceof LivingEntity && (living = (LivingEntity)class_12972) != mc.player && living.isAlive()) {
            TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getSelected());
            if (filter.isValid(living)) {
               e.setBox(this.getStandardExpandedBox(living));
            }
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.state && this.renderHitbox.isValue() && mc.world != null) {
         TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getSelected());

         for (Entity entity : mc.world.getEntities()) {
            LivingEntity living;
            if (entity instanceof LivingEntity && (living = (LivingEntity)entity) != mc.player && living.isAlive() && filter.isValid(living)) {
               Box box = this.getExpandedBox(living);
               Render3D.drawBox(e.getStack().peek(), box, this.hitboxColor.getColor(), 1.5F, true, true, true);
               if (this.isHitFlashing(living)) {
                  int flashColor = ColorAssist.setAlpha(this.hitboxColor.getColor(), 255);
                  Render3D.drawBox(e.getStack().peek(), box, flashColor, 1.8F, true, false, true);
                  Render3D.drawBox(e.getStack().peek(), box, flashColor, 1.0F, false, true, true);
                  Render3D.drawBox(e.getStack().peek(), box, flashColor, 1.0F, false, true, true);
                  Render3D.drawBox(e.getStack().peek(), box, flashColor, 1.0F, false, true, true);
               }
            }
         }
      }
   }

   private void rotateSmooth(LivingEntity target) {
      long now = System.currentTimeMillis();
      int tid = target.getId();
      if (tid != this.mpLastTargetId) {
         this.mpLastTargetId = tid;
         this.mpCurrent = Vec3d.ZERO;
         this.mpOrbit = this.random.nextFloat() * (float) (Math.PI * 2);
         this.mpLastUpdate = now;
         Turns current = TurnsConnection.INSTANCE.getRotation();
         if (current == null && mc.player != null) {
            current = new Turns(mc.player.getYaw(), mc.player.getPitch());
         }

         if (current != null) {
            this.smoothedYaw = current.getYaw();
            this.smoothedPitch = current.getPitch();
         }
      }

      long dtMs = Math.min(now - this.mpLastUpdate, 60L);
      this.mpLastUpdate = now;
      float dtScale = (float)dtMs / 50.0F;
      this.mpOrbit = this.mpOrbit + (0.36F + (this.random.nextFloat() - 0.5F) * 0.036F) * dtScale;
      Vec3d orbitPoint = this.calculateOrbitPoint(this.mpOrbit);
      if (this.mpCurrent == Vec3d.ZERO) {
         this.mpCurrent = orbitPoint;
      } else {
         float alpha = MathHelper.clamp((float)dtMs / 120.0F, 0.0F, 1.0F);
         alpha = alpha * alpha * (3.0F - 2.0F * alpha);
         this.mpCurrent = new Vec3d(
            MathHelper.lerp(alpha, this.mpCurrent.x, orbitPoint.x),
            MathHelper.lerp(alpha, this.mpCurrent.y, orbitPoint.y),
            MathHelper.lerp(alpha, this.mpCurrent.z, orbitPoint.z)
         );
      }

      Box box = this.getExpandedBox(target);
      if (this.isStandardHitboxMode()) {
         this.rotateToStandardBox(target, box);
      } else {
         double x = box.minX + (box.maxX - box.minX) * (0.5 + this.mpCurrent.x * 0.5);
         double y = box.minY + (box.maxY - box.minY) * MathHelper.clamp(this.mpCurrent.y, 0.36, 0.82);
         double z = box.minZ + (box.maxZ - box.minZ) * (0.5 + this.mpCurrent.z * 0.5);
         Vec3d targetPoint = new Vec3d(x, y, z);
         Turns targetAngle = MathAngle.calculateAngle(targetPoint);
         float yawError = MathHelper.wrapDegrees(targetAngle.getYaw() - this.smoothedYaw);
         float pitchError = targetAngle.getPitch() - this.smoothedPitch;
         float yawSpeed = 60.0F * dtScale;
         float pitchSpeed = 45.0F * dtScale;
         this.smoothedYaw = this.smoothedYaw + MathHelper.clamp(yawError, -yawSpeed, yawSpeed);
         this.smoothedPitch = this.smoothedPitch + MathHelper.clamp(pitchError, -pitchSpeed, pitchSpeed);
         float jitterYaw = (float)(Math.sin(now / 40.0) * 1.5);
         float jitterPitch = (float)(Math.cos(now / 40.0) * 0.8);
         Turns finalRotation = new Turns(this.smoothedYaw + jitterYaw, this.smoothedPitch + jitterPitch);
         TurnsConnection.INSTANCE.rotateTo(finalRotation, 1, this.rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         this.smoothTargetId = tid;
      }
   }

   private void rotateSharp(LivingEntity target) {
      if (mc.player != null) {
         Box box = this.getExpandedBox(target);
         Vec3d targetPoint = this.getClosestPoint(mc.player.getEyePos(), box);
         Turns targetAngle = MathAngle.calculateAngle(targetPoint);
         Turns.VecRotation finalRotation = new Turns.VecRotation(targetAngle, targetAngle.toVector());
         TurnsConnection.INSTANCE.rotateTo(finalRotation, target, 1, this.rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         this.smoothTargetId = target.getId();
      }
   }

   private Vec3d calculateOrbitPoint(float o) {
      float y = 0.56F
         + 0.1F * (float)Math.sin(o * 0.74F + 0.9F)
         + 0.06F * (float)Math.sin(o * 1.28F + 2.2F)
         - 0.05F * (float)(0.5 + 0.5 * Math.sin(o * 0.35F + 1.7F));
      float x = 0.22F * (float)Math.sin(o) + 0.06F * (float)Math.sin(o * 1.85F + 0.25F);
      float z = 0.08F * (float)Math.cos(o * 0.92F + 0.4F) + 0.03F * (float)Math.sin(o * 1.35F + 1.1F);
      return new Vec3d(x, y, z);
   }

   private void resetRotation() {
      if (this.smoothTargetId != -1) {
         TurnsConnection.INSTANCE.clear();
         TurnsConnection.INSTANCE.setRotation(null);
         this.smoothTargetId = -1;
         this.mpLastTargetId = -1;
         this.mpCurrent = Vec3d.ZERO;
      }

      if (!this.isSharpMode()) {
         this.snapTargetId = -1;
         this.snapEndTime = 0L;
      }

      if (this.hitFlashEndTime < System.currentTimeMillis()) {
         this.hitFlashTargetId = -1;
         this.hitFlashEndTime = 0L;
      }
   }

   private boolean isLookingAtExpandedHitbox(LivingEntity target) {
      if (mc.player == null) {
         return false;
      } else {
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         float yawRad = (float)Math.toRadians(yaw);
         float pitchRad = (float)Math.toRadians(pitch);
         float x = (float)(-Math.sin(yawRad) * Math.cos(pitchRad));
         float y = (float)(-Math.sin(pitchRad));
         float z = (float)(Math.cos(yawRad) * Math.cos(pitchRad));
         Vec3d direction = new Vec3d(x, y, z);
         Vec3d eyes = mc.player.getEyePos();
         Box expandedBox = this.getExpandedBox(target);
         if (expandedBox.contains(eyes)) {
            return true;
         } else {
            Vec3d end = eyes.add(direction.multiply(this.attackRange.getValue() + 2.0));
            return expandedBox.raycast(eyes, end).isPresent();
         }
      }
   }

   private boolean isAuraEnabled() {
      AttackAura aura = AttackAura.getInstance();
      return aura != null && aura.state;
   }

   private boolean isStandardHitboxMode() {
      return this.hitboxMode.isSelected("Стандартная");
   }

   private boolean isSharpMode() {
      return this.rotationMode.isSelected("Резкая");
   }

   private boolean shouldApplySharpRotation(LivingEntity target) {
      long now = System.currentTimeMillis();
      if (target.getId() != this.snapTargetId) {
         return false;
      } else if (now > this.snapEndTime) {
         this.snapTargetId = -1;
         this.snapEndTime = 0L;
         return false;
      } else {
         return true;
      }
   }

   private boolean isHitFlashing(LivingEntity target) {
      long now = System.currentTimeMillis();
      if (target.getId() != this.hitFlashTargetId) {
         return false;
      } else if (now > this.hitFlashEndTime) {
         this.hitFlashTargetId = -1;
         this.hitFlashEndTime = 0L;
         return false;
      } else {
         return true;
      }
   }

   private Vec3d getClosestPoint(Vec3d point, Box box) {
      return new Vec3d(
         MathHelper.clamp(point.x, box.minX, box.maxX), MathHelper.clamp(point.y, box.minY, box.maxY), MathHelper.clamp(point.z, box.minZ, box.maxZ)
      );
   }

   private Box getExpandedBox(LivingEntity target) {
      return this.isStandardHitboxMode() ? this.getStandardExpandedBox(target) : this.getOriginalBox(target).expand(this.expandSize.getValue());
   }

   private Box getStandardExpandedBox(LivingEntity target) {
      return this.getOriginalBox(target).expand(this.standardExpandXZ.getValue(), this.standardExpandY.getValue(), this.standardExpandXZ.getValue());
   }

   private Box getOriginalBox(LivingEntity target) {
      return target.getType().getDimensions().getBoxAt(target.getPos());
   }

   private void rotateToStandardBox(LivingEntity target, Box box) {
      if (mc.player != null) {
         Vec3d targetPoint = this.getClosestPoint(mc.player.getEyePos(), box);
         Turns targetAngle = MathAngle.calculateAngle(targetPoint);
         float yawError = MathHelper.wrapDegrees(targetAngle.getYaw() - this.smoothedYaw);
         float pitchError = targetAngle.getPitch() - this.smoothedPitch;
         this.smoothedYaw = this.smoothedYaw + MathHelper.clamp(yawError, -72.0F, 72.0F);
         this.smoothedPitch = this.smoothedPitch + MathHelper.clamp(pitchError, -54.0F, 54.0F);
         Turns finalRotation = new Turns(this.smoothedYaw, this.smoothedPitch);
         TurnsConnection.INSTANCE.rotateTo(finalRotation, 1, this.rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         this.smoothTargetId = target.getId();
      }
   }

   public SliderSettings getAttackRange() {
      return this.attackRange;
   }

   public SliderSettings getExpandSize() {
      return this.expandSize;
   }

   public SelectSetting getHitboxMode() {
      return this.hitboxMode;
   }

   public SliderSettings getStandardExpandXZ() {
      return this.standardExpandXZ;
   }

   public SliderSettings getStandardExpandY() {
      return this.standardExpandY;
   }

   public SelectSetting getRotationMode() {
      return this.rotationMode;
   }

   public MultiSelectSetting getTargetType() {
      return this.targetType;
   }

   public BooleanSetting getIgnoreWalls() {
      return this.ignoreWalls;
   }

   public BooleanSetting getAntiReach() {
      return this.antiReach;
   }

   public SliderSettings getAntiReachDistance() {
      return this.antiReachDistance;
   }

   public ColorSetting getHitboxColor() {
      return this.hitboxColor;
   }

   public BooleanSetting getRenderHitbox() {
      return this.renderHitbox;
   }

   public TargetFinder getTargetSelector() {
      return this.targetSelector;
   }

   public Random getRandom() {
      return this.random;
   }

   public TurnsConfig getRotationConfig() {
      return this.rotationConfig;
   }

   public Vec3d getMpCurrent() {
      return this.mpCurrent;
   }

   public float getMpOrbit() {
      return this.mpOrbit;
   }

   public long getMpLastUpdate() {
      return this.mpLastUpdate;
   }

   public int getMpLastTargetId() {
      return this.mpLastTargetId;
   }

   public float getSmoothedYaw() {
      return this.smoothedYaw;
   }

   public float getSmoothedPitch() {
      return this.smoothedPitch;
   }

   public int getSmoothTargetId() {
      return this.smoothTargetId;
   }

   public int getSnapTargetId() {
      return this.snapTargetId;
   }

   public long getSnapEndTime() {
      return this.snapEndTime;
   }

   public int getHitFlashTargetId() {
      return this.hitFlashTargetId;
   }

   public long getHitFlashEndTime() {
      return this.hitFlashEndTime;
   }
}
