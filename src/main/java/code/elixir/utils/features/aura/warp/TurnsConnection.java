package code.elixir.utils.features.aura.warp;

import code.elixir.Elixir;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.PlayerVelocityStrafeEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.AuraMixinHelper;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.math.task.TaskPriority;
import code.elixir.utils.math.task.TaskProcessor;
import java.util.Objects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TurnsConnection implements QuickImports {
   public static TurnsConnection INSTANCE = new TurnsConnection();
   private TurnsConstructor lastRotationPlan;
   private final TaskProcessor<TurnsConstructor> rotationPlanTaskProcessor = new TaskProcessor<>();
   public Turns currentAngle;
   private Turns previousAngle;
   private Turns serverAngle = Turns.DEFAULT;
   private boolean rotateBack = false;
   private long rotateBackTime = -1L;
   private Turns manualAngle;
   private int manualAngleTicks = 0;
   private LinearConstructor linearConstructor = new LinearConstructor();

   public TurnsConnection() {
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   public void setRotation(Turns value) {
      this.previousAngle = value == null ? (this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle()) : this.currentAngle;
      this.currentAngle = value;
   }

   public void holdRotation(Turns value) {
      this.rotationPlanTaskProcessor.activeTasks.clear();
      this.lastRotationPlan = null;
      this.rotationPlanTaskProcessor.tickCounter = 0;
      this.manualAngle = value;
      this.manualAngleTicks = 2;
      this.rotateBack = false;
      this.rotateBackTime = -1L;
      this.setRotation(value);
   }

   public Turns getRotation() {
      return this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
   }

   public Turns getPreviousRotation() {
      if (this.currentAngle != null && this.previousAngle != null) {
         return this.previousAngle;
      } else {
         return mc.player == null ? new Turns(0.0F, 0.0F) : new Turns(mc.player.prevYaw, mc.player.prevPitch);
      }
   }

   public Turns getMoveRotation() {
      TurnsConstructor rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : MathAngle.cameraAngle();
   }

   public TurnsConstructor getCurrentRotationPlan() {
      return this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? this.rotationPlanTaskProcessor.fetchActiveTaskValue() : this.lastRotationPlan;
   }

   public void rotateTo(Turns.VecRotation vecRotation, LivingEntity entity, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Turns angle, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
   }

   public void rotateTo(Turns angle, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
   }

   public void rotateTo(TurnsConstructor plan, TaskPriority taskPriority, Module provider) {
      this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
   }

   public void update() {
      if (this.manualAngleTicks > 0 && this.manualAngle != null) {
         this.rotateBack = false;
         this.rotateBackTime = -1L;
         this.setRotation(this.manualAngle.adjustSensitivity());
         this.manualAngleTicks--;
         if (this.manualAngleTicks <= 0) {
            this.manualAngle = null;
         }
      } else {
         TurnsConstructor activePlan = this.getCurrentRotationPlan();
         if (this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null) {
            if (!this.rotateBack) {
               this.rotateBackTime = System.currentTimeMillis();
               this.rotateBack = true;
            }
         } else {
            this.rotateBack = false;
            this.rotateBackTime = -1L;
         }

         Turns clientAngle = MathAngle.cameraAngle();
         boolean hasActiveTask = this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null;
         if (hasActiveTask) {
            Turns newAngle = activePlan.nextRotation(this.currentAngle != null ? this.currentAngle : clientAngle, false).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
         }

         if (this.rotateBack) {
            if (this.currentAngle == null) {
               this.rotateBack = false;
               return;
            }

            if (System.currentTimeMillis() - this.rotateBackTime < 1000L) {
               Turns delta = MathAngle.calculateDelta(this.currentAngle, clientAngle).adjustSensitivity();
               this.setRotation(
                  new Turns(
                     MathHelper.lerp(0.5F, this.currentAngle.getYaw(), this.currentAngle.getYaw() + delta.getYaw()),
                     MathHelper.lerp(0.5F, this.currentAngle.getPitch(), this.currentAngle.getPitch() + delta.getPitch())
                  )
               );
            } else {
               this.setRotation(null);
               this.lastRotationPlan = null;
               this.rotateBack = false;
            }
         }
      }
   }

   public static double computeRotationDifference(Turns a, Turns b) {
      return Math.hypot(Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
      Float yaw = this.resolveMoveCorrectionYaw();
      if (yaw == null) {
         return currVelocity;
      } else {
         double d = movementInput.lengthSquared();
         if (d < 1.0E-7) {
            return Vec3d.ZERO;
         } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
            float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
            float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
            return new Vec3d(vec3d.getX() * g - vec3d.getZ() * f, vec3d.getY(), vec3d.getZ() * g + vec3d.getX() * f);
         }
      }
   }

   private Float resolveMoveCorrectionYaw() {
      if (this.currentAngle != null) {
         return this.currentAngle.getYaw();
      } else if (AuraMixinHelper.isCombatActive() && AuraMixinHelper.hasMoveCorrection()) {
         RotationStorage storage = RotationStorage.instance;
         return storage != null && storage.isRotating() && storage.targetRotation() != null ? storage.targetRotation().getYaw() : null;
      } else {
         return null;
      }
   }

   public void clear() {
      this.rotationPlanTaskProcessor.activeTasks.clear();
      this.lastRotationPlan = null;
      this.rotationPlanTaskProcessor.tickCounter = 0;
      this.manualAngle = null;
      this.manualAngleTicks = 0;
   }

   @EventHandler
   public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
      TurnsConstructor currentRotationPlan = this.getCurrentRotationPlan();
      if (currentRotationPlan != null && currentRotationPlan.isMoveCorrection()) {
         e.setVelocity(this.fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
      } else {
         if (AuraMixinHelper.isCombatActive() && AuraMixinHelper.hasMoveCorrection()) {
            e.setVelocity(this.fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      SafeEvents.dispatch(() -> new RotationUpdateEvent((byte)0));
      this.update();
      SafeEvents.dispatch(() -> new RotationUpdateEvent((byte)2));
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (!event.isCancelled()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof PlayerMoveC2SPacket movePacket) {
            if (movePacket.changesLook()) {
               this.serverAngle = new Turns(movePacket.getYaw(1.0F), movePacket.getPitch(1.0F));
            }
         } else if (packet instanceof PlayerPositionLookS2CPacket posPacket) {
            this.serverAngle = new Turns(posPacket.change().yaw(), posPacket.change().pitch());
         }
      }
   }

   public TurnsConstructor getLastRotationPlan() {
      return this.lastRotationPlan;
   }

   public TaskProcessor<TurnsConstructor> getRotationPlanTaskProcessor() {
      return this.rotationPlanTaskProcessor;
   }

   public Turns getCurrentAngle() {
      return this.currentAngle;
   }

   public Turns getPreviousAngle() {
      return this.previousAngle;
   }

   public Turns getServerAngle() {
      return this.serverAngle;
   }

   public boolean isRotateBack() {
      return this.rotateBack;
   }

   public long getRotateBackTime() {
      return this.rotateBackTime;
   }

   public Turns getManualAngle() {
      return this.manualAngle;
   }

   public int getManualAngleTicks() {
      return this.manualAngleTicks;
   }

   public LinearConstructor getLinearConstructor() {
      return this.linearConstructor;
   }
}
