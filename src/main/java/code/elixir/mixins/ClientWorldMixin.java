package code.elixir.mixins;

import code.elixir.events.player.EntitySpawnEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientWorld.class})
public class ClientWorldMixin implements QuickImports {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void initHook(CallbackInfo info) {
      SafeEvents.dispatch(() -> new WorldLoadEvent());
   }

   @Inject(
      method = {"addEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void addEntityHook(Entity entity, CallbackInfo ci) {
      if (!PlayerInteractionHelper.nullCheck()) {
         EntitySpawnEvent event = SafeEvents.dispatch(() -> new EntitySpawnEvent(entity));
         if (event != null && event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
