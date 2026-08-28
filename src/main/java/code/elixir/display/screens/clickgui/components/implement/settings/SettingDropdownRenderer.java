package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.Elixir;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ClipBounds;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class SettingDropdownRenderer implements QuickImports {
   private static final Identifier SETTINGS_ICON = BackgroundIcons.SETTINGS;
   private static final float ICON_SIZE = 10.0F;
   private static final float OPEN_ANIM_SMOOTH = 4.0F;
   private static final float CLOSE_ANIM_SMOOTH = 9.0F;
   private static final float PANEL_HOVER_SMOOTH = 8.0F;
   private static final float OPTION_HOVER_SMOOTH = 14.0F;
   public static final float LABEL_HEIGHT = 19.0F;
   public static final float PANEL_HEIGHT = 18.0F;
   public static final float OPTION_HEIGHT = 16.0F;
   public static final float OPTION_GAP = 2.0F;
   private static final float CONTENT_PAD_X = 9.0F;
   private boolean open;
   private float openAnimation;
   private float panelHoverAnimation;
   private final Map<String, Float> optionHoverAnimations = new HashMap<>();

   public boolean isOpen() {
      return this.open;
   }

   public void close() {
      this.open = false;
   }

   public float getOpenAnimation() {
      return this.openAnimation;
   }

   public float computeHeight(int optionCount) {
      float collapsed = 40.0F;
      float expanded = optionCount * 18.0F;
      return collapsed + expanded * this.openAnimation;
   }

   public void render(
      DrawContext context,
      float x,
      float y,
      float width,
      String label,
      String panelText,
      List<String> options,
      Predicate<String> isSelected,
      int mouseX,
      int mouseY
   ) {
      float contentX = x + 10.0F;
      float contentY = y + 1.0F;
      float contentW = width - 20.0F;
      float panelY = contentY + 19.0F + 1.0F;
      boolean panelHovered = Calculate.isHovered(mouseX, mouseY, contentX, panelY, contentW, 18.0);
      this.updateAnimations(panelHovered);
      int headerColor = ColorAssist.lerp(
         this.panelHoverAnimation, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 191), ThemeManager.textColor.getColor()
      );
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).drawVerticallyCentered(context.getMatrices(), label, x + 10.0F, contentY, 19.0F, headerColor);
      int panelBg = ColorAssist.applyOpacity(
         ThemeManager.BackgroundSettings.getColor(), (int)(55.0F + 35.0F * this.panelHoverAnimation + 25.0F * this.openAnimation)
      );
      rectangle.render(ShapeProperties.create(context.getMatrices(), contentX, panelY, contentW, 18.0).round(5.0F).color(panelBg).build());
      if (this.openAnimation > 0.04F) {
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), contentX + 1.0F, panelY + 1.0F, contentW - 2.0F, 16.0)
               .round(4.0F)
               .color(ColorAssist.applyOpacity(ColorAssist.getClientColor(), (int)(18.0F * this.openAnimation)))
               .build()
         );
      }

      Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawVerticallyCentered(
            context.getMatrices(),
            panelText,
            contentX + 8.0F,
            panelY,
            18.0F,
            ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)(170.0F + 85.0F * this.panelHoverAnimation))
         );
      float iconX = contentX + contentW - 10.0F - 4.0F;
      float iconY = panelY + 4.0F;
      int iconColor = ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)(140.0F + 115.0F * this.openAnimation));
      BackgroundIcons.draw(context, SETTINGS_ICON, iconX, iconY, 10.0F, iconColor);
      if (!(this.openAnimation <= 0.01F) && !options.isEmpty()) {
         float optionsY = panelY + 18.0F + 3.0F;
         float step = 18.0F;
         float fullOptionsHeight = options.size() * step - 2.0F;
         float visibleOptionsHeight = fullOptionsHeight * this.openAnimation;
         float fade = this.openAnimation * this.openAnimation;
         ClipBounds clip = Elixir.getInstance().getScissorManager().getCurrentBounds();
         if (clip != null && visibleOptionsHeight > 0.01F) {
            float spaceBelow = clip.bottom() - optionsY;
            visibleOptionsHeight = Math.min(visibleOptionsHeight, Math.max(0.0F, spaceBelow));
            float maxRight = clip.right();
            if (contentX + contentW > maxRight) {
               contentW = Math.max(0.0F, maxRight - contentX);
            }
         }

         MatrixStack matrices = context.getMatrices();
         if (visibleOptionsHeight > 0.01F && contentW > 0.5F) {
            Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), contentX, optionsY, contentW, visibleOptionsHeight);
         }

         float offset = 0.0F;

         for (String value : options) {
            float collapsedOffset = offset * this.openAnimation;
            if (collapsedOffset > visibleOptionsHeight + 0.5F) {
               break;
            }

            float optionY = optionsY + collapsedOffset;
            boolean hovered = this.open && Calculate.isHovered(mouseX, mouseY, contentX, optionY, contentW, 16.0);
            boolean active = isSelected.test(value);
            float hoverAnim = Calculate.interpolateSmooth(14.0, this.optionHoverAnimations.getOrDefault(value, 0.0F), hovered ? 1.0F : 0.0F);
            this.optionHoverAnimations.put(value, hoverAnim);
            float activeAnim = active ? 1.0F : 0.0F;
            int baseBg = ColorAssist.applyOpacity(ThemeManager.BackgroundSettings.getColor(), (int)((38.0F + 30.0F * hoverAnim) * fade));
            int activeBg = ColorAssist.applyOpacity(ColorAssist.getClientColor(), (int)((40.0F + 45.0F * activeAnim) * fade));
            int chipColor = ColorAssist.lerp(activeAnim, baseBg, activeBg);
            rectangle.render(ShapeProperties.create(matrices, contentX, optionY, contentW, 16.0).round(4.0F).color(chipColor).build());
            int textColor = ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), (int)((150.0F + 35.0F * hoverAnim + 70.0F * activeAnim) * fade));
            Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM).drawVerticallyCentered(matrices, value, contentX + 8.0F, optionY, 16.0F, textColor);
            if (activeAnim > 0.01F) {
               Fonts.getSize(11, Fonts.Type.ELIXIR_ICONS)
                  .drawVerticallyCentered(
                     matrices,
                     "k",
                     contentX + contentW - 14.0F,
                     optionY,
                     16.0F,
                     ColorAssist.applyOpacity(ColorAssist.getClientColor(), (int)(255.0F * activeAnim * fade))
                  );
            }

            offset += step;
         }

         if (visibleOptionsHeight > 0.01F && contentW > 0.5F) {
            Elixir.getInstance().getScissorManager().pop();
         }
      }
   }

   public boolean handleClick(
      double mouseX, double mouseY, int button, float x, float y, float width, List<String> options, Function<String, Boolean> onOptionClick
   ) {
      if (button != 0) {
         return false;
      } else {
         float contentX = x + 10.0F;
         float contentY = y + 1.0F;
         float panelY = contentY + 19.0F + 1.0F;
         float contentW = width - 20.0F;
         if (Calculate.isHovered(mouseX, mouseY, contentX, panelY, contentW, 18.0)) {
            this.open = !this.open;
            return true;
         } else if (this.open && !(this.openAnimation < 0.22F)) {
            float optionsY = panelY + 18.0F + 3.0F;
            float step = 18.0F;
            float fullOptionsHeight = options.size() * step - 2.0F;
            float visibleOptionsHeight = fullOptionsHeight * this.openAnimation;
            float offset = 0.0F;

            for (String value : options) {
               float optionY = optionsY + offset * this.openAnimation;
               if (Calculate.isHovered(mouseX, mouseY, contentX, optionY, contentW, 16.0)) {
                  return Boolean.TRUE.equals(onOptionClick.apply(value));
               }

               offset += step;
            }

            return false;
         } else {
            return false;
         }
      }
   }

   private void updateAnimations(boolean panelHovered) {
      float openTarget = this.open ? 1.0F : 0.0F;
      float smooth = this.open ? 4.0F : 9.0F;
      this.openAnimation = Calculate.interpolateSmooth(smooth, this.openAnimation, openTarget);
      this.panelHoverAnimation = Calculate.interpolateSmooth(8.0, this.panelHoverAnimation, panelHovered ? 1.0F : 0.0F);
      if (!this.open && this.openAnimation < 0.002F) {
         this.openAnimation = 0.0F;
         this.optionHoverAnimations.clear();
      }
   }
}
