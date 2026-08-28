package code.elixir.display.hud;

import code.elixir.events.render.DrawEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.GuiRenderState;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CustomHudOverlay implements QuickImports {
   private CustomHudOverlay() {
   }

   public static void render(DrawContext context, RenderTickCounter tickCounter, boolean renderDraggables) {
      if (context != null && drawEngine != null && tickCounter != null && mc.player != null && mc.world != null) {
         if (mc.currentScreen == null || PlayerInteractionHelper.isChat(mc.currentScreen)) {
            if (Hud.blur.isValue()) {
               KawaseBlur.INSTANCE.render(context.getMatrices(), 2, 4.0F);
            }

            SafeEvents.dispatch(() -> new DrawEvent(context, drawEngine, tickCounter.getTickDelta(false)));
            Render2D.onRender(context);
            context.draw();
            if (renderDraggables) {
               HudDraggableRenderer.render(context);
            }

            context.draw();
            GuiRenderState.restoreAfterCustomDraw();
         }
      }
   }
}
