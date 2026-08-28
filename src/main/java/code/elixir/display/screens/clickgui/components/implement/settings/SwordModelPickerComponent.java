package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.module.ModuleDescriptions;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class SwordModelPickerComponent extends AbstractSettingComponent {
   private static final float ROW_HEIGHT = 16.0F;
   private static final float ROW_GAP = 2.0F;
   private static final float DESC_HEIGHT = 18.0F;
   private static final float LABEL_HEIGHT = 19.0F;
   private static final float PAD_X = 9.0F;
   private final SelectSetting selectSetting;
   private final Map<String, Float> hoverAnimations = new HashMap<>();

   public SwordModelPickerComponent(SelectSetting setting) {
      super(setting);
      this.selectSetting = setting;
   }

   @Override
   public void prepareLayout(float width) {
      this.width = width;
      List<String> options = this.selectSetting.getList();
      int count = options == null ? 0 : options.size();
      this.height = 43.0F + count * 18.0F + 4.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      List<String> options = this.selectSetting.getList();
      if (options != null && !options.isEmpty()) {
         String selected = this.selectSetting.getSelected();
         if (selected == null || selected.isEmpty()) {
            selected = options.getFirst();
         }

         float contentX = this.x + 9.0F;
         float contentW = this.width - 18.0F;
         float cursorY = this.y;
         String description = ModuleDescriptions.tryGetDescription(this.getModule());
         if (description != null && !ModuleDescriptions.isMissingDescription(description)) {
            Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
               .drawString(matrices, description, contentX, cursorY + 4.0F, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 185));
            cursorY += 18.0F;
         }

         int selectedIndex = Math.max(0, options.indexOf(selected)) + 1;
         String label = this.selectSetting.getName() + "  " + selectedIndex + "/" + options.size();
         Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).drawVerticallyCentered(matrices, label, contentX, cursorY, 19.0F, ThemeManager.textColor.getColor());
         float var24;
         rectangle.render(
            ShapeProperties.create(matrices, contentX, var24 = cursorY + 23.0F, contentW, 2.0)
               .round(1.0F)
               .color(ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 18))
               .build()
         );
         cursorY = var24 + 6.0F;

         for (String option : options) {
            boolean active = option.equals(selected);
            boolean hovered = Calculate.isHovered(mouseX, mouseY, contentX, cursorY, contentW, 16.0);
            float hoverAnim = Calculate.interpolateSmooth(14.0, this.hoverAnimations.getOrDefault(option, 0.0F), hovered ? 1.0F : 0.0F);
            this.hoverAnimations.put(option, hoverAnim);
            float activeAnim = active ? 1.0F : 0.0F;
            int baseBg = ColorAssist.applyOpacity(ThemeManager.BackgroundSettings.getColor(), (int)(55.0F + 30.0F * hoverAnim));
            int activeBg = ColorAssist.applyOpacity(ColorAssist.getClientColor(), (int)(70.0F + 50.0F * activeAnim));
            int chipColor = ColorAssist.lerp(activeAnim, baseBg, activeBg);
            rectangle.render(ShapeProperties.create(matrices, contentX, cursorY, contentW, 16.0).round(4.0F).color(chipColor).build());
            int textColor = ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)(170.0F + 40.0F * hoverAnim + 85.0F * activeAnim));
            Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM).drawVerticallyCentered(matrices, option, contentX + 8.0F, cursorY, 16.0F, textColor);
            if (activeAnim > 0.01F) {
               BackgroundIcons.draw(
                  context,
                  BackgroundIcons.SETTINGS,
                  contentX + contentW - 14.0F,
                  cursorY + 3.0F,
                  10.0F,
                  ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)(255.0F * activeAnim))
               );
            }

            cursorY += 18.0F;
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         List<String> options = this.selectSetting.getList();
         if (options != null && !options.isEmpty()) {
            float contentX = this.x + 9.0F;
            float contentW = this.width - 18.0F;
            float cursorY = this.y;
            String description = ModuleDescriptions.tryGetDescription(this.getModule());
            if (description != null && !ModuleDescriptions.isMissingDescription(description)) {
               cursorY += 18.0F;
            }

            cursorY += 29.0F;

            for (String option : options) {
               if (Calculate.isHovered(mouseX, mouseY, contentX, cursorY, contentW, 16.0)) {
                  if (this.selectSetting.getList().contains(option)) {
                     this.selectSetting.selected(option);
                  }

                  return true;
               }

               cursorY += 18.0F;
            }

            return false;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
