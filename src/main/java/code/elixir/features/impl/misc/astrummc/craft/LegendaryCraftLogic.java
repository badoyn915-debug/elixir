package code.elixir.features.impl.misc.astrummc.craft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class LegendaryCraftLogic {
   private LegendaryCraftLogic() {
   }

   public static LegendaryCraftLogic.RecipeStatus evaluate(LegendaryCraftRecipe recipe, Map<String, Integer> inventory) {
      ArrayList<LegendaryCraftLogic.IngredientStatus> statuses = new ArrayList<>();
      int satisfiedCount = 0;

      for (CraftIngredient ingredient : recipe.ingredients()) {
         int available = inventory.getOrDefault(ingredient.id(), 0);
         boolean satisfied = available >= ingredient.required();
         if (satisfied) {
            satisfiedCount++;
         }

         statuses.add(new LegendaryCraftLogic.IngredientStatus(ingredient, available, satisfied));
      }

      int totalCount = recipe.ingredients().size();
      int halfThreshold = (int)Math.ceil(totalCount * 0.5);
      boolean allSatisfied = satisfiedCount == totalCount;
      boolean halfOrMore = satisfiedCount >= halfThreshold;
      return new LegendaryCraftLogic.RecipeStatus(recipe, statuses, satisfiedCount, totalCount, allSatisfied, halfOrMore);
   }

   public static String formatHalfMessage(LegendaryCraftLogic.RecipeStatus status) {
      String available = status.ingredients()
         .stream()
         .filter(LegendaryCraftLogic.IngredientStatus::satisfied)
         .map(LegendaryCraftLogic::formatAvailable)
         .collect(Collectors.joining(", "));
      String missing = status.ingredients()
         .stream()
         .filter(ingredient -> !ingredient.satisfied())
         .map(LegendaryCraftLogic::formatMissing)
         .collect(Collectors.joining(", "));
      return "У вас есть ресурсы " + available + " для крафта " + status.recipe().name() + ". Осталось собрать: " + missing;
   }

   public static String formatCompleteMessage(LegendaryCraftLogic.RecipeStatus status) {
      String resources = status.recipe()
         .ingredients()
         .stream()
         .map(ingredient -> formatRequired(ingredient.required(), ingredient.label()))
         .collect(Collectors.joining(", "));
      return "У вас есть ресурсы " + resources + ", вы готовы скрафтить " + status.recipe().name();
   }

   private static String formatAvailable(LegendaryCraftLogic.IngredientStatus status) {
      return formatRequired(status.available(), status.ingredient().label());
   }

   private static String formatMissing(LegendaryCraftLogic.IngredientStatus status) {
      int remaining = status.ingredient().required() - status.available();
      return formatRequired(remaining, status.ingredient().label());
   }

   private static String formatRequired(int count, String label) {
      return count + " " + label;
   }

   public record IngredientStatus(CraftIngredient ingredient, int available, boolean satisfied) {
   }

   public record RecipeStatus(
      LegendaryCraftRecipe recipe,
      List<LegendaryCraftLogic.IngredientStatus> ingredients,
      int satisfiedCount,
      int totalCount,
      boolean allSatisfied,
      boolean halfOrMore
   ) {
   }
}
