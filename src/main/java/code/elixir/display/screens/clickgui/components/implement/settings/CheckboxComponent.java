package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.other.ToggleSwitchRenderer;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class CheckboxComponent extends AbstractSettingComponent {
   private final BooleanSetting setting;
   private float hoverAnimation;

   public CheckboxComponent(BooleanSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 21.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.hoverAnimation = MathHelper.lerp(0.2F, this.hoverAnimation, this.isHover(mouseX, mouseY) ? 1.0F : 0.0F);
      int textColor = ColorAssist.applyOpacity(
         ThemeManager.textColor.getColor(), (int)(255.0F * (0.72F + 0.28F * (this.setting.isValue() ? 1.0F : 0.0F) + 0.18F * this.hoverAnimation))
      );
      FontRenderer labelFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      ToggleSwitchRenderer.drawLabel(labelFont, context.getMatrices(), this.setting.getName(), this.x + 10.0F, this.y, this.height, textColor);
      float toggleX = this.x + this.width - 22.0F - 10.0F;
      float toggleY = ToggleSwitchRenderer.alignedControlY(labelFont, this.setting.getName(), this.y, this.height, 10.0F);
      ToggleSwitchRenderer.renderDefault(context, toggleX, toggleY, this.setting.isValue(), this.setting);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      FontRenderer labelFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      float toggleX = this.x + this.width - 22.0F - 10.0F;
      float toggleY = ToggleSwitchRenderer.alignedControlY(labelFont, this.setting.getName(), this.y, this.height, 10.0F);
      if (button == 0 && ToggleSwitchRenderer.isDefaultHovered(mouseX, mouseY, toggleX, toggleY)) {
         this.setting.setValue(!this.setting.isValue());
         return true;
      } else if (button == 0 && Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         this.setting.setValue(!this.setting.isValue());
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
