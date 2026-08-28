package code.elixir.mixins;

import code.elixir.display.screens.EmotionWheelScreen;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.mainmenu.MainMenu;
import code.elixir.events.chat.ChatEvent;
import code.elixir.features.impl.combat.autoswapUtil.AutoSwapWheelScreen;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.render.MainMenuSkyRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Screen.class})
public class ScreenMixin {
   private static final CubeMapRenderer CUSTOM_PANORAMA_RENDERER = new CubeMapRenderer(Identifier.of("minecraft", "panorama/panorama"));
   private static final Identifier CUSTOM_MENU_BACKGROUND = Identifier.of("minecraft", "textures/gui/background.png");

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void elixir$restoreGuiRenderState(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void elixir$restoreGuiRenderStateTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
   }

   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V",
         remap = false,
         ordinal = 1
      )},
      method = {"handleTextClick"},
      cancellable = true
   )
   public void handleCustomClickEvent(Style style, CallbackInfoReturnable<Boolean> cir) {
      ClickEvent clickEvent = style.getClickEvent();
      if (clickEvent != null) {
         SafeEvents.dispatch(() -> new ChatEvent(clickEvent.getValue()));
         cir.setReturnValue(true);
         cir.cancel();
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void disableBackgroundBlurAndDimming(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof MenuScreen || (Object)this instanceof EmotionWheelScreen || (Object)this instanceof AutoSwapWheelScreen) {
         ci.cancel();
      } else if (MinecraftClient.getInstance().world != null) {
         ci.cancel();
      } else {
         if (!((Object)this instanceof MainMenu)) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            MainMenuSkyRenderer.render(context, width, height);
            context.fill(0, 0, width, height, 1711276032);
            ci.cancel();
         }
      }
   }
}
