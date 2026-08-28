package code.elixir.features.impl.player;

import code.elixir.events.block.BlockBreakingEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FastBreak extends Module {
   public FastBreak() {
      super("FastBreak", "FastBreak", ModuleCategory.PLAYER);
   }

   @EventHandler
   public void onBlockBreaking(BlockBreakingEvent e) {
      BlockPos blockPos = e.blockPos();
      Direction direction = e.direction();
      if (mc.interactionManager.currentBreakingProgress >= 0.5) {
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction));
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, blockPos, direction));
      }
   }
}
