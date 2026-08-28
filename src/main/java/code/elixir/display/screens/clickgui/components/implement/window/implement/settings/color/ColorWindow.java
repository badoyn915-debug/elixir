package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component.AlphaComponent;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component.ColorEditorComponent;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component.HueComponent;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component.RGBInputComponent;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component.SaturationComponent;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class ColorWindow extends AbstractWindow {
   private final List<AbstractComponent> components = new ArrayList<>();
   private final ColorSetting setting;
   private int lastColor = -1;
   private final HueComponent hueComponent;
   private final SaturationComponent saturationComponent;
   private final AlphaComponent alphaComponent;
   private final ColorEditorComponent colorEditorComponent;
   private final RGBInputComponent rgbInputComponent;

   public ColorWindow(ColorSetting setting) {
      this.setting = setting;
      this.lastColor = setting.getColor();
      AbstractComponent[] abstractComponentArray = new AbstractComponent[5];
      this.hueComponent = new HueComponent(setting);
      abstractComponentArray[0] = this.hueComponent;
      this.saturationComponent = new SaturationComponent(setting);
      abstractComponentArray[1] = this.saturationComponent;
      this.alphaComponent = new AlphaComponent(setting);
      abstractComponentArray[2] = this.alphaComponent;
      this.colorEditorComponent = new ColorEditorComponent(setting);
      abstractComponentArray[3] = this.colorEditorComponent;
      this.rgbInputComponent = new RGBInputComponent(setting);
      abstractComponentArray[4] = this.rgbInputComponent;
      this.components.addAll(Arrays.asList(abstractComponentArray));
   }

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      if (ThemeManager.isLiquidGlassTheme()) {
         Render2D.rectangleWithMask(
            context.getMatrices().peek().getPositionMatrix(),
            this.x,
            this.y + 10.0F,
            this.width,
            this.height - 10.0F,
            6.0F,
            0,
            KawaseBlur.INSTANCE.getBlurTexture()
         );
      }

      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y + 10.0F, this.width, this.height - 10.0F)
            .round(6.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorAssist.getOutline())
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
      this.alphaComponent.position(this.x, this.y);
      this.hueComponent.position(this.x, this.y);
      this.saturationComponent.position(this.x, this.y);
      this.colorEditorComponent.position(this.x, this.y);
      this.rgbInputComponent.position(this.x, this.y);
      this.height = 105.0F;
      this.components.forEach(component -> component.render(context, mouseX, mouseY, delta));
      if (this.isThemeColor(this.setting) && this.setting.getColor() != this.lastColor) {
         ThemeManager.saveDefaultTheme();
         this.lastColor = this.setting.getColor();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.draggable(Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 17.0));
      this.components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      this.components.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
      if (this.isThemeColor(this.setting) && this.setting.getColor() != this.lastColor) {
         ThemeManager.saveDefaultTheme();
         this.lastColor = this.setting.getColor();
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   private boolean isThemeColor(ColorSetting setting) {
      return setting == ThemeManager.primaryColor
         || setting == ThemeManager.secondaryColor
         || setting == ThemeManager.BackgroundGui
         || setting == ThemeManager.offModuleColor
         || setting == ThemeManager.BackgroundSettings
         || setting == ThemeManager.textColor;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(component -> component.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public ColorSetting getSetting() {
      return this.setting;
   }
}
