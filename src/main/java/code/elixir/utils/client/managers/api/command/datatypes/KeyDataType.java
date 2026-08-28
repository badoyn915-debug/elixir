package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.client.util.InputUtil.Type;

public enum KeyDataType implements IDatatypeFor<Entry<String, Integer>> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> keys = getKeys().keySet().stream();
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(keys).filterPrefix(context).sortAlphabetically().stream();
   }

   public Entry<String, Integer> get(IDatatypeContext datatypeContext) throws CommandException {
      String key = datatypeContext.getConsumer().getString();
      return getKeys().entrySet().stream().filter(s -> s.getKey().equalsIgnoreCase(key)).findFirst().orElse(null);
   }

   private static Map<String, Integer> getKeys() {
      HashMap<String, Integer> keys = new HashMap<>();
      ObjectIterator var1 = Type.KEYSYM.map.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry entry = (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry)var1.next();
         int keyCode = entry.getIntKey();
         String bindName = StringHelper.getBindName(keyCode).toLowerCase();
         keys.put(bindName, keyCode);
      }

      return keys;
   }
}
