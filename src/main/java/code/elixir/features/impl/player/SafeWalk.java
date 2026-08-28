package code.elixir.features.impl.player;

import code.elixir.events.player.InputEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class SafeWalk extends Module {
   public SafeWalk() {
      super("SafeWalk", ModuleCategory.PLAYER);
      this.setup(new Setting[0]);
   }

   @EventHandler
   public void onInput(InputEvent e) {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = mc.player.getBlockPos().down();
         boolean atEdge = mc.world.getBlockState(pos).getBlock() == Blocks.AIR && mc.player.isOnGround();
         if (atEdge) {
            e.setSneak(true);
         }
      }
   }
}
