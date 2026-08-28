package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class StatusRender extends AbstractComponent {
   private boolean state;
   private Runnable runnable;
   private final Animation alphaAnimation = new Decelerate().setMs(400).setValue(100.0);
   private final Animation stencilAnimation = new Decelerate().setMs(200).setValue(8.0);
   private final Animation sliderAnimation = new Decelerate().setMs(225).setValue(8.0);

   public StatusRender() {
      this.alphaAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.stencilAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.sliderAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.alphaAnimation.reset();
      this.stencilAnimation.reset();
      this.sliderAnimation.reset();
   }

   public StatusRender position(float x, float y) {
      this.x = x - 8.0F;
      this.y = y;
      return this;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.alphaAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.stencilAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.sliderAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      int stateColor = new Color(128, 128, 128, 255).getRGB();
      int opacity = this.alphaAnimation.getOutput().intValue();
      float sliderX = this.x + this.sliderAnimation.getOutput().floatValue();
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, 16.0, 8.0)
            .round(4.0F)
            .thickness(0.0F)
            .softness(0.0F)
            .outlineColor(new Color(128, 128, 128, 255).getRGB())
            .color(new Color(128, 128, 128, 40).getRGB())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, 16.0, 8.0)
            .round(4.0F)
            .thickness(0.0F)
            .softness(0.0F)
            .outlineColor(new Color(128, 128, 128, 255).getRGB())
            .color(Calculate.applyOpacity(stateColor, opacity))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, sliderX - 0.5F, this.y - 0.5F, 9.0, 9.0)
            .round(4.5F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(new Color(155, 155, 165, 255).getRGB())
            .color(
               new Color(61, 67, 71, 255).getRGB(),
               new Color(71, 77, 81, 255).getRGB(),
               new Color(81, 87, 91, 255).getRGB(),
               new Color(91, 97, 101, 255).getRGB()
            )
            .build()
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, 16.0, 8.0) && button == 0) {
         this.state = !this.state;
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public StatusRender setState(boolean state) {
      this.state = state;
      return this;
   }

   public StatusRender setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }
}
