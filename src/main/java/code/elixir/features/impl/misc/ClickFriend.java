package code.elixir.features.impl.misc;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ClickFriend extends Module {
   private final BindSetting friendBind = new BindSetting("Добавить друга", "Добавить/удалить друга");

   public ClickFriend() {
      super("ClickFriend", "Click Friend", ModuleCategory.MISC);
      this.setup(new Setting[]{this.friendBind});
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.friendBind.getKey())) {
         HitResult class_2392 = mc.crosshairTarget;
         EntityHitResult result;
         if (mc.crosshairTarget instanceof EntityHitResult && (result = (EntityHitResult)class_2392).getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)result.getEntity();
            if (FriendUtils.isFriend(player)) {
               FriendUtils.removeFriend(player);
            } else {
               FriendUtils.addFriend(player);
            }
         }
      }
   }
}
