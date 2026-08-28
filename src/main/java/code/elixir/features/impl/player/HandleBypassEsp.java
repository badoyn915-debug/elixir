package code.elixir.features.impl.player;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleBypassEsp extends Module {
   private static final Logger log = LoggerFactory.getLogger(HandleBypassEsp.class);
   private final Set<ChunkPos> haha = new HashSet<>();

   public HandleBypassEsp() {
      super("HandleBypassEsp", ModuleCategory.COMBAT);
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (mc.world != null && mc.player != null && !event.isSend()) {
         if (event.getPacket() instanceof ChunkDataS2CPacket packet) {
            ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
            if (this.haha.add(chunkPos)) {
               mc.execute(
                  () -> {
                     for (Entity entity : mc.world.getEntities()) {
                        PlayerEntity player;
                        if (entity instanceof PlayerEntity
                           && (player = (PlayerEntity)entity) != mc.player
                           && new ChunkPos(player.getBlockPos()).equals(chunkPos)) {
                           String var4x = player.getName().getString();
                        }
                     }
                  }
               );
            }
         }
      }
   }

   @Override
   public void deactivate() {
      this.haha.clear();
      super.deactivate();
   }
}
