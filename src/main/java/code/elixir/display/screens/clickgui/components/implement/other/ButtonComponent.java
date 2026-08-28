package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class ButtonComponent extends AbstractComponent {
   private String text;
   private Runnable runnable;
   private int color = -8288257;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.width = Fonts.getSize(12).getStringWidth(this.text) + 13.0F;
      this.height = 12.0F;
      rectangle.render(ShapeProperties.create(matrix, this.x, this.y, this.width, this.height).round(2.0F).color(this.color).build());
      Fonts.getSize(12, Fonts.Type.BOLD).drawCenteredString(matrix, this.text, this.x + this.width / 2.0, this.y + 5.0F, -1);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && button == 0) {
         this.runnable.run();
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public ButtonComponent setText(String text) {
      this.text = text;
      return this;
   }

   public ButtonComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public ButtonComponent setColor(int color) {
      this.color = color;
      return this;
   }
}
