package code.elixir.mixins;

import code.elixir.utils.display.render.GuiRenderState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DrawContext.class})
public class DrawContextMixin {
   @Inject(
      method = {"drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaTextString(
      TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir
   ) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaOrderedText(
      TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir
   ) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaShadowString(TextRenderer textRenderer, String text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaShadowText(TextRenderer textRenderer, Text text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaShadowOrderedText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
      GuiRenderState.restoreFontAtlasesNearest();
   }

   @Inject(
      method = {"drawTextWithBackground"},
      at = {@At("HEAD")}
   )
   private void elixir$beforeVanillaTextBackground(
      TextRenderer textRenderer, Text text, int x, int y, int width, int color, CallbackInfoReturnable<Integer> cir
   ) {
      GuiRenderState.restoreFontAtlasesNearest();
   }
}
