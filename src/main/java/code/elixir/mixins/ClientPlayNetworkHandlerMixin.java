package code.elixir.mixins;

import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.events.chat.ChatEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.path.BaritoneHelper;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public class ClientPlayNetworkHandlerMixin implements QuickImports {
   @Inject(
      method = {"onChunkData"},
      at = {@At("RETURN")}
   )
   private void onChunkDataHook(ChunkDataS2CPacket packet, CallbackInfo ci) {
      if (mc != null && mc.world != null) {
         try {
            WorldChunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());
            if (chunk != null) {
               this.scanChunk(chunk, BlockUpdateEvent.Type.LOAD);
            }
         } catch (Throwable ignored) {
         }
      }
   }

   @Inject(
      method = {"onUnloadChunk"},
      at = {@At("HEAD")}
   )
   private void onUnloadChunkHook(UnloadChunkS2CPacket packet, CallbackInfo ci) {
      if (mc != null && mc.world != null) {
         try {
            WorldChunk chunk = mc.world.getChunk(packet.pos().x, packet.pos().z);
            if (chunk != null) {
               this.scanChunk(chunk, BlockUpdateEvent.Type.UNLOAD);
            }
         } catch (Throwable ignored) {
         }
      }
   }

   @Inject(
      method = {"sendChatMessage(Ljava/lang/String;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendChatMessage(String string, CallbackInfo ci) {
      if (BaritoneHelper.handleChatCommand(string)) {
         ci.cancel();
      } else {
         ChatEvent event = SafeEvents.dispatch(() -> new ChatEvent(string));
         if (event != null && event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Unique
   private void scanChunk(WorldChunk worldChunk, BlockUpdateEvent.Type type) {
      if (worldChunk == null || worldChunk.getPos() == null) {
         return;
      }
      CompletableFuture.runAsync(() -> {
         try {
            int startX = worldChunk.getPos().getStartX();
            int startZ = worldChunk.getPos().getStartZ();
            ArrayList<CompletableFuture<Void>> sectionFutures = new ArrayList<>();
            int sectionIndex = 0;

            while (sectionIndex <= worldChunk.getHighestNonEmptySection()) {
               int finalSectionIndex = sectionIndex++;
               sectionFutures.add(CompletableFuture.runAsync(() -> {
                  try {
                     ChunkSection section = worldChunk.getSection(finalSectionIndex);
                     if (section == null) {
                        return;
                     }

                     for (int sectionY = 0; sectionY < 16; sectionY++) {
                        int y = finalSectionIndex + (worldChunk.getBottomY() >> 4) << 4 | sectionY;

                        for (int x = 0; x < 16; x++) {
                           for (int z = 0; z < 16; z++) {
                              BlockState blockState = type.equals(BlockUpdateEvent.Type.LOAD) ? section.getBlockState(x, sectionY, z) : Blocks.AIR.getDefaultState();
                              BlockPos pos = new BlockPos(startX | x, y, startZ | z);
                              SafeEvents.dispatch(() -> new BlockUpdateEvent(blockState, pos, type));
                           }
                        }
                     }
                  } catch (Throwable ignored) {
                  }
               }));
            }

            CompletableFuture.allOf(sectionFutures.toArray(new CompletableFuture[0])).join();
         } catch (Throwable ignored) {
         }
      });
   }
}
