package code.elixir.utils.display.render.util;

import java.util.Stack;

public class CyclicStack<T> extends Stack<T> {
   int index = 0;

   public T next() {
      this.index = (this.index + 1) % this.size();
      return this.get(this.index);
   }
}
