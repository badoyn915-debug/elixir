package code.elixir.utils.features.aura;

import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.rotate.Rotation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NeuroAuraStorage implements QuickImports {
   private static final long MIN_RECORD_INTERVAL = 50L;
   private static final int MAX_FRAMES = 20000;
   private static final String PATTERNS_DIRECTORY = "data_patterns";
   private static final String LEGACY_PATTERNS_DIRECTORY = "neuro_patterns";
   private static final String PRIMARY_EXTENSION = ".data";
   private static final String LEGACY_EXTENSION = ".neuro";
   private static final float SYNC_SCORE_THRESHOLD = 45.0F;
   private static final float MAX_YAW_CORRECTION = 8.0F;
   private static final float MAX_PITCH_CORRECTION = 6.0F;
   private final List<NeuroPattern> recordedPatterns = new CopyOnWriteArrayList<>();
   private boolean isRecording = false;
   private boolean isUsingNeuro = false;
   private boolean showStats = true;
   private String currentPatternName = null;
   private String lastDebugMessage = "Готов!";
   private int recordedThisSession = 0;
   private long lastRecordTime = 0L;
   private float prevRecordYaw = 0.0F;
   private float prevRecordPitch = 0.0F;
   private boolean hasRecordedBefore = false;
   private final List<NeuroAuraStorage.Frame> frames = new CopyOnWriteArrayList<>();
   private int playbackIndex = -1;
   private int ticksSinceSync = 0;
   private float smoothedYawDelta = 0.0F;
   private float smoothedPitchDelta = 0.0F;
   private float smoothedOutputYaw = Float.NaN;
   private float smoothedOutputPitch = Float.NaN;
   private float yawSpeedFactor = 1.0F;
   private float pitchSpeedFactor = 1.0F;
   private int speedProfileTicks = 0;
   private Vec3d currentAimPoint = null;
   private Vec3d targetRandomPoint = null;
   private int aimPointTicks = 0;
   private LivingEntity lastAimTarget = null;
   private boolean lastWasIdle = true;
   private int attackCount = 0;
   private float randomXOffset = 0.0F;
   private float randomYRatio = 0.66F;
   private float randomZOffset = 0.0F;

   public NeuroAuraStorage() {
      this.createPatternsDirectory();
   }

   private void createPatternsDirectory() {
      try {
         Path path = Paths.get("data_patterns");
         if (!Files.exists(path)) {
            Files.createDirectories(path);
         }
      } catch (IOException var2) {
         this.lastDebugMessage = "§cОшибка папки";
      }
   }

   public void recordTick(LivingEntity target, float currentYaw, float currentPitch) {
      if (this.isRecording && mc.player != null) {
         long now = System.currentTimeMillis();
         if (now - this.lastRecordTime >= 50L) {
            float deltaYaw = 0.0F;
            float deltaPitch = 0.0F;
            if (this.hasRecordedBefore) {
               deltaYaw = MathHelper.wrapDegrees(currentYaw - this.prevRecordYaw);
               deltaPitch = currentPitch - this.prevRecordPitch;
            }

            float angleYaw = 0.0F;
            float anglePitch = 0.0F;
            double distance = 0.0;
            boolean hasTarget = target != null;
            if (hasTarget) {
               NeuroAuraStorage.AimData aimData = this.getAimData(target, currentYaw, currentPitch, null, true);
               angleYaw = aimData.angleYaw;
               anglePitch = aimData.anglePitch;
               distance = aimData.distance;
            }

            NeuroAuraStorage.Frame frame = new NeuroAuraStorage.Frame();
            frame.deltaYaw = deltaYaw;
            frame.deltaPitch = deltaPitch;
            frame.angleYaw = angleYaw;
            frame.anglePitch = anglePitch;
            frame.distance = distance;
            frame.hasTarget = hasTarget;
            frame.smoothness = this.calculateSmoothness(deltaYaw, deltaPitch);
            this.frames.add(frame);

            while (this.frames.size() > 20000) {
               this.frames.remove(0);
            }

            if (hasTarget) {
               boolean crit = mc.player.fallDistance > 0.0F && !mc.player.isOnGround();
               String type = target instanceof PlayerEntity ? "player" : "mob";
               this.recordedPatterns.add(new NeuroPattern(angleYaw, anglePitch, deltaYaw, deltaPitch, distance, crit, 0.0, type, frame.smoothness));

               while (this.recordedPatterns.size() > 20000) {
                  this.recordedPatterns.remove(0);
               }
            }

            this.prevRecordYaw = currentYaw;
            this.prevRecordPitch = currentPitch;
            this.hasRecordedBefore = true;
            this.lastRecordTime = now;
            this.recordedThisSession++;
            if (this.recordedThisSession % 20 == 0) {
               this.lastDebugMessage = "§aЗапись: §f" + this.frames.size();
            }
         }
      }
   }

   public Rotation getNeuroRotation(LivingEntity target, float currentYaw, float currentPitch, boolean idle) {
      if (this.isUsingNeuro && target != null && mc.player != null && !this.frames.isEmpty()) {
         if (!idle && this.lastWasIdle) {
            this.rollNewRandomPoint();
            this.attackCount++;
         }

         this.lastWasIdle = idle;
         boolean needSync = this.playbackIndex < 0 || this.playbackIndex >= this.frames.size();
         NeuroAuraStorage.AimData aimData = this.getAimData(target, currentYaw, currentPitch, null, idle);
         boolean airborne = !mc.player.isOnGround() || mc.player.getVelocity().y != 0.0;
         if (Math.abs(aimData.angleYaw) > 110.0F) {
            needSync = true;
            this.smoothedYawDelta = 0.0F;
            this.smoothedPitchDelta = 0.0F;
            this.smoothedOutputYaw = currentYaw;
            this.smoothedOutputPitch = currentPitch;
         }

         if (!needSync && this.ticksSinceSync >= 5) {
            NeuroAuraStorage.Frame currentFrame = this.frames.get(this.playbackIndex);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(currentFrame.angleYaw - aimData.angleYaw));
            if (yawDiff + Math.abs(currentFrame.anglePitch - aimData.anglePitch) + (float)Math.abs(currentFrame.distance - aimData.distance) * 0.3F > 45.0F) {
               needSync = true;
            }
         }

         if (needSync) {
            this.playbackIndex = this.findBest(aimData.angleYaw, aimData.anglePitch, aimData.distance);
            this.ticksSinceSync = 0;
         }

         NeuroAuraStorage.Frame frame = this.frames.get(this.playbackIndex);
         aimData = this.getAimData(target, currentYaw, currentPitch, frame, idle);
         float applyYaw = frame.deltaYaw;
         float applyPitch = frame.deltaPitch;
         this.updateSpeedProfile(idle, airborne, aimData);
         if (Math.abs(frame.angleYaw) > 3.0F && Math.abs(aimData.angleYaw) > 3.0F && Math.signum(frame.angleYaw) != Math.signum(aimData.angleYaw)) {
            applyYaw = -applyYaw;
         }

         if (Math.abs(frame.anglePitch) > 3.0F && Math.abs(aimData.anglePitch) > 3.0F && Math.signum(frame.anglePitch) != Math.signum(aimData.anglePitch)) {
            applyPitch = -applyPitch;
         }

         applyYaw = this.adaptRecordedDelta(applyYaw, aimData.angleYaw, frame.smoothness, idle, 8.0F);
         applyPitch = this.adaptRecordedDelta(applyPitch, aimData.anglePitch, frame.smoothness, idle, 6.0F);
         if (Math.abs(aimData.angleYaw) < 32.0F) {
            applyYaw = MathHelper.lerp(0.58F, applyYaw, aimData.angleYaw);
         }

         if (Math.abs(aimData.anglePitch) < 24.0F) {
            applyPitch = MathHelper.lerp(0.52F, applyPitch, aimData.anglePitch);
         }

         this.smoothedYawDelta = this.smoothDelta(this.smoothedYawDelta, applyYaw, frame.smoothness);
         this.smoothedPitchDelta = this.smoothDelta(this.smoothedPitchDelta, applyPitch, frame.smoothness);
         float quantizedYaw = this.quantizeToMouseStep(this.smoothedYawDelta, aimData.angleYaw);
         float quantizedPitch = this.quantizeToMouseStep(this.smoothedPitchDelta, aimData.anglePitch);
         float var25;
         float rawYaw = MathHelper.wrapDegrees(currentYaw + (var25 = quantizedYaw + this.getMicroJitter(true, idle, airborne, aimData)));
         float var26;
         float rawPitch = MathHelper.clamp(currentPitch + (var26 = quantizedPitch + this.getMicroJitter(false, idle, airborne, aimData)), -90.0F, 90.0F);
         float finalYaw = this.smoothOutputRotation(rawYaw, currentYaw, frame.smoothness, idle, true);
         float finalPitch = this.smoothOutputRotation(rawPitch, currentPitch, frame.smoothness, idle, false);
         this.playbackIndex++;
         this.ticksSinceSync++;

         for (int skipped = 0; this.playbackIndex < this.frames.size() && !this.frames.get(this.playbackIndex).hasTarget && skipped < 5; skipped++) {
            this.playbackIndex++;
         }

         if (this.playbackIndex >= this.frames.size()) {
            float newAngleYaw = MathHelper.wrapDegrees(aimData.perfectYaw - finalYaw);
            float newAnglePitch = aimData.perfectPitch - finalPitch;
            this.playbackIndex = this.findBest(newAngleYaw, newAnglePitch, aimData.distance);
            this.ticksSinceSync = 0;
         }

         this.lastDebugMessage = String.format("§a[%d/%d] dY%.2f dP%.2f", this.playbackIndex, this.frames.size(), var25, var26);
         return new Rotation(finalYaw, finalPitch);
      } else {
         this.resetState();
         return null;
      }
   }

   private void rollNewRandomPoint() {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      this.randomXOffset = r.nextFloat(-0.38F, 0.38F);
      this.randomYRatio = r.nextFloat(0.4F, 0.85F);
      this.randomZOffset = r.nextFloat(-0.38F, 0.38F);
   }

   private NeuroAuraStorage.AimData getAimData(LivingEntity target, float currentYaw, float currentPitch, NeuroAuraStorage.Frame frame, boolean relaxed) {
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d point = this.selectAimPoint(target, relaxed);
      double distance = eyePos.distanceTo(point);
      double dx = point.x - eyePos.x;
      double dy = point.y - eyePos.y;
      double dz = point.z - eyePos.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float perfectYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float perfectPitch = (float)Math.toDegrees(Math.atan2(-dy, distXZ));
      NeuroAuraStorage.AimData aimData = new NeuroAuraStorage.AimData();
      aimData.targetPoint = point;
      aimData.distance = distance;
      aimData.perfectYaw = perfectYaw;
      aimData.perfectPitch = perfectPitch;
      aimData.angleYaw = MathHelper.wrapDegrees(perfectYaw - currentYaw);
      aimData.anglePitch = perfectPitch - currentPitch;
      return aimData;
   }

   private float adaptRecordedDelta(float recordedDelta, float currentAngle, float smoothness, boolean idle, float maxCorrection) {
      float correctionWeight = idle ? 0.14F : 0.045F;
      float correctionLimit = idle ? maxCorrection * 0.65F : maxCorrection * 0.3F;
      float correction = MathHelper.clamp(currentAngle - recordedDelta, -correctionLimit, correctionLimit);
      float result = recordedDelta + correction * correctionWeight;
      if (Math.abs(currentAngle) < Math.abs(result) && Math.signum(currentAngle) == Math.signum(result)) {
         result = currentAngle;
      }

      float preserveFactor = idle ? MathHelper.clamp(1.0F - smoothness * 0.22F, 0.8F, 0.97F) : MathHelper.clamp(1.0F - smoothness * 0.1F, 0.91F, 0.99F);
      result *= preserveFactor;
      return Math.abs(currentAngle) <= GCDUtil.getGCDValue() ? currentAngle : result;
   }

   private Vec3d selectAimPoint(LivingEntity target, boolean relaxed) {
      if (target != this.lastAimTarget) {
         this.lastAimTarget = target;
         this.currentAimPoint = null;
         this.targetRandomPoint = null;
         this.aimPointTicks = 0;
         this.rollNewRandomPoint();
      }

      Box box = target.getBoundingBox();
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d stablePoint = new Vec3d(box.getCenter().x, box.minY + box.getLengthY() * 0.72, box.getCenter().z);
      if (!box.expand(0.12).contains(eyePos) && !(eyePos.squaredDistanceTo(stablePoint) <= 2.25)) {
         double xCenter = (box.minX + box.maxX) * 0.5;
         double zCenter = (box.minZ + box.maxZ) * 0.5;
         double halfW = box.getLengthX() * 0.5;
         double halfD = box.getLengthZ() * 0.5;
         double height = box.getLengthY();
         Vec3d desired = new Vec3d(xCenter + halfW * this.randomXOffset, box.minY + height * this.randomYRatio, zCenter + halfD * this.randomZOffset);
         if (this.targetRandomPoint == null) {
            this.targetRandomPoint = desired;
         } else {
            float driftLerp = relaxed ? 0.13F : 0.07F;
            this.targetRandomPoint = new Vec3d(
               MathHelper.lerp(driftLerp, this.targetRandomPoint.x, desired.x),
               MathHelper.lerp(driftLerp, this.targetRandomPoint.y, desired.y),
               MathHelper.lerp(driftLerp, this.targetRandomPoint.z, desired.z)
            );
         }

         if (this.currentAimPoint == null) {
            this.currentAimPoint = this.targetRandomPoint;
            this.aimPointTicks = 0;
            return this.currentAimPoint;
         } else {
            float pointLerp = relaxed ? 0.11F : 0.055F;
            this.currentAimPoint = new Vec3d(
               MathHelper.lerp(pointLerp, this.currentAimPoint.x, this.targetRandomPoint.x),
               MathHelper.lerp(pointLerp, this.currentAimPoint.y, this.targetRandomPoint.y),
               MathHelper.lerp(pointLerp, this.currentAimPoint.z, this.targetRandomPoint.z)
            );
            this.aimPointTicks++;
            return this.currentAimPoint;
         }
      } else {
         this.currentAimPoint = stablePoint;
         this.targetRandomPoint = stablePoint;
         this.aimPointTicks = 0;
         return stablePoint;
      }
   }

   private float smoothDelta(float current, float target, float smoothness) {
      float lerpFactor = MathHelper.clamp(0.035F + (1.0F - smoothness) * 0.12F, 0.035F, 0.15F);
      return current + (target - current) * lerpFactor;
   }

   private float smoothOutputRotation(float targetRotation, float currentRotation, float smoothness, boolean idle, boolean yawAxis) {
      float previous = yawAxis ? this.smoothedOutputYaw : this.smoothedOutputPitch;
      if (Float.isNaN(previous)) {
         previous = currentRotation;
      }

      float delta = yawAxis ? MathHelper.wrapDegrees(targetRotation - previous) : targetRotation - previous;
      float maxStep = yawAxis ? (idle ? 1.68F : 0.86F) : (idle ? 1.26F : 0.62F);
      float lerpFactor = idle
         ? MathHelper.clamp(0.08F + (1.0F - smoothness) * 0.09F, 0.08F, 0.17F)
         : MathHelper.clamp(0.04F + (1.0F - smoothness) * 0.055F, 0.04F, 0.095F);
      float var13;
      float var14;
      float smoothed = previous
         + MathHelper.clamp(
            delta * (var14 = lerpFactor * (yawAxis ? this.yawSpeedFactor : this.pitchSpeedFactor)),
            -(var13 = maxStep * (yawAxis ? this.yawSpeedFactor : this.pitchSpeedFactor)),
            var13
         );
      if (yawAxis) {
         this.smoothedOutputYaw = smoothed = MathHelper.wrapDegrees(smoothed);
      } else {
         this.smoothedOutputPitch = smoothed = MathHelper.clamp(smoothed, -90.0F, 90.0F);
      }

      return smoothed;
   }

   private void updateSpeedProfile(boolean idle, boolean airborne, NeuroAuraStorage.AimData aimData) {
      if (this.speedProfileTicks > 0) {
         this.speedProfileTicks--;
      } else {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         float anglePressure = MathHelper.clamp((Math.abs(aimData.angleYaw) + Math.abs(aimData.anglePitch)) / 35.0F, 0.0F, 1.0F);
         float baseYawMin = idle ? 1.06F : 0.96F;
         float baseYawMax = idle ? 1.34F : 1.12F;
         float basePitchMin = idle ? 1.0F : 0.9F;
         float basePitchMax = idle ? 1.24F : 1.05F;
         this.yawSpeedFactor = random.nextFloat(baseYawMin, baseYawMax + anglePressure * (idle ? 0.1F : 0.16F));
         this.pitchSpeedFactor = random.nextFloat(basePitchMin, basePitchMax + anglePressure * (idle ? 0.08F : 0.12F));
         if (!idle && anglePressure > 0.58F) {
            this.yawSpeedFactor = Math.max(this.yawSpeedFactor, 1.08F + anglePressure * 0.24F);
            this.pitchSpeedFactor = Math.max(this.pitchSpeedFactor, 1.0F + anglePressure * 0.18F);
         }

         if (airborne) {
            this.yawSpeedFactor *= 0.97F;
            this.pitchSpeedFactor *= 0.95F;
         }

         this.speedProfileTicks = random.nextInt(idle ? 3 : 2, idle ? 7 : 5);
      }
   }

   private float getMicroJitter(boolean yawAxis, boolean idle, boolean airborne, NeuroAuraStorage.AimData aimData) {
      float gcd = GCDUtil.getGCDValue();
      if (gcd <= 0.0F) {
         return 0.0F;
      } else {
         float pressure = Math.abs(yawAxis ? aimData.angleYaw : aimData.anglePitch);
         if (!idle) {
            float f = yawAxis ? 10.0F : 7.0F;
            if (pressure > f) {
               return 0.0F;
            }
         }

         float amplitude = yawAxis ? gcd * 0.018F : gcd * 0.012F;
         if (airborne) {
            amplitude *= 0.35F;
         }

         float wave = (float)Math.sin((mc.player.age + (yawAxis ? 0.0F : 7.0F)) * (idle ? 0.42F : 0.28F));
         return wave * amplitude;
      }
   }

   private float quantizeToMouseStep(float delta, float remainingAngle) {
      float gcd = GCDUtil.getGCDValue();
      if (gcd <= 0.0F) {
         return delta;
      } else {
         float limited = delta;
         if (Math.abs(remainingAngle) < Math.abs(delta) && Math.signum(remainingAngle) == Math.signum(delta)) {
            limited = remainingAngle;
         }

         float quantized;
         if ((quantized = Math.round(limited / gcd) * gcd) == 0.0F && Math.abs(remainingAngle) >= gcd * 0.35F && Math.abs(limited) > 0.001F) {
            quantized = Math.signum(limited) * gcd;
         }

         if (Math.abs(remainingAngle) < Math.abs(quantized) && Math.signum(remainingAngle) == Math.signum(quantized)) {
            quantized = remainingAngle;
         }

         return quantized;
      }
   }

   private float calculateSmoothness(float deltaYaw, float deltaPitch) {
      float magnitude = Math.abs(deltaYaw) + Math.abs(deltaPitch);
      float base = 1.0F - magnitude / 18.0F;
      float periodic = (float)Math.sin((this.recordedThisSession + mc.player.age * 0.31F) * 0.34F) * 0.012F;
      float noise = ThreadLocalRandom.current().nextFloat(-0.008F, 0.008F);
      return MathHelper.clamp(base + periodic + noise, 0.22F, 0.88F);
   }

   private int findBest(float angleYaw, float anglePitch, double distance) {
      int best = 0;
      float bestScore = Float.MAX_VALUE;

      for (int i = 0; i < this.frames.size(); i++) {
         NeuroAuraStorage.Frame frame = this.frames.get(i);
         float score;
         if (frame.hasTarget
            && (
                  score = Math.abs(MathHelper.wrapDegrees(frame.angleYaw - angleYaw))
                     + Math.abs(frame.anglePitch - anglePitch)
                     + (float)Math.abs(frame.distance - distance) * 0.3F
               )
               < bestScore) {
            bestScore = score;
            best = i;
         }
      }

      return best;
   }

   public boolean savePatterns(String profileName) {
      if (this.frames.isEmpty()) {
         this.lastDebugMessage = "§cНет записей";
         return false;
      } else {
         try {
            boolean var4;
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("data_patterns/" + profileName + ".data"))) {
               NeuroAuraStorage.SaveData data = new NeuroAuraStorage.SaveData();
               data.patterns = new ArrayList<>(this.recordedPatterns);
               data.frames = new ArrayList<>(this.frames);
               out.writeObject(data);
               this.currentPatternName = profileName;
               this.lastDebugMessage = "§aСохранено " + this.frames.size();
               var4 = true;
            }

            return var4;
         } catch (IOException var7) {
            this.lastDebugMessage = "§cОшибка сохранения";
            return false;
         }
      }
   }

   public boolean loadPatterns(String profileName) {
      File file = this.resolveProfileFile(profileName);
      if (!file.exists()) {
         this.lastDebugMessage = "§eНе найдено: " + profileName;
         return false;
      } else {
         try {
            boolean var10;
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
               Object obj = in.readObject();
               this.recordedPatterns.clear();
               this.frames.clear();
               if (obj instanceof NeuroAuraStorage.SaveData data) {
                  if (data.patterns != null) {
                     this.recordedPatterns.addAll(data.patterns);
                  }

                  if (data.frames != null) {
                     this.frames.addAll(data.frames);
                  }
               } else if (obj instanceof List list) {
                  this.recordedPatterns.addAll(list);
               }

               if (this.frames.isEmpty() && !this.recordedPatterns.isEmpty()) {
                  this.rebuildFramesFromPatterns();
               }

               this.currentPatternName = profileName;
               this.resetState();
               this.lastDebugMessage = "§aЗагружено " + this.frames.size();
               var10 = !this.frames.isEmpty();
            }

            return var10;
         } catch (ClassNotFoundException | IOException var8) {
            this.lastDebugMessage = "§cОшибка загрузки";
            return false;
         }
      }
   }

   private void rebuildFramesFromPatterns() {
      for (NeuroPattern pattern : this.recordedPatterns) {
         NeuroAuraStorage.Frame frame = new NeuroAuraStorage.Frame();
         frame.deltaYaw = pattern.getDeltaYaw();
         frame.deltaPitch = pattern.getDeltaPitch();
         frame.angleYaw = pattern.getYaw();
         frame.anglePitch = pattern.getPitch();
         frame.distance = pattern.getDistance();
         frame.hasTarget = true;
         frame.smoothness = MathHelper.clamp(pattern.getSmoothness(), 0.18F, 0.9F);
         this.frames.add(frame);
      }
   }

   public boolean deletePatterns(String profileName) {
      File primaryFile = new File("data_patterns/" + profileName + ".data");
      File legacyFile = new File("neuro_patterns/" + profileName + ".neuro");
      boolean deleted = false;
      if (primaryFile.exists()) {
         deleted = primaryFile.delete();
      }

      if (legacyFile.exists()) {
         deleted = legacyFile.delete() || deleted;
      }

      if (deleted) {
         if (profileName.equals(this.currentPatternName)) {
            this.currentPatternName = null;
         }

         this.lastDebugMessage = "§aУдалено";
         return true;
      } else {
         return false;
      }
   }

   public int getPatternCount() {
      return this.recordedPatterns.size();
   }

   public int getFrameCount() {
      return this.frames.size();
   }

   public void startRecording() {
      this.recordedPatterns.clear();
      this.frames.clear();
      this.isRecording = true;
      this.isUsingNeuro = false;
      this.recordedThisSession = 0;
      this.lastRecordTime = 0L;
      this.currentPatternName = null;
      this.hasRecordedBefore = false;
      this.prevRecordYaw = 0.0F;
      this.prevRecordPitch = 0.0F;
      this.resetState();
      this.lastDebugMessage = "§aЗапись";
   }

   public void stopRecording() {
      this.isRecording = false;
      this.lastDebugMessage = "§eСтоп: " + this.frames.size();
   }

   public void clearPatterns() {
      this.recordedPatterns.clear();
      this.frames.clear();
      this.isRecording = false;
      this.isUsingNeuro = false;
      this.recordedThisSession = 0;
      this.currentPatternName = null;
      this.hasRecordedBefore = false;
      this.prevRecordYaw = 0.0F;
      this.prevRecordPitch = 0.0F;
      this.resetState();
      this.lastDebugMessage = "§eОчищено";
   }

   public void resetState() {
      this.playbackIndex = -1;
      this.ticksSinceSync = 0;
      this.smoothedYawDelta = 0.0F;
      this.smoothedPitchDelta = 0.0F;
      this.smoothedOutputYaw = Float.NaN;
      this.smoothedOutputPitch = Float.NaN;
      this.yawSpeedFactor = 1.0F;
      this.pitchSpeedFactor = 1.0F;
      this.speedProfileTicks = 0;
      this.currentAimPoint = null;
      this.targetRandomPoint = null;
      this.aimPointTicks = 0;
      this.lastAimTarget = null;
      this.lastWasIdle = true;
      this.attackCount = 0;
      this.rollNewRandomPoint();
   }

   public String getStatusString() {
      String status = "§8[§bData§8] §f" + this.frames.size();
      if (this.isRecording) {
         status = status + " §a[REC]";
      }

      if (this.isUsingNeuro) {
         status = status + " §b[ON " + this.playbackIndex + "]";
      }

      return status;
   }

   public List<String> getPatternNames() {
      ArrayList<String> names = new ArrayList<>();
      this.collectPatternNames(names, new File("data_patterns"), ".data");
      this.collectPatternNames(names, new File("neuro_patterns"), ".neuro");
      return names;
   }

   private void collectPatternNames(List<String> names, File directory, String extension) {
      if (directory.exists() && directory.isDirectory()) {
         File[] files = directory.listFiles((dir, name) -> name.endsWith(extension));
         if (files != null) {
            for (File file : files) {
               String name2 = file.getName().replace(extension, "");
               if (!names.contains(name2)) {
                  names.add(name2);
               }
            }
         }
      }
   }

   private File resolveProfileFile(String profileName) {
      File primaryFile = new File("data_patterns/" + profileName + ".data");
      return primaryFile.exists() ? primaryFile : new File("neuro_patterns/" + profileName + ".neuro");
   }

   public List<NeuroPattern> getRecordedPatterns() {
      return this.recordedPatterns;
   }

   public boolean isRecording() {
      return this.isRecording;
   }

   public void setRecording(boolean isRecording) {
      this.isRecording = isRecording;
   }

   public boolean isUsingNeuro() {
      return this.isUsingNeuro;
   }

   public void setUsingNeuro(boolean isUsingNeuro) {
      this.isUsingNeuro = isUsingNeuro;
   }

   public boolean isShowStats() {
      return this.showStats;
   }

   public void setShowStats(boolean showStats) {
      this.showStats = showStats;
   }

   public String getCurrentPatternName() {
      return this.currentPatternName;
   }

   public void setCurrentPatternName(String currentPatternName) {
      this.currentPatternName = currentPatternName;
   }

   public String getLastDebugMessage() {
      return this.lastDebugMessage;
   }

   public int getRecordedThisSession() {
      return this.recordedThisSession;
   }

   private static class AimData {
      Vec3d targetPoint;
      float perfectYaw;
      float perfectPitch;
      float angleYaw;
      float anglePitch;
      double distance;
   }

   private static class Frame implements Serializable {
      private static final long serialVersionUID = 7L;
      float deltaYaw;
      float deltaPitch;
      float angleYaw;
      float anglePitch;
      double distance;
      boolean hasTarget;
      float smoothness;
   }

   private static class SaveData implements Serializable {
      private static final long serialVersionUID = 7L;
      List<NeuroPattern> patterns;
      List<NeuroAuraStorage.Frame> frames;
   }
}
