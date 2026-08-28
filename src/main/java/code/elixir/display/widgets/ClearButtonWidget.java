package code.elixir.display.widgets;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.Selectable.SelectionType;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ClearButtonWidget implements Drawable, Element, Selectable {
   private static final Identifier CLEAR_BUTTON_TEXTURE = Identifier.of("minecraft", "textures/clear_button.png");
   private static final Identifier HOVERED_CLEAR_BUTTON_TEXTURE = Identifier.of("minecraft", "textures/hovered_clear_button.png");
   private int x;
   private int y;
   private final int width;
   private final int height;
   private final Runnable onPress;
   private boolean hovered = false;

   public ClearButtonWidget(int x, int y, int width, int height, Runnable onPress) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.onPress = onPress;
   }

   public void setPosition(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
      Identifier texture = this.hovered ? HOVERED_CLEAR_BUTTON_TEXTURE : CLEAR_BUTTON_TEXTURE;
      context.drawTexture(RenderLayer::getGuiTextured, texture, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      if (this.hovered && QuickImports.mc.currentScreen != null) {
         context.drawTooltip(QuickImports.mc.textRenderer, Text.literal("Очистить инвентарь"), mouseX, mouseY);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height) {
         this.onPress.run();
         return true;
      } else {
         return false;
      }
   }

   public void setFocused(boolean focused) {
   }

   public boolean isFocused() {
      return false;
   }

   public SelectionType getType() {
      return SelectionType.NONE;
   }

   public void appendNarrations(NarrationMessageBuilder builder) {
   }
}
