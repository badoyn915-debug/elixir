package code.elixir.commands.defaults;

import code.elixir.common.repository.staff.StaffRepository;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.StaffDataType;
import code.elixir.utils.client.managers.api.command.datatypes.TabPlayerDataType;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StaffCommand extends Command {
   public StaffCommand() {
      super("staff");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String var4 = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (var4) {
         case "add":
            args.requireMin(1);
            String namex = args.getString();
            if (!StaffRepository.isStaff(namex)) {
               StaffRepository.addStaff(namex);
               this.save();
               this.logDirect("Вы успешно добавили " + Formatting.GREEN + namex + Formatting.GRAY + " в список персонала!");
            } else {
               this.logDirect(Formatting.RED + namex + " уже есть в списке персонала!", Formatting.RED);
            }
            break;
         case "remove":
            args.requireMin(1);
            String name = args.getString();
            if (StaffRepository.isStaff(name)) {
               StaffRepository.removeStaff(name);
               this.save();
               this.logDirect("Вы успешно удалили " + Formatting.RED + name + Formatting.GRAY + " из списка персонала!");
            } else {
               this.logDirect(Formatting.RED + name + " не найден в списке персонала", Formatting.RED);
            }
            break;
         case "list":
            args.requireMax(1);
            Paginator.paginate(args, new Paginator<>(StaffRepository.getStaff()), () -> this.logDirect("Список персонала:"), staff -> {
               MutableText namesComponent = Text.literal(staff.getName());
               namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
               return namesComponent;
            }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
            break;
         case "clear":
            args.requireMax(0);
            StaffRepository.clear();
            this.save();
            this.logDirect("Список персонала очищен.");
            break;
         default:
            this.logDirect("Неизвестная подкоманда. Используйте: add, remove, list, clear");
      }
   }

   private void save() {
      try {
         Method saveMethod = StaffRepository.class.getDeclaredMethod("save");
         saveMethod.setAccessible(true);
         saveMethod.invoke(null);
      } catch (ReflectiveOperationException var2) {
         throw new RuntimeException("Ошибка при сохранении списка персонала через рефлексию", var2);
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasExactlyOne()) {
         return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
      } else {
         if (args.hasAtMost(2)) {
            String arg = args.peekString(0).toLowerCase(Locale.US);
            if (arg.equals("add")) {
               return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
            }

            if (arg.equals("remove")) {
               return args.tabCompleteDatatype(StaffDataType.INSTANCE);
            }
         }

         return Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Управление списком персонала.";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Эта команда позволяет управлять списком персонала.",
         "",
         "Использование:",
         "> staff add <ник> - Добавляет игрока в список персонала.",
         "> staff remove <ник> - Удаляет игрока из списка персонала.",
         "> staff list - Показывает список персонала.",
         "> staff clear - Очищает список персонала."
      );
   }
}
