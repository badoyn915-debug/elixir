package code.elixir.features.impl.misc.serverhelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class ServerHelperItemRegistry {
   private static final Map<String, ServerHelperItemConfig> BY_NAME = new HashMap<>();

   private ServerHelperItemRegistry() {
   }

   private static void register(String name, Item item) {
      BY_NAME.put(cleanKey(name), new ServerHelperItemConfig(item, name));
   }

   private static void register(String name, Item item, String lore) {
      BY_NAME.put(cleanKey(name), new ServerHelperItemConfig(item, name, List.of(lore.toLowerCase())));
   }

   public static ServerHelperItemConfig get(String name) {
      return name == null ? null : BY_NAME.get(cleanKey(name));
   }

   private static String cleanKey(String name) {
      return name.toLowerCase().trim();
   }

   static {
      register("Пузырёк опыта", Items.EXPERIENCE_BOTTLE);
      register("Трапка", Items.POPPED_CHORUS_FRUIT, "при взрыве наносит урон");
      register("Взрывная трапка", Items.PRISMARINE_SHARD, "при взрыве наносит урон в радиусе");
      register("Прощальный гул", Items.JACK_O_LANTERN, "отталкивает всех игроков");
      register("Взрывная штучка", Items.FIRE_CHARGE, "при нажатии пкм вызывает взрыв");
      register("Стан", Items.NETHER_STAR, "создает куб");
      register("Ком снега", Items.SNOWBALL, "при попадании накладывает слепоту");
      register("Обычная ливалка", Items.FEATHER);
      register("Уникальное перышко", Items.FEATHER);
      register("Ливалка с платформой", Items.FEATHER);
      register("Уникальная трапка", Items.NETHERITE_SCRAP);
   }
}
