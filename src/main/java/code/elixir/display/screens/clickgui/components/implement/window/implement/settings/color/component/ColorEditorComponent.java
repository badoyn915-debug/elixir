package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ColorEditorComponent extends AbstractComponent {
   private final ColorSetting setting;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      int displayValue = (int)(this.setting.getAlpha() * 100.0F);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (Calculate.isHovered(mouseX, mouseY, this.x + 122.0F, this.y + 90.5F, 22.0, 14.0)) {
         this.setting.setAlpha(MathHelper.clamp((float)(this.setting.getAlpha() - amount * 2.0 / 100.0), 0.0F, 1.0F));
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public ColorEditorComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
