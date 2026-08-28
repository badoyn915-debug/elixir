package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ThemeCommand extends Command {
   protected ThemeCommand(Elixir main) {
      super("theme");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         Paginator.paginate(args, new Paginator<>(this.getThemes()), () -> this.logDirect("Список тем:"), theme -> {
            MutableText namesComponent = Text.literal(theme);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            return namesComponent;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
      } else {
         String arg = args.getString().toLowerCase(Locale.US);
         args.requireMax(1);
         if (arg.contains("load")) {
            String name = args.getString();
            if (ThemeManager.themeExists(name)) {
               if (ThemeManager.loadTheme(name)) {
                  this.logDirect(String.format("Тема %s загружена!", name));
               } else {
                  this.logDirect(String.format("Ошибка при загрузке темы %s!", name), Formatting.RED);
               }
            } else {
               this.logDirect(String.format("Тема %s не найдена!", name), Formatting.RED);
            }
         } else if (arg.contains("save")) {
            String name = args.getString();
            if (ThemeManager.saveTheme(name)) {
               this.logDirect(String.format("Тема %s сохранена!", name));
            } else {
               this.logDirect(String.format("Ошибка при сохранении темы %s!", name), Formatting.RED);
            }
         } else if (arg.contains("remove")) {
            String name = args.getString();
            if (name.equals("default")) {
               this.logDirect("Нельзя удалить тему 'default'!", Formatting.RED);
               return;
            }

            if (ThemeManager.deleteTheme(name)) {
               this.logDirect(String.format("Тема %s удалена!", name));
            } else {
               this.logDirect(String.format("Ошибка при удалении темы %s или тема не найдена!", name), Formatting.RED);
            }
         } else if (arg.contains("list")) {
            Paginator.paginate(args, new Paginator<>(this.getThemes()), () -> this.logDirect("Список тем:"), theme -> {
               MutableText namesComponent = Text.literal(theme);
               namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
               return namesComponent;
            }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
         } else {
            this.logDirect("Неизвестная команда! Используйте: load, save, remove, list", Formatting.RED);
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("load", "save", "remove", "list").filterPrefix(arg).stream();
         }

         if (arg.equalsIgnoreCase("load") || arg.equalsIgnoreCase("remove")) {
            return this.getThemes().stream().filter(theme -> !theme.equals("default")).map(theme -> (String)theme);
         }

         if (arg.equalsIgnoreCase("save")) {
            return Stream.of("<название>");
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Позволяет взаимодействовать с темами в чите";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно загружать/сохранять/удалять темы",
         "",
         "Использование:",
         "> theme load <name> - Загружает тему.",
         "> theme save <name> - Сохраняет тему.",
         "> theme remove <name> - Удаляет тему.",
         "> theme list - Возвращает список тем."
      );
   }

   private List<String> getThemes() {
      ArrayList<String> themes = new ArrayList<>();
      String[] availableThemes = ThemeManager.getAvailableThemes();
      themes.add("default_dark");

      for (String theme : availableThemes) {
         if (!themes.contains(theme)) {
            themes.add(theme);
         }
      }

      return themes;
   }
}
