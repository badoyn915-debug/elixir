package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.function.Supplier;

public class BooleanSetting extends Setting {
   private boolean value;
   private int key = -1;
   private int type = 1;

   public BooleanSetting(String name, String description) {
      super(name, description);
   }

   public BooleanSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public boolean isValue() {
      return this.value;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public BooleanSetting setValue(boolean value) {
      this.value = value;
      return this;
   }

   public BooleanSetting setKey(int key) {
      this.key = key;
      return this;
   }

   public BooleanSetting setType(int type) {
      this.type = type;
      return this;
   }
}
