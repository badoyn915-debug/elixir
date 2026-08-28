package code.elixir.utils.client.stealth;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

public final class ModStealth {
   public static final String MOD_ID = "renderextras";
   public static final String LEGACY_MOD_ID = "elixir";
   public static final String CLIENT_BRAND = "vanilla";
   private static final String[] BLOCKED_CHANNEL_FRAGMENTS = new String[]{
      "elixir", "renderextras", "elixir", "modlist", "mod_list", "mods_list", "fabric-mod", "fabricloader", "dreamix", "vmbridge"
   };

   public static boolean shouldBlockOutgoing(Packet<?> packet) {
      if (!(packet instanceof CustomPayloadC2SPacket customPayload)) {
         return false;
      } else {
         CustomPayload payload = customPayload.payload();
         if (payload == null) {
            return false;
         } else {
            Id id = payload.getId();
            if (id == null) {
               return false;
            } else {
               Identifier identifier = id.id();
               return identifier == null ? false : containsBlockedMarker(identifier.toString()) || containsBlockedMarker(payload.getClass().getName());
            }
         }
      }
   }

   private static boolean containsBlockedMarker(String value) {
      if (value != null && !value.isEmpty()) {
         String lower = value.toLowerCase();

         for (String fragment : BLOCKED_CHANNEL_FRAGMENTS) {
            if (lower.contains(fragment)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private ModStealth() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
