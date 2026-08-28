package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.math.calc.Calculate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;

public class RadioComponent extends AbstractSettingComponent {
   private final SelectSetting selectSetting;
   private final RadioSetting radioSetting;
   private final String name;
   private final SettingDropdownRenderer dropdown = new SettingDropdownRenderer();

   public RadioComponent(SelectSetting setting) {
      super(setting);
      this.selectSetting = setting;
      this.radioSetting = null;
      this.name = setting.getName();
      this.height = 41.0F;
   }

   public RadioComponent(RadioSetting setting) {
      super(setting);
      this.selectSetting = null;
      this.radioSetting = setting;
      this.name = setting.getName();
      this.height = 41.0F;
   }

   @Override
   public void prepareLayout(float width) {
      this.width = width;
      this.height = this.dropdown.computeHeight(this.getValues().size());
   }

   @Override
   public void collapseDropdown() {
      this.dropdown.close();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      List<String> values = this.getValues();
      String label = this.buildLabel(values);
      String panelText = this.getSelected();
      if (panelText == null || panelText.isEmpty()) {
         panelText = values.isEmpty() ? "—" : values.getFirst();
      }

      this.dropdown.render(context, this.x, this.y, this.width, label, panelText, values, this::isSelectedValue, mouseX, mouseY);
      this.height = this.dropdown.computeHeight(values.size());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      List<String> values = this.getValues();
      return this.dropdown.handleClick(mouseX, mouseY, button, this.x, this.y, this.width, values, value -> {
         this.setSelected(value);
         this.dropdown.close();
         return true;
      });
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   private String buildLabel(List<String> values) {
      return this.selectSetting != null && !values.isEmpty()
         ? this.name + String.format(Locale.ROOT, "  %d/%d", this.getSelectedCount(), values.size())
         : this.name;
   }

   private boolean isSelectedValue(String value) {
      return value.equals(this.getSelected());
   }

   private List<String> getValues() {
      if (this.selectSetting != null) {
         return this.selectSetting.getList() == null ? Collections.emptyList() : this.selectSetting.getList();
      } else {
         return this.radioSetting != null ? Arrays.asList(this.radioSetting.getOptions()) : Collections.emptyList();
      }
   }

   private String getSelected() {
      if (this.selectSetting != null) {
         return this.selectSetting.getSelected();
      } else {
         return this.radioSetting != null ? this.radioSetting.get() : "";
      }
   }

   private void setSelected(String value) {
      if (this.selectSetting != null) {
         this.selectSetting.setSelected(value);
      } else if (this.radioSetting != null) {
         this.radioSetting.set(value);
      }
   }

   private int getSelectedCount() {
      return this.getValues().isEmpty() ? 0 : 1;
   }
}
