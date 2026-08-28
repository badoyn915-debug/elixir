package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.function.Supplier;

public class TextSetting extends Setting {
   private String text;
   private int min;
   private int max;

   public TextSetting(String name, String description) {
      super(name, description);
   }

   public TextSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public String getText() {
      return this.text;
   }

   public int getMin() {
      return this.min;
   }

   public int getMax() {
      return this.max;
   }

   public TextSetting setText(String text) {
      this.text = text;
      return this;
   }

   public TextSetting setMin(int min) {
      this.min = min;
      return this;
   }

   public TextSetting setMax(int max) {
      this.max = max;
      return this;
   }
}
