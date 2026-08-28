package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class SaturationComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean saturationDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 73.5F;
      this.W = 88.0F;
      this.H = 4.0F;
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getHue(), this.X, this.X + this.W - 4.0F);
      float min = MathHelper.clamp((mouseX - this.X) / this.W, 0.0F, 1.0F);
      image.setTexture("textures/hue.png").render(ShapeProperties.create(matrix, this.X, this.Y + 0.5, this.W, this.H - 1.0F).build());
      rectangle.render(
         ShapeProperties.create(matrix, clampedX, this.Y, this.H, this.H).round(this.H / 2.0F).thickness(3.0F).color(16777215).outlineColor(-1).build()
      );
      if (this.saturationDragging) {
         this.setting.setHue(min);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean bl = this.saturationDragging = button == 0 && Calculate.isHovered(mouseX, mouseY, this.X, this.Y, this.W, this.H);
      if (this.saturationDragging) {
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.saturationDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public SaturationComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
