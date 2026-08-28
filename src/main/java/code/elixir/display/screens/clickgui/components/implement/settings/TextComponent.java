package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class TextComponent extends AbstractSettingComponent {
   private final TextSetting setting;
   private boolean localTyping;
   public static boolean typing;
   private static final float INPUT_X = 6.0F;
   private static final float INPUT_Y = 14.0F;
   private static final float INPUT_H = 14.0F;
   private static final float INPUT_TEXT_X = 9.0F;

   public TextComponent(TextSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 32.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(context.getMatrices(), this.setting.getName(), this.x + 6.0F, this.y + 9.5F, ThemeManager.textColor.getColor());
      float inputW = Math.max(1.0F, this.width - 12.0F);
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + 6.0F, this.y + 14.0F, inputW, 14.0)
            .round(3.0F)
            .color(ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), 120))
            .build()
      );
      String value = this.setting.getText() == null ? "" : this.setting.getText();
      Object shown = value.isEmpty() ? "Type text..." : value;
      if (this.localTyping && System.currentTimeMillis() % 1000L < 500L) {
         shown = (String)shown + "|";
      }

      int textColor = value.isEmpty() ? ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 120) : ThemeManager.textColor.getColor();
      Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawVerticallyCentered(context.getMatrices(), (String)shown, this.x + 9.0F, this.y + 14.0F, 14.0F, textColor);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         typing = this.localTyping = Calculate.isHovered(mouseX, mouseY, this.x + 6.0F, this.y + 14.0F, Math.max(1.0F, this.width - 12.0F), 14.0);
         return this.localTyping;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.localTyping) {
         return false;
      } else {
         String value = this.setting.getText() == null ? "" : this.setting.getText();
         if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            this.localTyping = false;
            typing = false;
            return true;
         } else if (keyCode == 259 && !value.isEmpty()) {
            this.setting.setText(value.substring(0, value.length() - 1));
            return true;
         } else if ((modifiers & 2) != 0 && keyCode == 86) {
            String clipboard = GLFW.glfwGetClipboardString(window.getHandle());
            if (clipboard != null) {
               int max = Math.max(0, this.setting.getMax());
               Object next = value + clipboard;
               if (max > 0 && ((String)next).length() > max) {
                  next = ((String)next).substring(0, max);
               }

               this.setting.setText((String)next);
            }

            return true;
         } else {
            return true;
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.localTyping && !Character.isISOControl(chr)) {
         String value = this.setting.getText() == null ? "" : this.setting.getText();
         int max = this.setting.getMax();
         if (max <= 0 || value.length() < max) {
            this.setting.setText(value + chr);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
