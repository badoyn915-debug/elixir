package code.elixir.utils.features.aura.striking;

import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.Turns;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

public class StrikerConstructor implements QuickImports {
   private final StrikeManager attackHandler = new StrikeManager();

   public void onPacket(PacketEvent event) {
      this.attackHandler.onPacket(event);
   }

   public void onUsingItem(UsingItemEvent event) {
      this.attackHandler.onUsingItem(event);
   }

   public void performAttack(StrikerConstructor.AttackPerpetratorConfigurable configurable) {
      this.attackHandler.handleAttack(configurable);
   }

   public StrikeManager getAttackHandler() {
      return this.attackHandler;
   }

   public static class AttackPerpetratorConfigurable {
      private final LivingEntity target;
      private final Turns angle;
      private final float maximumRange;
      private final boolean onlyCritical;
      private final boolean shouldBreakShield;
      private final boolean shouldUnPressShield;
      private final boolean eatAndAttack;
      private final Box box;
      private final RadioSetting aimMode;
      private final String sprintResetOverride;
      private final Boolean smartCritsOverride;

      public AttackPerpetratorConfigurable(LivingEntity target, Turns angle, float maximumRange, List<String> options, RadioSetting aimMode, Box box) {
         this(target, angle, maximumRange, options, aimMode, box, null, null);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target, Turns angle, float maximumRange, List<String> options, RadioSetting aimMode, Box box, String sprintResetOverride
      ) {
         this(target, angle, maximumRange, options, aimMode, box, sprintResetOverride, null);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target,
         Turns angle,
         float maximumRange,
         List<String> options,
         RadioSetting aimMode,
         Box box,
         String sprintResetOverride,
         Boolean smartCritsOverride
      ) {
         this.target = target;
         this.angle = angle;
         this.maximumRange = maximumRange;
         this.onlyCritical = options.contains("Только криты");
         this.shouldBreakShield = options.contains("Ломать щит");
         this.shouldUnPressShield = options.contains("Отжимать щит");
         this.eatAndAttack = options.contains("Не бить если ешь");
         this.box = box;
         this.aimMode = aimMode;
         this.sprintResetOverride = sprintResetOverride;
         this.smartCritsOverride = smartCritsOverride;
      }

      public LivingEntity getTarget() {
         return this.target;
      }

      public Turns getAngle() {
         return this.angle;
      }

      public float getMaximumRange() {
         return this.maximumRange;
      }

      public boolean isOnlyCritical() {
         return this.onlyCritical;
      }

      public boolean isShouldBreakShield() {
         return this.shouldBreakShield;
      }

      public boolean isShouldUnPressShield() {
         return this.shouldUnPressShield;
      }

      public boolean isEatAndAttack() {
         return this.eatAndAttack;
      }

      public Box getBox() {
         return this.box;
      }

      public RadioSetting getAimMode() {
         return this.aimMode;
      }

      public String getSprintResetOverride() {
         return this.sprintResetOverride;
      }

      public Boolean getSmartCritsOverride() {
         return this.smartCritsOverride;
      }
   }
}
