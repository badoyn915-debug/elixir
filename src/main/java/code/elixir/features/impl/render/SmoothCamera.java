package code.elixir.features.impl.render;

import code.elixir.events.render.CameraEvent;
import code.elixir.events.render.CameraPositionEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.Turns;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SmoothCamera extends Module {
   private final SliderSettings speed = new SliderSettings("Smooth", "Скорость следования камеры").range(0.01F, 0.3F).setValue(0.08F);
   private float smoothYaw = 0.0F;
   private float smoothPitch = 0.0F;
   private Vec3d smoothPos = null;
   private boolean initialized = false;

   public static SmoothCamera getInstance() {
      return Instance.get(SmoothCamera.class);
   }

   public SmoothCamera() {
      super("SmoothCamera", "SmoothCamera", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.speed});
   }

   @Override
   public void activate() {
      this.initialized = false;
      this.smoothPos = null;
   }

   @Override
   public void deactivate() {
      this.initialized = false;
      this.smoothPos = null;
   }

   @EventHandler
   public void onCamera(CameraEvent e) {
      Turns target = e.getAngle();
      if (target != null) {
         if (!this.initialized) {
            this.smoothYaw = target.getYaw();
            this.smoothPitch = target.getPitch();
            this.initialized = true;
         } else {
            float factor = this.speed.getValue();
            float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - this.smoothYaw);
            this.smoothYaw += deltaYaw * factor;
            this.smoothPitch = this.smoothPitch + (target.getPitch() - this.smoothPitch) * factor;
            e.setAngle(new Turns(this.smoothYaw, this.smoothPitch));
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onCameraPos(CameraPositionEvent e) {
      Vec3d target = e.getPos();
      if (target != null) {
         if (this.smoothPos == null) {
            this.smoothPos = target;
         } else {
            double factor = this.speed.getValue();
            this.smoothPos = new Vec3d(
               this.smoothPos.x + (target.x - this.smoothPos.x) * factor,
               this.smoothPos.y + (target.y - this.smoothPos.y) * factor,
               this.smoothPos.z + (target.z - this.smoothPos.z) * factor
            );
            e.setPos(this.smoothPos);
         }
      }
   }
}
