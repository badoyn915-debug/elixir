package code.elixir.features.impl.player.autoMine.region.utils;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Optional;
import java.util.Stack;
import org.jetbrains.annotations.Nullable;

public class RandStack<T> extends Stack<T> {
   @Nullable
   public T random() {
      return this.get(new SecureRandom().nextInt(0, this.size() - 1));
   }

   public Optional<T> fromComparator(Comparator<? super T> comparable) {
      return this.stream().min(comparable);
   }
}
