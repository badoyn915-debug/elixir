package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.GameUpdateEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.PostMotionEvent;
import code.elixir.events.player.PostTickEvent;
import code.elixir.events.player.SprintEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.components.RotationsSystem;
import code.elixir.features.impl.combat.components.interpolation.BestPoint;
import code.elixir.features.impl.combat.components.interpolation.HitChance;
import code.elixir.features.impl.combat.components.interpolation.HumanCps;
import code.elixir.features.impl.combat.components.rotations.FuntimeRotation;
import code.elixir.features.impl.combat.components.rotations.FuntimeTestRotation;
import code.elixir.features.impl.combat.components.rotations.KinematicRotation;
import code.elixir.features.impl.combat.components.rotations.LegitRotation;
import code.elixir.features.impl.combat.components.rotations.NeuroRotation;
import code.elixir.features.impl.combat.components.rotations.PolarACRotation;
import code.elixir.features.impl.combat.components.rotations.SlothACRotation;
import code.elixir.features.impl.combat.components.rotations.SpookyTimeRotation;
import code.elixir.features.impl.combat.components.rotations.TestRotation;
import code.elixir.features.impl.movement.AirStuck;
import code.elixir.features.impl.movement.ElytraTarget;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.mixins.ClientPlayerEntityAccessor;
import code.elixir.mixins.ILivingEntity;
import code.elixir.utils.client.FakePlayerManager;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.AuraMixinHelper;
import code.elixir.utils.features.aura.FreeLookStorage;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.combat.IdealHitUtils;
import code.elixir.utils.features.combat.LegitSprintReset;
import code.elixir.utils.features.combat.LegitTestSprintReset;
import code.elixir.utils.features.combat.PredictUtils;
import code.elixir.utils.features.combat.TestSprintReset;
import code.elixir.utils.features.rotate.MultipointUtils;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.features.rotate.RotationUtils;
import code.elixir.utils.interactions.inv.HotbarUtil;
import code.elixir.utils.interactions.inv.SlotSearchResult;
import code.elixir.utils.interactions.item.ItemUseHelper;
import code.elixir.utils.interactions.simulate.MovingUtil;
import code.elixir.utils.math.TimerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class AttackAura extends Module {
   public static float RANGE_MARGIN = 0.153F;
   public static final RadioSetting aimMode = new RadioSetting("Наводка", "Наводка", new String[]{"Smooth"}, "Smooth");
   public final RadioSetting rotationType = new RadioSetting("Ротация", "Ротация", new String[]{"SlothAC", "PolarAC", "SpookyTime", "Фантайм", "Фантайм Тест", "Легит"}, "SlothAC");
   private final MultiSelectSetting targets = new MultiSelectSetting("Таргеты", "Таргеты")
      .value("Игроки", "Голые", "Невидимки", "Мирные", "Мобы")
      .selected("Игроки", "Голые", "Невидимки", "Мобы");
   public final SliderSettings attackRange = new SliderSettings("Дистанция атаки", "Дистанция атаки").setValue(3.0F).range(0.0F, 6.0F).step(0.05F);
   private final SliderSettings aimRange = new SliderSettings("Дистанция наводки", "Дистанция наводки").setValue(3.0F).range(0.0F, 6.0F).step(0.05F);
   private final SliderSettings elytraAimRange = new SliderSettings("Дистанция на элитрах", "Дистанция на элитрах")
      .setValue(50.0F)
      .range(10.0F, 100.0F)
      .step(0.05F);
   public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Умные криты").setValue(true);
   public final BooleanSetting onlyCrits = new BooleanSetting("Только криты", "Бить исключительно в тайминг критического удара при прыжке/падении").setValue(false);
   public final RadioSetting sprintReset = new RadioSetting(
      "Тип спринта", "Сброс спринта перед ударом", new String[]{"Пакетно", "Легитно", "Легит Тест"}, "Легитно"
   );
   private final BooleanSetting throughWalls = new BooleanSetting("Бить через стены", "Бить через стены").setValue(true);
   private final BooleanSetting unpressShield = new BooleanSetting("Отжимать щит", "Отжимать щит").setValue(false);
   private final BooleanSetting breakShield = new BooleanSetting("Ломать щит", "Ломать щит").setValue(true);
   private final BooleanSetting attackOnEating = new BooleanSetting("Не бить когда ешь", "Не бить когда ешь").setValue(true);
   private final RadioSetting moveFix = new RadioSetting("Коррекция", "Коррекия", new String[]{"Нет", "Свободная", "Сфокусированная", "Таргет"}, "Нет");
   private final RadioSetting priority = new RadioSetting(
      "Приоритет", "Приоритет", new String[]{"Оптимальный", "Дистанция", "Здоровье", "Угол поворота", "Никакой"}, "Оптимальный"
   );
   private static final double OPTIMAL_DISTANCE_WEIGHT = 3.0;
   private static final double OPTIMAL_ANGLE_WEIGHT = 2.0;
   private static final double OPTIMAL_HEALTH_WEIGHT = 1.5;
   private static final double ARMOR_HEALTH_DIVISOR = 20.0;
   private static final double ANGLE_BODY_HEIGHT_DIVISOR = 2.0;
   private static final double BLOCKING_BONUS = 2.0;
   private static final double NAKED_PENALTY = 1.5;
   private static final float LOW_HP_THRESHOLD = 8.0F;
   private static final double LOW_HP_BONUS = 1.0;
   private static final double VISIBLE_TARGET_BONUS = 2.0;
   private static final float STICKY_TARGET_RANGE_BONUS = 0.75F;
   private LivingEntity target;
   private LivingEntity lastTarget;
   private Vec2f currentRotations = new Vec2f(0.0F, 0.0F);
   private Vec2f targetRotations = new Vec2f(0.0F, 0.0F);
   private final TimerUtils attackTimer = new TimerUtils();
   private final SlothACRotation slothACRotation = new SlothACRotation();
   private final PolarACRotation polarACRotation = new PolarACRotation();
   private final KinematicRotation kinematicRotation = new KinematicRotation();
   private final LegitRotation legitRotation = new LegitRotation();
   private final FuntimeRotation funtimeRotation = new FuntimeRotation();
   private final FuntimeTestRotation funtimeTestRotation = new FuntimeTestRotation();
   private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
   private final NeuroRotation neuroRotation = new NeuroRotation();
   private final TestRotation testRotation = new TestRotation();
   private final TimerUtils backTimer = new TimerUtils();
   private long cps = 0L;
   private int ticksToAttack = 0;
   private int snapAttackAge = -1;
   private boolean snapAttackQueued = false;
   private LivingEntity snapAttackTarget = null;
   private int shieldRestoreSlot = -1;
   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;
   private String lastRotationMode = "";
   private boolean movementCanCrit;
   private float focusMoveYaw = Float.NaN;
   private boolean silentLookReleased = false;
   private int lostTargetTicks = 0;
   private boolean gameUpdateRotatedThisTick;
   public static float adjYaw;
   public static float adjPitch;
   public static float otvodkaYaw;
   public static float otvodkaPitch;
   public boolean isRotated;
   private float targetMoveYaw = Float.NaN;
   private int comboHitCount = 0;
   private long lastAttackMillis = 0L;
   private long jumpStartTime = 0L;
   private boolean wasOnGround = true;

   public static AttackAura getInstance() {
      return Instance.get(AttackAura.class);
   }

   public AttackAura() {
      super("AttackAura", "AttackAura", ModuleCategory.COMBAT);
      this.setup(
         new Setting[]{
            this.rotationType,
            this.targets,
            this.attackRange,
            this.aimRange,
            this.elytraAimRange,
            this.smartCrit,
            this.onlyCrits,
            this.sprintReset,
            this.attackOnEating,
            this.throughWalls,
            this.unpressShield,
            this.breakShield,
            this.moveFix,
            this.priority
         }
      );
   }

   public boolean isSyncTpsEnabled() {
      return TpsSync.getInstance() != null && TpsSync.getInstance().isState();
   }

   private int moveFixIndex() {
      return Arrays.asList(this.moveFix.getOptions()).indexOf(this.moveFix.get());
   }

   public boolean isCombatRotationActive() {
      RotationStorage storage = RotationStorage.instance;
      return storage != null && storage.isCameraReturning() ? false : this.isState() && this.target != null;
   }

   public boolean hasMoveCorrection() {
      boolean isBypass = this.rotationType.get().equals("SlothAC") || this.rotationType.get().equals("PolarAC");
      return this.isCombatRotationActive() && (isBypass || this.moveFixIndex() != 0);
   }

   public boolean isCameraReturning() {
      RotationStorage storage = RotationStorage.instance;
      return storage != null && storage.isCameraReturning();
   }

   @EventHandler
   public void onPlayerTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.lastYaw++;
         this.updateTarget();
      }
   }

   @EventHandler
   public void onAttackEntity(AttackEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.getEntity() instanceof LivingEntity living) {
            if (this.isValidTarget(living)) {
               this.target = living;
            }
         }
      }
   }

   @EventHandler
   public void onMoveInput(InputEvent event) {
      this.applyMoveFix(event);
      this.applyKeyboardMoveFix(event);
      if (this.target != null) {
         if (this.isTestSprintReset()) {
            TestSprintReset.applyInputReset(event);
         } else if (this.isLegitSprintReset()) {
            LegitSprintReset.applyInputReset(event);
         }
      }
   }

   @EventHandler
   public void onSprint(SprintEvent event) {
      if (this.target != null && mc.player != null) {
         if (this.isTestSprintReset()) {
            TestSprintReset.handleSprintEvent(event);
         } else if (this.isLegitSprintReset() && LegitSprintReset.isServerSprinting()) {
            event.setSprinting(false);
         } else if (this.isLegitTestSprintReset() && LegitSprintReset.isServerSprinting()) {
            event.setSprinting(false);
         }
      }
   }

   public boolean isSprintResetEnabled() {
      return this.isState();
   }

   public boolean isLegitSprintReset() {
      return "Легитно".equals(this.sprintReset.get());
   }

   public boolean isLegitTestSprintReset() {
      return "Легит Тест".equals(this.sprintReset.get());
   }

   public boolean isPacketSprintReset() {
      return "Пакетно".equals(this.sprintReset.get());
   }

   public boolean isTestSprintReset() {
      return false;
   }

   public boolean usesSilentRotation() {
      return true;
   }

   private void applyMoveFix(InputEvent event) {
      if (mc.player != null && this.target != null && this.moveFixIndex() != 0) {
         if (this.moveFixIndex() == 1) {
            MovingUtil.fixMovementFree(event);
         }
      }
   }

   private void applyKeyboardMoveFix(InputEvent event) {
      if (mc.player != null && mc.world != null && this.target != null && this.moveFixIndex() != 0) {
         if (this.moveFixIndex() == 2) {
            MovingUtil.fixMovementFree(event, mc.player.getYaw(), this.getTargetDirectionYaw());
         } else if (this.moveFixIndex() == 3) {
            this.applyTargetMoveFix(event);
         }
      }
   }

   private void applyTargetMoveFix(InputEvent event) {
      if (mc.player != null && this.target != null) {
         double dx = this.target.getX() - mc.player.getX();
         double dz = this.target.getZ() - mc.player.getZ();
         double distSq = dx * dx + dz * dz;
         float rawYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         if (Float.isNaN(this.targetMoveYaw)) {
            this.targetMoveYaw = rawYaw;
         } else {
            float yawDiff = MathHelper.wrapDegrees(rawYaw - this.targetMoveYaw);
            this.targetMoveYaw += yawDiff * 0.45F;
         }

         if (distSq > 0.64) {
            event.setForward(1.0F);
            event.setStrafe(0.0F);
         }

         float currentYaw = mc.player.getYaw();
         MovingUtil.fixMovementFree(event, currentYaw, this.targetMoveYaw);
      } else {
         this.targetMoveYaw = Float.NaN;
      }
   }

   private float getTargetDirectionYaw() {
      float raw = RotationUtils.getRotations(this.target.getBoundingBox().getCenter()).x;
      this.focusMoveYaw = !Float.isNaN(this.focusMoveYaw) && !(Math.abs(MathHelper.wrapDegrees(raw - this.focusMoveYaw)) > 90.0F)
         ? MathHelper.wrapDegrees(this.focusMoveYaw + MathHelper.wrapDegrees(raw - this.focusMoveYaw) * 0.25F)
         : raw;
      return this.focusMoveYaw;
   }

   @EventHandler
   private void onGameUpdate(GameUpdateEvent e) {
      if (mc.player != null && mc.world != null && this.target != null) {
         this.gameUpdateRotatedThisTick = true;
         this.rotate();
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.flushShieldRestore();
         if (this.ticksToAttack > 0) {
            this.ticksToAttack--;
         }

         this.updateTarget();
         if (this.target != null) {
            this.rotate();
         }

         this.processAttack();
         this.gameUpdateRotatedThisTick = false;
      }
   }

   private boolean isPacketCritAttackPhase() {
      PacketCriticals packetCriticals = CombatModules.getPacketCriticals();
      return packetCriticals != null && packetCriticals.shouldDeferMainTickAttack();
   }

   @EventHandler
   public void onPost(PostTickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.updateMovementCanCrit();
         this.processAttack();
      }
   }

   @EventHandler
   public void onPostMotion(PostMotionEvent e) {
      if (mc.player != null && mc.world != null) {
         this.updateMovementCanCrit();
         this.processAttack();
      }
   }

   private void updateMovementCanCrit() {
      if (mc.player == null) {
         this.movementCanCrit = false;
         this.jumpStartTime = 0L;
         this.wasOnGround = true;
      } else {
         boolean onGround = mc.player.isOnGround();
         if (this.wasOnGround && !onGround) {
            this.jumpStartTime = System.currentTimeMillis();
         } else if (onGround) {
            this.jumpStartTime = 0L;
         }
         this.wasOnGround = onGround;

         AirStuck airStuck = Instance.get(AirStuck.class);
         if (airStuck != null && airStuck.isState()) {
            this.movementCanCrit = true;
         } else {
            this.movementCanCrit = !onGround && mc.player.fallDistance > 0.0F && mc.player.getVelocity().y < 0.0;
         }
      }
   }

   private void processAttack() {
      this.updateTarget();
      if (this.target != null) {
         if (this.onlyCrits.isValue() && !IdealHitUtils.isInCritWindow(this.target)) {
            return;
         }

         if (this.shouldAttack() || this.shouldUseQueuedSnapAttack()) {
            if (this.attackOnEating.isValue() && this.shouldBlockAttackWhileUsingItem()) {
               return;
            }

            if (this.isLegitSprintReset() && !LegitSprintReset.prepareForAttack()) {
               return;
            }

            if (this.isLegitTestSprintReset()) {
               LegitTestSprintReset.prepareForAttack();
            }

            if (this.isTestSprintReset() && !TestSprintReset.prepareForAttack()) {
               return;
            }

            if (this.isSnapRotationActive() && !this.prepareSnapAttack()) {
               return;
            }

            this.attack();
            this.resetSnapAttack();
         }
      } else {
         this.cps = System.currentTimeMillis();
         this.backTimer.reset();
         adjPitch = 0.0F;
         adjYaw = 0.0F;
         this.kinematicRotation.reset();
         this.neuroRotation.reset();
         this.legitRotation.reset();
         this.funtimeRotation.reset();
         this.funtimeTestRotation.reset();
         this.spookyTimeRotation.reset();
         this.testRotation.reset();
         this.focusMoveYaw = Float.NaN;
         this.ticksToAttack = 0;
         this.resetSnapAttack();
      }
   }

   public void Rotate() {
      this.rotate();
   }

   private void rotate() {
      ElytraResolver elytraResolver = CombatModules.getElytraResolver();
      if (mc.player != null && elytraResolver != null && elytraResolver.isEscaping() && elytraResolver.getEscapePos() != null) {
         Vec3d escapePos = elytraResolver.getEscapePos();
         Vec3d eyePos = mc.player.getEyePos();
         double diffX = escapePos.x - eyePos.x;
         double diffY = escapePos.y - eyePos.y;
         double diffZ = escapePos.z - eyePos.z;
         double horizontal = Math.sqrt(diffX * diffX + diffZ * diffZ);
         float yaw = (float)(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
         float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, horizontal)));
         float sens = ((Double)mc.options.getMouseSensitivity().getValue()).floatValue();
         float gcd = sens * 0.6F + 0.2F;
         gcd = gcd * gcd * gcd * 1.2F;
         yaw -= yaw % gcd;
         pitch -= pitch % gcd;
         RotationStorage.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 25.0F, 20.0F, 1, 1, false);
      } else if (this.target != null && !this.isCameraReturning()) {
         if (!this.rotationType.get().equals(this.lastRotationMode)) {
            this.silentLookReleased = false;
            this.releaseSilentLook();
            this.slothACRotation.reset();
            this.polarACRotation.reset();
            this.kinematicRotation.reset();
            this.neuroRotation.reset();
            this.legitRotation.reset();
            this.funtimeRotation.reset();
            this.funtimeTestRotation.reset();
            this.spookyTimeRotation.reset();
            this.testRotation.reset();
            this.resetSnapAttack();
            this.silentLookReleased = true;
            this.lastRotationMode = this.rotationType.get();
         }

         if (this.isSnapRotationActive()) {
            this.updateSnapRotation(this.target);
         } else {
            RotationsSystem system;
            if (this.rotationType.get().equals("SlothAC")) {
               system = this.slothACRotation;
            } else if (this.rotationType.get().equals("PolarAC")) {
               system = this.polarACRotation;
            } else if (this.rotationType.get().equals("SpookyTime")) {
               system = this.spookyTimeRotation;
            } else if (this.rotationType.get().equals("Фантайм Тест")) {
               system = this.funtimeTestRotation;
            } else if (this.rotationType.get().equals("Фантайм")) {
               system = this.funtimeRotation;
            } else {
               system = this.legitRotation;
            }

            system.configureAim(this.attackRange.getValue(), this.throughWalls.isValue());
            system.updateRotations(this.target);
         }
      }
   }

   private void updateSnapRotation(LivingEntity target) {
      Vec3d point = MultipointUtils.getClosestPoint(target);
      if (point == null) {
         point = this.isFallingCritPhase() ? this.getStableBodyPoint(target) : target.getBoundingBox().getCenter();
      }

      Vec3d predicted = this.getPredictedRotationPoint(target, point);
      Vec2f targetRot;
      this.targetRotations = targetRot = RotationUtils.getRotations(predicted);
      this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      boolean isAttackSnapTick = mc.player.age <= this.snapAttackAge;
      boolean critSnapPhase = this.canAttackCritical(target) || IdealHitUtils.isInCritWindow(target);
      float finalYaw = targetRot.x;
      float finalPitch = targetRot.y;
      if (!isAttackSnapTick && !critSnapPhase) {
         finalYaw = FreeLookStorage.getFreeYaw();
         finalPitch = FreeLookStorage.getFreePitch();
      }

      RotationStorage.update(new Rotation(finalYaw, finalPitch), 360.0F, 360.0F, 360.0F, 360.0F, 0, 6, false);
   }

   private boolean isSnapRotationActive() {
      return this.rotationType.get().equals("Snap");
   }

   private boolean prepareSnapAttack() {
      if (!this.canAttackCritical(this.target)) {
         this.resetSnapAttack();
         return false;
      } else if (!this.snapAttackQueued) {
         this.snapAttackQueued = true;
         this.snapAttackAge = mc.player.age;
         this.snapAttackTarget = this.target;
         this.forceSnapRotation(this.target);
         return this.isSnapAimReadyForAttack();
      } else if (mc.player.age > this.snapAttackAge + 1) {
         this.resetSnapAttack();
         return false;
      } else if (mc.player.age >= this.snapAttackAge) {
         this.forceSnapRotation(this.target);
         return this.isSnapAimReadyForAttack();
      } else {
         return false;
      }
   }

   private void forceSnapRotation(LivingEntity target) {
      Vec3d point = MultipointUtils.getClosestPoint(target);
      if (point == null) {
         point = this.isFallingCritPhase() ? this.getStableBodyPoint(target) : target.getBoundingBox().getCenter();
      }

      Vec3d predicted = this.getPredictedRotationPoint(target, point);
      Vec2f targetRot;
      this.targetRotations = targetRot = RotationUtils.getRotations(predicted);
      this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      float finalYaw = targetRot.x;
      float finalPitch = targetRot.y;
      RotationStorage.update(new Rotation(finalYaw, finalPitch), 360.0F, 360.0F, 360.0F, 360.0F, 0, 6, false);
   }

   private boolean shouldUseQueuedSnapAttack() {
      if (!this.snapAttackQueued || mc.player == null || this.target == null || this.target != this.snapAttackTarget) {
         return false;
      } else if (mc.player.age > this.snapAttackAge + 1) {
         this.resetSnapAttack();
         return false;
      } else {
         return mc.player.age >= this.snapAttackAge;
      }
   }

   private boolean isSnapAimReadyForAttack() {
      if (this.target != null && mc.player != null) {
         float aimYaw = AuraMixinHelper.resolveYaw(mc.player.getYaw());
         float aimPitch = AuraMixinHelper.resolvePitch(mc.player.getPitch());
         float yawDiff = Math.abs(MathHelper.wrapDegrees(this.targetRotations.x - aimYaw));
         float pitchDiff = Math.abs(this.targetRotations.y - aimPitch);
         boolean onTarget = mc.player.isGliding();
         if (!onTarget) {
            EntityHitResult result = this.getAttackRaycastResult();
            onTarget = result != null && result.getEntity() == this.target;
         }

         return yawDiff <= 3.0F && pitchDiff <= 2.5F && onTarget;
      } else {
         return false;
      }
   }

   private EntityHitResult getAttackRaycastResult() {
      Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
      Vec3d lookVec;
      if (AuraMixinHelper.isCombatActive() && this.usesSilentRotation()) {
         float yaw = AuraMixinHelper.resolveYaw(mc.player.getYaw());
         float pitch = AuraMixinHelper.resolvePitch(mc.player.getPitch());
         lookVec = Vec3d.fromPolar(pitch, yaw);
      } else {
         lookVec = mc.player.getRotationVec(1.0F);
      }

      boolean isBypassMode = this.rotationType.get().equals("SlothAC") || this.rotationType.get().equals("PolarAC");
      float reach = isBypassMode ? Math.min(this.attackRange.getValue(), 3.0F) : this.attackRange.getValue() + 0.5F;
      Vec3d reachVec = eyePos.add(lookVec.multiply(reach));
      return ProjectileUtil.raycast(mc.player, eyePos, reachVec, mc.player.getBoundingBox().expand(reach), ex -> ex != mc.player && ex.isAlive(), reach * reach);
   }

   private Vec3d getPredictedRotationPoint(LivingEntity target, Vec3d point) {
      return mc.player != null && target != null && mc.player.isGliding() && target.isGliding() && CombatModules.isElytraPredictActive()
         ? PredictUtils.bypasselytrahacking(target)
         : point;
   }

   private Vec3d getStableBodyPoint(LivingEntity target) {
      Box box = target.getBoundingBox();
      return new Vec3d(box.getCenter().x, box.minY + box.getLengthY() * 0.72, box.getCenter().z);
   }

   private LivingEntity findTarget() {
      ArrayList<LivingEntity> entities = new ArrayList<>();

      for (Entity entity2 : mc.world.getEntities()) {
         LivingEntity living;
         if (entity2 instanceof LivingEntity && this.isValidTarget(living = (LivingEntity)entity2, false)) {
            entities.add(living);
         }
      }

      if (!entities.isEmpty() && this.isState()) {
         if (entities.size() == 1) {
            return entities.getFirst();
         } else {
            Comparator<LivingEntity> priorityComparator = this.getPriorityComparator();
            ElytraTarget elytraTarget = ElytraTarget.getInstance();
            if (elytraTarget != null && elytraTarget.isState() && elytraTarget.preferElytraTargets() && mc.player.isGliding()) {
               entities.sort(Comparator.<LivingEntity, Boolean>comparing(entity -> !entity.isGliding()).thenComparing(priorityComparator));
            } else {
               entities.sort(priorityComparator);
            }

            return entities.getFirst();
         }
      } else {
         return null;
      }
   }

   private Comparator<LivingEntity> getPriorityComparator() {
      String var1 = this.priority.get();

      return switch (var1) {
         case "Дистанция" -> Comparator.comparingDouble(entity -> mc.player.distanceTo(entity));
         case "Здоровье" -> Comparator.comparingDouble(this::getTargetHealthScore);
         case "Угол", "Угол поворота" -> Comparator.comparingDouble(this::getTargetAngleScore);
         case "Никакой" -> Comparator.comparingInt(Entity::getId);
         default -> Comparator.comparingDouble(this::getOptimalTargetScore);
      };
   }

   private double getOptimalTargetScore(LivingEntity entity) {
      if (mc.player == null) {
         return Double.MAX_VALUE;
      } else {
         double score = mc.player.distanceTo(entity) * 3.0;
         score += this.getTargetAngleScore(entity) * 2.0;
         score += this.getTargetHealthScore(entity) * 1.5;
         score += this.getTargetProtection(entity);
         if (entity instanceof PlayerEntity player) {
            if (player.isBlocking()) {
               score += 2.0;
            }

            if (player.getArmor() == 0) {
               score--;
            }

            if (player.getHealth() <= 8.0F) {
               score--;
            }
         }

         if (mc.player.canSee(entity)) {
            score -= 2.0;
         }

         return score;
      }
   }

   private double getTargetHealthScore(LivingEntity entity) {
      double health = entity.getHealth() + entity.getAbsorptionAmount();
      if (entity instanceof PlayerEntity player) {
         health *= 1.0 + this.getTargetProtection(player) / 20.0;
      }

      return health;
   }

   private double getTargetAngleScore(LivingEntity entity) {
      if (mc.player == null) {
         return 0.0;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d bodyCenter = entity.getPos().add(0.0, entity.getHeight() / 2.0, 0.0);
         Vec3d delta = bodyCenter.subtract(eyePos);
         double targetYaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
         double targetPitch = -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
         double yawDiff = MathHelper.wrapDegrees((float)(targetYaw - mc.player.getYaw()));
         double pitchDiff = targetPitch - mc.player.getPitch();
         return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
      }
   }

   private double getTargetProtection(LivingEntity entity) {
      return entity instanceof PlayerEntity player ? player.getAttributeValue(EntityAttributes.ARMOR) : entity.getArmor();
   }

   private void updateTarget() {
      if (!this.isState()) {
         this.target = null;
         this.lostTargetTicks = 0;
         this.releaseSilentLookOnce();
      } else if (this.target != null && this.isValidTarget(this.target, true)) {
         this.lastTarget = this.target;
         this.silentLookReleased = false;
         this.lostTargetTicks = 0;
      } else {
         LivingEntity oldTarget = this.target;
         this.target = this.findTarget();
         if (this.target != null) {
            if (oldTarget != this.target) {
               this.resetSnapAttack();
            }

            this.lastTarget = this.target;
            this.silentLookReleased = false;
            this.lostTargetTicks = 0;
         } else {
            this.lostTargetTicks++;
            if (this.lostTargetTicks > 8) {
               this.releaseSilentLookOnce();
            }
         }
      }
   }

   private void releaseSilentLookOnce() {
      if (!this.silentLookReleased) {
         if (!this.hasActiveSilentLook()) {
            this.silentLookReleased = true;
         } else {
            this.releaseSilentLook();
            this.silentLookReleased = true;
         }
      }
   }

   private boolean hasActiveSilentLook() {
      RotationStorage storage = RotationStorage.instance;
      if (storage != null && storage.isRotating()) {
         return true;
      } else {
         return FreeLookStorage.isActive() ? true : TurnsConnection.INSTANCE.getCurrentAngle() != null;
      }
   }

   private void releaseSilentLook() {
      this.finishCombatRotation();
   }

   private void finishCombatRotation() {
      if (RotationStorage.instance != null) {
         RotationStorage.instance.stopRotation();
      } else {
         FreeLookStorage.setActive(false);
         this.clearSilentRotationState();
      }
   }

   private void clearSilentRotationState() {
      TurnsConnection.INSTANCE.clear();
      TurnsConnection.INSTANCE.setRotation(null);
   }

   private boolean isFallingCritPhase() {
      return this.movementCanCrit && IdealHitUtils.isDivineCriticalState()
         || IdealHitUtils.isInCritWindow(this.target)
         || IdealHitUtils.isNoJumpDelayJumpCritWindow();
   }

   private float getAttackCooldownThreshold() {
      return IdealHitUtils.getAICooldown();
   }

   public boolean isLegitAimCommit() {
      return mc.player == null ? true : mc.player.getAttackCooldownProgress(1.5F) >= this.getAttackCooldownThreshold();
   }

   private void attack() {
      Hand blockedShieldHand = null;
      if (this.unpressShield.isValue() && (blockedShieldHand = this.getBlockingShieldHand()) != null) {
         mc.interactionManager.stopUsingItem(mc.player);
      }

      boolean isBypassAC = this.rotationType.get().equals("SlothAC") || this.rotationType.get().equals("PolarAC");
      boolean wasSprinting = mc.player.isSprinting();
      boolean sprintStop = false;

      if (wasSprinting || (mc.player.input != null && mc.player.input.hasForwardMovement())) {
         mc.options.sprintKey.setPressed(false);
         mc.player.setSprinting(false);
         if (mc.player.networkHandler != null && !mc.player.isSubmergedInWater() && !mc.player.isTouchingWater()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
         }
         sprintStop = true;
      }

      if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance <= 0.0F) {
         mc.player.fallDistance = 0.2F;
      }

      boolean attacked = false;
      LivingEntity class_13092 = this.target;
      PlayerEntity player;
      if (class_13092 instanceof PlayerEntity && (player = (PlayerEntity)class_13092).isBlocking() && this.breakShield.isValue()) {
         attacked = this.shieldBreak(player);
      }

      if (!attacked) {
         mc.interactionManager.attackEntity(mc.player, this.target);
      }

      this.comboHitCount++;
      this.lastAttackMillis = System.currentTimeMillis();

      ElytraResolver elytraResolver;
      if ((elytraResolver = CombatModules.getElytraResolver()) != null) {
         elytraResolver.onAuraAttack();
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      if (this.isTestSprintReset()) {
         TestSprintReset.onAttack();
      }

      if (!isBypassAC && this.isPacketSprintReset() && sprintStop && this.canRestartSprintAfterAttack() && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
         mc.player.setSprinting(true);
      } else if (wasSprinting && mc.player.input != null && mc.player.input.movementForward > 0.8F) {
         mc.options.sprintKey.setPressed(true);
      }

      if (blockedShieldHand != null && !ItemUseHelper.isUsingPriorityItem(mc.player)) {
         this.restoreShieldBlocking(blockedShieldHand);
      }

      long cooldown = this.calculateWeaponCooldownMs();
      if (TpsSync.getInstance() != null && TpsSync.getInstance().isState()) {
         cooldown = (long)((float)CombatModules.getAdjustedCooldown(cooldown, true) * 1.05F);
      }

      if (this.rotationType.get().equals("Legit")
         || this.rotationType.get().equals("SlothAC")
         || this.rotationType.get().equals("PolarAC")
         || this.rotationType.get().equals("Test")
         || this.rotationType.get().equals("SpookyTime")
         || this.rotationType.get().equals("Фантайм Тест")
         || this.rotationType.get().equals("Фантайм")) {
         cooldown = HumanCps.humanize(cooldown);
      }

      this.ticksToAttack = 11;
      this.cps = System.currentTimeMillis() + cooldown;
      this.attackTimer.reset();
   }

   private long calculateWeaponCooldownMs() {
      if (mc.player == null) return 550L;
      try {
         double speed = mc.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_SPEED);
         if (speed > 0.0) {
            long ticks = Math.max(11L, (long)Math.ceil(20.0 / speed));
            return ticks * 50L;
         }
      } catch (Throwable ignored) {
      }
      return 550L;
   }

   public boolean canRestartSprintAfterAttack() {
      return mc.player.input.movementForward > 0.0F
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !mc.player.isGliding()
         && !mc.player.isUsingItem()
         && !mc.player.horizontalCollision
         && mc.player.getHungerManager().getFoodLevel() > 6
         && !mc.player.isSneaking();
   }

   private boolean shieldBreak(PlayerEntity entity) {
      SlotSearchResult axeSlot = HotbarUtil.getAxe();
      if (!axeSlot.found()) {
         return false;
      } else {
         int previousSlot = mc.player.getInventory().selectedSlot;
         if (axeSlot.slot() == previousSlot) {
            mc.interactionManager.attackEntity(mc.player, entity);
            return true;
         } else if (axeSlot.isInHotBar()) {
            return this.attackWithDeferredHotbarSlot(entity, axeSlot.slot(), previousSlot);
         } else if (mc.player.currentScreenHandler.syncId != 0) {
            return false;
         } else {
            int swapHotbarSlot = this.findSilentSwapHotbarSlot(previousSlot);
            if (swapHotbarSlot == -1) {
               return false;
            } else {
               this.swapInventoryIntoHotbar(axeSlot.slot(), swapHotbarSlot);
               boolean attacked = false;

               try {
                  attacked = this.attackWithSilentHotbarSlot(entity, swapHotbarSlot, previousSlot);
               } finally {
                  this.swapInventoryIntoHotbar(axeSlot.slot(), swapHotbarSlot);
               }

               return attacked;
            }
         }
      }
   }

   private boolean attackWithSilentHotbarSlot(PlayerEntity entity, int attackSlot, int previousSlot) {
      if (mc.player != null && mc.player.networkHandler != null && mc.interactionManager != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(attackSlot));

         boolean var5;
         try {
            mc.interactionManager.attackEntity(mc.player, entity);
            boolean bl = true;
            var5 = bl;
         } finally {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
         }

         return var5;
      } else {
         return false;
      }
   }

   private boolean attackWithDeferredHotbarSlot(PlayerEntity entity, int attackSlot, int previousSlot) {
      if (mc.player != null && mc.player.networkHandler != null && mc.interactionManager != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(attackSlot));
         mc.interactionManager.attackEntity(mc.player, entity);
         this.shieldRestoreSlot = previousSlot;
         return true;
      } else {
         return false;
      }
   }

   private void flushShieldRestore() {
      if (this.shieldRestoreSlot != -1) {
         if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.shieldRestoreSlot));
         }

         this.shieldRestoreSlot = -1;
      }
   }

   private void swapInventoryIntoHotbar(int inventorySlot, int hotbarSlot) {
      if (mc.player != null && mc.interactionManager != null && mc.player.networkHandler != null) {
         int syncId = mc.player.currentScreenHandler.syncId;
         mc.interactionManager.clickSlot(syncId, inventorySlot, hotbarSlot, SlotActionType.SWAP, mc.player);
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
      }
   }

   private int findSilentSwapHotbarSlot(int previousSlot) {
      for (int slot = 8; slot >= 0; slot--) {
         if (slot != previousSlot) {
            return slot;
         }
      }

      return previousSlot >= 0 && previousSlot < 9 ? previousSlot : -1;
   }

   private Hand getBlockingShieldHand() {
      if (mc.player != null && mc.player.isBlocking()) {
         Hand activeHand = mc.player.getActiveHand();
         if (activeHand == null) {
            return null;
         } else {
            return this.isShieldStack(mc.player.getStackInHand(activeHand)) ? activeHand : null;
         }
      } else {
         return null;
      }
   }

   private boolean shouldBlockAttackWhileUsingItem() {
      if (mc.player == null || !mc.player.isUsingItem()) {
         return false;
      } else {
         return this.unpressShield.isValue() && this.getBlockingShieldHand() != null ? false : ItemUseHelper.isPriorityUseItem(mc.player.getActiveItem());
      }
   }

   private void restoreShieldBlocking(Hand hand) {
      if (mc.player != null && mc.interactionManager != null && hand != null) {
         if (this.isShieldStack(mc.player.getStackInHand(hand)) && !mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, hand);
         }
      }
   }

   private boolean isShieldStack(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
   }

   private boolean isWeapon() {
      Item item = mc.player.getMainHandStack().getItem();
      return item != Items.AIR
         && (
            item instanceof SwordItem
               || item instanceof PickaxeItem
               || item instanceof AxeItem
               || item instanceof HoeItem
               || item instanceof ShovelItem
               || item instanceof MaceItem
               || item == Items.MACE
         );
   }

   private boolean isValidTarget(LivingEntity entity) {
      return this.isValidTarget(entity, false);
   }

   private boolean isValidTarget(LivingEntity entity, boolean sticky) {
      if (entity != null && entity != mc.player) {
         if (!entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
         } else if (entity instanceof ArmorStandEntity) {
            return false;
         } else if (!(entity instanceof IronGolemEntity) && !(entity instanceof BatEntity)) {
            if (entity instanceof PlayerEntity player && AntiBot.getInstance().isState() && AntiBot.checkBot(player)) {
               return false;
            } else {
               if (entity instanceof PlayerEntity player) {
                  if (!this.targets.isSelected("Игроки")) {
                     return false;
                  }

                  if (this.isNaked(player) && !this.targets.isSelected("Голые") && !FakePlayerManager.isFakePlayer(player)) {
                     return false;
                  }

                  if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !this.targets.isSelected("Невидимки")) {
                     return false;
                  }

                  if (FriendUtils.isFriend(entity.getName().getString())) {
                     return false;
                  }
               } else if (!(entity instanceof PassiveEntity) && !(entity instanceof CodEntity)) {
                  if (!(entity instanceof HostileEntity)) {
                     return false;
                  }

                  if (!this.targets.isSelected("Мобы")) {
                     return false;
                  }
               } else if (!this.targets.isSelected("Мирные")) {
                  return false;
               }

               Vec3d nearestPoint;
               if ((nearestPoint = BestPoint.getNearestPoint(entity)) == null) {
                  nearestPoint = MultipointUtils.getClosestPoint(entity);
               }

               float maxRange = this.getMaxAimRange() + (sticky ? 0.75F : 0.0F);
               return mc.player.getEyePos().distanceTo(nearestPoint) > maxRange ? false : this.throughWalls.isValue() || mc.player.canSee(entity);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isNaked(PlayerEntity player) {
      for (ItemStack armorStack : player.getArmorItems()) {
         if (!armorStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private boolean shouldAttack() {
      if (mc.player.getAttackCooldownProgress(0.5F) < this.getAttackCooldownThreshold()) {
         return false;
      } else {
         EntityHitResult result = this.getAttackRaycastResult();
         boolean raycastMiss = result == null || result.getEntity() != this.target;
         if (raycastMiss && !mc.player.isGliding()) {
            return false;
         } else {
            if (mc.player.isGliding() && this.target.isGliding()) {
               Vec3d targetPos = this.target.getPos().add(0.0, this.target.getHeight() / 2.0, 0.0);
               int predict = CombatModules.isElytraPredictActive() ? CombatModules.getElytraForwardTicks() : 0;
               Vec3d predictedPos = PredictUtils.predict(this.target, targetPos, predict);
               if (mc.player.getEyePos().distanceTo(predictedPos) > 5.0) {
                  return false;
               }
            } else if (mc.player.isGliding()) {
               if (mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter()) > this.attackRange.getValue()) {
                  return false;
               }
            } else {
               double distanceCheck = mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
               Vec3d checkPoint = distanceCheck > 3.0 ? BestPoint.getNearestPoint(this.target) : this.target.getBoundingBox().getCenter();
               if (checkPoint == null) {
                  checkPoint = MultipointUtils.getClosestPoint(this.target);
               }

               if (mc.player.getEyePos().distanceTo(checkPoint) > this.attackRange.getValue()) {
                  return false;
               }
            }
            if (this.rotationType.get().equals("Legit") && HitChance.get(this.target) < 0.1F) {
               return false;
            } else {
               if (this.rotationType.get().equals("Test")
                  || this.rotationType.get().equals("SlothAC")
                  || this.rotationType.get().equals("PolarAC")
                  || this.rotationType.get().equals("SpookyTime")
                  || this.rotationType.get().equals("Фантайм Тест")
                  || this.rotationType.get().equals("Фантайм")) {
                  float effectiveReach = Math.min(this.attackRange.getValue(), 3.0F);
                  Vec3d eyePos = mc.player.getEyePos();
                  float yaw = AuraMixinHelper.resolveYaw(mc.player.getYaw());
                  float pitch = AuraMixinHelper.resolvePitch(mc.player.getPitch());
                  Vec3d lookVec = Vec3d.fromPolar(pitch, yaw);
                  Vec3d reachVec = eyePos.add(lookVec.multiply(effectiveReach));
                  Box aabb = this.target.getBoundingBox().expand(0.12);
                  boolean hitsTarget = aabb.contains(eyePos) || aabb.raycast(eyePos, reachVec).isPresent();
                  if (!hitsTarget) {
                     return false;
                  }
               }

               return this.shouldAttackMovementCrit();
            }
         }
      }
   }

   private boolean shouldAttackMovementCrit() {
      return this.shouldAttackMovementCrit(this.target);
   }

   private boolean shouldAttackMovementCrit(LivingEntity attackTarget) {
      if (this.target != this.lastTarget || System.currentTimeMillis() - this.lastAttackMillis > 2500L) {
         this.comboHitCount = 0;
      }

      if (this.comboHitCount < 3 && !this.onlyCrits.isValue()) {
         return mc.player.getAttackCooldownProgress(0.5F) >= 0.90F;
      }

      if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.01) {
         return IdealHitUtils.isInCritWindow(attackTarget);
      }

      if (!mc.player.isOnGround() && mc.player.getVelocity().y >= -0.01) {
         return false;
      }

      if (IdealHitUtils.cannotPerformCrit()) {
         return mc.player.getAttackCooldownProgress(0.5F) >= 0.90F;
      }

      if (this.onlyCrits.isValue()) {
         return false;
      }

      return mc.player.getAttackCooldownProgress(0.5F) >= 0.90F;
   }

   private boolean canAttackCritical(LivingEntity attackTarget) {
      return this.shouldAttackMovementCrit(attackTarget);
   }

   public boolean isAboveWater() {
      BlockPos pos = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4, 0.0));
      return !mc.player.isSubmergedInWater() && mc.world.getBlockState(pos).isOf(Blocks.WATER);
   }

   public float getAttackCooldown() {
      return MathHelper.clamp(((ILivingEntity)mc.player).getLastAttackedTicks() / this.getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
   }

   public float getAttackCooldownProgressPerTick() {
      return (float)(1.0 / mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20.0);
   }

   private float getMaxAimRange() {
      return mc.player.isGliding() ? this.elytraAimRange.getValue() : this.attackRange.getValue() + this.aimRange.getValue();
   }

   @Override
   public void deactivate() {
      LegitSprintReset.reset();
      TestSprintReset.reset();
      this.silentLookReleased = false;
      this.releaseSilentLook();
      this.silentLookReleased = true;
      super.deactivate();
      if (this.target != null) {
         this.backTimer.reset();
      }

      this.target = null;
      this.lastTarget = null;
      this.lostTargetTicks = 0;
      this.slothACRotation.reset();
      this.polarACRotation.reset();
      this.kinematicRotation.reset();
      this.neuroRotation.reset();
      this.legitRotation.reset();
      this.funtimeRotation.reset();
      this.funtimeTestRotation.reset();
      this.spookyTimeRotation.reset();
      this.testRotation.reset();
      this.movementCanCrit = false;
      this.lastRotationMode = "";
      this.focusMoveYaw = Float.NaN;
      this.targetMoveYaw = Float.NaN;
      this.comboHitCount = 0;
      this.lastAttackMillis = 0L;
      this.ticksToAttack = 0;
      this.resetSnapAttack();
      this.flushShieldRestore();
      adjYaw = 0.0F;
      adjPitch = 0.0F;
      otvodkaYaw = 0.0F;
      otvodkaPitch = 0.0F;
      this.isRotated = false;
      this.lastYaw = 0.0F;
      this.lastPitch = 0.0F;
   }

   @Override
   public void activate() {
      super.activate();
      this.silentLookReleased = false;
      this.lostTargetTicks = 0;
      this.normalizeRotationType();
      TestSprintReset.reset();
      this.kinematicRotation.reset();
      this.neuroRotation.reset();
      this.legitRotation.reset();
      this.funtimeRotation.reset();
      this.funtimeTestRotation.reset();
      this.spookyTimeRotation.reset();
      this.testRotation.reset();
      this.movementCanCrit = false;
      this.lastRotationMode = this.rotationType.get();
      this.focusMoveYaw = Float.NaN;
      this.targetMoveYaw = Float.NaN;
      this.ticksToAttack = 0;
      this.resetSnapAttack();
      adjYaw = 0.0F;
      adjPitch = 0.0F;
      otvodkaYaw = 0.0F;
      otvodkaPitch = 0.0F;
      this.isRotated = false;
      if (mc.player != null) {
         this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
      }
   }

   private void normalizeRotationType() {
      String rotation = this.rotationType.get();
      if (!Arrays.asList(this.rotationType.getOptions()).contains(rotation)) {
         this.rotationType.set("SpookyTime");
      }
   }

   private void resetSnapAttack() {
      this.snapAttackAge = -1;
      this.snapAttackQueued = false;
      this.snapAttackTarget = null;
   }

   public RadioSetting getSprintReset() {
      return this.sprintReset;
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }

   public Vec2f getCurrentRotations() {
      return this.currentRotations;
   }

   public Vec2f getTargetRotations() {
      return this.targetRotations;
   }

   public TimerUtils getAttackTimer() {
      return this.attackTimer;
   }
}
