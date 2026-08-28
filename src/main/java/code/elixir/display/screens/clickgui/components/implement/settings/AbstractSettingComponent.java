package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.Module;
import code.elixir.features.module.setting.Setting;

public abstract class AbstractSettingComponent extends AbstractComponent {
   public static final int MODULE_TITLE_FONT_SIZE = 16;
   public static final int HEADER_FONT_SIZE = 14;
   public static final int BODY_FONT_SIZE = 12;
   public static final int VALUE_FONT_SIZE = 11;
   private final Setting setting;
   protected Module module;

   protected AbstractSettingComponent(Setting setting) {
      this.setting = setting;
   }

   public void prepareLayout(float width) {
      this.width = width;
   }

   public void collapseDropdown() {
   }

   public Setting getSetting() {
      return this.setting;
   }

   public Module getModule() {
      return this.module;
   }

   public void setModule(Module module) {
      this.module = module;
   }
}
