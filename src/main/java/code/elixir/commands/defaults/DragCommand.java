package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.display.hud.Notifications;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.managers.api.draggable.DraggableRepository;
import code.elixir.utils.client.managers.file.FileController;
import code.elixir.utils.client.managers.file.impl.ElementsFile;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class DragCommand extends Command implements QuickImports {
   protected DragCommand() {
      super("drag");
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      Logger.info("[DragCommand] Команда execute вызвана, label: " + label);
      Logger.info("[DragCommand] args.hasAny() = " + args.hasAny());

      try {
         if (!args.hasAny()) {
            Logger.info("[DragCommand] Аргументы отсутствуют");
            ChatMessage.brandmessage("Использование: .drag reset");
         } else {
            Logger.info("[DragCommand] Аргументы есть, получаю первую подкоманду...");
            String subCommand = args.getString();
            Logger.info("[DragCommand] Получена подкоманда: '" + subCommand + "'");
            if (subCommand.equalsIgnoreCase("reset")) {
               Logger.info("[DragCommand] Выполняется reset");
               this.resetAllDraggables();
               Notifications.getInstance().addList("Позиции всех элементов HUD " + Formatting.GREEN + "сброшены", 3000);
               Logger.info("[DragCommand] Reset завершен успешно");
            } else {
               Logger.warn("[DragCommand] Неизвестная подкоманда: " + subCommand);
               ChatMessage.brandmessage("Неизвестная подкоманда: " + subCommand);
               ChatMessage.brandmessage("Использование: .drag reset");
            }
         }
      } catch (Exception var4) {
         Logger.error("[DragCommand] Ошибка при выполнении команды: " + var4.getMessage());
         var4.printStackTrace();
         throw var4;
      }
   }

   private void resetAllDraggables() {
      DraggableRepository repo = Elixir.getInstance().getDraggableRepository();
      Logger.info("[DragCommand] Начало сброса позиций элементов. Всего элементов: " + repo.draggable().size());
      int resetCount = 0;

      for (AbstractDraggable draggable : repo.draggable()) {
         String name = draggable.getName();
         int oldX = draggable.getX();
         int oldY = draggable.getY();
         Logger.info("[DragCommand] Обработка элемента: '" + name + "' (текущая позиция: " + oldX + ", " + oldY + ")");
         short newX;
         short newY;
         if (name.equals("Info")) {
            newX = 0;
            newY = 0;
         } else if (name.equals("Watermark")) {
            newX = 10;
            newY = 10;
         } else if (name.equals("Target Hud")) {
            newX = 10;
            newY = 80;
         } else if (name.equals("Keybinds")) {
            newX = 300;
            newY = 40;
         } else if (name.equals("Armor")) {
            newX = 10;
            newY = 120;
         } else if (name.equals("Cooldowns")) {
            newX = 10;
            newY = 40;
         } else if (name.equals("Potions")) {
            newX = 10;
            newY = 200;
         } else if (name.equals("Binds")) {
            newX = 10;
            newY = 180;
         } else if (name.equals("Notifications")) {
            newX = 0;
            newY = 350;
         } else if (name.equals("ArrayList")) {
            newX = 10;
            newY = 120;
         } else {
            if (!name.equals("Inventory")) {
               Logger.warn("[DragCommand] Элемент '" + name + "' не найден в списке для сброса");
               continue;
            }

            newX = 385;
            newY = 40;
         }

         if (newX == oldX && newY == oldY) {
            Logger.info("[DragCommand] Позиция элемента '" + name + "' уже правильная (" + newX + ", " + newY + ")");
         } else {
            draggable.setX(newX);
            draggable.setY(newY);
            resetCount++;
            Logger.info("[DragCommand] Позиция элемента '" + name + "' изменена с (" + oldX + ", " + oldY + ") на (" + newX + ", " + newY + ")");
         }
      }

      Logger.info("[DragCommand] Сброшено позиций: " + resetCount + " из " + repo.draggable().size());

      try {
         FileController fileController = Elixir.getInstance().getFileController();
         Logger.info("[DragCommand] Сохранение файла элементов...");
         fileController.saveFile(ElementsFile.class);
         Logger.info("[DragCommand] Файл элементов успешно сохранен");
      } catch (Exception var10) {
         Logger.error("[DragCommand] Ошибка при сохранении файла элементов: " + var10.getMessage());
         var10.printStackTrace();
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return args.hasAny() && args.getArgs().size() == 1 ? Stream.of("reset") : Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Управление позициями элементов HUD";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Управление позициями элементов HUD", "", "Использование:", "> drag reset - Сбрасывает позиции всех элементов HUD на дефолтные");
   }
}
