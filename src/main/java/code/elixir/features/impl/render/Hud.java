package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import java.util.ArrayList;
import java.util.List;

public class Hud extends Module {
   public static BooleanSetting blur = new BooleanSetting("Блюр", "Выберите состояние блюра").setValue(true);
   public static BooleanSetting shine = new BooleanSetting("Блик", "Анимированная полоска блика на элементах интерфейса").setValue(true);
   public static SliderSettings shineSize = new SliderSettings("Размер блика", "Ширина полоски блика")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(() -> shine.isValue());
   public static SliderSettings shineSpeed = new SliderSettings("Скорость блика", "Скорость движения блика")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(() -> shine.isValue());
   public static SliderSettings shineBrightness = new SliderSettings("Яркость блика", "Яркость свечения блика")
      .setValue(0.45F)
      .range(0.1F, 1.0F)
      .step(0.05F)
      .visible(() -> shine.isValue());
   public static MultiSelectSetting shineElements = new MultiSelectSetting("Блик на элементах", "Элементы, на которых будет отображаться блик")
      .value(
         "Watermark",
         "Target Hud",
         "Potions",
         "Cooldowns",
         "Notifications",
         "Keybinds",
         "StaffList",
         "Info",
         "ArmorBar",
         "TotemBar",
         "CustomHotbar",
         "Inventory"
      )
      .selected(
         "Watermark",
         "Target Hud",
         "Potions",
         "Cooldowns",
         "Notifications",
         "Keybinds",
         "StaffList",
         "Info",
         "ArmorBar",
         "TotemBar",
         "CustomHotbar",
         "Inventory"
      )
      .visible(() -> shine.isValue());
   public static SliderSettings setAlpha = new SliderSettings("Set Alpha", "Прозрачность элементов интерфейса").setValue(1.0F).range(0.1F, 1.0F).step(0.05F);
   public static MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
      .value(
         "Watermark",
         "Target Hud",
         "Potions",
         "Cooldowns",
         "Notifications",
         "Keybinds",
         "StaffList",
         "Info",
         "ArmorBar",
         "TotemBar",
         "CustomHotbar",
         "Inventory"
      )
      .selected("Watermark", "Target Hud", "Potions", "Notifications", "Info");
   public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомлять об", "Выберите, когда будут появляться уведомления")
      .value(
         "Переключение модулей",
         "Входе админа",
         "Выходе админа",
         "Поднятии предмета",
         "Ломании щита",
         "Полученном зелье",
         "Заканчивающемся зелье",
         "Использовании предмета",
         "Сносе тотема",
         "Скорой поломке брони",
         "Скором открытии сундука"
      )
      .selected(
         "Переключение модулей",
         "Поднятии предмета",
         "Ломании щита",
         "Использовании предмета",
         "Сносе тотема",
         "Скорой поломке брони",
         "Скором открытии сундука"
      )
      .visible(() -> interfaceSettings.isSelected("Notifications"));

   public static Hud getInstance() {
      return Instance.get(Hud.class);
   }

   public Hud() {
      super("Hud", ModuleCategory.RENDER);
      this.setup(new Setting[]{interfaceSettings, this.notificationSettings, blur, shine, shineSize, shineSpeed, shineBrightness, shineElements, setAlpha});
      this.setState(true);
   }

   @Override
   public void activate() {
      if (!this.isState()) {
         this.setState(true);
      }
   }

   @Override
   public void deactivate() {
      this.setState(true);
   }

   public boolean isHudElementEnabled(String elementName) {
      return interfaceSettings.getList() != null && interfaceSettings.getList().contains(elementName) && interfaceSettings.isSelected(elementName);
   }

   public void setHudElementEnabled(String elementName, boolean enabled) {
      List<String> values = interfaceSettings.getList();
      if (values != null && values.contains(elementName)) {
         ArrayList<String> selectedValues = new ArrayList<>(interfaceSettings.getSelected());
         if (enabled) {
            if (!selectedValues.contains(elementName)) {
               selectedValues.add(elementName);
            }
         } else {
            selectedValues.remove(elementName);
         }

         interfaceSettings.setSelected(selectedValues);
      }
   }
}
