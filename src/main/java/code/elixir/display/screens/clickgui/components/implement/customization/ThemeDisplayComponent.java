package code.elixir.display.screens.clickgui.components.implement.customization;

import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class ThemeDisplayComponent extends AbstractComponent {
   private final Rectangle rectangle = new Rectangle();
   private final List<ThemeDisplayComponent.ThemeButton> themeButtons = new ArrayList<>();
   private float buttonSize = 16.0F;
   private float buttonSpacing = -2.0F;

   public ThemeDisplayComponent() {
      this.updateThemeButtons();
   }

   private void updateThemeButtons() {
      this.themeButtons.clear();
      this.themeButtons.add(new ThemeDisplayComponent.ThemeButton("default_dark", true));

      for (String themeName : ThemeManager.getAvailableThemes()) {
         if (!themeName.equals("default_dark") && !themeName.equals("default_light") && !themeName.equals("default_liquidglass")) {
            this.themeButtons.add(new ThemeDisplayComponent.ThemeButton(themeName, false));
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.updateThemeButtons();
      ThemeDisplayComponent.ThemeButton hoveredButton = null;
      float currentX = this.x + 5.0F;

      for (ThemeDisplayComponent.ThemeButton button : this.themeButtons) {
         button.x = currentX;
         button.y = this.y;
         button.size = this.buttonSize;
         this.renderThemeButton(context, matrix, button, mouseX, mouseY);
         if (Calculate.isHovered(mouseX + 6, mouseY + 4, button.x, button.y, button.size, button.size - 5.0F)) {
            hoveredButton = button;
         }

         currentX += this.buttonSize + this.buttonSpacing;
      }

      if (hoveredButton != null) {
         this.renderTooltip(matrix, hoveredButton);
      }
   }

   private void renderThemeButton(DrawContext context, MatrixStack matrix, ThemeDisplayComponent.ThemeButton button, int mouseX, int mouseY) {
      String icon = "l";
      float iconX = button.x + (button.size - Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).getStringWidth(icon)) / 2.0F;
      float iconY = button.y + (button.size - Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).getStringHeight(icon)) / 2.0F;
      int[] themeColors = this.getThemeColors(button.themeName);
      Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).drawHalfSplitString(matrix, icon, iconX - 6.0F, iconY, themeColors[0], themeColors[1]);
   }

   private void renderTooltip(MatrixStack matrix, ThemeDisplayComponent.ThemeButton button) {
      FontRenderer font = Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold);
      int paddingX = 3;
      float paddingY = 0.75F;
      float width = font.getStringWidth(button.themeName);
      float height = font.getFont().getSize() / 1.5F;
      float tooltipWidth = width + paddingX * 2;
      float iconOffset = -6.0F;
      float posX = button.x + button.size / 2.0F - tooltipWidth / 2.0F + iconOffset;
      float posY = button.y - height - paddingY * 2.0F - 4.0F;
      this.rectangle
         .render(
            ShapeProperties.create(matrix, posX, posY, width + paddingX * 2, height + paddingY * 2.0F)
               .round(1.0F)
               .outlineColor(new Color(33, 33, 33, 150).getRGB())
               .color(ColorAssist.HALF_BLACK)
               .build()
         );
      font.drawString(matrix, button.themeName, posX + paddingX, posY + 3.5F, -1);
   }

   private int[] getThemeColors(String themeName) {
      try {
         File themeDir = ThemeManager.getThemeDir();
         File themeFile = new File(themeDir, themeName + ".json");
         if (!themeFile.exists()) {
            System.out.println("[ThemeDisplayComponent] Theme file not found: " + themeFile.getAbsolutePath());
            return new int[]{-1, -1};
         } else {
            String content = Files.readString(themeFile.toPath());
            JsonObject theme = JsonParser.parseString(content).getAsJsonObject();
            if (theme.has("colors")) {
               JsonObject colors = theme.getAsJsonObject("colors");
               int primaryColor = colors.has("Первый цвет") ? colors.get("Первый цвет").getAsInt() : -1;
               int secondaryColor = colors.has("Второй цвет") ? colors.get("Второй цвет").getAsInt() : -1;
               return new int[]{primaryColor, secondaryColor};
            } else {
               return new int[]{-1, -1};
            }
         }
      } catch (Exception var9) {
         System.out.println("Error loading theme '" + themeName + "': " + var9.getMessage());
         var9.printStackTrace();
         return new int[]{-1, -1};
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for (ThemeDisplayComponent.ThemeButton themeButton : this.themeButtons) {
         if (Calculate.isHovered(mouseX + 6.0, mouseY + 4.0, themeButton.x, themeButton.y, themeButton.size, themeButton.size - 5.0F)) {
            if (button == 0) {
               this.loadTheme(themeButton.themeName);
               return true;
            }

            if (button == 1 && !themeButton.isDefault) {
               this.deleteTheme(themeButton.themeName);
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void loadTheme(String themeName) {
      try {
         ThemeManager.loadTheme(themeName);
      } catch (Exception var3) {
         System.out.println("Error loading theme: " + var3.getMessage());
      }
   }

   private void deleteTheme(String themeName) {
      try {
         ThemeManager.deleteTheme(themeName);
      } catch (Exception var3) {
         System.out.println("Error deleting theme: " + var3.getMessage());
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      for (ThemeDisplayComponent.ThemeButton button : this.themeButtons) {
         if (Calculate.isHovered(mouseX, mouseY, button.x, button.y, button.size, button.size)) {
            return true;
         }
      }

      return false;
   }

   private static class ThemeButton {
      public String themeName;
      public boolean isDefault;
      public float x;
      public float y;
      public float size;

      public ThemeButton(String themeName, boolean isDefault) {
         this.themeName = themeName;
         this.isDefault = isDefault;
      }
   }
}
