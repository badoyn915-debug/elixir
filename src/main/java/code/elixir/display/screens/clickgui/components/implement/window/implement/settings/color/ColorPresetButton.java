package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;

public class ColorPresetButton extends AbstractComponent {
   private final ColorSetting setting;
   private final int color;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      rectangle.render(ShapeProperties.create(context.getMatrices(), this.x, this.y, 8.0, 8.0).round(2.0F).color(this.color).build());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, 8.0, 8.0) && button == 0) {
         this.setting.setColor(this.color);
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public ColorPresetButton(ColorSetting setting, int color) {
      this.setting = setting;
      this.color = color;
   }
}
