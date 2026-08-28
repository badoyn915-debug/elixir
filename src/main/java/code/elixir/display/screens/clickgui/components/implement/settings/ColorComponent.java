package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.other.ToggleSwitchRenderer;
import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class ColorComponent extends AbstractSettingComponent {
   private final ColorSetting setting;
   private float hoverAnimation;

   public ColorComponent(ColorSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 21.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.hoverAnimation = MathHelper.lerp(0.2F, this.hoverAnimation, this.isHover(mouseX, mouseY) ? 1.0F : 0.0F);
      float leftPadding = 10.0F;
      float checkSize = 8.0F;
      int textColor = ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)(255.0F * (0.75F + 0.25F * this.hoverAnimation)));
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(
            context.getMatrices(),
            this.setting.getName(),
            this.x + leftPadding,
            ToggleSwitchRenderer.labelDrawY(Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM), this.setting.getName(), this.y, this.height),
            textColor
         );
      float outlineX = this.x + this.width - leftPadding - checkSize;
      float outlineY = this.y + (this.height - checkSize) / 2.0F;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), outlineX, outlineY, checkSize, checkSize).round(4.5F).color(ColorAssist.getOutline()).build()
      );
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), outlineX + 1.0F, outlineY + 1.0F, 6.0, 6.0).round(4.5F).color(this.setting.getColor()).build()
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isHover(mouseX, mouseY)) {
         this.openColorWindow((int)mouseX, (int)mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   private void openColorWindow(int mouseX, int mouseY) {
      AbstractWindow existing = null;

      for (AbstractWindow win : windowManager.getWindows()) {
         ColorWindow colorWindow;
         if (win instanceof ColorWindow && (colorWindow = (ColorWindow)win).getSetting() == this.setting) {
            existing = win;
            break;
         }
      }

      if (existing != null) {
         existing.startCloseAnimation();
      } else {
         windowManager.add(new ColorWindow(this.setting).position(mouseX - 90, mouseY - 10).size(100.0F, 155.0F).draggable(true));
      }
   }
}
