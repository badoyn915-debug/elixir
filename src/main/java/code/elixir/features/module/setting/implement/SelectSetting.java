package code.elixir.features.module.setting.implement;

import code.elixir.features.module.setting.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class SelectSetting extends Setting {
   private String selected;
   private List<String> list;

   public SelectSetting(String name, String description) {
      super(name, description);
   }

   public SelectSetting value(String... values) {
      this.list = Arrays.asList(values);
      this.selected = this.list.isEmpty() ? "" : this.list.get(0);
      return this;
   }

   public SelectSetting values(List<String> values) {
      this.list = new ArrayList<>(values);
      if (this.selected == null || !this.list.contains(this.selected)) {
         this.selected = this.list.isEmpty() ? "" : this.list.get(0);
      }

      return this;
   }

   public SelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public SelectSetting selected(String string) {
      if (this.list.contains(string)) {
         this.selected = string;
      }

      return this;
   }

   public boolean isSelected(String name) {
      return this.selected.equals(name);
   }

   public String getSelected() {
      return this.selected;
   }

   public List<String> getList() {
      return this.list;
   }

   public void setSelected(String selected) {
      this.selected = selected;
   }
}
