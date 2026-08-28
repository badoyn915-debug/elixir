package code.elixir.features.impl.misc.astrummc.craft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LegendaryCraftNotifier {
   private final Map<String, LegendaryCraftNotifier.NotificationState> states = new HashMap<>();

   public List<String> process(LegendaryCraftRecipe recipe, LegendaryCraftLogic.RecipeStatus status) {
      LegendaryCraftNotifier.NotificationState state = this.states.computeIfAbsent(recipe.name(), ignored -> new LegendaryCraftNotifier.NotificationState());
      ArrayList<String> messages = new ArrayList<>();
      if (status.allSatisfied()) {
         if (!state.notifiedComplete) {
            state.notifiedComplete = true;
            messages.add(LegendaryCraftLogic.formatCompleteMessage(status));
         }

         return messages;
      } else {
         state.notifiedComplete = false;
         if (status.halfOrMore()) {
            if (!state.notifiedHalf) {
               state.notifiedHalf = true;
               messages.add(LegendaryCraftLogic.formatHalfMessage(status));
            }
         } else {
            state.notifiedHalf = false;
         }

         return messages;
      }
   }

   public void reset() {
      this.states.clear();
   }

   private static final class NotificationState {
      private boolean notifiedHalf;
      private boolean notifiedComplete;
   }
}
