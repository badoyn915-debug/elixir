package code.elixir.common.repository.box;

import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.projection.Projection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class BoxESPRepository implements QuickImports, QuickLogger {
   private final Map<BlockPos, Pair<VoxelShape, Integer>> boxes = new HashMap<>();
   public final Map<EntityType<?>, Integer> entities = new HashMap<>();
   public final Map<Block, Integer> blocks = new HashMap<>();
   public boolean drawFill = true;

   public BoxESPRepository(EventManager eventManager) {
      EventManager.register(this);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.boxes.forEach((pos, pair) -> {
         if (this.drawFill) {
            Render3D.drawShape(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0F);
         } else {
            Render3D.drawShapeAlternative(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0F, false, false);
         }
      });
      PlayerInteractionHelper.streamEntities().filter(ent -> this.entities.containsKey(ent.getType()) && ent != mc.player).forEach(ent -> {
         int entityColor = this.entities.get(ent.getType());
         int color = entityColor == 0 ? ColorAssist.getClientColor() : entityColor;
         Box box = ent.getBoundingBox().offset(Calculate.interpolate(ent).subtract(ent.getPos()));
         if (Projection.canSee(box)) {
            Render3D.drawBox(box, color, 1.0F);
         }
      });
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent e) {
      this.boxes.clear();
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      BlockPos pos = e.pos();
      BlockState state = e.state();
      Block block = state.getBlock();
      switch (e.type()) {
         case LOAD:
            if (this.blocks.containsKey(block)) {
               this.putBox(pos, state, block);
            }
            break;
         case UPDATE:
            if (this.blocks.containsKey(block) && !this.boxes.containsKey(pos)) {
               this.putBox(pos, state, block);
            }

            if (this.boxes.containsKey(pos) && (state.isAir() || !((VoxelShape)this.boxes.get(pos).getLeft()).equals(state.getOutlineShape(mc.world, pos)))) {
               this.boxes.remove(pos);
            }
            break;
         case UNLOAD:
            this.boxes.remove(pos);
      }
   }

   private void putBox(BlockPos pos, BlockState state, Block block) {
      VoxelShape shape = state.getOutlineShape(mc.world, pos);
      int blockColor = this.blocks.get(block);
      int color = blockColor == 0 ? ColorAssist.replAlpha(state.getMapColor(mc.world, pos).color, 1.0F) : blockColor;
      this.boxes.put(pos, new Pair(shape, color));
   }
}
