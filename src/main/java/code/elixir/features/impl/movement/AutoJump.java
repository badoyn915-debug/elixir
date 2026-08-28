package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;

public class AutoJump extends Module {
   public AutoJump() {
      super("AutoJump", "Прыгает автоматически при ауре", ModuleCategory.MOVEMENT);
   }

   public static AutoJump getInstance() {
      return Instance.get(AutoJump.class);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         AttackAura aura = AttackAura.getInstance();
         if (aura != null && aura.isState()) {
            if (aura.getTarget() != null && mc.player.isOnGround()) {
               mc.player.jump();
            }
         }
      }
   }
}
