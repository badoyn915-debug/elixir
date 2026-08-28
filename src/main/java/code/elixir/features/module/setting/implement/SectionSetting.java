package code.elixir.features.module.setting.implement;

import code.elixir.features.module.Module;
import code.elixir.features.module.setting.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class SectionSetting extends Setting {
   private final Module boundModule;
   private final List<Setting> subSettings = new ArrayList<>();

   public SectionSetting(String name, Module boundModule) {
      super(name);
      this.boundModule = boundModule;
      if (boundModule != null) {
         this.subSettings.addAll(boundModule.settings());
      }
   }

   public SectionSetting settings(Setting... setting) {
      this.subSettings.addAll(Arrays.asList(setting));
      return this;
   }

   public boolean isOn() {
      return this.boundModule != null && this.boundModule.isState();
   }

   public void toggle() {
      if (this.boundModule != null) {
         this.boundModule.switchState();
      }
   }

   public void setOn(boolean on) {
      if (this.boundModule != null && this.boundModule.isState() != on) {
         this.boundModule.switchState();
      }
   }

   public SectionSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public Module getBoundModule() {
      return this.boundModule;
   }

   public List<Setting> getSubSettings() {
      return this.subSettings;
   }
}
