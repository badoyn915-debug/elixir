package code.elixir.mixins;

import code.elixir.events.render.FogEvent;
import code.elixir.features.impl.render.NoRender;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.color.ColorAssist;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BackgroundRenderer.class})
public class BackGroundRendererMixin {
   @Inject(
      method = {"getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && entity instanceof LivingEntity living) {
         Object modifier = info.getReturnValue();
         if (modifier != null) {
            if (noRender.shouldHideBadEffects() && living.hasStatusEffect(StatusEffects.BLINDNESS)) {
               info.setReturnValue(null);
               return;
            }

            if (noRender.shouldHideDarkness() && living.hasStatusEffect(StatusEffects.DARKNESS)) {
               info.setReturnValue(null);
               return;
            }
         }
      }
   }

   @Inject(
      method = {"getFogColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getFogColorHook(
      Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      FogEvent event = SafeEvents.dispatch(() -> new FogEvent());
      if (event != null && event.isCancelled()) {
         int color = event.getColor();
         cir.setReturnValue(new Vector4f(ColorAssist.redf(color), ColorAssist.greenf(color), ColorAssist.bluef(color), ColorAssist.alphaf(color)));
      }
   }

   @Inject(
      method = {"applyFog"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void modifyFog(
      Camera camera, FogType fogType, Vector4f vector4f, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir
   ) {
      FogEvent event = SafeEvents.dispatch(() -> new FogEvent());
      if (event != null && event.isCancelled()) {
         int color = event.getColor();
         float fogEnd = event.getDistance();
         float fogStart = Math.max(0.1F, fogEnd * (1.0F - event.getDensity() / 100.0F));
         cir.setReturnValue(
            new Fog(
               fogStart, fogEnd, FogShape.CYLINDER, ColorAssist.redf(color), ColorAssist.greenf(color), ColorAssist.bluef(color), ColorAssist.alphaf(color)
            )
         );
      }
   }
}
