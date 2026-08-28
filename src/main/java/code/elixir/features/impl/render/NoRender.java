package code.elixir.features.impl.render;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.ArrayList;
import net.minecraft.entity.effect.StatusEffects;

public class NoRender extends Module {
   public final MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
      .value(
         "Огонь",
         "Плохие эффекты",
         "Тьма",
         "Оверлей блоков",
         "Тряска экрана",
         "Анимация тотема",
         "Боссбар",
         "Тени",
         "Дым",
         "Виньетка",
         "Стрелы в игроке",
         "Свечение игроков",
         "Оверлей лавы",
         "Оверлей воды",
         "Погода",
         "Скорборд",
         "Сердца иссушения",
         "Пузырьки воздуха",
         "Арморстенды",
         "Друзья",
         "Тайтлы",
         "Растительность",
         "Удочка"
      )
      .selected("Огонь", "Плохие эффекты", "Оверлей блоков", "Боссбар");

   public static NoRender getInstance() {
      return Instance.get(NoRender.class);
   }

   public NoRender() {
      super("NoRender", "NoRender", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.modeSetting});
      this.setState(true);
   }

   public static void enforceDefaults() {
      NoRender noRender = getInstance();
      if (noRender != null) {
         if (!noRender.isState()) {
            noRender.setState(true);
         }

         if (!noRender.modeSetting.isSelected("Боссбар")) {
            ArrayList<String> selected = new ArrayList<>(noRender.modeSetting.getSelected());
            selected.add("Боссбар");
            noRender.modeSetting.setSelected(selected);
         }
      }
   }

   public boolean shouldHideTotem() {
      return this.isState() && this.modeSetting.isSelected("Анимация тотема");
   }

   public boolean shouldHideBossbar() {
      return this.isState() && this.modeSetting.isSelected("Боссбар");
   }

   public boolean shouldHideShadows() {
      return this.isState() && this.modeSetting.isSelected("Тени");
   }

   public boolean shouldHideSmoke() {
      return this.isState() && this.modeSetting.isSelected("Дым");
   }

   public boolean shouldHideVignette() {
      return this.isState() && this.modeSetting.isSelected("Виньетка");
   }

   public boolean shouldHideStuckArrows() {
      return this.isState() && this.modeSetting.isSelected("Стрелы в игроке");
   }

   public boolean shouldHidePlayerGlow() {
      return this.isState() && this.modeSetting.isSelected("Свечение игроков");
   }

   public boolean shouldHideLavaOverlay() {
      return this.isState() && this.modeSetting.isSelected("Оверлей лавы");
   }

   public boolean shouldHideWaterOverlay() {
      return this.isState() && this.modeSetting.isSelected("Оверлей воды");
   }

   public boolean shouldHideWeather() {
      return this.isState() && this.modeSetting.isSelected("Погода");
   }

   public boolean shouldHideScoreboard() {
      return this.isState() && this.modeSetting.isSelected("Скорборд");
   }

   public boolean shouldHideWitherHearts() {
      return this.isState() && this.modeSetting.isSelected("Сердца иссушения");
   }

   public boolean shouldHideAirBubbles() {
      return this.isState() && this.modeSetting.isSelected("Пузырьки воздуха");
   }

   public boolean shouldHideArmorStands() {
      return this.isState() && this.modeSetting.isSelected("Арморстенды");
   }

   public boolean shouldHideBadEffects() {
      return this.isState() && this.modeSetting.isSelected("Плохие эффекты");
   }

   public boolean shouldHideDarkness() {
      return this.isState() && this.modeSetting.isSelected("Тьма");
   }

   public boolean shouldHideFriends() {
      return this.isState() && this.modeSetting.isSelected("Друзья");
   }

   public boolean shouldHideTitles() {
      return this.isState() && this.modeSetting.isSelected("Тайтлы");
   }

   public boolean shouldHideVegetation() {
      return this.isState() && this.modeSetting.isSelected("Растительность");
   }

   public boolean shouldHideFishingRod() {
      return this.isState() && this.modeSetting.isSelected("Удочка");
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.shouldHideDarkness() && mc.player != null && mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
         mc.player.removeStatusEffect(StatusEffects.DARKNESS);
      }
   }
}
