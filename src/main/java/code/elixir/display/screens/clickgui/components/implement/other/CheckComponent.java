package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.animation.AnimationHelper;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class CheckComponent extends AbstractComponent {
   private boolean state;
   private Runnable runnable;
   private boolean moduleEnabled = true;

   public CheckComponent position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      String toggleAnimationKey = "checkbox_toggle_" + System.identityHashCode(this);
      float scale = AnimationHelper.getAnimationValue(toggleAnimationKey, 1.0F);
      if (this.state) {
         if (this.moduleEnabled) {
            rectangle.render(
               ShapeProperties.create(matrix, this.x, this.y, 7.0F * scale, 7.0F * scale)
                  .round(1.5F)
                  .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                  .build()
            );
         } else {
            rectangle.render(
               ShapeProperties.create(matrix, this.x, this.y, 7.0F * scale, 7.0F * scale).round(1.5F).color(new Color(57, 57, 60, 255).getRGB()).build()
            );
         }
      } else {
         rectangle.render(
            ShapeProperties.create(matrix, this.x, this.y, 7.0F * scale, 7.0F * scale)
               .round(1.5F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(new Color(57, 57, 60, 255).getRGB())
               .color(new Color(32, 32, 35, 40).getRGB())
               .build()
         );
      }

      if (this.state && this.moduleEnabled) {
         Fonts.getSize(13, Fonts.Type.ELIXIR_ICONS).drawString(matrix, "k", this.x + 0.2F, this.y + 2.9F, ThemeManager.textColor.getColor());
      } else if (this.state && !this.moduleEnabled) {
         Fonts.getSize(13, Fonts.Type.ELIXIR_ICONS).drawString(matrix, "k", this.x + 0.2F, this.y + 2.9F, new Color(137, 137, 140, 255).getRGB());
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, 6.0, 6.0) && button == 0) {
         String toggleAnimationKey = "checkbox_toggle_" + System.identityHashCode(this);
         AnimationHelper.startAnimation(toggleAnimationKey, 1.0F, 1.2F, 100L, AnimationHelper.EasingType.EASE_OUT);
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public CheckComponent setState(boolean state) {
      this.state = state;
      return this;
   }

   public CheckComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public CheckComponent setModuleEnabled(boolean moduleEnabled) {
      this.moduleEnabled = moduleEnabled;
      return this;
   }
}
