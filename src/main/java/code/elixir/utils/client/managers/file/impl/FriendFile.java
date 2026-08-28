package code.elixir.utils.client.managers.file.impl;

import code.elixir.common.repository.friend.Friend;
import code.elixir.common.repository.friend.FriendUtils;
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
import java.util.Arrays;

public class FriendFile extends ClientFile {
   public FriendFile() {
      super("friends");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(FriendUtils.getFriends(), writer);
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
            Friend[] friends = (Friend[])gson.fromJson(reader, Friend[].class);
            FriendUtils.clear();
            FriendUtils.getFriends().addAll(Arrays.asList(friends));
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
