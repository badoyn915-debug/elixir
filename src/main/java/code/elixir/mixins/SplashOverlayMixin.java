package code.elixir.mixins;

import code.elixir.utils.display.gif.GifTextureRenderer;
import code.elixir.utils.display.render.StartupBackgroundRenderer;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SplashOverlay.class})
public abstract class SplashOverlayMixin {
   private static final Identifier[] SPLASH_GIFS = new Identifier[]{
      Identifier.of("minecraft", "gifs/blatnoy.gif"),
      Identifier.of("minecraft", "gifs/clean.gif"),
      Identifier.of("minecraft", "gifs/duck.gif"),
      Identifier.of("minecraft", "gifs/nozik.gif"),
      Identifier.of("minecraft", "gifs/poplach.gif"),
      Identifier.of("minecraft", "gifs/wait.gif"),
      Identifier.of("minecraft", "gifs/sleep.gif")
   };
   private static final GifTextureRenderer SPLASH_GIF_RENDERER = new GifTextureRenderer(SPLASH_GIFS[new Random().nextInt(SPLASH_GIFS.length)]);
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private boolean reloading;
   @Shadow
   private long reloadCompleteTime;
   @Shadow
   private long reloadStartTime;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onSplashRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      float opacity = this.elixir$getOverlayOpacity();
      if (!(opacity <= 0.01F)) {
         SPLASH_GIF_RENDERER.preload();
         int screenW = context.getScaledWindowWidth();
         int screenH = context.getScaledWindowHeight();
         StartupBackgroundRenderer.drawBackground(context, 0.0F, 0.0F, screenW, screenH, opacity);
         float gifSize = Math.min(screenW, screenH) * 0.32F;
         float centerX = screenW / 2.0F;
         float centerY = screenH / 2.0F;
         SPLASH_GIF_RENDERER.render(context, centerX - gifSize / 2.0F, centerY - gifSize / 2.0F, gifSize, gifSize, opacity);
      }
   }

   @Unique
   private float elixir$getOverlayOpacity() {
      long now = Util.getMeasuringTimeMs();
      float fadeOut = this.reloadCompleteTime > -1L ? (float)(now - this.reloadCompleteTime) / 1000.0F : -1.0F;
      float fadeIn = this.reloadStartTime > -1L ? (float)(now - this.reloadStartTime) / 500.0F : -1.0F;
      if (fadeOut >= 1.0F) {
         return 1.0F - MathHelper.clamp(fadeOut - 1.0F, 0.0F, 1.0F);
      } else {
         return this.reloading ? MathHelper.clamp(fadeIn, 0.0F, 1.0F) : 1.0F;
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"
      ),
      require = 0
   )
   private void skipMojangLogoDraw(
      DrawContext context,
      Function<Identifier, RenderLayer> renderLayers,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      if (!SplashOverlay.LOGO.equals(texture)) {
         context.drawTexture(renderLayers, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, color);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/SplashOverlay;renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V"
      )
   )
   private void skipVanillaProgressBar(SplashOverlay instance, DrawContext context, int width, int height, int centerX, int bottomY, float progress) {
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;fill(Lnet/minecraft/client/render/RenderLayer;IIIII)V"
      )
   )
   private void replaceStartupBrandOverlay(DrawContext context, RenderLayer layer, int x1, int y1, int x2, int y2, int color) {
   }

   static {
      SPLASH_GIF_RENDERER.preloadEarly();
   }
}
