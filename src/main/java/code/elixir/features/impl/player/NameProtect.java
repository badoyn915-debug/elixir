package code.elixir.features.impl.player;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.render.TextFactoryEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.managers.event.EventHandler;

public class NameProtect extends Module {
   TextSetting nameSetting = new TextSetting("Имя", "").setText("Protected").setMax(16);
   BooleanSetting friendsSetting = new BooleanSetting("Скрывать друзей", "Скрывает никнеймы друзей").setValue(true);

   public NameProtect() {
      super("NameProtect", "NameProtect", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.nameSetting, this.friendsSetting});
   }

   @EventHandler
   public void onTextFactory(TextFactoryEvent e) {
      e.replaceText(mc.getSession().getUsername(), this.nameSetting.getText());
      if (this.friendsSetting.isValue()) {
         FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), this.nameSetting.getText()));
      }
   }
}
