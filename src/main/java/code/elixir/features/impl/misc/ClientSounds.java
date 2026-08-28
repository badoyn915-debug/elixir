package code.elixir.features.impl.misc;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;

public class ClientSounds extends Module {
   public SliderSettings volumeSetting = new SliderSettings("Громкость", "Громкость звуков клиента").range(0.0F, 1.0F).setValue(1.0F);
   public SliderSettings pitchSetting = new SliderSettings("Тон", "Тональность (pitch) звуков клиента").range(0.5F, 2.0F).setValue(1.0F);

   public static ClientSounds getInstance() {
      return Instance.get(ClientSounds.class);
   }

   public ClientSounds() {
      super("ClientSounds", "ClientSounds", ModuleCategory.MISC);
      this.setup(new Setting[]{this.volumeSetting, this.pitchSetting});
   }

   public float getVolume() {
      return this.volumeSetting.getValue();
   }

   public float getPitch() {
      return this.pitchSetting.getValue();
   }
}
