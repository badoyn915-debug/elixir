package code.elixir.display.screens.clickgui.components.implement.customization;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.utils.animation.AnimationHelper;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

public class ThemeInputComponent extends AbstractComponent implements MenuScreen.ClickGuiInput, MenuScreen.ClearableClickGuiInput {
   private boolean typing = false;
   private boolean dragging;
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private String text = "";
   private Runnable onTextChanged;
   private long squareClickTime = 0L;
   private final ThemeDisplayComponent themeDisplay = new ThemeDisplayComponent();

   public void setText(String text) {
      this.text = text;
      this.cursorPosition = 0;
      this.clearSelection();
   }

   public void setOnTextChanged(Runnable callback) {
      this.onTextChanged = callback;
   }

   @Override
   public void setTyping(boolean typing) {
      this.typing = typing;
   }

   @Override
   public boolean isTyping() {
      return this.typing;
   }

   private void notifyTextChanged() {
      if (this.onTextChanged != null) {
         this.onTextChanged.run();
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(12);
      this.updateXOffset(font, this.cursorPosition);
      this.width = 100.0F;
      this.height = 21.0F;
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width - 16.0F, this.height)
            .round(6.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(new Color(47, 47, 50, 255).getRGB())
            .color(ThemeManager.offModuleColor.getColor())
            .build()
      );
      float squareSize = 20.0F;
      float squareX = this.x + this.width - squareSize - 5.0F;
      float squareY = this.y + (this.height - squareSize) / 2.0F;
      long currentTime = System.currentTimeMillis();
      boolean isClicked = currentTime - this.squareClickTime < 200L;
      int[] colors = isClicked
         ? new int[]{-1, -1, -1, 5}
         : new int[]{ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2()};
      String buttonAnimationKey = "theme_save_button_" + System.identityHashCode(10);
      float buttonScale = AnimationHelper.getAnimationValue(buttonAnimationKey, 1.0F);
      rectangle.render(
         ShapeProperties.create(matrix, squareX + 12.0F, squareY, squareSize * buttonScale, squareSize * buttonScale)
            .round(4.0F)
            .color(colors[0], colors[1], colors[2], colors[3])
            .build()
      );
      float iconX = squareX + (squareSize - Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).getStringWidth("y")) / 2.0F;
      float iconY = squareY + (squareSize - Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).getStringHeight("y")) / 2.0F;
      Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).drawString(context.getMatrices(), "y", iconX + 12.3, iconY + 7.6, ThemeManager.textColor.getColor());
      String displayText = this.text.equalsIgnoreCase("") && !this.typing ? "New Theme..." : this.text;
      ScissorAssist scissor = Elixir.getInstance().getScissorManager();
      scissor.push(matrix.peek().getPositionMatrix(), this.x - 1.0F, this.y, this.width - 25.0F, this.height);
      int end;
      int start;
      if (this.typing
         && this.selectionStart != -1
         && this.selectionEnd != -1
         && this.selectionStart != this.selectionEnd
         && (start = Math.max(0, Math.min(this.getStartOfSelection(), this.text.length())))
            < (end = Math.max(0, Math.min(this.getEndOfSelection(), this.text.length())))) {
         float selectionXStart = this.x + 4.0F - this.xOffset + font.getStringWidth(this.text.substring(0, start));
         float selectionXEnd = this.x + 4.0F - this.xOffset + font.getStringWidth(this.text.substring(0, end));
         float selectionWidth = selectionXEnd - selectionXStart;
         rectangle.render(ShapeProperties.create(matrix, selectionXStart, this.y + this.height / 2.0F - 4.0F, selectionWidth, 8.0).color(-11172376).build());
      }

      float textX = this.x + 5.0F;
      Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold)
         .drawString(
            context.getMatrices(),
            displayText,
            textX,
            this.y + this.height / 2.0F - 1.0F,
            this.typing ? ThemeManager.textColor.getColor() : new Color(137, 137, 140, 255).getRGB()
         );
      scissor.pop();
      long cursorTime = System.currentTimeMillis();
      boolean focused = this.typing && cursorTime % 1000L < 500L;
      if (focused && (this.selectionStart == -1 || this.selectionStart == this.selectionEnd)) {
         FontRenderer textFont = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
         int safeCursorPosition = Math.min(this.cursorPosition, this.text.length());
         float cursorX = textFont.getStringWidth(this.text.substring(0, safeCursorPosition));
         float leftCursorX = this.x + 5.0F + cursorX;
         rectangle.render(
            ShapeProperties.create(matrix, leftCursorX, this.y + this.height / 2.0F - 3.5F, 0.5, 7.0).color(ThemeManager.textColor.getColor()).build()
         );
      }

      if (this.dragging && GLFW.glfwGetMouseButton(window.getHandle(), 0) == 1) {
         this.cursorPosition = this.getCursorIndexAt(mouseX);
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition + 1;
         }

         this.selectionEnd = this.cursorPosition;
      } else if (this.dragging) {
         this.dragging = false;
      }

      this.themeDisplay.x = this.x;
      this.themeDisplay.y = this.y - 11.0F;
      this.themeDisplay.width = this.width;
      this.themeDisplay.height = 20.0F;
      this.themeDisplay.render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.themeDisplay.x = this.x;
      this.themeDisplay.y = this.y - 11.0F;
      this.themeDisplay.width = this.width;
      this.themeDisplay.height = 20.0F;
      if (this.themeDisplay.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         float squareSize = 20.0F;
         float squareX = this.x + this.width - squareSize - 12.0F;
         float squareY = this.y + (this.height - squareSize) / 2.0F;
         if (Calculate.isHovered(mouseX, mouseY, squareX + 12.0F, squareY, squareSize, squareSize) && button == 0) {
            String buttonAnimationKey = "theme_save_button_" + System.identityHashCode(this);
            AnimationHelper.startAnimation(buttonAnimationKey, 1.0F, 1.2F, 100L, AnimationHelper.EasingType.EASE_OUT);
            this.squareClickTime = System.currentTimeMillis();
            this.saveTheme();
            return true;
         } else if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width - 25.0F, this.height) && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClickTime < 250L) {
               this.selectionStart = 0;
               this.selectionEnd = this.text.length();
            } else {
               MenuScreen.activateClickGuiInput(this);
               this.dragging = true;
               this.lastClickTime = currentTime;
               this.selectionStart = this.cursorPosition = this.getCursorIndexAt(mouseX);
               this.selectionEnd = this.cursorPosition;
            }

            return true;
         } else {
            this.typing = false;
            this.clearSelection();
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.typing) {
         String testText = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
         float testWidth = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold).getStringWidth(testText);
         float maxWidth;
         if (testWidth <= (maxWidth = this.width - 30.0F)) {
            this.deleteSelectedText();
            this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.clearSelection();
            this.notifyTextChanged();
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         if (Screen.hasControlDown()) {
            switch (keyCode) {
               case 65:
                  this.selectAllText();
                  break;
               case 67:
                  this.copyToClipboard();
                  break;
               case 86:
                  this.pasteFromClipboard();
            }
         } else {
            switch (keyCode) {
               case 257:
               case 259:
                  this.handleTextModification(keyCode);
               case 258:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
                  this.moveCursor(keyCode);
            }
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
      if (clipboardText != null) {
         this.replaceText(this.cursorPosition, this.cursorPosition, clipboardText);
      }
   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(window.getHandle(), this.getSelectedText());
      }
   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.text.length();
   }

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.hasSelection()) {
            this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
         } else if (this.cursorPosition > 0) {
            this.replaceText(this.cursorPosition - 1, this.cursorPosition, "");
         }
      } else if (keyCode == 257) {
         this.typing = false;
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.text.length()) {
         this.cursorPosition++;
      }

      this.updateSelectionAfterCursorMove();
   }

   private void updateSelectionAfterCursorMove() {
      if (Screen.hasShiftDown()) {
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      } else {
         this.clearSelection();
      }
   }

   private void replaceText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.text.length()) {
         end = this.text.length();
      }

      if (start > end) {
         start = end;
      }

      this.text = this.text.substring(0, start) + replacement + this.text.substring(end);
      this.cursorPosition = start + replacement.length();
      this.clearSelection();
      this.notifyTextChanged();
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return this.text.substring(this.getStartOfSelection(), this.getEndOfSelection());
   }

   private int getStartOfSelection() {
      return Math.min(this.selectionStart, this.selectionEnd);
   }

   private int getEndOfSelection() {
      return Math.max(this.selectionStart, this.selectionEnd);
   }

   private void clearSelection() {
      this.selectionStart = -1;
      this.selectionEnd = -1;
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
      float textStartX = this.x + 5.0F;
      float relativeX = (float)mouseX - textStartX;
      int position = 0;

      while (position < this.text.length() && !(font.getStringWidth(this.text.substring(0, position + 1)) > relativeX)) {
         position++;
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      this.xOffset = 0.0F;
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      }
   }

   private void saveTheme() {
      if (this.text.isEmpty()) {
         System.out.println("ThemeInputComponent: Cannot save theme - name is empty");
      } else {
         try {
            ThemeManager.saveTheme(this.text);
            this.text = "";
            this.cursorPosition = 0;
            this.clearSelection();
         } catch (Exception var2) {
            System.out.println("ThemeInputComponent: Error saving theme: " + var2.getMessage());
            var2.printStackTrace();
         }
      }
   }

   @Override
   public void clearText() {
      this.text = "";
      this.cursorPosition = 0;
      this.clearSelection();
   }

   public String getText() {
      return this.text;
   }
}
