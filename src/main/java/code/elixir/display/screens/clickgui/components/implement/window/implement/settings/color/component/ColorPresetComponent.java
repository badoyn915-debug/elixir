package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.ColorPresetButton;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.display.render.font.Fonts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class ColorPresetComponent extends AbstractComponent {
   private final List<ColorPresetButton> colorPresetButtonList = new ArrayList<>();
   private final ColorSetting setting;
   private float windowHeight;
   private float windowWidth;

   public ColorPresetComponent(ColorSetting setting) {
      this.setting = setting;

      for (int preset : setting.getPresets()) {
         this.colorPresetButtonList.add(new ColorPresetButton(setting, preset));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.colorPresetButtonList.isEmpty()) {
         Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Готовые тсвета", this.x + 6.0F, this.y + 95.0F, -1);
      }

      int xOffset = 0;
      int yOffset = 0;
      int colorIndex = 0;
      int size = 13;

      for (ColorPresetButton button : this.colorPresetButtonList) {
         button.x = this.x + 6.0F + xOffset;
         button.y = this.y + 103.0F + yOffset;
         button.render(context, mouseX, mouseY, delta);
         xOffset += size;
         if (++colorIndex >= 11) {
            colorIndex = 0;
            xOffset = 0;
            yOffset += size - 1;
         }
      }

      this.windowHeight = this.colorPresetButtonList.isEmpty() ? 132.0F : 166 + yOffset - yOffset / 2.0F;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.colorPresetButtonList.forEach(colorPresetButton -> colorPresetButton.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public List<ColorPresetButton> getColorPresetButtonList() {
      return this.colorPresetButtonList;
   }

   public ColorSetting getSetting() {
      return this.setting;
   }

   public float getWindowHeight() {
      return this.windowHeight;
   }

   public float getWindowWidth() {
      return this.windowWidth;
   }
}
