package code.elixir.mixins;

import code.elixir.common.proxy.Proxy;
import code.elixir.common.proxy.ProxyServer;
import code.elixir.events.packet.PacketEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.client.stealth.ModStealth;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetSocketAddress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   @Unique
   private static final ThreadLocal<Boolean> elixir$sendingChestBypassPosition = ThreadLocal.withInitial(() -> false);

   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void handlePacketPre(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      PacketEvent packetEvent = SafeEvents.dispatch(() -> new PacketEvent(packet, PacketEvent.Type.RECEIVE));
      if (packetEvent != null && packetEvent.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendPre(Packet<?> packet, CallbackInfo info) {
      if (ModStealth.shouldBlockOutgoing(packet)) {
         info.cancel();
      } else {
         PacketEvent packetEvent = SafeEvents.dispatch(() -> new PacketEvent(packet, PacketEvent.Type.SEND));
         if (packetEvent != null && packetEvent.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"addHandlers"},
      at = {@At("RETURN")}
   )
   private static void addHandlersHook(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
      Proxy proxy = ProxyServer.proxy;
      if (proxy != null && ProxyServer.proxyEnabled && side == NetworkSide.CLIENTBOUND && !local) {
         pipeline.addFirst(new ChannelHandler[]{new Socks5ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username, proxy.password)});
         ProxyServer.lastUsedProxy = proxy;
      }
   }
}
