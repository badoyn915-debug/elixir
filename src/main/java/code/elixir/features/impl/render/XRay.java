package code.elixir.features.impl.render;

import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.geometry.Render3D;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class XRay extends Module {
   private final Map<BlockPos, BlockState> map = new HashMap<>();
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Режим работы для XRay").value("Обновление блоков");
   private final MultiSelectSetting blockTypeSetting = new MultiSelectSetting("Блоки", "Блоки, которые будут отображаться")
      .value("Древние обломки", "Алмазы", "Изумруды", "Железо", "Золото");

   public XRay() {
      super("XRay", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.modeSetting, this.blockTypeSetting});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.map.forEach((key, value) -> {
         if (this.blockTypeSetting.getSelected().toString().toLowerCase().contains(this.getBlockName(value))) {
            Render3D.drawBox(new Box(key), this.getColorByBlock(value), 1.0F);
         }
      });
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent e) {
      this.map.clear();
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      BlockState state = e.state();
      BlockPos pos = e.pos();
      switch (e.type()) {
         case UPDATE:
            if (this.getColorByBlock(state) != -1 && !this.map.containsKey(pos)) {
               this.map.put(pos, state);
            }

            if (this.map.containsKey(pos) && !this.map.get(pos).equals(state)) {
               this.map.remove(pos);
            }
            break;
         case UNLOAD:
            this.map.remove(pos);
      }
   }

   private int getColorByBlock(BlockState block) {
      String var2 = this.getBlockName(block);

      return switch (var2) {
         case "Древние обломки" -> -5868204;
         case "Алмазы" -> -15107199;
         case "Изумруды" -> -12482789;
         case "Железо" -> -9090017;
         case "Золото" -> -3819208;
         default -> -1;
      };
   }

   private String getBlockName(BlockState state) {
      return state.getBlock().asItem().toString().replace("minecraft:", "").replace("_ore", "").replace("_", " ");
   }
}
