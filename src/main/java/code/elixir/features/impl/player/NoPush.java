package code.elixir.features.impl.player;

import code.elixir.events.block.PushEvent;
import code.elixir.events.player.PlayerCollisionEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class NoPush extends Module {
   private final MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "Разрешает выбранные вами действия")
      .value("Вода", "Коллизия блоков", "Коллизия энтити", "Рыхлый снег", "Сладкие ягоды");

   public NoPush() {
      super("NoPush", "NoPush", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.ignoreSetting});
   }

   @EventHandler
   public void onPush(PushEvent e) {
      switch (e.getType()) {
         case COLLISION:
            e.setCancelled(this.ignoreSetting.isSelected("Коллизия энтити"));
            break;
         case WATER:
            e.setCancelled(this.ignoreSetting.isSelected("Вода"));
            break;
         case BLOCK:
            e.setCancelled(this.ignoreSetting.isSelected("Коллизия блоков"));
      }
   }

   @EventHandler
   public void onPlayerCollision(PlayerCollisionEvent e) {
      Block block = e.getBlock();
      if (block.equals(Blocks.POWDER_SNOW)) {
         e.setCancelled(this.ignoreSetting.isSelected("Рыхлый снег"));
      } else if (block.equals(Blocks.SWEET_BERRY_BUSH)) {
         e.setCancelled(this.ignoreSetting.isSelected("Сладкие ягоды"));
      }
   }
}
