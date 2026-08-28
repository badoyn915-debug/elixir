package code.elixir.features.impl.misc.astrummc.craft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class LegendaryCraftDatabase {
   private static final Map<String, Item> ITEMS = Map.ofEntries(
      Map.entry("obsidian", Items.OBSIDIAN),
      Map.entry("diamond_hoe", Items.DIAMOND_HOE),
      Map.entry("coal", Items.COAL),
      Map.entry("soul_sand", Items.SOUL_SAND),
      Map.entry("golden_apple", Items.GOLDEN_APPLE),
      Map.entry("quartz", Items.QUARTZ),
      Map.entry("golden_sword", Items.GOLDEN_SWORD),
      Map.entry("diamond", Items.DIAMOND),
      Map.entry("echo_shard", Items.ECHO_SHARD),
      Map.entry("iron_sword", Items.IRON_SWORD),
      Map.entry("emerald", Items.EMERALD),
      Map.entry("bell", Items.BELL),
      Map.entry("golden_carrot", Items.GOLDEN_CARROT),
      Map.entry("feather", Items.FEATHER),
      Map.entry("cake", Items.CAKE),
      Map.entry("spider_eye", Items.SPIDER_EYE),
      Map.entry("poisonous_potato", Items.POISONOUS_POTATO),
      Map.entry("raw_copper_block", Items.RAW_COPPER_BLOCK),
      Map.entry("blaze_powder", Items.BLAZE_POWDER),
      Map.entry("iron_block", Items.IRON_BLOCK),
      Map.entry("blaze_rod", Items.BLAZE_ROD),
      Map.entry("ender_pearl", Items.ENDER_PEARL),
      Map.entry("netherite_scrap", Items.NETHERITE_SCRAP),
      Map.entry("wind_charge", Items.WIND_CHARGE),
      Map.entry("redstone_torch", Items.REDSTONE_TORCH),
      Map.entry("red_candle", Items.RED_CANDLE),
      Map.entry("gold_ingot", Items.GOLD_INGOT),
      Map.entry("bone", Items.BONE),
      Map.entry("experience_bottle", Items.EXPERIENCE_BOTTLE),
      Map.entry("redstone_block", Items.REDSTONE_BLOCK),
      Map.entry("skeleton_skull", Items.SKELETON_SKULL),
      Map.entry("pufferfish", Items.PUFFERFISH),
      Map.entry("copper_block", Items.COPPER_BLOCK),
      Map.entry("tnt", Items.TNT),
      Map.entry("nautilus_shell", Items.NAUTILUS_SHELL),
      Map.entry("seagrass", Items.SEAGRASS),
      Map.entry("trident", Items.TRIDENT),
      Map.entry("gold_block", Items.GOLD_BLOCK),
      Map.entry("lodestone", Items.LODESTONE),
      Map.entry("iron_axe", Items.IRON_AXE),
      Map.entry("magma_cream", Items.MAGMA_CREAM),
      Map.entry("totem", Items.TOTEM_OF_UNDYING),
      Map.entry("bow", Items.BOW),
      Map.entry("ender_eye", Items.ENDER_EYE),
      Map.entry("lapis_block", Items.LAPIS_BLOCK),
      Map.entry("clock", Items.CLOCK),
      Map.entry("diamond_sword", Items.DIAMOND_SWORD),
      Map.entry("diamond_axe", Items.DIAMOND_AXE),
      Map.entry("lightning_rod", Items.LIGHTNING_ROD),
      Map.entry("firework_star", Items.FIREWORK_STAR),
      Map.entry("crying_obsidian", Items.CRYING_OBSIDIAN),
      Map.entry("enchanted_golden_apple", Items.ENCHANTED_GOLDEN_APPLE),
      Map.entry("emerald_block", Items.EMERALD_BLOCK)
   );

   private LegendaryCraftDatabase() {
   }

   public static Item getItem(String id) {
      return ITEMS.get(id);
   }

   public static Map<String, Integer> collectInventoryCounts(ToIntFunction<Item> counter) {
      HashMap<String, Integer> inventory = new HashMap<>();

      for (LegendaryCraftRecipe recipe : LegendaryCraftRecipes.ALL) {
         for (CraftIngredient ingredient : recipe.ingredients()) {
            Item item = ITEMS.get(ingredient.id());
            if (item != null) {
               inventory.put(ingredient.id(), counter.applyAsInt(item));
            }
         }
      }

      return inventory;
   }

   public static String buildFingerprint(Map<String, Integer> inventory) {
      StringBuilder builder = new StringBuilder();
      ITEMS.keySet().stream().sorted().forEach(id -> builder.append(id).append('=').append(inventory.getOrDefault(id, 0)).append(';'));
      return builder.toString();
   }
}
