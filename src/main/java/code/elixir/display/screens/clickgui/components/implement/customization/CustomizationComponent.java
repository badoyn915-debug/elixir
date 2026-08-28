package code.elixir.display.screens.clickgui.components.implement.customization;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.utils.customization.CustomizationManager;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class CustomizationComponent extends AbstractComponent {
   private static final float ROW_HEIGHT = 28.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float SWATCH_SIZE = 16.0F;
   private static final float LABEL_SWATCH_GAP = 10.0F;
   private static final float HORIZONTAL_PADDING = 10.0F;
   private final Rectangle rectangle = new Rectangle();
   private final List<ColorSetting> settings = CustomizationManager.getCustomizationSettings();

   public CustomizationComponent() {
      this.height = this.calculateHeight();
   }

   private int calculateHeight() {
      return (int)(20.0F + this.settings.size() * 32.0F);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      float settingY = this.y + 4.0F;
      Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(matrix, "Colors", this.x + 10.0F, settingY + 2.0F, Calculate.applyOpacity(ThemeManager.textColor.getColor(), 170.0F));
      settingY += 22.0F;

      for (ColorSetting setting : this.settings) {
         this.renderColorSetting(context, matrix, setting, this.x, settingY, this.width, 28.0F, mouseX, mouseY);
         settingY += 32.0F;
      }
   }

   private void renderColorSetting(
      DrawContext context, MatrixStack matrix, ColorSetting setting, float rowX, float rowY, float rowWidth, float rowHeight, int mouseX, int mouseY
   ) {
      boolean hovered = Calculate.isHovered(mouseX, mouseY, rowX, rowY, rowWidth, rowHeight);
      this.rectangle
         .render(
            ShapeProperties.create(matrix, rowX, rowY, rowWidth, rowHeight)
               .round(6.0F)
               .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), hovered ? 110.0F : 75.0F))
               .build()
         );
      String displayName = this.getDisplayName(setting);
      float textX = rowX + 10.0F;
      FontRenderer labelFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      float textY = labelFont.getCenteredY(rowY, rowHeight, displayName);
      labelFont.drawString(matrix, displayName, textX, textY, ThemeManager.textColor.getColor());
      float swatchX = textX + labelFont.getStringWidth(displayName) + 10.0F;
      float swatchY = rowY + (rowHeight - 16.0F) / 2.0F;
      this.rectangle.render(ShapeProperties.create(matrix, swatchX, swatchY, 16.0, 16.0).round(4.0F).color(Calculate.applyOpacity(-16777216, 40.0F)).build());
      this.rectangle.render(ShapeProperties.create(matrix, swatchX + 1.0F, swatchY + 1.0F, 14.0, 14.0).round(3.5F).color(setting.getColor()).build());
   }

   private String getDisplayName(ColorSetting setting) {
      if (setting == ThemeManager.primaryColor) {
         return "Primary";
      } else if (setting == ThemeManager.secondaryColor) {
         return "Secondary";
      } else if (setting == ThemeManager.BackgroundGui) {
         return "GUI";
      } else if (setting == ThemeManager.offModuleColor) {
         return "Modules";
      } else if (setting == ThemeManager.BackgroundSettings) {
         return "Settings";
      } else {
         return setting == ThemeManager.textColor ? "Text" : setting.getName();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         return false;
      } else {
         float settingY = this.y + 26.0F;

         for (ColorSetting setting : this.settings) {
            if (Calculate.isHovered(mouseX, mouseY, this.x, settingY, this.width, 28.0) && button == 0) {
               AbstractWindow existingWindow = null;

               for (AbstractWindow window : windowManager.getWindows()) {
                  ColorWindow colorWindow;
                  if (window instanceof ColorWindow && (colorWindow = (ColorWindow)window).getSetting() == setting) {
                     existingWindow = window;
                     break;
                  }
               }

               if (existingWindow != null) {
                  windowManager.delete(existingWindow);
               } else {
                  AbstractWindow colorWindow = new ColorWindow(setting)
                     .position((int)(mouseX - 110.0), (int)(mouseY - 20.0))
                     .size(100.0F, 155.0F)
                     .draggable(true);
                  windowManager.add(colorWindow);
               }

               return true;
            }

            settingY += 32.0F;
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
