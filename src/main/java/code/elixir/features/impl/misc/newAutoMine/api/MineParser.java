package code.elixir.features.impl.misc.newAutoMine.api;

import code.elixir.features.impl.misc.newAutoMine.MiningAnarchyState;
import code.elixir.features.impl.player.autoMine.api.MineType;
import code.elixir.utils.display.interfaces.QuickImports;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public class MineParser implements QuickImports, MineApi {
   private MiningAnarchyState anarchyState = null;

   public static int formatMMSS(String string) {
      String[] mmss = string.split(":");
      return mmss.length > 1 ? Integer.parseInt(mmss[0]) * 60 + Integer.parseInt(mmss[1]) : Integer.parseInt(mmss[0]);
   }

   public int getTimeToMine() {
      for (Entity entity : mc.world.getEntities()) {
         Entity nextArmorEntity;
         ArmorStandEntity armorStandEntity;
         if (entity instanceof ArmorStandEntity
            && (armorStandEntity = (ArmorStandEntity)entity).getName().getString().contains("Обновление через")
            && (nextArmorEntity = mc.world.getEntityById(armorStandEntity.getId() + 1)) != null) {
            String duration = nextArmorEntity.getName().getString().replaceAll("[^0-9:]", "");
            return formatMMSS(duration);
         }
      }

      return 0;
   }

   public CompletableFuture<Void> sendRequest() {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://85.208.139.128:8000/api/mines")).timeout(Duration.ofSeconds(5L)).GET().build();
      return HTTP_CLIENT.sendAsync(request, BodyHandlers.ofString()).thenAccept(response -> {
         if (response.statusCode() == 200) {
            this.readResponse(response.body());
         }
      }).exceptionally(e -> null);
   }

   public void readResponse(String json) {
      JsonObject mainObject = (JsonObject)GSON.fromJson(json, JsonObject.class);
      if (mainObject.has("mines")) {
         String bestAnarchy = null;
         String bestType = null;
         int bestTime = 0;

         for (JsonElement jsonElement : mainObject.get("mines").getAsJsonArray()) {
            String[] anTypeTime = jsonElement.getAsString().split(" ");
            String anarchyString = anTypeTime[0];
            String typeString = anTypeTime[1];
            String timeString = anTypeTime[2];
            int time = formatMMSS(timeString);
            if (typeString.equals("Легендарная") && time < bestTime) {
               bestTime = time;
               bestAnarchy = anarchyString;
               bestType = typeString;
            }
         }

         MiningAnarchyState anarchyState1;
         if (bestAnarchy != null
            && bestType != null
            && !this.anarchyState.equals(anarchyState1 = new MiningAnarchyState(bestAnarchy, this.getTypeFromString(bestType), bestTime))) {
            this.anarchyState = anarchyState1;
         }
      }
   }

   private MineType getTypeFromString(String string) {
      if (string == null) {
         return null;
      } else if (string.equals("Легендарная")) {
         return MineType.LEGENDARY;
      } else {
         return string.equals("Мифическая") ? MineType.MYTHICAL : null;
      }
   }

   public MiningAnarchyState getAnarchyState() {
      return this.anarchyState;
   }
}
