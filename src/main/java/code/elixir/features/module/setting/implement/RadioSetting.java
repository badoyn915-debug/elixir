package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.function.Supplier;

public class RadioSetting extends Setting {
   private String value;
   private final String[] options;

   public RadioSetting(String name, String description, String[] options) {
      super(name, description);
      this.options = options;
      this.value = options.length > 0 ? options[0] : "";
   }

   public RadioSetting(String name, String description, String[] options, String defaultValue) {
      super(name, description);
      this.options = options;
      this.value = defaultValue;
   }

   public void set(String value) {
      this.value = value;
   }

   public String get() {
      return this.value;
   }

   public RadioSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public String getValue() {
      return this.value;
   }

   public RadioSetting setValue(String value) {
      this.value = value;
      return this;
   }

   public String[] getOptions() {
      return this.options;
   }
}
