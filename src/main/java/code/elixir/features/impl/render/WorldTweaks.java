package code.elixir.features.impl.render;

import code.elixir.events.render.FogEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import java.awt.Color;

public class WorldTweaks extends Module {
   public final MultiSelectSetting modeSetting = new MultiSelectSetting("Настройки мира", "Позволяет настроить мир").value("Время", "Туман");
   public final SliderSettings timeSetting = new SliderSettings("Время", "Устанавливает значение времени")
      .setValue(12.0F)
      .range(0, 24)
      .visible(() -> this.modeSetting.isSelected("Время"));
   public final SliderSettings distanceSetting = new SliderSettings("Дистанция тумана", "Устанавливает расстояние тумана")
      .setValue(100.0F)
      .range(20, 200)
      .visible(() -> this.modeSetting.isSelected("Туман"));
   public final SliderSettings densitySetting = new SliderSettings("Плотность", "Устанавливает плотность тумана")
      .setValue(98.0F)
      .range(0, 100)
      .visible(() -> this.modeSetting.isSelected("Туман"));
   public final ColorSetting fogColor = new ColorSetting("Цвет тумана", "Цвет тумана")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB())
      .visible(() -> this.modeSetting.isSelected("Туман"));

   public static WorldTweaks getInstance() {
      return Instance.get(WorldTweaks.class);
   }

   public WorldTweaks() {
      super("WorldTweaks", "WorldTweaks", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.modeSetting, this.timeSetting, this.distanceSetting, this.densitySetting, this.fogColor});
   }

   @EventHandler
   public void onFog(FogEvent e) {
      if (this.modeSetting.isSelected("Туман")) {
         e.setDistance(this.distanceSetting.getValue());
         e.setDensity(this.densitySetting.getValue());
         e.setColor(this.fogColor.getColor());
         e.cancel();
      }
   }
}
