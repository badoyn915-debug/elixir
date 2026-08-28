package code.elixir.mixins;

import code.elixir.features.impl.misc.Panic;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientBrandRetriever.class})
public class ClientBrandRetrieverMixin {
   @Inject(
      method = {"getClientModName"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
      if (Panic.isPanicMode()) {
         cir.setReturnValue("vanilla");
      } else {
         cir.setReturnValue("vanilla");
      }
   }
}
