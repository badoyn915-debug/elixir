package code.elixir.utils.network;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public class NetworkUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Set<Packet<?>> silentPackets = new HashSet<>();

   public static void sendSilentPacket(Packet<?> packet) {
      if (mc.player != null && mc.player.networkHandler != null) {
         silentPackets.add(packet);
         mc.player.networkHandler.sendPacket(packet);
      }
   }

   public static void sendPacket(Packet<?> packet) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(packet);
      }
   }

   public static boolean isSilent(Packet<?> packet) {
      return silentPackets.contains(packet);
   }

   public static void removeSilent(Packet<?> packet) {
      silentPackets.remove(packet);
   }

   public static void clearSilent() {
      silentPackets.clear();
   }
}
