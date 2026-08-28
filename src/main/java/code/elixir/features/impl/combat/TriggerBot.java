package code.elixir.features.impl.combat;

import code.elixir.Elixir;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.striking.StrikerConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.combat.LegitSprintReset;
import code.elixir.utils.features.combat.TargetFinder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public class TriggerBot extends Module implements QuickImports {
   private LivingEntity target;
   private LivingEntity lastTarget;
   private MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтр целей по типу")
      .value("Игроки", "Мобы", "Животные", "Друзья", "Подставка для брони")
      .selected("Игроки", "Мобы", "Животные");
   private SliderSettings attackRange = new SliderSettings("Дистанция удара", "Максимальная дистанция атаки").setValue(3.0F).range(1.0F, 6.0F);
   private MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Опции атаки")
      .value("Только криты", "Ломать щит", "Отжимать щит", "Не бить если ешь")
      .selected("Только криты", "Ломать щит");
   private RadioSetting sprintReset = new RadioSetting(
      "Сброс спринта", "Сброс спринта перед ударом", new String[]{"Легитно", "Пакетно", "Не сбрасывать"}, "Легитно"
   );
   public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Умные криты").setValue(true);
   private BooleanSetting targetEsp = new BooleanSetting("Target ESP", "Показывать ESP на цели").setValue(false);

   public static TriggerBot getInstance() {
      return Instance.get(TriggerBot.class);
   }

   public TriggerBot() {
      super("TriggerBot", "TriggerBot", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.targetType, this.attackRange, this.attackSetting, this.sprintReset, this.smartCrit, this.targetEsp});
   }

   @Override
   public void deactivate() {
      LegitSprintReset.reset();
      this.target = null;
      this.lastTarget = null;
      super.deactivate();
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.target != null && "Легитно".equals(this.sprintReset.get())) {
         LegitSprintReset.applyInputReset(event);
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (mc.player != null && mc.world != null) {
         switch (e.getType()) {
            case 0:
               this.target = this.updateTarget();
               break;
            case 2:
               if (this.target != null) {
                  this.tryAttack();
               }
         }
      }
   }

   private LivingEntity updateTarget() {
      if (mc.currentScreen == null && mc.interactionManager != null) {
         this.target = null;
         if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) {
            return null;
         } else if (!(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
         } else {
            TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getSelected());
            if (!filter.isValid(living)) {
               return null;
            } else {
               float range = this.attackRange.getValue() + AttackAura.RANGE_MARGIN;
               if (mc.player.distanceTo(living) > range) {
                  return null;
               } else {
                  this.lastTarget = living;
                  return living;
               }
            }
         }
      } else {
         return null;
      }
   }

   private void tryAttack() {
      if (this.target != null) {
         TurnsConnection.INSTANCE.setRotation(null);
         Vec3d vec = MathAngle.getClosestVec(this.target).subtract(mc.player.getEyePos());
         float range = this.attackRange.getValue() + AttackAura.RANGE_MARGIN;
         StrikerConstructor.AttackPerpetratorConfigurable config = new StrikerConstructor.AttackPerpetratorConfigurable(
            this.target,
            MathAngle.fromVec3d(vec),
            range,
            this.attackSetting.getSelected(),
            AttackAura.aimMode,
            this.target.getBoundingBox(),
            this.sprintReset.get(),
            this.smartCrit.isValue()
         );
         Elixir.getInstance().getAttackPerpetrator().performAttack(config);
      }
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }

   public MultiSelectSetting getTargetType() {
      return this.targetType;
   }

   public SliderSettings getAttackRange() {
      return this.attackRange;
   }

   public MultiSelectSetting getAttackSetting() {
      return this.attackSetting;
   }

   public RadioSetting getSprintReset() {
      return this.sprintReset;
   }

   public BooleanSetting getSmartCrit() {
      return this.smartCrit;
   }

   public BooleanSetting getTargetEsp() {
      return this.targetEsp;
   }
}
