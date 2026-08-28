package code.elixir.utils.client.managers.file.impl;

import code.elixir.commands.CommandDispatcher;
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

public class PrefixFile extends ClientFile {
   public PrefixFile() {
      super("prefix");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(CommandDispatcher.prefix, writer);
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
            String string = (String)gson.fromJson(reader, String.class);
            CommandDispatcher.prefix = string.isEmpty() ? "." : CommandDispatcher.prefix;
         }
      } catch (IOException var9) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), var9);
      } catch (JsonSyntaxException var10) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), var10);
      } catch (JsonIOException var11) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), var11);
      }
   }
}
