package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.common.repository.way.WayRepository;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.IBaritoneChatControl;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.datatypes.WayDataType;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.Paginator;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class WayCommand extends Command implements QuickImports {
   private final WayRepository wayRepository;

   protected WayCommand(Elixir main) {
      super("way", "gps");
      this.wayRepository = main.getWayRepository();
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         this.handleListWays(label, args);
      } else {
         String var4 = args.peekString().toLowerCase(Locale.US);
         switch (var4) {
            case "add":
               args.getString();
               this.handleAddWay(args);
               break;
            case "remove":
               args.getString();
               this.handleRemoveWay(args);
               break;
            case "clear":
               args.getString();
               this.handleClearWays();
               break;
            case "list":
               args.getString();
               this.handleListWays(label, args);
               break;
            default:
               this.handleAddWayShort(args);
         }
      }
   }

   private void handleAddWayShort(IArgConsumer args) throws CommandException {
      String address = "vanilla";
      String first = args.peekString();
      boolean firstIsNumber = false;

      try {
         Integer.parseInt(first);
         firstIsNumber = true;
      } catch (NumberFormatException var9) {
      }

      address = mc.getNetworkHandler() == null
         ? "vanilla"
         : (mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address);
      if (firstIsNumber && args.has(3)) {
         int x = args.getAs(Integer.class);
         int y = args.getAs(Integer.class);
         int z = args.getAs(Integer.class);
         String name = "Метка";
         this.wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
         this.logDirect("Метка сохранена: " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
      } else if (!firstIsNumber || !args.has(2)) {
         String name = args.getString();
         if (args.has(3)) {
            int x = args.getAs(Integer.class);
            int y = args.getAs(Integer.class);
            int z = args.getAs(Integer.class);
            this.wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
            this.logDirect("Метка сохранена " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
         } else if (!args.hasAny()) {
            if (mc.player == null) {
               this.logDirect("Игрок не найден", Formatting.RED);
               return;
            }

            BlockPos pos = mc.player.getBlockPos();
            this.wayRepository.upsertWay(name, pos, address);
            this.logDirect("Метка добавлена: " + name + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "), Сервер: " + address, Formatting.WHITE);
         } else {
            args.requireMax(0);
         }
      } else if (mc.player == null) {
         this.logDirect("Игрок не найден", Formatting.RED);
      } else {
         int x = args.getAs(Integer.class);
         int z = args.getAs(Integer.class);
         int y = mc.player.getBlockPos().getY();
         String name = "Метка";
         this.wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
         this.logDirect("Метка сохранена: " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
      }
   }

   private void handleAddWay(IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         if (mc.player == null) {
            this.logDirect("Игрок не найден", Formatting.RED);
         } else {
            BlockPos pos = mc.player.getBlockPos();
            String name = "Метка";
            String address = mc.getNetworkHandler() == null
               ? "vanilla"
               : (mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address);
            this.wayRepository.upsertWay(name, pos, address);
            this.logDirect("Метка добавлена: " + name + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "), Сервер: " + address, Formatting.WHITE);
         }
      } else {
         args.requireMin(4);
         String name = args.getString();
         int x = args.getArgs().get(0).getAs(Integer.class);
         int y = args.getArgs().get(1).getAs(Integer.class);
         int z = args.getArgs().get(2).getAs(Integer.class);
         String address = mc.getNetworkHandler() == null
            ? "vanilla"
            : (mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address);
         this.wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
         this.logDirect("Метка сохранена " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
      }
   }

   private void handleRemoveWay(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      String name = args.getString();
      if (this.wayRepository.hasWay(name)) {
         this.wayRepository.deleteWay(name);
         this.logDirect(Formatting.GREEN + "Метка " + Formatting.RED + name + Formatting.GREEN + " была успешна удалена!");
      } else {
         this.logDirect("Метка с названием '" + name + "' не найдена!");
      }
   }

   private void handleListWays(String label, IArgConsumer args) throws CommandException {
      args.requireMax(1);
      Paginator.paginate(
         args,
         new Paginator<>(this.wayRepository.wayList),
         () -> this.logDirect("Список меток:"),
         way -> Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + way.name())
            .append(
               Text.literal(
                     Formatting.GRAY + " Координаты: " + Formatting.WHITE + " (" + way.pos().getX() + ", " + way.pos().getY() + ", " + way.pos().getZ() + ")"
                  )
                  .append(Text.literal(Formatting.GRAY + " Сервер: " + Formatting.WHITE + way.server()))
            ),
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
      );
   }

   private void handleClearWays() {
      this.wayRepository.clearList();
      this.logDirect(Formatting.GREEN + "Все метки были удалены.");
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (!args.hasAny()) {
         return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").stream();
      } else {
         String first = args.peekString().toLowerCase(Locale.US);
         if (first.equals("remove")) {
            args.getString();
            return args.hasExactlyOne() ? args.tabCompleteDatatype(WayDataType.INSTANCE) : Stream.empty();
         } else if (first.equals("add")) {
            args.getString();
            String hint = args.has(4) ? "" : (args.has(3) ? "z" : (args.has(2) ? "y" : (args.has(1) ? "x" : "Название")));
            return new TabCompleteHelper().sortAlphabetically().prepend(hint).stream();
         } else if (!first.equals("list") && !first.equals("clear")) {
            int left = args.getArgs().size();
            String hint = left >= 4 ? "" : (left == 3 ? "z" : (left == 2 ? "y" : (left == 1 ? "x" : "название")));
            return new TabCompleteHelper().sortAlphabetically().prepend(hint).stream();
         } else {
            return Stream.empty();
         }
      }
   }

   @Override
   public String getShortDesc() {
      return "Позволяет ставить метки в мире";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно добавлять/удалять метки в мире (.way и .gps)",
         "",
         "Использование (короткая форма — без add):",
         "> way <name> - Добавляет метку на текущих координатах",
         "> way <name> <x> <y> <z> - Добавляет метку по координатам",
         "> way <x> <y> <z> - Добавляет метку с автоименем по координатам",
         "> way <x> <z> - Добавляет метку с автоименем (Y = текущий Y)",
         "> way add - То же, метка на текущих координатах (имя = координаты)",
         "> way add <name> <x> <y> <z> - Добавляет/перезаписывает метку",
         "> way remove <name> - Удаляет метку",
         "> way list - Список меток",
         "> way clear - Очищает все метки"
      );
   }
}
