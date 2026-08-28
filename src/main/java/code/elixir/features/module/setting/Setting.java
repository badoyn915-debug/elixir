package code.elixir.features.module.setting;

import java.util.function.Supplier;

public class Setting {
   private final String name;
   private String description;
   private Supplier<Boolean> visible;

   public Setting(String name) {
      this.name = name;
   }

   public Setting(String name, String description) {
      this.name = name;
      this.description = description;
   }

   public boolean isVisible() {
      return this.visible == null || this.visible.get();
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Supplier<Boolean> getVisible() {
      return this.visible;
   }

   public void setVisible(Supplier<Boolean> visible) {
      this.visible = visible;
   }
}
