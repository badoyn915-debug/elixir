package code.elixir.mixins;

import code.elixir.events.container.HandledScreenEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HandledScreen.class})
public abstract class HandledScreenMixin {
   @Shadow
   public int backgroundWidth;
   @Shadow
   public int backgroundHeight;
   @Shadow
   @Nullable
   protected Slot focusedSlot;

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      SafeEvents.dispatch(() -> new HandledScreenEvent(context, this.focusedSlot, this.backgroundWidth, this.backgroundHeight, mouseX, mouseY, delta));
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof InventoryScreen) {
         for (Element element : ((IScreen)this).getChildren()) {
            if ("code.elixir.display.widgets.ClearButtonWidget".equals(element.getClass().getName()) && element.mouseClicked(mouseX, mouseY, button)) {
               cir.setReturnValue(true);
               cir.cancel();
               return;
            }
         }
      }
   }
}
