package code.elixir.display.screens.altscreen;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class Alt extends Screen implements QuickImports {
   public static Alt INSTANCE = new Alt();
   public int x;
   public int y;
   public int width;
   public int height;

   public void initialize() {
   }

   public Alt() {
      super(Text.of("Alt"));
      this.initialize();
   }

   public void tick() {
      super.tick();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.x = window.getScaledWidth() / 2 - 200;
      this.y = window.getScaledHeight() / 2 - 125;
      this.width = window.getScaledWidth();
      this.height = window.getScaledHeight();
      super.render(context, mouseX, mouseY, delta);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }
}
