package code.elixir.display.screens.clickgui.components.implement.settings.multiselect;

import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.SettingDropdownRenderer;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.math.calc.Calculate;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class MultiSelectComponent extends AbstractSettingComponent {
   private final MultiSelectSetting setting;
   private final SettingDropdownRenderer dropdown = new SettingDropdownRenderer();

   public MultiSelectComponent(MultiSelectSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 41.0F;
   }

   @Override
   public void prepareLayout(float width) {
      this.width = width;
      this.height = this.dropdown.computeHeight(this.setting.getList().size());
   }

   @Override
   public void collapseDropdown() {
      this.dropdown.close();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      List<String> values = this.setting.getList();
      String label = this.setting.getName() + " " + this.setting.getSelected().size() + "/" + values.size();
      String panelText = this.buildPanelText();
      this.dropdown.render(context, this.x, this.y, this.width, label, panelText, values, this.setting.getSelected()::contains, mouseX, mouseY);
      this.height = this.dropdown.computeHeight(values.size());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.dropdown.handleClick(mouseX, mouseY, button, this.x, this.y, this.width, this.setting.getList(), value -> {
         if (this.setting.getSelected().contains(value)) {
            this.setting.getSelected().remove(value);
         } else {
            this.setting.getSelected().add(value);
         }

         return true;
      });
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   private String buildPanelText() {
      List<String> selected = this.setting.getSelected();
      if (selected.isEmpty()) {
         return "Не выбрано";
      } else {
         String joined = String.join(", ", selected);
         float maxWidth = this.width - 50.0F;
         FontRenderer font = Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM);
         if (font.getStringWidth(joined) <= maxWidth) {
            return joined;
         } else {
            String ellipsis = "...";

            while (joined.length() > 1 && font.getStringWidth(joined + ellipsis) > maxWidth) {
               joined = joined.substring(0, joined.length() - 1).trim();
            }

            return joined.isEmpty() ? selected.getFirst() : joined + ellipsis;
         }
      }
   }
}
