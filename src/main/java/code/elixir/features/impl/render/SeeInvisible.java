package code.elixir.features.impl.render;

import code.elixir.events.render.EntityColorEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;

public class SeeInvisible extends Module {
   private final SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока").setValue(0.5F).range(0.1F, 1.0F);

   public SeeInvisible() {
      super("SeeInvisible", "SeeInvisible", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.alphaSetting});
   }

   @EventHandler
   public void onEntityColor(EntityColorEvent e) {
      e.setColor(ColorAssist.multAlpha(e.getColor(), this.alphaSetting.getValue()));
      e.cancel();
   }
}
