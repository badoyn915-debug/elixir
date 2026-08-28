package code.elixir.features.impl.player.autoMine.mine.state;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public class MineState implements QuickImports {
   private long duration;

   public void onTick() {
      this.duration = getMineDuration();
   }

   public static long getMineDuration() {
      for (Entity entity : mc.world.getEntities()) {
         Entity nextArmorEntity;
         ArmorStandEntity armorStandEntity;
         if (entity instanceof ArmorStandEntity
            && (armorStandEntity = (ArmorStandEntity)entity).getName().getString().contains("Обновление через")
            && (nextArmorEntity = mc.world.getEntityById(armorStandEntity.getId() + 1)) != null) {
            String duration = nextArmorEntity.getName().getString().replaceAll("[^0-9:]", "");
            String[] mmss = duration.split(":");
            if (mmss.length > 1) {
               return Integer.parseInt(mmss[0]) * 60L + Integer.parseInt(mmss[1]);
            }

            return Integer.parseInt(mmss[0]);
         }
      }

      return 0L;
   }

   public long getDuration() {
      return this.duration;
   }
}
