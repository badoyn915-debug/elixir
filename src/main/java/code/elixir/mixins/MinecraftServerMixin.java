package code.elixir.mixins;

import code.elixir.Elixir;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.file.exception.FileProcessingException;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftServer.class})
public class MinecraftServerMixin {
   @Inject(
      method = {"shutdown"},
      at = {@At("HEAD")}
   )
   public void shutdown(CallbackInfo ci) {
      if (Elixir.getInstance().isInitialized()) {
         try {
            Elixir.getInstance().getFileController().saveFiles();
         } catch (FileProcessingException var3) {
            Logger.error("Error occurred while saving files: " + var3.getMessage());
         }
      }
   }
}
