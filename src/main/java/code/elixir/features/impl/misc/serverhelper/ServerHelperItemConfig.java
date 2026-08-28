package code.elixir.features.impl.misc.serverhelper;

import java.util.List;
import net.minecraft.item.Item;

public record ServerHelperItemConfig(Item item, String displayName, List<String> loreParams) {
   public ServerHelperItemConfig(Item item, String displayName) {
      this(item, displayName, List.of());
   }
}
