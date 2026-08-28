package code.elixir.mixins;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public class TitleScreenIASMixin {
   @Unique
   private static ButtonWidget accountSwitcherButton;

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo ci) {
      TitleScreen screen = (TitleScreen)(Object)this;
      IScreen si = (IScreen)screen;
      si.getDrawables().add(accountSwitcherButton);
      si.getSelectables().add(accountSwitcherButton);
      si.getChildren().add(accountSwitcherButton);
   }
}
