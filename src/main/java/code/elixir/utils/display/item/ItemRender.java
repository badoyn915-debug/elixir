package code.elixir.utils.display.item;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class ItemRender implements QuickImports {
   public static final HashMap<ItemStack, ItemRender.CachedSprite> SPRITE_CACHE = new HashMap<>();

   public static void drawItem(MatrixStack matrix, ItemStack stack, float x, float y, boolean count, boolean bar) {
      ItemRender.CachedSprite cachedSprite = getSpriteTexture(stack);
      if (cachedSprite != null) {
         Render2D.drawSprite(matrix, cachedSprite.sprite, x, y, 16.0F, 16);
         if (stack != null && stack.hasGlint()) {
            drawGlint(matrix, cachedSprite.sprite, x, y);
         }
      }

      if (count) {
         drawCount(matrix, stack, x, y);
      }

      if (bar) {
         drawBar(matrix, stack, x, y);
      }
   }

   private static void drawGlint(MatrixStack matrix, Sprite sprite, float x, float y) {
      long now = System.currentTimeMillis();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      drawGlintLayer(matrix, sprite, x, y, (float)(now % 1300L) / 1300.0F, false, 230, 150, 255, 0.95F);
      drawGlintLayer(matrix, sprite, x, y, (float)(now % 1900L) / 1900.0F, true, 150, 90, 255, 0.85F);
      drawGlintLayer(matrix, sprite, x, y, (float)(now % 850L) / 850.0F, false, 255, 255, 255, 0.5F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private static void drawGlintLayer(
      MatrixStack matrix, Sprite sprite, float x, float y, float phase, boolean reversed, int r, int g, int b, float maxIntensity
   ) {
      int tl = glintColor(cornerDiagonal(0.0F, 0.0F, reversed), phase, r, g, b, maxIntensity);
      int bl = glintColor(cornerDiagonal(0.0F, 1.0F, reversed), phase, r, g, b, maxIntensity);
      int br = glintColor(cornerDiagonal(1.0F, 1.0F, reversed), phase, r, g, b, maxIntensity);
      int tr = glintColor(cornerDiagonal(1.0F, 0.0F, reversed), phase, r, g, b, maxIntensity);
      Render2D.drawSpriteColored(matrix, sprite, x, y, 16.0F, 16.0F, tl, bl, br, tr);
   }

   private static float cornerDiagonal(float u, float v, boolean reversed) {
      return reversed ? (u + (1.0F - v)) * 0.5F : (u + v) * 0.5F;
   }

   private static int glintColor(float diagonal, float phase, int r, int g, int b, float maxIntensity) {
      float band = 0.5F + 0.5F * MathHelper.cos((float)((Math.PI * 2) * (diagonal - phase)));
      float intensity = (0.25F + 0.75F * band * band) * maxIntensity;
      int alpha = MathHelper.clamp((int)(intensity * 255.0F), 0, 255);
      return ColorHelper.getArgb(alpha, r, g, b);
   }

   private static void drawCount(MatrixStack matrix, ItemStack stack, float x, float y) {
      int count = stack.getCount();
      Object text = count > 1 ? count + "" : "";
      if (!((String)text).isEmpty()) {
         FontRenderer font = Fonts.getSize(16);
         font.drawString(matrix, (String)text, x + 16.0F - font.getStringWidth((String)text), y + 11.0F, ColorAssist.getText());
      }
   }

   private static void drawBar(MatrixStack matrix, ItemStack stack, float x, float y) {
      if (stack.isItemBarVisible()) {
         rectangle.render(ShapeProperties.create(matrix, x + 1.5F, y + 13.0F, 13.0, 2.0).color(-16777216).build());
         rectangle.render(
            ShapeProperties.create(matrix, x + 1.5F, y + 13.0F, stack.getItemBarStep(), 1.0).color(ColorHelper.fullAlpha(stack.getItemBarColor())).build()
         );
      }
   }

   private static ItemRender.CachedSprite getSpriteTexture(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      } else if (stack.isOf(Items.COMPASS)) {
         return null;
      } else if (mc == null || mc.getItemModelManager() == null) {
         return null;
      } else {
         return mc.getOverlay() instanceof SplashOverlay ? null : SPRITE_CACHE.computeIfAbsent(stack, key -> {
            ItemRenderState state = new ItemRenderState();

            try {
               mc.getItemModelManager().update(state, stack, ModelTransformationMode.GUI, mc.world, null, 0);
            } catch (Throwable var5) {
               return null;
            }

            Sprite sprite = getFirstParticleSprite(state);
            if (sprite != null) {
               int glId = mc.getTextureManager().getTexture(sprite.getAtlasId()).getGlId();
               return new ItemRender.CachedSprite(sprite, glId, 16777215);
            } else {
               return null;
            }
         });
      }
   }

   private static Sprite getFirstParticleSprite(ItemRenderState state) {
      if (state != null && state.layerCount != 0) {
         BakedModel model = state.layers[0].model;
         return model == null ? null : model.getParticleSprite();
      } else {
         return null;
      }
   }

   private ItemRender() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public record CachedSprite(Sprite sprite, int glId, int color) {
   }
}
