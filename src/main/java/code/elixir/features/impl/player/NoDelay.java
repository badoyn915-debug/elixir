package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.item.Items;

public class NoDelay extends Module {
   public final MultiSelectSetting ignoreSetting = new MultiSelectSetting("Убирать заддержку у", "Разрешает выбранные вами действия")
      .value("Прыжка", "ПКМ", "Ломания блока", "Пузырек опыта");

   public static NoDelay getInstance() {
      return Instance.get(NoDelay.class);
   }

   public NoDelay() {
      super("NoDelay", "NoDelay", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.ignoreSetting});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.ignoreSetting.isSelected("Ломания блока")) {
         mc.interactionManager.blockBreakingCooldown = 0;
      }

      if (this.ignoreSetting.isSelected("Прыжка")) {
         mc.player.jumpingCooldown = 0;
      }

      if (this.ignoreSetting.isSelected("ПКМ") && (mc.player == null || !mc.player.isUsingItem())) {
         mc.itemUseCooldown = 0;
      }

      if (this.ignoreSetting.isSelected("Пузырек опыта")
         && mc.player != null
         && !mc.player.isUsingItem()
         && mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
         mc.itemUseCooldown = 0;
      }
   }
}
