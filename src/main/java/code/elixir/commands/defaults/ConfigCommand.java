package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.main.client.ClientInfoProvider;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.ConfigFileDataType;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.client.managers.file.FileController;
import code.elixir.utils.client.managers.file.FileRepository;
import code.elixir.utils.client.managers.file.exception.FileProcessingException;
import code.elixir.utils.config.ConfigManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfigCommand extends Command {
   private final FileController fileController;
   private final ClientInfoProvider clientInfoProvider;

   protected ConfigCommand(Elixir main) {
      super("config", "cfg");
      this.fileController = main.getFileController();
      this.clientInfoProvider = main.getClientInfoProvider();
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      args.requireMax(1);
      if (arg.contains("load")) {
         String name = args.getString();
         if (new File(this.clientInfoProvider.configsDir(), name + ".json").exists()) {
            try {
               FileRepository fileRepository = new FileRepository();
               fileRepository.setup(Elixir.getInstance());
               FileController fileController = new FileController(
                  fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir()
               );
               fileController.loadFile(name + ".json");
               ChatMessage.brandmessage(String.format("Конфигурация %s загружена!", name));
            } catch (FileProcessingException var9) {
               this.logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", var9.getCause().getMessage()), Formatting.RED);
            }
         } else {
            ChatMessage.brandmessage(String.format("Конфигурация %s не найдена!", name));
         }
      }

      if (arg.contains("save")) {
         String name = args.getString();

         try {
            this.fileController.saveFile(name + ".json");
            ChatMessage.brandmessage(String.format("Конфигурация %s сохранена!", name));
            System.out.println("loaded");
         } catch (Exception var8) {
            System.out.println(var8.getMessage());
            System.out.printf("error %s%n", var8.getCause().getMessage());
            this.logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", var8.getCause().getMessage()), Formatting.RED);
         }
      }

      if (arg.contains("list")) {
         Paginator.paginate(args, new Paginator<>(this.getConfigs()), () -> this.logDirect("Список конфигов:"), config -> {
            MutableText namesComponent = Text.literal(config);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            return namesComponent;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
      }

      if (arg.contains("dir")) {
         try {
            Runtime.getRuntime().exec("explorer " + this.clientInfoProvider.configsDir().getAbsolutePath());
         } catch (IOException var7) {
            ChatMessage.brandmessage("Папка с конфигурациями не найдена!" + var7.getMessage());
         }
      }

      if (arg.contains("reset")) {
         if (ConfigManager.resetConfig()) {
            ChatMessage.brandmessage("Все настройки сброшены к дефолтным значениям!");
         } else {
            this.logDirect("Ошибка при сбросе конфига!", Formatting.RED);
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("load", "save", "list", "dir", "reset").filterPrefix(arg).stream();
         }

         if (arg.equalsIgnoreCase("load")) {
            return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
         }

         if (arg.equalsIgnoreCase("save")) {
            return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Позволяет взаимодействовать с конфигами в чите";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно загружать/сохранять конфиги",
         "",
         "Использование:",
         "> config load <name> - Загружает конфиг.",
         "> config save <name> - Сохраняет конфиг.",
         "> config list - Возвращает список конфигов",
         "> config dir - Открывает папку с конфигами.",
         "> config reset - Сбрасывает все настройки к дефолтным значениям."
      );
   }

   public List<String> getConfigs() {
      ArrayList<String> configs = new ArrayList<>();
      File[] configFiles = Elixir.getInstance().getClientInfoProvider().configsDir().listFiles();
      if (configFiles != null) {
         for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(".json")) {
               String configName = configFile.getName().replace(".json", "");
               configs.add(configName);
            }
         }
      }

      return configs;
   }
}
