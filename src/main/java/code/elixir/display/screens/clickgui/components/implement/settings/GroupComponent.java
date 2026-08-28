package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.group.GroupWindow;
import code.elixir.features.module.setting.implement.GroupSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

public class GroupComponent extends AbstractSettingComponent {
   private final GroupSetting setting;

   public GroupComponent(GroupSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 18.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(context.getMatrices(), this.setting.getName(), this.x + 6.0F, this.y + 10.0F, ThemeManager.textColor.getColor());
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + this.width - 24.0F, this.y + 5.0F, 7.0, 7.0)
            .round(2.0F)
            .color(ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 40))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + this.width - 13.0F, this.y + 5.0F, 7.0, 7.0)
            .round(2.0F)
            .color(this.setting.isValue() ? ColorAssist.getClientColor() : ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 35))
            .build()
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         return false;
      } else if (button == 0) {
         if (Calculate.isHovered(mouseX, mouseY, this.x + this.width - 24.0F, this.y + 5.0F, 7.0, 7.0)) {
            this.openGroupWindow((int)mouseX, (int)mouseY);
         } else {
            this.setting.setValue(!this.setting.isValue());
         }

         return true;
      } else if (button == 1) {
         this.openGroupWindow((int)mouseX, (int)mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   private void openGroupWindow(int mouseX, int mouseY) {
      AbstractWindow existing = null;

      for (AbstractWindow win : windowManager.getWindows()) {
         GroupWindow groupWindow;
         if (win instanceof GroupWindow && (groupWindow = (GroupWindow)win).getSetting() == this.setting) {
            existing = win;
            break;
         }
      }

      if (existing != null) {
         existing.startCloseAnimation();
      } else {
         windowManager.add(new GroupWindow(this.setting).position(mouseX + 4, mouseY).size(137.0F, 23.0F).draggable(false));
      }
   }
}
