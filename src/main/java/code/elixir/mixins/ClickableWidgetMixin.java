package code.elixir.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClickableWidget.class})
public abstract class ClickableWidgetMixin {
   @Unique
   private float Elixir$hoverAnim;
   @Unique
   private float Elixir$focusAnim;
   @Unique
   private boolean Elixir$pushed;

   @Shadow
   public abstract boolean isHovered();

   @Shadow
   public abstract int getX();

   @Shadow
   public abstract int getY();

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract int getHeight();

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void Elixir$animateWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      boolean isTextField = (Object)this instanceof TextFieldWidget;
      boolean focused = isTextField && ((TextFieldWidget)(Object)this).isFocused();
      float targetHover = this.isHovered() ? 1.0F : 0.0F;
      this.Elixir$hoverAnim = this.Elixir$hoverAnim + (targetHover - this.Elixir$hoverAnim) * 0.22F;
      this.Elixir$focusAnim = this.Elixir$focusAnim + ((focused ? 1.0F : 0.0F) - this.Elixir$focusAnim) * 0.14F;
      float hoverLift = -1.4F * this.Elixir$hoverAnim;
      float focusScale = isTextField ? 1.0F + 0.006F * this.Elixir$focusAnim : 1.0F;
      if (Math.abs(hoverLift) < 0.01F && Math.abs(focusScale - 1.0F) < 0.001F) {
         this.Elixir$pushed = false;
      } else {
         float centerX = this.getX() + this.getWidth() / 2.0F;
         float centerY = this.getY() + this.getHeight() / 2.0F;
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, hoverLift, 0.0F);
         context.getMatrices().translate(centerX, centerY, 0.0F);
         context.getMatrices().scale(focusScale, focusScale, 1.0F);
         context.getMatrices().translate(-centerX, -centerY, 0.0F);
         this.Elixir$pushed = true;
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void Elixir$restoreWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.Elixir$pushed) {
         context.getMatrices().pop();
         this.Elixir$pushed = false;
      }
   }
}
