package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.AttackEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;

public class NoFriendDamage extends Module {
   public NoFriendDamage() {
      super("NoFriendDamage", "NoFriendDamage", ModuleCategory.COMBAT);
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      e.setCancelled(FriendUtils.isFriend(e.getEntity()));
   }
}
