package code.elixir.features.impl.render;

import code.elixir.events.render.AspectRatioEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;

public class AspectRatio extends Module {
   private final SliderSettings ratioSetting = new SliderSettings("Соотношение", "Настройка значения соотношения сторон").setValue(1.0F).range(0.1F, 2.0F);

   public AspectRatio() {
      super("AspectRatio", "AspectRatio", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.ratioSetting});
   }

   @EventHandler
   public void onAspectRatio(AspectRatioEvent e) {
      e.setRatio(this.ratioSetting.getValue());
      e.cancel();
   }
}
