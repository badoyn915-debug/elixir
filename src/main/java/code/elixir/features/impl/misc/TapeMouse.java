package code.elixir.features.impl.misc;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.math.time.TimerUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class TapeMouse extends Module implements QuickImports {
   private final SelectSetting buttonSetting = new SelectSetting("Кнопка", "Выберите кнопку мыши")
      .value("Левая кнопка", "Правая кнопка")
      .selected("Левая кнопка");
   private final SliderSettings delaySetting = new SliderSettings("Задержка (мс)", "Интервал между кликами").range(100, 30000).step(50.0F).setValue(450.0F);
   private final TimerUtil timer = new TimerUtil();

   public TapeMouse() {
      super("TapeMouse", "Tape Mouse", ModuleCategory.MISC);
      this.setup(new Setting[]{this.buttonSetting, this.delaySetting});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.state) {
         if (mc.player != null && mc.world != null && mc.currentScreen == null && mc.interactionManager != null) {
            long delayMs = (long)this.delaySetting.getValue();
            if (this.timer.hasTimeElapsed(delayMs)) {
               if (this.buttonSetting.isSelected("Левая кнопка")) {
                  if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
                     mc.interactionManager.attackEntity(mc.player, entityHit.getEntity());
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.timer.resetCounter();
            }
         }
      }
   }
}
