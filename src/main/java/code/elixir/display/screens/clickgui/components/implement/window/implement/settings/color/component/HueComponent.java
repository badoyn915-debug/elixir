package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class HueComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean hueDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 18.5F;
      this.W = 88.0F;
      this.H = 50.0F;
      int[] color = new int[]{-16777216, -1, -16777216, Color.HSBtoRGB(this.setting.getHue(), 1.0F, 1.0F)};
      rectangle.render(ShapeProperties.create(matrix, this.X, this.Y, this.W, this.H).round(2.0F).color(color).build());
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getSaturation(), this.X, this.X + this.W - 5.0F);
      float clampedY = MathHelper.clamp(this.Y + this.H * (1.0F - this.setting.getBrightness()), this.Y, this.Y + this.H - 5.0F);
      rectangle.render(
         ShapeProperties.create(matrix, clampedX, clampedY, 5.0, 5.0).round(2.5F).softness(1.0F).thickness(3.0F).color(16777215).outlineColor(-1).build()
      );
      float min = MathHelper.clamp((mouseX - this.X) / this.W, 0.0F, 1.0F);
      if (this.hueDragging) {
         this.setting.setBrightness(MathHelper.clamp(1.0F - (mouseY - this.Y) / this.H, 0.0F, 1.0F));
         this.setting.setSaturation(min);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean bl = this.hueDragging = button == 0 && Calculate.isHovered(mouseX, mouseY, this.X, this.Y, this.W, this.H);
      if (this.hueDragging) {
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.hueDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public HueComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
