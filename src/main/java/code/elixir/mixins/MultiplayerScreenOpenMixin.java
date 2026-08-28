package code.elixir.mixins;

import code.elixir.common.proxy.Config;
import code.elixir.common.proxy.Proxy;
import code.elixir.common.proxy.ProxyScreens;
import code.elixir.common.proxy.ProxyServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MultiplayerScreen.class})
public class MultiplayerScreenOpenMixin {
   @Inject(
      method = {"init()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"
      )}
   )
   public void multiplayerGuiOpen(CallbackInfo ci) {
      try {
         String playerName = MinecraftClient.getInstance().getSession().getUsername();
         if (!playerName.equals(Config.lastPlayerName)) {
            Config.lastPlayerName = playerName;
            ProxyServer.proxy = Config.accounts.containsKey(playerName)
               ? Config.accounts.get(playerName)
               : (Config.accounts.containsKey("") ? Config.accounts.get("") : new Proxy());
         }

         MultiplayerScreen ms = (MultiplayerScreen)(Object)this;
         ProxyServer.proxyMenuButton = ButtonWidget.builder(Text.literal("Proxy"), buttonWidget -> ProxyScreens.open(ms)).dimensions(5, 5, 120, 20).build();
         IScreen si = (IScreen)ms;
         si.getDrawables().add(ProxyServer.proxyMenuButton);
         si.getSelectables().add(ProxyServer.proxyMenuButton);
         si.getChildren().add(ProxyServer.proxyMenuButton);
      } catch (Throwable var5) {
         System.err.println("Failed to initialize proxy menu on multiplayer screen");
         var5.printStackTrace();
      }
   }
}
