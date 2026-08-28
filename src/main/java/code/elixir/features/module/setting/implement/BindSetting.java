package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.function.Supplier;

public class BindSetting extends Setting {
   private int key = -1;
   private int secondKey = -1;
   private int type = 1;

   public BindSetting(String name, String description) {
      super(name, description);
   }

   public BindSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public boolean isCombo() {
      return this.secondKey != -1 && this.secondKey != -1;
   }

   public void setCombo(int firstKey, int secondKey) {
      this.key = firstKey;
      this.secondKey = secondKey;
   }

   public void clearSecondKey() {
      this.secondKey = -1;
   }

   public int getKey() {
      return this.key;
   }

   public int getSecondKey() {
      return this.secondKey;
   }

   public int getType() {
      return this.type;
   }

   public BindSetting setKey(int key) {
      this.key = key;
      return this;
   }

   public BindSetting setSecondKey(int secondKey) {
      this.secondKey = secondKey;
      return this;
   }

   public BindSetting setType(int type) {
      this.type = type;
      return this;
   }
}
