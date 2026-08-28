package code.elixir.features.impl.render;

import code.elixir.events.keyboard.HotBarScrollEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.render.CameraEvent;
import code.elixir.events.render.FovEvent;
import code.elixir.features.impl.player.FreeLook;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.util.math.MathHelper;

public class CameraSettings extends Module {
   private float fov = 110.0F;
   private float smoothFov = 30.0F;
   private float lastChangedFov = 30.0F;
   static BooleanSetting clipSetting = new BooleanSetting("Сквозь блоки", "Камера проходит сквозь блоки").setValue(true);
   private SliderSettings distanceSetting = new SliderSettings("Дистанция камеры", "Настройка расстояния камеры").setValue(3.0F).range(2.0F, 15.0F);
   private BindSetting zoomSetting = new BindSetting("Зум", "Клавиша для увеличения камеры");

   public static CameraSettings getInstance() {
      return Instance.get(CameraSettings.class);
   }

   public CameraSettings() {
      super("CameraSettings", "CameraSettings", ModuleCategory.RENDER);
      this.setup(new Setting[]{clipSetting, this.zoomSetting, this.distanceSetting});
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.zoomSetting.getKey())) {
         this.fov = Math.min(this.lastChangedFov, (float)((Integer)mc.options.getFov().getValue() - 20));
      }

      if (e.isKeyReleased(this.zoomSetting.getKey(), true)) {
         this.lastChangedFov = this.fov;
         this.fov = ((Integer)mc.options.getFov().getValue()).intValue();
      }
   }

   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent e) {
      if (PlayerInteractionHelper.isKey(this.zoomSetting)) {
         this.fov = (int)MathHelper.clamp(this.fov - e.getVertical() * 10.0, 10.0, ((Integer)mc.options.getFov().getValue()).intValue());
         e.cancel();
      }
   }

   @EventHandler
   public void onFov(FovEvent e) {
      this.smoothFov = Calculate.interpolateSmooth(1.6, this.smoothFov, this.fov);
      e.setFov((int)MathHelper.clamp(this.smoothFov + 1.0F, 10.0F, ((Integer)mc.options.getFov().getValue()).intValue()));
      e.cancel();
   }

   @EventHandler
   public void onCamera(CameraEvent e) {
      e.setCameraClip(clipSetting.isValue());
      e.setDistance(this.distanceSetting.getValue());
      FreeLook freeLook = Instance.get(FreeLook.class);
      if (!freeLook.isState() || !PlayerInteractionHelper.isKey(FreeLook.freeLookSetting)) {
         e.setAngle(MathAngle.cameraAngle());
      }

      e.cancel();
   }
}
