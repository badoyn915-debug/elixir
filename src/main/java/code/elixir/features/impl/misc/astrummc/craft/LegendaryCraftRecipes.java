package code.elixir.features.impl.misc.astrummc.craft;

import java.util.List;

public final class LegendaryCraftRecipes {
   public static final List<LegendaryCraftRecipe> ALL = List.of(
      recipe(
         "Коса Бедствия",
         ing("firework_star", 1, "пиротехническая звезда"),
         ing("crying_obsidian", 2, "плачущего обсидиана"),
         ing("diamond_hoe", 1, "алмазная мотыга"),
         ing("netherite_scrap", 1, "незеритовый лом")
      ),
      recipe(
         "Меч Мидаса",
         ing("enchanted_golden_apple", 1, "зачарованное золотое яблоко"),
         ing("quartz", 2, "незеритовый кварц"),
         ing("golden_apple", 1, "золотое яблоко"),
         ing("golden_sword", 1, "золотой меч")
      ),
      recipe("Теневой Клинок", ing("diamond", 4, "алмаза"), ing("echo_shard", 2, "осколка эха"), ing("iron_sword", 1, "железный меч")),
      recipe(
         "Изумрудный Клинок",
         ing("golden_carrot", 1, "золотая морковь"),
         ing("emerald_block", 2, "изумрудных блока"),
         ing("diamond_sword", 1, "алмазный меч"),
         ing("bell", 1, "колокол")
      ),
      recipe(
         "Небесный Меч",
         ing("feather", 4, "пера"),
         ing("diamond", 2, "алмаза"),
         ing("cake", 1, "торт"),
         ing("iron_sword", 1, "железный меч"),
         ing("golden_carrot", 1, "золотая морковь")
      ),
      recipe(
         "Отравленный Кинжал",
         ing("spider_eye", 2, "паучьих глаза"),
         ing("poisonous_potato", 1, "ядовитый картофель"),
         ing("raw_copper_block", 1, "блок сырой меди"),
         ing("iron_sword", 1, "железный меч")
      ),
      recipe(
         "Экскалибур",
         ing("blaze_powder", 4, "огненного порошка"),
         ing("diamond_sword", 1, "алмазный меч"),
         ing("enchanted_golden_apple", 1, "зачарованное золотое яблоко"),
         ing("iron_block", 2, "железных блока"),
         ing("blaze_rod", 1, "огненный стержень")
      ),
      recipe(
         "Катана Дракона",
         ing("diamond", 4, "алмаза"),
         ing("obsidian", 1, "обсидиан"),
         ing("ender_pearl", 2, "жемчуга Края"),
         ing("diamond_sword", 1, "алмазный меч"),
         ing("netherite_scrap", 1, "незеритовый лом")
      ),
      recipe(
         "Клинок Бури",
         ing("wind_charge", 4, "заряда ветра"),
         ing("diamond", 2, "алмаза"),
         ing("diamond_sword", 1, "алмазный меч"),
         ing("lightning_rod", 1, "громоотвод"),
         ing("gold_block", 1, "золотой блок")
      ),
      recipe(
         "Посох Жителя",
         ing("red_candle", 2, "красные свечи"),
         ing("bell", 1, "колокол"),
         ing("emerald_block", 2, "изумрудных блока"),
         ing("experience_bottle", 1, "пузырек опыта"),
         ing("bone", 1, "кость")
      ),
      recipe(
         "Посох Нежити",
         ing("diamond", 4, "алмаза"),
         ing("gold_ingot", 2, "золотых слитка"),
         ing("skeleton_skull", 1, "голова скелета"),
         ing("bone", 1, "кость"),
         ing("redstone_block", 1, "блок редстоуна")
      ),
      recipe(
         "Фугу-Пушка",
         ing("diamond", 2, "алмаза"),
         ing("pufferfish", 2, "иглобрюха"),
         ing("copper_block", 2, "медных блока"),
         ing("redstone_block", 2, "блока редстоуна"),
         ing("tnt", 1, "динамит")
      ),
      recipe(
         "Трезубец Посейдона",
         ing("diamond", 4, "алмаза"),
         ing("nautilus_shell", 1, "раковина наутилуса"),
         ing("seagrass", 2, "морские травы"),
         ing("gold_block", 1, "золотой блок"),
         ing("trident", 1, "трезубец")
      ),
      recipe(
         "Мьёльнир",
         ing("diamond", 4, "алмаза"),
         ing("lodestone", 1, "магнетит"),
         ing("iron_block", 2, "железных блока"),
         ing("diamond_axe", 1, "алмазный топор"),
         ing("magma_cream", 1, "сгусток магмы")
      ),
      recipe("Лук Артемиды", ing("feather", 4, "пера"), ing("totem", 1, "тотем бессмертия"), ing("bow", 1, "лук"), ing("diamond", 1, "алмаз")),
      recipe(
         "Эндер-лук",
         ing("diamond", 4, "алмаза"),
         ing("ender_eye", 1, "око Края"),
         ing("lapis_block", 2, "лазуритовых блока"),
         ing("bow", 1, "лук"),
         ing("clock", 1, "часы")
      )
   );

   private LegendaryCraftRecipes() {
   }

   private static LegendaryCraftRecipe recipe(String name, CraftIngredient... ingredients) {
      return new LegendaryCraftRecipe(name, List.of(ingredients));
   }

   private static CraftIngredient ing(String id, int required, String label) {
      return new CraftIngredient(id, required, label);
   }
}
