package code.elixir.features.impl.movement;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class ElytraTarget extends Module {
   private static final double MIN_SPEED_SQ = 0.01;
   private static final double MIN_OFFSET_SQ = 0.01;
   private static final double HEIGHT_FACTOR = 0.5;
   private final SelectSetting priority = new SelectSetting("Приоритет цели", "Как выбирать цель на элитре").value("Обычно", "Сперва элитры");
   private final BooleanSetting overtake = new BooleanSetting("Перегон", "Перегон цели на элитре").setValue(true);
   private final BooleanSetting bravoFfa = new BooleanSetting("BravoFFA", "Режим для Bravo FFA").setValue(false).visible(this.overtake::isValue);
   private final SliderSettings predict = new SliderSettings("Сила предикта", "Сила предикта перегона").setValue(3.0F).range(1.0F, 5.0F).step(0.1F);

   public ElytraTarget() {
      super("ElytraTarget", "ElytraTarget", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.priority, this.overtake, this.bravoFfa, this.predict});
   }

   public static ElytraTarget getInstance() {
      return Instance.get(ElytraTarget.class);
   }

   public boolean isOvertakeActive() {
      return this.isState() && this.overtake.isValue();
   }

   public boolean isBravoFfa() {
      return this.isOvertakeActive() && this.bravoFfa.isValue();
   }

   public float getPredictStrength() {
      return this.predict.getValue();
   }

   public boolean preferElytraTargets() {
      return this.priority.isSelected("Сперва элитры");
   }

   public boolean shouldTargetEntity(Entity entity) {
      return this.isOvertakeActive() && entity instanceof LivingEntity living ? living.isGliding() && living.fallDistance > 0.0F : false;
   }

   public boolean isBehindTarget(LivingEntity target) {
      if (mc.player != null && target != null) {
         Vec3d velocity = mc.player.getVelocity();
         Vec3d horizontal = new Vec3d(velocity.x, 0.0, velocity.z);
         if (horizontal.lengthSquared() < 0.01) {
            return false;
         } else {
            horizontal = horizontal.normalize();
            Vec3d eye = mc.player.getEyePos();
            Vec3d aim = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(eye);
            Vec3d aimHorizontal = new Vec3d(aim.x, 0.0, aim.z);
            return aimHorizontal.lengthSquared() < 0.01 ? false : horizontal.dotProduct(aimHorizontal.normalize()) < 0.35;
         }
      } else {
         return false;
      }
   }
}
