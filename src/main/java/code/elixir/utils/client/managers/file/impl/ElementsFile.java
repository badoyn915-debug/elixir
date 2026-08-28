package code.elixir.utils.client.managers.file.impl;

import code.elixir.display.hud.TargetHud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.managers.api.draggable.DraggableRepository;
import code.elixir.utils.client.managers.file.ClientFile;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ElementsFile extends ClientFile {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final DraggableRepository draggableRepository;

   public ElementsFile(DraggableRepository draggableRepository) {
      super("elements");
      this.draggableRepository = draggableRepository;
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      JsonObject elementsObject = new JsonObject();

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         JsonObject draggableObject = new JsonObject();
         int posX = draggable.getX();
         int posY = draggable.getY();
         float scaleFactor = draggable.getScaleFactor();
         draggableObject.addProperty("posX", posX);
         draggableObject.addProperty("posY", posY);
         draggableObject.addProperty("scaleFactor", scaleFactor);
         if (draggable instanceof TargetHud targetHud) {
            draggableObject.addProperty("showOnHover", targetHud.isShowOnHover());
         }

         String name = draggable.getName().toLowerCase().replace(" ", "");
         elementsObject.add(name, draggableObject);
      }

      File file = new File(path, this.getName() + ".json");
      this.writeJsonToFile(elementsObject, file);
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      File file = new File(path, this.getName() + ".json");
      JsonObject elementsObject = this.readJsonFromFile(file);
      if (elementsObject != null) {
         this.updateElementsFromJsonObject(elementsObject);
      }
   }

   private void updateElementsFromJsonObject(JsonObject elementsObject) {
      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         String name = draggable.getName().toLowerCase().replace(" ", "");
         JsonObject draggableObject = elementsObject.getAsJsonObject(name);
         if (draggableObject != null) {
            if (draggableObject.has("posX") && draggableObject.has("posY")) {
               int posX = draggableObject.get("posX").getAsInt();
               int posY = draggableObject.get("posY").getAsInt();
               draggable.setX(posX);
               draggable.setY(posY);
            }

            if (draggableObject.has("scaleFactor")) {
               float scaleFactor = draggableObject.get("scaleFactor").getAsFloat();
               draggable.setScaleFactor(scaleFactor);
            } else {
               draggable.setScaleFactor(1.0F);
            }

            if (draggable instanceof TargetHud targetHud && draggableObject.has("showOnHover")) {
               boolean showOnHover = draggableObject.get("showOnHover").getAsBoolean();
               targetHud.setShowOnHover(showOnHover);
            }
         }
      }
   }

   private void writeJsonToFile(JsonObject jsonObject, File file) throws FileSaveException {
      try {
         try (FileWriter writer = new FileWriter(file)) {
            this.GSON.toJson(jsonObject, writer);
         }
      } catch (IOException var8) {
         throw new FileSaveException("Failed to save elements to file", var8);
      }
   }

   private JsonObject readJsonFromFile(File file) throws FileLoadException {
      if (!file.exists()) {
         return null;
      } else {
         try {
            JsonObject var3;
            try (FileReader reader = new FileReader(file)) {
               var3 = JsonParser.parseReader(reader).getAsJsonObject();
            }

            return var3;
         } catch (IOException var7) {
            throw new FileLoadException("Failed to load elements from file", var7);
         } catch (JsonSyntaxException | JsonIOException var8) {
            throw new FileLoadException("Failed to parse JSON from file", var8);
         }
      }
   }
}
