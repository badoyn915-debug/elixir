package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class ElytraWhatsapp extends Module {
   private static final double FREEZE_DISTANCE = 3.0;
   private static final double INTERP_Y = 0.5;
   private boolean releasedFreeze;

   public ElytraWhatsapp() {
      super("ElytraWhatsapp", "Elytra Whatsapp", ModuleCategory.MOVEMENT);
      this.setup(new Setting[0]);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.state && !PlayerInteractionHelper.nullCheck()) {
         AttackAura aura = Instance.get(AttackAura.class);
         LivingEntity target = aura != null ? aura.getTarget() : null;
         if (target == null) {
            if (!this.releasedFreeze) {
               mc.player.setVelocity(mc.player.getVelocity().multiply(1.0, 0.0, 1.0));
               this.releasedFreeze = true;
            }
         } else {
            this.releasedFreeze = false;
            boolean auraFar = aura == null
               || mc.player.distanceTo(target) >= 6.0F
               || aura.getTarget() != null && mc.player.distanceTo(aura.getTarget()) == 0.0F;
            Vec3d targetCenter = target.getBoundingBox().getCenter().add(0.0, (target.getY() - target.prevY) * 0.5, 0.0);
            if (mc.player.isGliding() && mc.player.getEyePos().distanceTo(targetCenter) < 3.0 && !auraFar) {
               mc.player.setVelocity(Vec3d.ZERO);
               mc.player.setNoGravity(true);
            } else {
               mc.player.setNoGravity(false);
            }
         }
      }
   }

   @Override
   public void deactivate() {
      if (mc.player != null) {
         this.releasedFreeze = false;
         mc.player.setNoGravity(false);
      }

      super.deactivate();
   }
}
