package code.elixir.utils.features.aura;

import code.elixir.Elixir;
import code.elixir.events.keyboard.MouseRotationEvent;
import code.elixir.events.render.CameraEvent;
import code.elixir.features.impl.combat.components.gcd.GCDUtil;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.util.math.MathHelper;

public class FreeLookStorage implements QuickImports {
   private static boolean active;
   private static float freeYaw;
   private static float freePitch;

   public FreeLookStorage() {
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   public static boolean isActive() {
      return active;
   }

   public static void setActive(boolean value) {
      if (!value) {
         active = false;
      } else {
         activateForSilentRotation();
      }
   }

   public static void activateForSilentRotation() {
      if (mc != null && mc.player != null) {
         captureCameraAngles();
         active = true;
      }
   }

   private static void captureCameraAngles() {
      if (mc.gameRenderer != null) {
         freeYaw = mc.gameRenderer.getCamera().getYaw();
         freePitch = mc.gameRenderer.getCamera().getPitch();
      } else {
         freeYaw = mc.player.getYaw();
         freePitch = mc.player.getPitch();
      }
   }

   @EventHandler
   public void onLook(MouseRotationEvent event) {
      if (active) {
         this.rotateTowards(event.getCursorDeltaX(), event.getCursorDeltaY());
         event.cancel();
      }
   }

   @EventHandler
   public void onRotation(CameraEvent event) {
      if (active) {
         event.setAngle(new Turns(freeYaw, freePitch));
         event.cancel();
      } else if (event.getAngle() != null) {
         freeYaw = event.getAngle().getYaw();
         freePitch = event.getAngle().getPitch();
      }
   }

   private void rotateTowards(double targetYaw, double targetPitch) {
      float gcd = GCDUtil.getGCDValue();
      freePitch = MathHelper.clamp((float)(freePitch + targetPitch * gcd), -89.0F, 89.0F);
      freeYaw = (float)(freeYaw + targetYaw * gcd);
   }

   public static void blendToward(float targetYaw, float targetPitch, float factor) {
      if (active) {
         float blend = MathHelper.clamp(factor, 0.0F, 1.0F);
         float yawDelta = MathHelper.wrapDegrees(targetYaw - freeYaw);
         float pitchDelta = targetPitch - freePitch;
         freeYaw = MathHelper.wrapDegrees(freeYaw + yawDelta * blend);
         freePitch = MathHelper.clamp(freePitch + pitchDelta * blend, -90.0F, 90.0F);
      }
   }

   public static float getFreeYaw() {
      return freeYaw;
   }

   public static float getFreePitch() {
      return freePitch;
   }

   public static void setFreeYaw(float freeYaw) {
      FreeLookStorage.freeYaw = freeYaw;
   }

   public static void setFreePitch(float freePitch) {
      FreeLookStorage.freePitch = freePitch;
   }
}
