package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.entity.effect.StatusEffects;

public class LevitationControl extends Module {
   SliderSettings up = new SliderSettings("Скорость вверх", "").setValue(0.5F).range(0, 2);
   SliderSettings down = new SliderSettings("Скорость вниз", "").setValue(0.5F).range(0, 2);
   BooleanSetting stuck = new BooleanSetting("Зависать", "Стопит вас на месте");

   public LevitationControl() {
      super("LevitationControl", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.up, this.down, this.stuck});
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            switch (0) {
               default:
                  double test1214 = mc.options.jumpKey.isPressed()
                     ? this.up.getValue()
                     : (mc.options.sneakKey.isPressed() ? -this.down.getValue() : (this.stuck.isValue() ? 0.0 : mc.player.getVelocity().y));
                  if (Math.abs(mc.player.getVelocity().y - test1214) > 0.001) {
                     mc.player.setVelocity(mc.player.getVelocity().x, test1214, mc.player.getVelocity().z);
                  }
            }
         }
      }
   }
}
