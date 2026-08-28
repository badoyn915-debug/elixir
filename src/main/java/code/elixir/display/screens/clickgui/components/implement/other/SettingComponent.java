package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class SettingComponent extends AbstractComponent {
   private Runnable runnable;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Fonts.getSize(15, Fonts.Type.GUIICONS).drawString(context.getMatrices(), "B", this.x - 5.0F, this.y + 6.0F, new Color(128, 128, 128, 255).getRGB());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x - 5.0F, this.y + 6.0F, 7.0, 7.0) && button == 0) {
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public SettingComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }
}
