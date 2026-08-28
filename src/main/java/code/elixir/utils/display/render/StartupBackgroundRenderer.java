package code.elixir.utils.display.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public final class StartupBackgroundRenderer {
   public static final Identifier BACKGROUND = BackgroundIcons.BACKGROUND;
   private static final int BG_TEX_WIDTH = 1672;
   private static final int BG_TEX_HEIGHT = 941;

   private StartupBackgroundRenderer() {
   }

   public static void drawStartupBackdrop(DrawContext context, int width, int height) {
      drawBackground(context, width, height);
   }

   public static void drawBackground(DrawContext context, int width, int height) {
      drawTexture(context, BACKGROUND, 0, 0, width, height, 1672, 941);
   }

   public static void drawBackground(DrawContext context, float x, float y, float renderWidth, float renderHeight, float alpha) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
      context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND, (int)x, (int)y, 0.0F, 0.0F, (int)renderWidth, (int)renderHeight, 1672, 941);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight);
      RenderSystem.disableBlend();
   }
}
