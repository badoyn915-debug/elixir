package code.elixir.mixins;

import code.elixir.display.hud.CustomHudOverlay;
import code.elixir.display.hud.HudEditorOverlay;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChatScreen.class})
public class ChatScreenMixin extends Screen implements QuickImports {
   protected ChatScreenMixin() {
      super(Text.empty());
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void onRenderAboveChat(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CustomHudOverlay.render(context, mc.getRenderTickCounter(), true);
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState()) {
         HudEditorOverlay.getInstance().render(context, mouseX, mouseY, delta);
      }
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (HudEditorOverlay.getInstance().mouseClicked(mouseX, mouseY, button)) {
         cir.setReturnValue(true);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return HudEditorOverlay.getInstance().mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
   }

   @Inject(
      method = {"mouseScrolled"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
      if (HudEditorOverlay.getInstance().mouseScrolled(mouseX, mouseY, verticalAmount)) {
         cir.setReturnValue(true);
      }
   }
}
