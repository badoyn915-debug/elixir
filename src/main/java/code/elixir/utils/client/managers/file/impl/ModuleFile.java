package code.elixir.utils.client.managers.file.impl;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.misc.Globals;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.impl.render.NoRender;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleRepository;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.GroupSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.managers.api.draggable.DraggableRepository;
import code.elixir.utils.client.managers.file.ClientFile;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleFile extends ClientFile {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final ModuleRepository moduleRepository;

   public ModuleFile(ModuleRepository moduleRepository, DraggableRepository draggableRepository) {
      super("autoCfg");
      this.moduleRepository = moduleRepository;
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      this.saveToFile(path, this.getName() + ".json");
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      this.loadFromFile(path, this.getName() + ".json");
   }

   @Override
   public void saveToFile(File path, String fileName) throws FileSaveException {
      JsonObject functionObject = this.createJsonObjectFromModules();
      File file = new File(path, fileName);
      this.writeJsonToFile(functionObject, file);
      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      JsonObject functionObject = this.readJsonFromFile(file);
      if (functionObject != null) {
         this.updateModulesFromJsonObject(functionObject);
      }

      ClickGui.enforceOpenKey();
      Globals.enforceEnabled();
      NoRender.enforceDefaults();
      super.loadFromFile(path, fileName);
   }

   private JsonObject createJsonObjectFromModules() {
      JsonObject functionObject = new JsonObject();

      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("bind", module.getKey());
         if (module.getSecondKey() != -1 && module.getSecondKey() != -1) {
            moduleObject.addProperty("secondBind", module.getSecondKey());
         }

         moduleObject.addProperty("state", module.isState());
         module.settings().forEach(setting -> this.addSettingToJsonObject(moduleObject, setting));
         if (module instanceof AttackAura) {
            moduleObject.addProperty("rotationLayoutVersion", 2);
         }

         functionObject.add(module.getName().toLowerCase(), moduleObject);
      }

      return functionObject;
   }

   private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
      if (setting instanceof BooleanSetting booleanSetting) {
         moduleObject.addProperty(setting.getName(), booleanSetting.isValue());
      }

      if (setting instanceof SliderSettings valueSetting) {
         moduleObject.addProperty(setting.getName(), valueSetting.getValue());
      }

      if (setting instanceof ColorSetting colorSetting) {
         moduleObject.addProperty(setting.getName(), colorSetting.getColor());
      }

      if (setting instanceof BindSetting bindSetting) {
         moduleObject.addProperty(setting.getName(), bindSetting.getKey());
         if (bindSetting.isCombo()) {
            moduleObject.addProperty(setting.getName() + "_second", bindSetting.getSecondKey());
         }
      }

      if (setting instanceof TextSetting textSetting) {
         moduleObject.addProperty(setting.getName(), textSetting.getText());
      }

      if (setting instanceof SelectSetting selectSetting) {
         moduleObject.addProperty(setting.getName(), selectSetting.getSelected());
      }

      if (setting instanceof RadioSetting radioSetting) {
         moduleObject.addProperty(setting.getName(), radioSetting.get());
      }

      if (setting instanceof MultiSelectSetting multiSelectSetting) {
         List<String> selected = multiSelectSetting.getSelected();
         String selectedAsString = String.join(",", selected);
         moduleObject.addProperty(setting.getName(), selectedAsString);
      }

      if (setting instanceof GroupSetting groupSetting) {
         JsonObject groupObject = new JsonObject();
         groupObject.addProperty("state", groupSetting.isValue());

         for (Setting subSetting : groupSetting.getSubSettings()) {
            this.addSettingToJsonObject(groupObject, subSetting);
         }

         moduleObject.add(setting.getName(), groupObject);
      }
   }

   private void writeJsonToFile(JsonObject functionObject, File file) throws FileSaveException {
      try {
         try (FileWriter writer = new FileWriter(file)) {
            this.GSON.toJson(functionObject, writer);
         }
      } catch (IOException var8) {
         throw new FileSaveException("Failed to save module to file", var8);
      }
   }

   private JsonObject readJsonFromFile(File file) throws FileLoadException {
      try {
         JsonObject var3;
         try (FileReader reader = new FileReader(file)) {
            var3 = JsonParser.parseReader(reader).getAsJsonObject();
         }

         return var3;
      } catch (IOException var7) {
         throw new FileLoadException("Failed to load module from file", var7);
      } catch (JsonSyntaxException | JsonIOException var8) {
         throw new FileLoadException("Failed to parse JSON from file", var8);
      }
   }

   public void resetConfiguration(File configDirectory) throws FileSaveException, FileLoadException {
      File defaultFile = new File(configDirectory, "default.json");

      for (Module module : this.moduleRepository.modules()) {
         if (!(module instanceof ClickGui) && !(module instanceof Globals)) {
            if (module.isState()) {
               module.setState(false);
            }

            module.setKey(-1);
            module.setSecondKey(-1);

            for (Setting setting : module.settings()) {
               this.resetSettingToDefault(setting);
            }
         }
      }

      JsonObject clickGuiSnapshot = this.createJsonObjectFromModules().getAsJsonObject("clickgui");
      JsonObject resetJson;
      if (defaultFile.exists()) {
         resetJson = this.readJsonFromFile(defaultFile);

         for (String moduleKey : resetJson.keySet()) {
            if (resetJson.get(moduleKey).isJsonObject() && !"clickgui".equals(moduleKey)) {
               if ("globals".equals(moduleKey)) {
                  JsonObject globalsObject = resetJson.getAsJsonObject(moduleKey);
                  globalsObject.addProperty("state", true);
               } else {
                  JsonObject moduleObject = resetJson.getAsJsonObject(moduleKey);
                  moduleObject.addProperty("state", false);
                  moduleObject.addProperty("bind", -1);
                  moduleObject.remove("secondBind");
               }
            }
         }

         if (resetJson.has("attackaura")) {
            resetJson.getAsJsonObject("attackaura").addProperty("Ротация", "Фантайм Тест");
         }

         if (resetJson.has("aura")) {
            JsonObject auraConfig = resetJson.getAsJsonObject("aura");
            resetJson.add("attackaura", auraConfig);
            resetJson.remove("aura");
         }
      } else {
         resetJson = this.createJsonObjectFromModules();
      }

      if (clickGuiSnapshot != null) {
         clickGuiSnapshot.addProperty("bind", 344);
         clickGuiSnapshot.addProperty("state", true);
         clickGuiSnapshot.remove("secondBind");
         resetJson.add("clickgui", clickGuiSnapshot);
      }

      this.updateModulesFromJsonObject(resetJson);
      ClickGui.enforceOpenKey();
      Globals.enforceEnabled();
      NoRender.enforceDefaults();
      this.writeJsonToFile(resetJson, new File(configDirectory, "autoCfg.json"));
   }

   private void resetSettingToDefault(Setting setting) {
   }

   private void deleteConfigFiles(File configDirectory) {
      File[] files = configDirectory.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
               file.delete();
            }
         }
      }
   }

   private void updateModulesFromJsonObject(JsonObject functionObject) {
      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = functionObject.getAsJsonObject(module.getName().toLowerCase());
         if (moduleObject != null) {
            if (module instanceof ClickGui) {
               if (moduleObject.has("state")) {
                  module.setState(moduleObject.get("state").getAsBoolean());
               }
            } else if (module instanceof Globals) {
               if (moduleObject.has("bind")) {
                  module.setKey(moduleObject.get("bind").getAsInt());
                  if (moduleObject.has("secondBind")) {
                     module.setSecondKey(moduleObject.get("secondBind").getAsInt());
                  } else {
                     module.setSecondKey(-1);
                  }
               }

               module.setState(true);
            } else if (moduleObject.has("bind") && moduleObject.has("state")) {
               module.setKey(moduleObject.get("bind").getAsInt());
               if (moduleObject.has("secondBind")) {
                  module.setSecondKey(moduleObject.get("secondBind").getAsInt());
               } else {
                  module.setSecondKey(-1);
               }

               module.setState(moduleObject.get("state").getAsBoolean());
            }

            module.settings().forEach(setting -> this.updateSettingFromJsonObject(moduleObject, setting));
         }
      }

      ClickGui.enforceOpenKey();
      Globals.enforceEnabled();
   }

   private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
      JsonElement settingElement = moduleObject.get(setting.getName());
      if (settingElement != null && !settingElement.isJsonNull()) {
         if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.setValue(settingElement.getAsBoolean());
         }

         if (setting instanceof SliderSettings valueSetting) {
            valueSetting.setValue(settingElement.getAsFloat());
         }

         if (setting instanceof ColorSetting colorSetting) {
            colorSetting.setColor(settingElement.getAsInt());
         }

         if (setting instanceof BindSetting bindSetting) {
            try {
               if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isBoolean()) {
                  bindSetting.setKey(-1);
               } else {
                  bindSetting.setKey(settingElement.getAsInt());
               }

               JsonElement secondKeyElement = moduleObject.get(setting.getName() + "_second");
               if (secondKeyElement != null && !secondKeyElement.isJsonNull()) {
                  bindSetting.setSecondKey(secondKeyElement.getAsInt());
               }
            } catch (NumberFormatException var10) {
               bindSetting.setKey(-1);
            }
         }

         if (setting instanceof TextSetting textSetting) {
            textSetting.setText(settingElement.getAsString());
         }

         if (setting instanceof SelectSetting selectSetting) {
            selectSetting.setSelected(settingElement.getAsString());
         }

         if (setting instanceof RadioSetting radioSetting) {
            String value = settingElement.getAsString();
            if ("Ротация".equals(setting.getName())) {
               value = this.migrateAuraRotation(value, moduleObject);
            }

            for (String option : radioSetting.getOptions()) {
               if (option.equals(value)) {
                  radioSetting.set(value);
                  break;
               }
            }
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            String asString = settingElement.getAsString();
            ArrayList<String> selectedList = new ArrayList<>(Arrays.asList(asString.split(",")));
            selectedList.removeIf(s -> !multiSelectSetting.getList().contains(s));
            multiSelectSetting.setSelected(selectedList);
         }

         if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupObject = settingElement.getAsJsonObject();
            if (groupObject.has("state")) {
               groupSetting.setValue(groupObject.get("state").getAsBoolean());
            }

            for (Setting subSetting : groupSetting.getSubSettings()) {
               this.updateSettingFromJsonObject(groupObject, subSetting);
            }
         }
      }
   }

   private String migrateAuraRotation(String value, JsonObject moduleObject) {
      if ("NoRotate".equals(value)) {
         return "Фантайм Тест";
      } else if ("Sloth 2".equals(value) || "Astrum".equals(value)) {
         return "Sloth";
      } else {
         return "FT-New".equals(value) ? "Фантайм Тест" : value;
      }
   }
}
