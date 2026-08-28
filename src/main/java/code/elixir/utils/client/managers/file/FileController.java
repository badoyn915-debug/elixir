package code.elixir.utils.client.managers.file;

import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import code.elixir.utils.client.managers.file.impl.ModuleFile;
import code.elixir.utils.theme.ThemeManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileController {
   private final List<ClientFile> clientFiles;
   private final File directory;
   private final File moduleConfigDirectory;
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   public FileController(List<ClientFile> clientFiles, File directory, File moduleConfigDirectory) {
      this.clientFiles = clientFiles;
      this.directory = directory;
      this.moduleConfigDirectory = moduleConfigDirectory;
      this.startAutoSave();
   }

   public void startAutoSave() {
      Logger.info("Auto-save system started! (Every 5 minutes)");
      this.scheduler.scheduleAtFixedRate(() -> {
         try {
            Logger.info("Saving with auto-save.");
            this.saveFiles();
         } catch (FileSaveException var2) {
            Logger.error("Failed to auto-save files: " + var2.getMessage());
         }
      }, 5L, 5L, TimeUnit.MINUTES);
   }

   public void stopAutoSave() {
      Logger.info("Auto-save shutdown!");
      this.scheduler.shutdown();

      try {
         if (!this.scheduler.awaitTermination(1L, TimeUnit.MINUTES)) {
            this.scheduler.shutdownNow();
         }
      } catch (InterruptedException var2) {
         this.scheduler.shutdownNow();
      }
   }

   public void saveFiles() throws FileSaveException {
      if (this.clientFiles.isEmpty()) {
         Logger.warn("No files to save from directory: " + this.directory.getPath());
      } else {
         for (ClientFile clientFile : this.clientFiles) {
            if (!(clientFile instanceof ModuleFile)) {
               try {
                  clientFile.saveToFile(this.directory);
               } catch (FileSaveException var6) {
                  throw new FileSaveException("Failed to save file: " + clientFile.getName(), var6);
               }
            }
         }

         try {
            this.saveFile("default.json");
         } catch (FileSaveException var5) {
            Logger.error("Failed to auto-save default config: " + var5.getMessage());
         }

         try {
            ThemeManager.saveTheme("default");
         } catch (Exception var4) {
            Logger.error("Failed to auto-save default theme: " + var4.getMessage());
         }
      }
   }

   public void loadFiles() throws FileLoadException {
      if (this.clientFiles.isEmpty()) {
         Logger.warn("No files to load from directory: " + this.directory.getPath());
      } else {
         for (ClientFile clientFile : this.clientFiles) {
            try {
               if (!(clientFile instanceof ModuleFile)) {
                  clientFile.loadFromFile(this.directory);
               }
            } catch (FileLoadException var4) {
               Logger.warn("Failed to load file: " + clientFile.getName() + " - " + var4.getMessage());
            }
         }
      }
   }

   public void saveFile(String fileName) throws FileSaveException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.saveToFile(this.moduleConfigDirectory, fileName);
            } catch (FileSaveException var5) {
               throw new FileSaveException("Failed to save file: " + fileName, var5);
            }
         }
      }
   }

   public void loadFile(String fileName) throws FileLoadException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.loadFromFile(this.moduleConfigDirectory, fileName);
            } catch (FileLoadException var5) {
               throw new FileLoadException("Failed to load file: " + fileName, var5);
            }
         }
      }
   }

   public void resetConfig() throws FileLoadException, FileSaveException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile moduleFile) {
            moduleFile.resetConfiguration(this.moduleConfigDirectory);
            return;
         }
      }

      throw new FileSaveException("ModuleFile not found", null);
   }

   public void saveFile(Class<? extends ClientFile> fileClass) {
      this.clientFiles.stream().filter(fileClass::isInstance).findFirst().ifPresent(file -> {
         try {
            file.saveToFile(this.directory);
         } catch (FileSaveException var3) {
            Logger.error("Failed to save file on-demand: " + file.getName(), var3);
         }
      });
   }
}
