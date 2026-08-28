package code.elixir.features.impl.player;

import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.keyboard.MouseRotationEvent;
import code.elixir.events.render.CameraEvent;
import code.elixir.events.render.FovEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class FreeLook extends Module {
   private Perspective perspective;
   private Turns angle;
   public static BindSetting freeLookSetting = new BindSetting("Свободный обзор", "Клавиша для свободного обзора");

   public FreeLook() {
      super("FreeLook", "FreeLook", ModuleCategory.RENDER);
      this.setup(new Setting[]{freeLookSetting});
      this.angle = null;
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(freeLookSetting.getKey())) {
         this.perspective = mc.options.getPerspective();
         if (this.angle == null) {
            this.angle = MathAngle.cameraAngle();
         }
      }
   }

   @EventHandler
   public void onFov(FovEvent e) {
      if (PlayerInteractionHelper.isKey(freeLookSetting)) {
         if (mc.options.getPerspective().isFirstPerson()) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         }

         if (this.angle == null) {
            this.angle = MathAngle.cameraAngle();
         }
      } else if (this.perspective != null) {
         mc.options.setPerspective(this.perspective);
         this.perspective = null;
         this.angle = null;
      }
   }

   @EventHandler
   public void onMouseRotation(MouseRotationEvent e) {
      if (PlayerInteractionHelper.isKey(freeLookSetting)) {
         if (this.angle == null) {
            this.angle = MathAngle.cameraAngle();
         }

         this.angle.setYaw(this.angle.getYaw() + e.getCursorDeltaX() * 0.15F);
         this.angle.setPitch(MathHelper.clamp(this.angle.getPitch() + e.getCursorDeltaY() * 0.15F, -90.0F, 90.0F));
         e.cancel();
      } else {
         this.angle = null;
      }
   }

   @EventHandler
   public void onCamera(CameraEvent e) {
      if (PlayerInteractionHelper.isKey(freeLookSetting) && this.angle != null) {
         e.setAngle(this.angle);
         e.cancel();
      }
   }
}
