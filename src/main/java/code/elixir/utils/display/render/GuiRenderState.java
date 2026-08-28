package code.elixir.utils.display.render;

import code.elixir.mixins.accessor.FontManagerAccessor;
import code.elixir.mixins.accessor.FontStorageAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphAtlasTexture;
import net.minecraft.client.gl.ShaderProgramKeys;

public final class GuiRenderState {
   private static volatile boolean fontAtlasesDirty = true;

   private GuiRenderState() {
   }

   public static void markFontAtlasesDirty() {
      fontAtlasesDirty = true;
   }

   public static void restoreAfterCustomDraw() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null) {
         client.getFramebuffer().beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         restoreFontAtlasesNearest();
      }
   }

   public static void restoreFontAtlasesNearest() {
      if (fontAtlasesDirty) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null) {
            FontManager fontManager = client.fontManager;
            if (fontManager != null) {
               for (FontStorage storage : ((FontManagerAccessor)fontManager).elixir$getFontStorages().values()) {
                  for (GlyphAtlasTexture atlas : ((FontStorageAccessor)storage).elixir$getGlyphAtlases()) {
                     atlas.setFilter(false, false);
                  }
               }

               fontAtlasesDirty = false;
            }
         }
      }
   }
}
