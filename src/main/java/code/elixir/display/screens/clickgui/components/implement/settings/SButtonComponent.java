package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.features.module.setting.implement.ButtonSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

public class SButtonComponent extends AbstractSettingComponent {
   private final ButtonSetting setting;

   public SButtonComponent(ButtonSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 20.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      boolean hovered = Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + 10.0F, this.y + 2.0F, Math.max(1.0F, this.width - 20.0F), 16.0)
            .round(4.0F)
            .color(ColorAssist.applyOpacity(ThemeManager.BackgroundGui.getColor(), hovered ? 110 : 85))
            .build()
      );
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_SEMIBOLD)
         .drawCenteredString(context.getMatrices(), this.setting.getName(), this.x + this.width / 2.0F, this.y + 10.0F, ThemeManager.textColor.getColor());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         if (this.setting.getRunnable() != null) {
            this.setting.getRunnable().run();
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
