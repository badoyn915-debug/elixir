package code.elixir.features.module.setting;

import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.BindComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.CheckboxComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.ColorComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.GroupComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.RadioComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.SButtonComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.SectionComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.SwordModelPickerComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.TextComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.multiselect.MultiSelectComponent;
import code.elixir.features.impl.render.ItemReplacer;
import code.elixir.features.module.Module;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ButtonSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.GroupSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SectionSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.features.module.setting.implement.TextSetting;
import java.util.List;

public class SettingComponentAdder {
   public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components, Module module) {
      settings.forEach(
         setting -> {
            AbstractSettingComponent component = null;
            if (setting instanceof BooleanSetting booleanSetting) {
               component = new CheckboxComponent(booleanSetting);
            }

            if (setting instanceof BindSetting bindSetting) {
               component = new BindComponent(bindSetting);
            }

            if (setting instanceof ColorSetting colorSetting) {
               component = new ColorComponent(colorSetting);
            }

            if (setting instanceof TextSetting textSetting) {
               component = new TextComponent(textSetting);
            }

            if (setting instanceof SliderSettings valueSetting) {
               component = new SliderComponent(valueSetting);
            }

            if (setting instanceof GroupSetting groupSetting) {
               component = new GroupComponent(groupSetting);
            }

            if (setting instanceof ButtonSetting buttonSetting) {
               component = new SButtonComponent(buttonSetting);
            }

            if (setting instanceof SelectSetting selectSetting) {
               component = (AbstractSettingComponent)(module instanceof ItemReplacer && selectSetting.getName().startsWith("Модель")
                  ? new SwordModelPickerComponent(selectSetting)
                  : new RadioComponent(selectSetting));
            }

            if (setting instanceof MultiSelectSetting multiSelectSetting) {
               component = new MultiSelectComponent(multiSelectSetting);
            }

            if (setting instanceof RadioSetting radioSetting) {
               component = new RadioComponent(radioSetting);
            }

            if (setting instanceof SectionSetting sectionSetting) {
               component = new SectionComponent(sectionSetting);
            }

            if (component != null) {
               component.setModule(module);
               components.add(component);
            }
         }
      );
   }
}
