package code.elixir.mixins;

import code.elixir.events.player.DeathScreenEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DeathScreen.class})
public class DeathScreenMixin {
   @Shadow
   private int ticksSinceDeath;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      SafeEvents.dispatch(() -> new DeathScreenEvent(this.ticksSinceDeath));
   }
}
