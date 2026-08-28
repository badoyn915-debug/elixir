package code.elixir.mixins;

import code.elixir.utils.client.timer.IRenderTickCounterDynamic;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Dynamic.class})
public class RenderTickCounterDynamicMixin implements IRenderTickCounterDynamic {
   @Shadow
   @Final
   @Mutable
   private float tickTime;
   @Unique
   private float speedMultiplier = 1.0F;
   @Unique
   private final float baseTickTime = 50.0F;

   @Override
   public void setSpeedMultiplier(float speed) {
      this.speedMultiplier = Math.max(0.1F, Math.min(speed, 10.0F));
      this.tickTime = 50.0F / this.speedMultiplier;
   }

   @Override
   public float getSpeedMultiplier() {
      return this.speedMultiplier;
   }
}
