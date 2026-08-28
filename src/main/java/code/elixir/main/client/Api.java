package code.elixir.main.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public interface Api {
   MinecraftClient mc = MinecraftClient.getInstance();

   static void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }
}
