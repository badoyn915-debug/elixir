package code.elixir.features.impl.player;

import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import net.minecraft.item.Items;

public class ClickPearl extends Module {
   private final BindSetting keySetting = new BindSetting("Кнопка нажатия", "Кнопка для броска эндер-жемчуга");
   private boolean queued;

   public ClickPearl() {
      super("ClickPearl", "ClickPearl", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.keySetting});
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (event.isKeyDown(this.keySetting.getKey())) {
         this.queued = true;
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && this.queued) {
         if (ItemTask.getCooldownProgress(Items.ENDER_PEARL) > 0.0F) {
            this.queued = false;
         } else {
            InventoryTask.swapAndUse(Items.ENDER_PEARL, true);
            this.queued = false;
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.queued = false;
   }
}
