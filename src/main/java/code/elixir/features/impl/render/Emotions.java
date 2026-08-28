package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;

public class Emotions extends Module {
   private final BindSetting wheelBind = new BindSetting("Открыть колесо", "Клавиша открытия колеса эмоций").setKey(86);

   public Emotions() {
      super("Emotions", "Emotions", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.wheelBind});
      this.setState(true);
   }

   public BindSetting getWheelBind() {
      return this.wheelBind;
   }
}
