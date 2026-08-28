package code.elixir.utils.display.render;

import net.minecraft.client.gui.DrawContext;

public final class MenuBackgroundRenderer {
   private MenuBackgroundRenderer() {
   }

   public static boolean hasBackgroundTexture() {
      return true;
   }

   public static boolean renderFullscreen(DrawContext context, int width, int height) {
      MainMenuSkyRenderer.render(context, width, height);
      return true;
   }
}
