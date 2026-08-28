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
import net.minecraft.registry.Registries;

public class BlockESPFile extends ClientFile {
   private final BoxESPRepository repository;

   public BlockESPFile(BoxESPRepository repository) {
      super("blockESP");
      this.repository = repository;
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileWriter writer = new FileWriter(file)) {
            ArrayList blocksList = new ArrayList();
            this.repository.blocks.forEach((block, color) -> blocksList.add(new BlockESPFile.BlockESP(block.getTranslationKey(), color)));
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
            BlockESPFile.BlockESP[] blockESP = (BlockESPFile.BlockESP[])gson.fromJson(reader, BlockESPFile.BlockESP[].class);
            this.repository.blocks.clear();
            Arrays.asList(blockESP)
               .forEach(
                  esp -> Registries.BLOCK
                     .stream()
                     .filter(type -> type.getTranslationKey().equals(esp.block))
                     .findFirst()
                     .ifPresent(type -> this.repository.blocks.put(type, esp.color))
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

   private record BlockESP(String block, int color) {
   }
}
