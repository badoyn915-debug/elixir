package code.elixir.common.repository.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TargetRepository {
   private static final TargetRepository INSTANCE = new TargetRepository();
   private final List<String> targets = new ArrayList<>();

   public static TargetRepository getInstance() {
      return INSTANCE;
   }

   public void addTarget(String name) {
      if (!this.isTarget(name)) {
         this.targets.add(name.toLowerCase(Locale.US));
      }
   }

   public void removeTarget(String name) {
      this.targets.remove(name.toLowerCase(Locale.US));
   }

   public void clearTargets() {
      this.targets.clear();
   }

   public List<String> getTargets() {
      return Collections.unmodifiableList(this.targets);
   }

   public boolean isTarget(String name) {
      return this.targets.contains(name.toLowerCase(Locale.US));
   }
}
