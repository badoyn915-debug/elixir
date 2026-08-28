package code.elixir.utils.client.managers.file.impl;

import code.elixir.common.repository.box.BoxESPRepository;
import code.elixir.utils.client.managers.file.ClientFile;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;

public class EntityESPFile extends ClientFile {
   private final BoxESPRepository repository;

   public EntityESPFile(BoxESPRepository repository) {
      super("entityESP");
      this.repository = repository;
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileWriter writer = new FileWriter(file)) {
            ArrayList blocksList = new ArrayList();
            this.repository.entities.forEach((entity, color) -> blocksList.add(new EntityESPFile.EntityESP(entity.getTranslationKey(), color)));
            gson.toJson(blocksList, writer);
         }
      } catch (IOException | JsonIOException var9) {
         throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), var9);
      }
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileReader reader = new FileReader(file)) {
            EntityESPFile.EntityESP[] blockESP = (EntityESPFile.EntityESP[])gson.fromJson(reader, EntityESPFile.EntityESP[].class);
            this.repository.entities.clear();
            Arrays.asList(blockESP)
               .forEach(
                  esp -> Registries.ENTITY_TYPE
                     .stream()
                     .filter(type -> type.getTranslationKey().equals(esp.entity))
                     .findFirst()
                     .ifPresent(type -> this.repository.entities.put((EntityType<?>)type, esp.color))
               );
         }
      } catch (IOException var9) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), var9);
      } catch (JsonSyntaxException var10) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), var10);
      } catch (JsonIOException var11) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), var11);
      }
   }

   private record EntityESP(String entity, int color) {
   }
}
