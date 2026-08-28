package code.elixir.utils.client.managers.file.impl;

import code.elixir.common.repository.staff.StaffRepository;
import code.elixir.utils.client.managers.file.ClientFile;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class StaffFile extends ClientFile {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public StaffFile() {
      super("staff");
   }

   @Override
   public void loadFromFile(File directory) throws FileLoadException {
      File file = new File(directory, this.getName() + ".json");
      if (file.exists()) {
         try {
            String content = Files.readString(file.toPath());
            if (!content.isEmpty()) {
               List staff = (List)GSON.fromJson(content, (new TypeToken<List<StaffRepository.Staff>>() {}).getType());
               if (staff != null) {
                  StaffRepository.getStaff().clear();
                  StaffRepository.getStaff().addAll(staff);
               }
            }
         } catch (IOException var5) {
            throw new FileLoadException("Не удалось прочитать файл персонала", var5);
         } catch (JsonSyntaxException var6) {
            throw new FileLoadException("JSON syntax error, staff config cannot be loaded", var6);
         } catch (JsonIOException var7) {
            throw new FileLoadException("JSON IO error, staff config cannot be loaded", var7);
         }
      }
   }

   @Override
   public void saveToFile(File directory) throws FileSaveException {
      File file = new File(directory, this.getName() + ".json");

      try {
         Files.writeString(file.toPath(), GSON.toJson(StaffRepository.getStaff()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (IOException var4) {
         throw new FileSaveException("Не удалось сохранить файл персонала", var4);
      }
   }
}
