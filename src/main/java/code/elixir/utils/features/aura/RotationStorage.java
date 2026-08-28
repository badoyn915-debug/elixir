package code.elixir.utils.features.aura;

import code.elixir.Elixir;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.features.impl.combat.components.interpolation.HumanAim;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.interactions.simulate.MovingUtil;
import net.minecraft.util.math.MathHelper;

public class RotationStorage implements QuickImports {
   public static RotationStorage instance;
   private RotationStorage.RotationTask currentTask = RotationStorage.RotationTask.IDLE;
   private float currentYawSpeed;
   private float currentPitchSpeed;
   private float currentYawReturnSpeed;
   private float currentPitchReturnSpeed;
   private int currentPriority;
   private int currentTimeout;
   private int idleTicks;
   private Rotation targetRotation;
   private boolean clientRotation;
   private float resetVelYaw = 0.0F;
   private float resetVelPitch = 0.0F;
   private long lastResetMs = 0L;

   public RotationStorage() {
      instance = this;
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   private void resetRotation() {
      if (mc.player != null && FreeLookStorage.isActive()) {
         float targetYaw = FreeLookStorage.getFreeYaw();
         float targetPitch = FreeLookStorage.getFreePitch();
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
         float pitchDelta = targetPitch - currentPitch;
         float dist = (float)Math.hypot(yawDelta, pitchDelta);
         float gcd = GCDUtil.getGCDValue();
         if (dist <= Math.max(gcd * 1.5F, 1.2F)) {
            this.finishStop();
         } else {
            float speedNorm = MathHelper.clamp(dist / 30.0F, 0.15F, 1.0F);
            float easedSpeed = HumanAim.bezierEase(speedNorm);
            float speed = MathHelper.clamp(0.38F + 0.46F * easedSpeed, 0.28F, 0.84F);
            float targetVelYaw = yawDelta * speed;
            float targetVelPitch = pitchDelta * speed;
            this.resetVelYaw = this.resetVelYaw + (targetVelYaw - this.resetVelYaw) * 0.42F;
            this.resetVelPitch = this.resetVelPitch + (targetVelPitch - this.resetVelPitch) * 0.42F;
            float deltaYaw = GCDUtil.getFixedRotation(this.resetVelYaw);
            float deltaPitch = GCDUtil.getFixedRotation(this.resetVelPitch);
            if (deltaYaw == 0.0F && Math.abs(yawDelta) > 0.01F) {
               deltaYaw = yawDelta > 0.0F ? gcd : -gcd;
            }

            if (deltaPitch == 0.0F && Math.abs(pitchDelta) > 0.01F) {
               deltaPitch = pitchDelta > 0.0F ? gcd : -gcd;
            }

            float outYaw = currentYaw + deltaYaw;
            float outPitch = MathHelper.clamp(currentPitch + deltaPitch, -89.0F, 89.0F);
            mc.player.setYaw(outYaw);
            mc.player.setPitch(outPitch);
            if (!this.clientRotation) {
               TurnsConnection.INSTANCE.holdRotation(new Turns(outYaw, outPitch));
            }

            this.idleTicks(0);
         }
      } else {
         this.finishStop();
      }
   }

   @EventHandler
   public void onEventTick(TickEvent event) {
      if (mc.player != null) {
         if (this.currentTask().equals(RotationStorage.RotationTask.AIM) && this.currentTimeout() > 0 && this.idleTicks() > this.currentTimeout()) {
            this.resetVelYaw = 0.0F;
            this.resetVelPitch = 0.0F;
            this.lastResetMs = System.currentTimeMillis();
            this.currentTask(RotationStorage.RotationTask.RESET);
         }

         if (this.currentTask().equals(RotationStorage.RotationTask.RESET)) {
            this.resetRotation();
         }

         this.idleTicks++;
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.isRotating() && FreeLookStorage.isActive() && !AuraMixinHelper.hasMoveCorrection()) {
         MovingUtil.fixMovementFree(event);
      }
   }

   public static void update(
      Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation
   ) {
      RotationStorage storage = instance;
      if (mc.player != null && storage != null) {
         if (storage.currentPriority() <= priority || storage.idleTicks() != 0) {
            if (clientRotation) {
               TurnsConnection.INSTANCE.clear();
               if (FreeLookStorage.isActive()) {
                  FreeLookStorage.setActive(false);
               }
            } else if (!FreeLookStorage.isActive()) {
               FreeLookStorage.activateForSilentRotation();
            }

            storage.currentYawSpeed(yawSpeed);
            storage.currentPitchSpeed(pitchSpeed);
            storage.currentYawReturnSpeed(yawReturnSpeed);
            storage.currentPitchReturnSpeed(pitchReturnSpeed);
            storage.currentTimeout(timeout);
            storage.currentPriority(priority);
            storage.currentTask(RotationStorage.RotationTask.AIM);
            storage.resetVelYaw = 0.0F;
            storage.resetVelPitch = 0.0F;
            storage.lastResetMs = 0L;
            storage.targetRotation(target);
            storage.clientRotation = clientRotation;
            storage.updateRotation(target, yawSpeed, pitchSpeed);
         }
      }
   }

   public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
      update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
   }

   public static void update(Rotation targetRotation, float yawSpeed, float pitchSpeed, float returnSpeed, int timeout, int priority) {
      update(targetRotation, yawSpeed, pitchSpeed, returnSpeed, returnSpeed, timeout, priority, false);
   }

   private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
      if (mc.player == null) {
         return false;
      } else {
         Rotation currentRotation = new Rotation(mc.player);
         float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
         float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
         float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
         float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);
         if (this.currentTask == RotationStorage.RotationTask.RESET) {
            clampedYaw = Math.max(Math.min(Math.abs(yawDelta) * 0.25F, yawSpeed), 1.5F);
            clampedPitch = Math.max(Math.min(Math.abs(pitchDelta) * 0.25F, pitchSpeed), 1.5F);
         }

         float yaw = mc.player.getYaw();
         float pitch = MathHelper.clamp(
            mc.player.getPitch() + GCDUtil.getFixedRotation(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90.0F, 90.0F
         );
         float var11;
         mc.player.setYaw(var11 = yaw + GCDUtil.getFixedRotation(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw)));
         mc.player.setPitch(pitch);
         if (!this.clientRotation) {
            TurnsConnection.INSTANCE.holdRotation(new Turns(var11, pitch));
         }

         this.idleTicks(0);
         return new Rotation(var11, pitch).getDelta(targetRotation) < 1.0F;
      }
   }

   public void stopRotation() {
      if (this.isRotating() && FreeLookStorage.isActive()) {
         this.resetVelYaw = 0.0F;
         this.resetVelPitch = 0.0F;
         this.lastResetMs = System.currentTimeMillis();
         this.currentTask(RotationStorage.RotationTask.RESET);
      } else {
         this.finishStop();
      }
   }

   public void stopWithoutCameraRestore() {
      this.resetVelYaw = 0.0F;
      this.resetVelPitch = 0.0F;
      this.lastResetMs = 0L;
      this.clientRotation = false;
      this.currentTask(RotationStorage.RotationTask.IDLE);
      this.currentPriority(0);
      FreeLookStorage.setActive(false);
      TurnsConnection.INSTANCE.clear();
      TurnsConnection.INSTANCE.setRotation(null);
   }

   private void finishStop() {
      if (mc.player != null && FreeLookStorage.isActive()) {
         mc.player.setYaw(FreeLookStorage.getFreeYaw());
         mc.player.setPitch(FreeLookStorage.getFreePitch());
      }

      this.resetVelYaw = 0.0F;
      this.resetVelPitch = 0.0F;
      this.lastResetMs = 0L;
      this.clientRotation = false;
      this.currentTask(RotationStorage.RotationTask.IDLE);
      this.currentPriority(0);
      FreeLookStorage.setActive(false);
      TurnsConnection.INSTANCE.clear();
      TurnsConnection.INSTANCE.setRotation(null);
   }

   public boolean isRotating() {
      return !this.currentTask.equals(RotationStorage.RotationTask.IDLE);
   }

   public boolean isCameraReturning() {
      return this.currentTask.equals(RotationStorage.RotationTask.RESET);
   }

   public RotationStorage.RotationTask currentTask() {
      return this.currentTask;
   }

   public float currentYawSpeed() {
      return this.currentYawSpeed;
   }

   public float currentPitchSpeed() {
      return this.currentPitchSpeed;
   }

   public float currentYawReturnSpeed() {
      return this.currentYawReturnSpeed;
   }

   public float currentPitchReturnSpeed() {
      return this.currentPitchReturnSpeed;
   }

   public int currentPriority() {
      return this.currentPriority;
   }

   public int currentTimeout() {
      return this.currentTimeout;
   }

   public int idleTicks() {
      return this.idleTicks;
   }

   public Rotation targetRotation() {
      return this.targetRotation;
   }

   public boolean clientRotation() {
      return this.clientRotation;
   }

   public RotationStorage currentTask(RotationStorage.RotationTask currentTask) {
      this.currentTask = currentTask;
      return this;
   }

   public RotationStorage currentYawSpeed(float currentYawSpeed) {
      this.currentYawSpeed = currentYawSpeed;
      return this;
   }

   public RotationStorage currentPitchSpeed(float currentPitchSpeed) {
      this.currentPitchSpeed = currentPitchSpeed;
      return this;
   }

   public RotationStorage currentYawReturnSpeed(float currentYawReturnSpeed) {
      this.currentYawReturnSpeed = currentYawReturnSpeed;
      return this;
   }

   public RotationStorage currentPitchReturnSpeed(float currentPitchReturnSpeed) {
      this.currentPitchReturnSpeed = currentPitchReturnSpeed;
      return this;
   }

   public RotationStorage currentPriority(int currentPriority) {
      this.currentPriority = currentPriority;
      return this;
   }

   public RotationStorage currentTimeout(int currentTimeout) {
      this.currentTimeout = currentTimeout;
      return this;
   }

   public RotationStorage idleTicks(int idleTicks) {
      this.idleTicks = idleTicks;
      return this;
   }

   public RotationStorage targetRotation(Rotation targetRotation) {
      this.targetRotation = targetRotation;
      return this;
   }

   public RotationStorage clientRotation(boolean clientRotation) {
      this.clientRotation = clientRotation;
      return this;
   }

   public static enum RotationTask {
      AIM,
      RESET,
      IDLE;
   }
}
