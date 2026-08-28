package code.elixir.features.impl.render;

import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.WorldChunk;

public class BlockESP extends Module {
   ColorSetting color = new ColorSetting("Цвет", "Цвет подсветки блоков").value(ColorAssist.getColor(255, 0, 0, 255));
   SliderSettings range = new SliderSettings("Радиус", "Радиус поиска блоков").range(1, 128).setValue(32.0F);
   BooleanSetting notifyInChat = new BooleanSetting("Уведомления", "Показывать координаты найденных блоков в чате").setValue(false);
   Set<String> blocksToHighlight = new CopyOnWriteArraySet<>();
   Map<BlockPos, BlockState> renderBlocks = new HashMap<>();
   Set<BlockPos> notifiedBlocks = new CopyOnWriteArraySet<>();
   long lastScanTime = 0L;
   int checkCounter = 0;

   public BlockESP() {
      super("BlockESP", "BlockESP", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.color, this.range, this.notifyInChat});
   }

   public Set<String> getBlocksToHighlight() {
      return this.blocksToHighlight;
   }

   @Override
   public void activate() {
      super.activate();
      this.notifiedBlocks.clear();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.renderBlocks.clear();
      this.notifiedBlocks.clear();
   }

   @EventHandler
   public void onRender3D(WorldRenderEvent event) {
      if (!this.state || mc.world == null || mc.player == null) {
         this.renderBlocks.clear();
      } else if (this.blocksToHighlight.isEmpty()) {
         this.renderBlocks.clear();
      } else {
         BlockPos playerPos = mc.player.getBlockPos();
         long currentTime = System.nanoTime() / 1000000L;
         if (currentTime - this.lastScanTime >= 2000L) {
            this.renderBlocks.clear();
            int chunkRange = 2;
            int yRange = 48;

            for (int x = -chunkRange; x <= chunkRange; x++) {
               for (int z = -chunkRange; z <= chunkRange; z++) {
                  int chunkX = (playerPos.getX() >> 4) + x;
                  int chunkZ = (playerPos.getZ() >> 4) + z;
                  WorldChunk chunk;
                  if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ) && (chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ)) != null) {
                     int cx = chunk.getPos().x << 4;
                     int cz = chunk.getPos().z << 4;

                     for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                           int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - yRange);
                           int maxY = Math.min(mc.world.getTopY(Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + yRange);

                           for (int by = minY; by <= maxY; by++) {
                              BlockPos pos2 = new BlockPos(cx + bx, by, cz + bz);
                              double dist = mc.player.squaredDistanceTo(pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5);
                              String blockName;
                              if (!(dist > this.range.getValue() * this.range.getValue())
                                 && this.blocksToHighlight.contains(blockName = Registries.BLOCK.getId(mc.world.getBlockState(pos2).getBlock()).toString())) {
                                 this.renderBlocks.put(pos2.toImmutable(), mc.world.getBlockState(pos2));
                                 if (this.notifyInChat.isValue() && !this.notifiedBlocks.contains(pos2)) {
                                    this.notifyBlockFound(pos2, blockName);
                                    this.notifiedBlocks.add(pos2);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.lastScanTime = currentTime;
            this.checkCounter = 0;
         }

         if (this.checkCounter % 5 == 0) {
            int nearChunkRange = 1;

            for (int x = -nearChunkRange; x <= nearChunkRange; x++) {
               for (int zx = -nearChunkRange; zx <= nearChunkRange; zx++) {
                  int chunkX = (playerPos.getX() >> 4) + x;
                  int chunkZ = (playerPos.getZ() >> 4) + zx;
                  WorldChunk chunk;
                  if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ) && (chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ)) != null) {
                     int cx = chunk.getPos().x << 4;
                     int cz = chunk.getPos().z << 4;

                     for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                           int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - 24);
                           int maxY = Math.min(mc.world.getTopY(Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + 24);

                           for (int byx = minY; byx <= maxY; byx++) {
                              BlockPos pos3 = new BlockPos(cx + bx, byx, cz + bz);
                              double dist = mc.player.squaredDistanceTo(pos3.getX() + 0.5, pos3.getY() + 0.5, pos3.getZ() + 0.5);
                              String blockName;
                              if (!(dist > 16.0)
                                 && this.blocksToHighlight.contains(blockName = Registries.BLOCK.getId(mc.world.getBlockState(pos3).getBlock()).toString())
                                 && !this.renderBlocks.containsKey(pos3)) {
                                 this.renderBlocks.put(pos3.toImmutable(), mc.world.getBlockState(pos3));
                                 if (this.notifyInChat.isValue() && !this.notifiedBlocks.contains(pos3)) {
                                    this.notifyBlockFound(pos3, blockName);
                                    this.notifiedBlocks.add(pos3);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.checkCounter % 60 == 0) {
            this.renderBlocks.entrySet().removeIf(entry -> {
               BlockPos pos = entry.getKey();
               Block blockx = mc.world.getBlockState(pos).getBlock();
               String blockNamex = Registries.BLOCK.getId(blockx).toString();
               boolean shouldRemove = !this.blocksToHighlight.contains(blockNamex);
               if (shouldRemove) {
                  this.notifiedBlocks.remove(pos);
               }

               return shouldRemove;
            });
         }

         this.checkCounter++;
         this.renderBlocks.forEach((pos, state) -> Render3D.drawBox(new Box(pos), this.color.getColor(), 1.0F));
      }
   }

   private void notifyBlockFound(BlockPos pos, String blockName) {
      if (mc.player != null) {
         mc.player
            .sendMessage(
               ChatMessage.brandmessage().append("Найден блок " + blockName + " на координатах -> " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
               false
            );
      }
   }

   private String getBlockName(BlockState state) {
      return state.getBlock().asItem().toString().replace("minecraft:", "").replace("_ore", "").replace("_", " ");
   }
}
