package code.elixir.display.screens.clickgui.components.implement.window;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;

public abstract class AbstractWindow extends AbstractComponent {
   protected boolean dragging;
   private boolean draggable;
   protected int dragX;
   protected int dragY;
   private final Animation scaleAnimation = new Decelerate().setValue(1.0).setMs(100);

   public AbstractWindow() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public AbstractWindow draggable(boolean draggable) {
      this.draggable = draggable;
      return this;
   }

   public AbstractWindow size(float width, float height) {
      this.width = width;
      this.height = height;
      return this;
   }

   public AbstractWindow position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isHovered(mouseX, mouseY) && button == 0 && this.draggable) {
         this.dragging = true;
         this.dragX = (int)(this.x - mouseX);
         this.dragY = (int)(this.y - mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.dragging && this.draggable) {
         this.x = mouseX + this.dragX;
         this.y = mouseY + this.dragY;
      }

      float scale = this.scaleAnimation.getOutput().floatValue();
      Calculate.scale(
         context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, scale, () -> this.drawWindow(context, mouseX, mouseY, delta)
      );
   }

   protected abstract void drawWindow(DrawContext var1, int var2, int var3, float var4);

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return true;
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public void startCloseAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   public boolean isCloseAnimationFinished() {
      return this.scaleAnimation.isFinished(Direction.BACKWARDS);
   }

   public Animation getScaleAnimation() {
      return this.scaleAnimation;
   }
}
