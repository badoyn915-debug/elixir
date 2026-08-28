package code.elixir.features.impl.misc;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.display.hud.Notifications;
import code.elixir.display.hud.StaffList;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import net.minecraft.text.Text;

public class AutoLeave extends Module {
   private final SelectSetting leaveType = new SelectSetting("Режим", "Позволяет выбрать тип выхода").value("/hub", "Выход с сервера", "/clan home", "/spawn");
   private final MultiSelectSetting triggerSetting = new MultiSelectSetting("Срабатывать на", "Выберите, в каких случаях произойдет выход")
      .value("Игроков", "Админов");
   private final SliderSettings distanceSetting = new SliderSettings("Дистанция", "Максимальная дистанция для активации авто-выхода")
      .setValue(10.0F)
      .range(5, 40)
      .visible(() -> this.triggerSetting.isSelected("Игроков"));

   public AutoLeave() {
      super("AutoLeave", "AutoLeave", ModuleCategory.MISC);
      this.setup(new Setting[]{this.leaveType, this.triggerSetting, this.distanceSetting});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!Network.isPvp()) {
         if (this.triggerSetting.isSelected("Игроков")) {
            mc.world
               .getPlayers()
               .stream()
               .filter(p -> mc.player.distanceTo(p) < this.distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p))
               .findFirst()
               .ifPresent(p -> this.leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
         }

         StaffList staffList = Instance.getDraggable(StaffList.class);
         if (this.triggerSetting.isSelected("Админов") && staffList != null && staffList.hasStaff()) {
            this.leave(Text.of("Стафф на сервере"));
         }
      }
   }

   public void leave(Text text) {
      String var2 = this.leaveType.getSelected();
      switch (var2) {
         case "/hub":
            Notifications.getInstance().addList(Text.of("[AutoLeave] ").copy().append(text), 10000);
            mc.getNetworkHandler().sendChatCommand("hub");
            break;
         case "выход с сервера":
            mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
            break;
         case "/spawn":
            assert mc.player != null;

            mc.player.networkHandler.sendChatMessage("/spawn");
            break;
         case "/clan home":
            assert mc.player != null;

            mc.player.networkHandler.sendChatMessage("/clan home");
      }

      this.setState(false);
   }
}
