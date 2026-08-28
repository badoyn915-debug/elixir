package code.elixir.mixins;

import code.elixir.utils.interactions.path.BaritoneHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public class ChatHudMixin {
   @Inject(
      method = {"addMessage(Lnet/minecraft/text/Text;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAddMessage(Text message, CallbackInfo ci) {
      if (BaritoneHelper.isSuppressChat() && BaritoneHelper.isBaritoneMessage(message.getString())) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAddHudLine(ChatHudLine message, CallbackInfo ci) {
      if (BaritoneHelper.isSuppressChat() && BaritoneHelper.isBaritoneMessage(message.content().getString())) {
         ci.cancel();
      }
   }
}
