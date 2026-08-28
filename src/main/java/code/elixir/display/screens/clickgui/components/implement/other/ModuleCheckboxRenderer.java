package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class ModuleCheckboxRenderer {
   public static final float SIZE = 12.0F;
   public static final float LABEL_GAP = 5.0F;

   private ModuleCheckboxRenderer() {
   }

   public static float centerY(float rowY, float rowHeight) {
      return rowY + (rowHeight - 12.0F) / 2.0F;
   }

   public static void render(DrawContext context, float x, float y, boolean checked) {
      int left = Math.round(x);
      int top = Math.round(y);
      int right = Math.round(x + 12.0F);
      int bottom = Math.round(y + 12.0F);
      int fill = checked ? ColorAssist.getClientColor() | 0xFF000000 : ColorAssist.applyOpacity(ThemeManager.BackgroundSettings.getColor(), 200);
      int border = checked ? -1512204 : -7629400;
      context.fill(left, top, right, bottom, fill);
      context.fill(left, top, right, top + 1, border);
      context.fill(left, bottom - 1, right, bottom, border);
      context.fill(left, top, left + 1, bottom, border);
      context.fill(right - 1, top, right, bottom, border);
      if (checked) {
         MatrixStack matrices = context.getMatrices();
         Fonts.getSize(10, Fonts.Type.ELIXIR_ICONS).drawString(matrices, "k", x + 1.8F, y + 2.2F, -1);
      }
   }

   public static boolean isHovered(double mouseX, double mouseY, float x, float y) {
      return Calculate.isHovered(mouseX, mouseY, x, y, 12.0, 12.0);
   }
}
