package code.elixir.emotions;

import code.elixir.display.screens.EmotionWheelScreen;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.misc.Globals;
import code.elixir.main.listener.Listener;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.MathHelper;

public class EmotionWheelManager implements Listener, QuickImports {
   private static final String SYNC_URL = "wss://Elixir-emotions.onrender.com";
   private static final String PRESENCE_EMOTION = "__presence__";
   private static final long PRESENCE_INTERVAL_MS = 20000L;
   private static final long PRESENCE_TIMEOUT_MS = 35000L;
   private static EmotionWheelManager instance;
   private final Map<UUID, EmotionWheelManager.ActiveEmotion> activeEmotions = new ConcurrentHashMap<>();
   private final Map<UUID, EmotionWheelManager.PendingEmotion> pendingEmotions = new ConcurrentHashMap<>();
   private final Map<UUID, Long> knownClientPlayers = new ConcurrentHashMap<>();
   private final CopyOnWriteArrayList<Consumer<JsonObject>> relayListeners = new CopyOnWriteArrayList<>();
   private EmotionWheelScreen wheelScreen;
   private EmotionSyncWebSocketClient syncClient;
   private long nextReconnectAttemptMs;
   private long nextPresenceBroadcastMs;
   private EmotionType previewEmotion = EmotionType.HELLO;
   private long previewStartedAtMs;

   public static EmotionWheelManager create() {
      instance = new EmotionWheelManager();
      return instance;
   }

   public static EmotionWheelManager getInstance() {
      return instance;
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      Globals globalsModule = Instance.get(Globals.class);
      if (globalsModule != null && globalsModule.isState()) {
         int openKey = globalsModule.getEmotionWheelBind().getKey();
         if (openKey != -1 && event.key() == openKey && event.type() == Type.KEYSYM) {
            if (event.action() == 1) {
               if (mc.player != null && mc.world != null && mc.currentScreen == null) {
                  this.openWheel();
               }
            } else if (event.action() == 0) {
               Screen class_4372 = mc.currentScreen;
               if (mc.currentScreen instanceof EmotionWheelScreen) {
                  EmotionWheelScreen screen = (EmotionWheelScreen)class_4372;
                  EmotionType emotion = screen.getHoveredEmotion();
                  this.closeWheel(false);
                  if (emotion != null) {
                     this.playEmotion(emotion);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      Globals globalsModule = Instance.get(Globals.class);
      if (globalsModule != null && globalsModule.isState()) {
         this.ensureSyncConnection();
         if (mc.player != null && mc.world != null) {
            this.sendPresenceIfNeeded();
            this.pruneKnownClients();
            this.applyPendingEmotions();
            Iterator<Entry<UUID, EmotionWheelManager.ActiveEmotion>> iterator = this.activeEmotions.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<UUID, EmotionWheelManager.ActiveEmotion> entry = iterator.next();
               if (entry.getValue().isExpired()) {
                  if (entry.getValue().local && mc.player != null && mc.player.getUuid().equals(entry.getKey())) {
                     mc.options.setPerspective(entry.getValue().previousPerspective);
                  }

                  iterator.remove();
               }
            }
         } else {
            this.closeWheel(false);
            this.stopEmotion();
            this.knownClientPlayers.clear();
            this.pendingEmotions.clear();
         }
      } else {
         this.closeWheel(false);
         this.stopEmotion();
         this.disconnectSync();
         this.knownClientPlayers.clear();
         this.pendingEmotions.clear();
      }
   }

   public void closeWheel(boolean forceGame) {
      if (mc.currentScreen instanceof EmotionWheelScreen) {
         this.wheelScreen = null;
         mc.setScreen(null);
      }
   }

   public void applyToModel(
      PlayerEntityRenderState state,
      ModelPart head,
      ModelPart hat,
      ModelPart body,
      ModelPart rightArm,
      ModelPart leftArm,
      ModelPart rightLeg,
      ModelPart leftLeg
   ) {
      if (mc.world != null) {
         if (mc.world.getEntityById(state.id) instanceof PlayerEntity player) {
            EmotionWheelManager.ActiveEmotion activeEmotion = this.activeEmotions.get(player.getUuid());
            EmotionType renderEmotion = activeEmotion != null ? activeEmotion.type : null;
            long startedAt = activeEmotion != null ? activeEmotion.startedAt : 0L;
            float alpha = activeEmotion != null ? activeEmotion.getBlend() : 0.0F;
            if (renderEmotion == null
               && mc.currentScreen instanceof EmotionWheelScreen
               && mc.player != null
               && player == mc.player
               && this.previewEmotion != null) {
               renderEmotion = this.previewEmotion;
               startedAt = this.previewStartedAtMs;
               alpha = 1.0F;
            }

            if (renderEmotion != null) {
               this.resetModelPose(head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
               long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
               boolean animateHands = this.canAnimateHands(player);
               this.applyEmotionPose(renderEmotion, alpha, elapsed, animateHands, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
            }
         }
      }
   }

   private void applyEmotionPose(
      EmotionType renderEmotion,
      float alpha,
      long elapsed,
      boolean animateHands,
      ModelPart head,
      ModelPart hat,
      ModelPart body,
      ModelPart rightArm,
      ModelPart leftArm,
      ModelPart rightLeg,
      ModelPart leftLeg
   ) {
      if (renderEmotion == EmotionType.DEB) {
         this.applyDeb(alpha, animateHands, head, hat, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.FLOSS) {
         this.applyFloss(alpha, elapsed, animateHands, body, rightArm, leftArm, rightLeg, leftLeg);
      } else if (renderEmotion == EmotionType.GET_GRIDDY) {
         this.applyGetGriddy(alpha, elapsed, animateHands, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
      } else if (renderEmotion == EmotionType.HELLO) {
         this.applyHello(alpha, elapsed, animateHands, rightArm);
      } else if (renderEmotion == EmotionType.HAPPY) {
         this.applyHappy(alpha, animateHands, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.CLAP) {
         this.applyClap(alpha, elapsed, animateHands, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.CRY) {
         this.applyCry(alpha, elapsed, animateHands, head, hat, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.POINT) {
         this.applyPoint(alpha, animateHands, head, hat, body, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.SALUTE) {
         this.applySalute(alpha, animateHands, head, hat, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.THINK) {
         this.applyThink(alpha, animateHands, head, hat, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.STRETCH) {
         this.applyStretch(alpha, animateHands, body, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.DANCE) {
         this.applyDance(alpha, elapsed, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
      } else if (renderEmotion == EmotionType.FACEPALM) {
         this.applyFacepalm(alpha, animateHands, head, hat, rightArm, leftArm);
      } else if (renderEmotion == EmotionType.SHRUG) {
         this.applyShrug(alpha, animateHands, head, hat, rightArm, leftArm);
      }
   }

   private void openWheel() {
      this.wheelScreen = new EmotionWheelScreen(this);
      mc.setScreen(this.wheelScreen);
   }

   public void openFromGlobals() {
      Globals globalsModule = Instance.get(Globals.class);
      if (globalsModule != null && globalsModule.isState()) {
         if (mc.player != null && mc.world != null && mc.currentScreen == null) {
            this.openWheel();
         }
      }
   }

   public void setPreviewEmotion(EmotionType emotionType) {
      if (emotionType != null) {
         if (this.previewEmotion != emotionType) {
            this.previewEmotion = emotionType;
            this.previewStartedAtMs = System.currentTimeMillis();
         }
      }
   }

   public EmotionType getPreviewEmotion() {
      return this.previewEmotion;
   }

   private void playEmotion(EmotionType emotionType) {
      if (mc.player != null) {
         long startedAt = System.currentTimeMillis();
         this.startEmotion(mc.player, emotionType, true, startedAt);
         this.sendEmotionPacket(emotionType, startedAt);
      }
   }

   private void stopEmotion() {
      if (mc.player == null) {
         this.activeEmotions.clear();
      } else {
         EmotionWheelManager.ActiveEmotion activeEmotion = this.activeEmotions.remove(mc.player.getUuid());
         if (activeEmotion != null && activeEmotion.local) {
            mc.options.setPerspective(activeEmotion.previousPerspective);
         }
      }
   }

   private void applyDeb(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -0.7F, alpha);
         rightArm.roll = this.move(rightArm.roll, -2.0F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, -0.7F, alpha);
         leftArm.roll = this.move(leftArm.roll, -2.0F, alpha);
      }

      head.pitch = this.move(head.pitch, 0.7F, alpha);
      head.yaw = this.move(head.yaw, 0.5F, alpha);
      hat.pitch = this.move(hat.pitch, 0.7F, alpha);
      hat.yaw = this.move(hat.yaw, 0.5F, alpha);
   }

   private void applyHello(float alpha, long elapsed, boolean animateHands, ModelPart rightArm) {
      if (animateHands) {
         float wave = this.pingPong(elapsed, 250L);
         rightArm.pitch = this.move(rightArm.pitch, -0.2F, alpha);
         rightArm.roll = this.move(rightArm.roll, 2.75F - 0.25F * wave, alpha);
      }
   }

   private void applyHappy(float alpha, boolean animateHands, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         float pulse = this.pingPong(System.currentTimeMillis(), 260L);
         rightArm.pitch = this.move(rightArm.pitch, -0.35F - 0.2F * pulse, alpha);
         rightArm.roll = this.move(rightArm.roll, 2.1F + 0.3F * pulse, alpha);
         leftArm.pitch = this.move(leftArm.pitch, -0.35F - 0.2F * pulse, alpha);
         leftArm.roll = this.move(leftArm.roll, -2.1F - 0.3F * pulse, alpha);
      }
   }

   private void applyClap(float alpha, long elapsed, boolean animateHands, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         float t = this.pingPong(elapsed, 220L);
         float xBend = -1.1F * t;
         float yInward = 0.95F * t;
         float zFlat = 0.55F * t;
         rightArm.pitch = this.move(rightArm.pitch, xBend, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -yInward, alpha);
         rightArm.roll = this.move(rightArm.roll, zFlat, alpha);
         leftArm.pitch = this.move(leftArm.pitch, xBend, alpha);
         leftArm.yaw = this.move(leftArm.yaw, yInward, alpha);
         leftArm.roll = this.move(leftArm.roll, -zFlat, alpha);
      }
   }

   private void applyCry(float alpha, long elapsed, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         float hand = this.pingPong(elapsed, 320L);
         float xBend = -1.9F * hand - 0.25F;
         float yInward = 0.62F * hand;
         float zAcross = 1.05F * hand;
         rightArm.pitch = this.move(rightArm.pitch, xBend, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -yInward, alpha);
         rightArm.roll = this.move(rightArm.roll, zAcross, alpha);
         leftArm.pitch = this.move(leftArm.pitch, xBend, alpha);
         leftArm.yaw = this.move(leftArm.yaw, yInward, alpha);
         leftArm.roll = this.move(leftArm.roll, -zAcross, alpha);
      }

      float headBob = this.pingPong(elapsed, 220L);
      head.pitch = this.move(head.pitch, 0.62F + 0.25F * headBob, alpha);
      head.roll = this.move(head.roll, -0.1F + 0.2F * headBob, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      hat.roll = this.move(hat.roll, head.roll, alpha);
   }

   private void applyFloss(
      float alpha, long elapsed, boolean animateHands, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg
   ) {
      float action = this.pingPong(elapsed, 250L);
      float rightSide = this.pingPong(elapsed, 500L);
      float leftSide = this.pingPong(elapsed + 250L, 500L);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, 0.5F - rightSide, alpha);
         rightArm.roll = this.move(rightArm.roll, 1.0F - 2.0F * action, alpha);
         leftArm.pitch = this.move(leftArm.pitch, 0.5F - leftSide, alpha);
         leftArm.roll = this.move(leftArm.roll, 1.5F - 2.0F * action, alpha);
      }

      body.roll = this.move(body.roll, 0.3F * action - 0.15F, alpha);
      rightLeg.pivotX = this.move(rightLeg.pivotX, -0.5F - 3.0F * action, alpha);
      leftLeg.pivotX = this.move(leftLeg.pivotX, 3.0F - 2.5F * action, alpha);
   }

   private void applyGetGriddy(
      float alpha,
      long elapsed,
      boolean animateHands,
      ModelPart head,
      ModelPart hat,
      ModelPart body,
      ModelPart rightArm,
      ModelPart leftArm,
      ModelPart rightLeg,
      ModelPart leftLeg
   ) {
      float action = this.pingPong(elapsed, 350L);
      float leg = this.pingPong(elapsed, 300L);
      float eye = this.griddyEye(elapsed);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, 1.0F - 1.5F * action - 3.5F * eye, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.5F - 0.75F * action, alpha);
         leftArm.pitch = this.move(leftArm.pitch, 1.0F - 1.5F * action - 3.5F * eye, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.5F + 0.75F * action, alpha);
      }

      body.pitch = this.move(body.pitch, 0.2F - 0.05F * action, alpha);
      rightLeg.pivotZ = this.move(rightLeg.pivotZ, 2.0F - 0.5F * action, alpha);
      rightLeg.pitch = this.move(rightLeg.pitch, 0.3F * leg - 0.3F, alpha);
      leftLeg.pivotZ = this.move(leftLeg.pivotZ, 2.0F - 0.5F * action, alpha);
      leftLeg.pitch = this.move(leftLeg.pitch, -0.3F * leg - 0.1F, alpha);
      head.yaw = this.move(head.yaw, -0.4F + 0.4F * eye, alpha);
      head.pitch = this.move(head.pitch, -0.3F * eye, alpha);
      head.roll = this.move(head.roll, 0.0F, alpha);
      hat.yaw = this.move(hat.yaw, -0.4F + 0.4F * eye, alpha);
      hat.pitch = this.move(hat.pitch, -0.3F * eye, alpha);
      hat.roll = this.move(hat.roll, 0.0F, alpha);
   }

   private void applyPoint(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
      body.yaw = this.move(body.yaw, -0.18F, alpha);
      body.pitch = this.move(body.pitch, -0.04F, alpha);
      head.yaw = this.move(head.yaw, -0.3F, alpha);
      head.pitch = this.move(head.pitch, -0.1F, alpha);
      hat.yaw = this.move(hat.yaw, head.yaw, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -1.52F, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -0.22F, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.09F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, 0.1F, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.2F, alpha);
      }
   }

   private void applySalute(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      head.yaw = this.move(head.yaw, -0.12F, alpha);
      head.pitch = this.move(head.pitch, -0.08F, alpha);
      hat.yaw = this.move(hat.yaw, head.yaw, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -1.26F, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -0.38F, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.7F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, 0.04F, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.1F, alpha);
      }
   }

   private void applyThink(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      head.yaw = this.move(head.yaw, 0.36F, alpha);
      head.pitch = this.move(head.pitch, 0.22F, alpha);
      head.roll = this.move(head.roll, 0.05F, alpha);
      hat.yaw = this.move(hat.yaw, head.yaw, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      hat.roll = this.move(hat.roll, head.roll, alpha);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -1.04F, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -0.28F, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.48F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, 0.1F, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.16F, alpha);
      }
   }

   private void applyStretch(float alpha, boolean animateHands, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
      float stretchWave = this.pingPong(System.currentTimeMillis(), 320L);
      body.pitch = this.move(body.pitch, -0.12F - 0.06F * stretchWave, alpha);
      body.pivotY = this.move(body.pivotY, -0.8F - 0.3F * stretchWave, alpha);
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -2.8F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, -2.8F, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.22F, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.22F, alpha);
         rightArm.yaw = this.move(rightArm.yaw, 0.05F, alpha);
         leftArm.yaw = this.move(leftArm.yaw, -0.05F, alpha);
      }
   }

   private void applyDance(
      float alpha, long elapsed, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg
   ) {
      float sway = this.pingPong(elapsed, 220L) * 2.0F - 1.0F;
      float bounce = this.pingPong(elapsed + 90L, 150L);
      body.roll = this.move(body.roll, 0.28F * sway, alpha);
      body.yaw = this.move(body.yaw, 0.25F * sway, alpha);
      body.pitch = this.move(body.pitch, -0.03F + 0.06F * bounce, alpha);
      head.roll = this.move(head.roll, -0.18F * sway, alpha);
      head.yaw = this.move(head.yaw, 0.22F * sway, alpha);
      hat.roll = this.move(hat.roll, head.roll, alpha);
      hat.yaw = this.move(hat.yaw, head.yaw, alpha);
      rightArm.pitch = this.move(rightArm.pitch, -0.8F + 0.5F * sway, alpha);
      leftArm.pitch = this.move(leftArm.pitch, -0.8F - 0.5F * sway, alpha);
      rightArm.roll = this.move(rightArm.roll, 0.55F + 0.3F * bounce, alpha);
      leftArm.roll = this.move(leftArm.roll, -0.55F - 0.3F * bounce, alpha);
      rightLeg.pitch = this.move(rightLeg.pitch, 0.18F + 0.38F * sway, alpha);
      leftLeg.pitch = this.move(leftLeg.pitch, 0.18F - 0.38F * sway, alpha);
   }

   private void applyFacepalm(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         rightArm.pitch = this.move(rightArm.pitch, -1.62F, alpha);
         rightArm.yaw = this.move(rightArm.yaw, -0.3F, alpha);
         rightArm.roll = this.move(rightArm.roll, 0.48F, alpha);
         leftArm.pitch = this.move(leftArm.pitch, -0.06F, alpha);
         leftArm.roll = this.move(leftArm.roll, -0.05F, alpha);
      }

      head.pitch = this.move(head.pitch, 0.48F, alpha);
      head.roll = this.move(head.roll, -0.08F, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      hat.roll = this.move(hat.roll, head.roll, alpha);
   }

   private void applyShrug(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
      if (animateHands) {
         float shrugPulse = this.pingPong(System.currentTimeMillis(), 260L);
         rightArm.pitch = this.move(rightArm.pitch, -0.42F - 0.12F * shrugPulse, alpha);
         leftArm.pitch = this.move(leftArm.pitch, -0.42F - 0.12F * shrugPulse, alpha);
         rightArm.roll = this.move(rightArm.roll, 1.05F + 0.15F * shrugPulse, alpha);
         leftArm.roll = this.move(leftArm.roll, -1.05F - 0.15F * shrugPulse, alpha);
      }

      head.pitch = this.move(head.pitch, 0.1F, alpha);
      head.yaw = this.move(head.yaw, 0.08F, alpha);
      hat.pitch = this.move(hat.pitch, head.pitch, alpha);
      hat.yaw = this.move(hat.yaw, head.yaw, alpha);
   }

   private boolean canAnimateHands(PlayerEntity targetPlayer) {
      return mc.player == null || targetPlayer != mc.player || !mc.options.getPerspective().isFirstPerson();
   }

   private void resetModelPose(ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
      head.pivotX = 0.0F;
      head.pivotY = 0.0F;
      head.pivotZ = 0.0F;
      hat.pivotX = 0.0F;
      hat.pivotY = 0.0F;
      hat.pivotZ = 0.0F;
      body.pivotX = 0.0F;
      body.pivotY = 0.0F;
      body.pivotZ = 0.0F;
      rightArm.pivotX = -5.0F;
      rightArm.pivotY = 2.0F;
      rightArm.pivotZ = 0.0F;
      leftArm.pivotX = 5.0F;
      leftArm.pivotY = 2.0F;
      leftArm.pivotZ = 0.0F;
      rightLeg.pivotX = -1.9F;
      rightLeg.pivotY = 12.0F;
      rightLeg.pivotZ = 0.0F;
      leftLeg.pivotX = 1.9F;
      leftLeg.pivotY = 12.0F;
      leftLeg.pivotZ = 0.0F;
   }

   private float move(float current, float target, float alpha) {
      return MathHelper.lerp(alpha, current, target);
   }

   private float pingPong(long elapsed, long halfCycleMs) {
      if (halfCycleMs <= 0L) {
         return 0.0F;
      } else {
         float cycle = (float)elapsed % ((float)halfCycleMs * 2.0F) / (float)halfCycleMs;
         return cycle <= 1.0F ? cycle : 2.0F - cycle;
      }
   }

   private float griddyEye(long elapsed) {
      long cycle = elapsed % 3100L;
      return cycle < 2100L ? 0.0F : this.pingPong(cycle - 2100L, 500L);
   }

   private void ensureSyncConnection() {
      if (this.syncClient == null || !this.syncClient.isOpen() && !this.syncClient.isConnectingState()) {
         long now = System.currentTimeMillis();
         if (now >= this.nextReconnectAttemptMs) {
            this.nextReconnectAttemptMs = now + 10000L;

            try {
               this.syncClient = new EmotionSyncWebSocketClient(URI.create("wss://Elixir-emotions.onrender.com"), this::handleIncomingMessage);
               this.syncClient.connect();
            } catch (Exception var4) {
            }
         }
      }
   }

   private void disconnectSync() {
      if (this.syncClient != null) {
         try {
            this.syncClient.close();
         } catch (Exception var2) {
         }

         this.syncClient = null;
      }
   }

   public boolean isSyncConnected() {
      return this.syncClient != null && this.syncClient.isOpen();
   }

   public boolean isSyncConnecting() {
      return this.syncClient != null && this.syncClient.isConnectingState();
   }

   public String getSyncUrl() {
      return "wss://Elixir-emotions.onrender.com";
   }

   public String getDebugServerId() {
      return this.getCurrentServerId();
   }

   public boolean hasCompatibleClient(UUID uuid) {
      return this.knownClientPlayers.containsKey(uuid);
   }

   public void addRelayListener(Consumer<JsonObject> listener) {
      if (listener != null && !this.relayListeners.contains(listener)) {
         this.relayListeners.add(listener);
      }
   }

   public void removeRelayListener(Consumer<JsonObject> listener) {
      this.relayListeners.remove(listener);
   }

   public int getKnownClientCount() {
      return this.knownClientPlayers.size();
   }

   private void sendEmotionPacket(EmotionType emotionType, long startedAt) {
      this.sendRelayPacket(emotionType.name(), startedAt, null);
   }

   private void sendPresenceIfNeeded() {
      long now = System.currentTimeMillis();
      if (now >= this.nextPresenceBroadcastMs) {
         this.nextPresenceBroadcastMs = now + 20000L;
         this.sendSpecialPacket("__presence__", now);
      }
   }

   private void sendSpecialPacket(String emotionValue, long startedAt) {
      this.sendRelayPacket(emotionValue, startedAt, null);
   }

   public boolean sendRelayPacket(String emotionValue, long startedAt, Consumer<JsonObject> packetCustomizer) {
      if (this.syncClient != null && this.syncClient.isOpen() && mc.player != null) {
         JsonObject json = new JsonObject();
         json.addProperty("type", "play_emotion");
         json.addProperty("playerUuid", mc.player.getUuidAsString());
         json.addProperty("playerName", mc.player.getName().getString());
         json.addProperty("emotion", emotionValue);
         json.addProperty("startedAt", startedAt);
         json.addProperty("serverId", this.getCurrentServerId());
         if (packetCustomizer != null) {
            packetCustomizer.accept(json);
         }

         this.syncClient.send(gson.toJson(json));
         return true;
      } else {
         return false;
      }
   }

   private void pruneKnownClients() {
      long cutoff = System.currentTimeMillis() - 35000L;
      this.knownClientPlayers.entrySet().removeIf(entry -> entry.getValue() < cutoff);
   }

   private void handleIncomingMessage(String message) {
      try {
         JsonObject json = JsonParser.parseString(message).getAsJsonObject();
         if (!json.has("type") || !"play_emotion".equals(json.get("type").getAsString())) {
            return;
         }

         if (!json.has("serverId") || !this.getCurrentServerId().equals(json.get("serverId").getAsString())) {
            return;
         }

         if (mc.world == null || !json.has("playerUuid") || !json.has("emotion")) {
            return;
         }

         UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
         if (mc.player != null && mc.player.getUuid().equals(uuid)) {
            return;
         }

         this.knownClientPlayers.put(uuid, System.currentTimeMillis());

         for (Consumer<JsonObject> relayListener : this.relayListeners) {
            relayListener.accept(json);
         }

         String emotionValue = json.get("emotion").getAsString();
         if ("__presence__".equals(emotionValue)) {
            return;
         }

         EmotionType type = EmotionType.valueOf(emotionValue);
         long startedAt = json.has("startedAt") ? json.get("startedAt").getAsLong() : System.currentTimeMillis();
         if (this.applyEmotionToPlayer(uuid, type, startedAt)) {
            return;
         }

         this.pendingEmotions.put(uuid, new EmotionWheelManager.PendingEmotion(type, startedAt));
      } catch (Exception var10) {
      }
   }

   private void applyPendingEmotions() {
      if (!this.pendingEmotions.isEmpty() && mc.world != null) {
         Iterator<Entry<UUID, EmotionWheelManager.PendingEmotion>> iterator = this.pendingEmotions.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, EmotionWheelManager.PendingEmotion> entry = iterator.next();
            EmotionWheelManager.PendingEmotion pending = entry.getValue();
            if (pending.isExpired()) {
               iterator.remove();
            } else if (this.applyEmotionToPlayer(entry.getKey(), pending.type(), pending.startedAt())) {
               iterator.remove();
            }
         }
      }
   }

   private boolean applyEmotionToPlayer(UUID uuid, EmotionType type, long startedAt) {
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player.getUuid().equals(uuid)) {
            this.startEmotion(player, type, false, startedAt);
            return true;
         }
      }

      return false;
   }

   private void startEmotion(PlayerEntity player, EmotionType emotionType, boolean local, long startedAt) {
      Perspective previous = Perspective.FIRST_PERSON;
      if (local) {
         EmotionWheelManager.ActiveEmotion previousLocal = this.activeEmotions.get(player.getUuid());
         previous = previousLocal != null && previousLocal.local ? previousLocal.previousPerspective : mc.options.getPerspective();
         mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
      }

      this.activeEmotions.put(player.getUuid(), new EmotionWheelManager.ActiveEmotion(emotionType, startedAt, previous, local));
   }

   private String getCurrentServerId() {
      if (mc.isInSingleplayer()) {
         IntegratedServer server = mc.getServer();
         return server != null ? "singleplayer:" + server.getSaveProperties().getLevelName() : "singleplayer";
      } else {
         return mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "unknown";
      }
   }

   private static final class ActiveEmotion {
      private final EmotionType type;
      private final long startedAt;
      private final Perspective previousPerspective;
      private final boolean local;

      private ActiveEmotion(EmotionType type, long startedAt, Perspective previousPerspective, boolean local) {
         this.type = type;
         this.startedAt = startedAt;
         this.previousPerspective = previousPerspective;
         this.local = local;
      }

      private long getElapsedMs() {
         return System.currentTimeMillis() - this.startedAt;
      }

      private boolean isExpired() {
         return this.getElapsedMs() >= this.type.getDurationMs();
      }

      private float getBlend() {
         long elapsed = this.getElapsedMs();
         long remaining = Math.max(0L, this.type.getDurationMs() - elapsed);
         float fadeIn = MathHelper.clamp((float)elapsed / 300.0F, 0.0F, 1.0F);
         float fadeOut = MathHelper.clamp((float)remaining / 300.0F, 0.0F, 1.0F);
         float blend = Math.min(fadeIn, fadeOut);
         return blend * blend * (3.0F - 2.0F * blend);
      }
   }

   private static final class PendingEmotion {
      private final EmotionType type;
      private final long startedAt;

      private PendingEmotion(EmotionType type, long startedAt) {
         this.type = type;
         this.startedAt = startedAt;
      }

      private EmotionType type() {
         return this.type;
      }

      private long startedAt() {
         return this.startedAt;
      }

      private boolean isExpired() {
         return System.currentTimeMillis() - this.startedAt >= this.type.getDurationMs();
      }
   }
}
