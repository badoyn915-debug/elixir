package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.input.MoveUtil;

public class DragonFly extends Module {
   private SliderSettings xzSpeed = new SliderSettings("Скорость по X/Z", "Горизонтальная скорость полёта").range(0.0F, 2.0F).step(0.1F).setValue(1.0F);
   private SliderSettings ySpeed = new SliderSettings("Скорость по Y", "Вертикальная скорость полёта").range(0.0F, 2.0F).step(0.1F).setValue(1.0F);

   public DragonFly() {
      super("DragonFly", "DragonFly", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.xzSpeed, this.ySpeed});
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.player.getAbilities().flying) {
         MoveUtil.setMotion(this.xzSpeed.getValue());
         if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, this.ySpeed.getValue(), mc.player.getVelocity().z);
         }

         if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, -this.ySpeed.getValue(), mc.player.getVelocity().z);
         }
      }
   }
}
