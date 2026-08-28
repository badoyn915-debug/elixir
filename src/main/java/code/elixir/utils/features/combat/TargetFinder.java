package code.elixir.utils.features.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public class TargetFinder implements QuickImports {
   private LivingEntity currentTarget;

   public void searchTargets(Iterable<Entity> entities, float range, float fov, boolean ignoreWalls) {
      this.currentTarget = StreamSupport.stream(entities.spliterator(), false)
         .filter(e -> {
            LivingEntity living;
            return e instanceof LivingEntity && (living = (LivingEntity)e) != mc.player && living.isAlive();
         })
         .map(e -> (LivingEntity)e)
         .filter(l -> mc.player != null && mc.player.distanceTo(l) <= range)
         .filter(l -> ignoreWalls || mc.player.canSee(l))
         .min(Comparator.comparingDouble(l -> mc.player.distanceTo(l)))
         .orElse(null);
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
         this.currentTarget = null;
      }
   }

   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   public static class EntityFilter implements Predicate<LivingEntity> {
      private final List<String> selected;

      public EntityFilter(List<String> selected) {
         this.selected = selected;
      }

      public boolean test(LivingEntity entity) {
         if (entity == null || entity == QuickImports.mc.player || !entity.isAlive()) {
            return false;
         } else if (entity instanceof ArmorStandEntity) {
            return this.selected.contains("Подставка для брони");
         } else if (entity instanceof PlayerEntity player) {
            if (!this.selected.contains("Игроки")) {
               return false;
            } else {
               return this.selected.contains("Друзья") && FriendUtils.isFriend(player.getName().getString())
                  ? true
                  : !FriendUtils.isFriend(player.getName().getString());
            }
         } else if (entity instanceof AnimalEntity || entity instanceof PassiveEntity) {
            return this.selected.contains("Животные");
         } else {
            return !(entity instanceof HostileEntity) && !(entity instanceof MobEntity) ? false : this.selected.contains("Мобы");
         }
      }

      public boolean isValid(LivingEntity entity) {
         return this.test(entity);
      }
   }
}
