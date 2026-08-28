package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.combat.IdealHitUtils;
import net.minecraft.entity.effect.StatusEffects;

public class AutoSprint extends Module {
   public static int tickStop;
   public final RadioSetting mode = new RadioSetting("Режим", "Режим работы автоспринта", new String[]{"PolarAC", "Легитный", "Обычный"}, "PolarAC");
   private final MultiSelectSetting settings = new MultiSelectSetting("Игнорировать эффект", "Игнорировать эффекты игрока").value("Замедление", "Слепота");
   private final BooleanSetting syncWithAura = new BooleanSetting("Синх с Aura", "Синхронизирует автоспринт со сбросом спринта в Aura").setValue(true);

   public static AutoSprint getInstance() {
      return Instance.get(AutoSprint.class);
   }

   public AutoSprint() {
      super("AutoSprint", "AutoSprint", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.settings, this.syncWithAura});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         boolean isPolarMode = "PolarAC".equals(this.mode.get()) || "Легитный".equals(this.mode.get());
         
         boolean hasHunger = mc.player.getHungerManager().getFoodLevel() > 6.0F || mc.player.getAbilities().allowFlying;
         boolean ignoreBlindness = this.settings.isSelected("Слепота") && mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
         boolean notBlind = !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || ignoreBlindness;
         boolean notSneaking = !mc.player.isSneaking() || mc.player.isSwimming();
         boolean notCollided = !mc.player.horizontalCollision || mc.player.collidedSoftly;
         
         boolean hasForwardInput = mc.player.input != null ? mc.player.input.movementForward > 0.8F : mc.player.forwardSpeed > 0.0F;
         boolean ignoreSlowness = this.settings.isSelected("Замедление") && mc.player.hasStatusEffect(StatusEffects.SLOWNESS);
         boolean shouldStopFromTimer = tickStop > 0 && !ignoreBlindness && !ignoreSlowness;
         boolean syncedWithAura = this.shouldUseAuraSync();

         boolean canSprint = hasHunger && notBlind && notSneaking && notCollided && hasForwardInput && !shouldStopFromTimer;

         if (syncedWithAura && !this.canDivineSprint()) {
            canSprint = false;
         }

         if (canSprint) {
            mc.options.sprintKey.setPressed(true);
            if (!isPolarMode && !mc.player.isSprinting()) {
               mc.player.setSprinting(true);
            }
         } else {
            mc.options.sprintKey.setPressed(false);
            if (!hasHunger || mc.player.isSneaking() || shouldStopFromTimer) {
               if (mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
               }
            }
         }

         if (tickStop > 0) {
            tickStop--;
         }
      }
   }

   private boolean shouldUseAuraSync() {
      if (!this.syncWithAura.isValue()) {
         return false;
      } else {
         AttackAura aura = AttackAura.getInstance();
         return aura != null && aura.isState() && aura.getTarget() != null && aura.isSprintResetEnabled();
      }
   }

   private boolean canDivineSprint() {
      if (mc.player != null && mc.world != null) {
         boolean inCobweb = IdealHitUtils.isInCobweb();
         boolean inLiquid = mc.player.isTouchingWater() || mc.player.isSubmergedInWater();
         return !inCobweb && !mc.player.getAbilities().flying && !inLiquid && !mc.player.hasVehicle()
            ? mc.player.fallDistance <= 0.0F && mc.player.isOnGround()
            : true;
      } else {
         return false;
      }
   }

   public boolean shouldKeepSprintInWater() {
      return false;
   }
}
