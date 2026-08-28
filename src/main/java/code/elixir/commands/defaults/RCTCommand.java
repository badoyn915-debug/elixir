package code.elixir.commands.defaults;

import code.elixir.Elixir;
import code.elixir.common.repository.rct.RCTRepository;
import code.elixir.display.hud.Notifications;
import code.elixir.features.impl.misc.HolyWorldAutoJoin;
import code.elixir.utils.client.managers.api.command.Command;
import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

public class RCTCommand extends Command implements QuickImports {
   private final RCTRepository repository;

   protected RCTCommand(Elixir main) {
      super("rct");
      this.repository = main.getRCTRepository();
   }

   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (!Network.isHolyWorld() && !Network.isReallyWorld() && !Network.isSpookyTime() && !Network.isFunTime()) {
         Notifications.getInstance().addList("[RCT] Не работает на этом " + Formatting.RED + "сервере", 3000);
      } else if (Network.isPvp()) {
         Notifications.getInstance().addList("[RCT] Вы находитесь в режиме " + Formatting.RED + "пвп", 3000);
      } else if (Network.isHolyWorld()) {
         HolyWorldAutoJoin autoJoin = Elixir.getInstance().getHolyWorldAutoJoin();
         if (autoJoin == null) {
            Notifications.getInstance().addList("[RCT] HolyWorldAutoJoin не найден", 3000);
         } else {
            if (args.hasAny()) {
               args.requireMin(1);
               int anarchy = args.getArgs().getFirst().getAs(Integer.class);
               autoJoin.reset();
               autoJoin.setTargetAnarchy(anarchy);
               autoJoin.setActive(true);
            } else {
               int currentAnarchy = Network.getAnarchy();
               if (currentAnarchy > 0) {
                  if (!autoJoin.isActive()) {
                     autoJoin.setActive(true);
                  } else {
                     autoJoin.reset();
                  }
               } else {
                  Notifications.getInstance().addList("[RCT] Не удалось определить номер " + Formatting.RED + "анархии", 3000);
               }
            }
         }
      } else {
         if (args.hasAny()) {
            args.requireMin(1);
            int anarchy = args.getArgs().getFirst().getAs(Integer.class);
            if (!Network.isFunTime() && !Network.isSpookyTime()) {
               this.repository.reconnect(anarchy);
            } else if (mc.player != null && mc.player.networkHandler != null) {
               mc.player.networkHandler.sendChatMessage("/an" + anarchy);
               Notifications.getInstance().addList("[RCT] Переход на анархию #" + Formatting.BLUE + anarchy + Formatting.RESET, 3000);
            }
         } else {
            int currentAnarchy = Network.getAnarchy();
            if (currentAnarchy > 0) {
               this.repository.reconnect(currentAnarchy);
            } else {
               Notifications.getInstance().addList("[RCT] Не удалось определить номер " + Formatting.RED + "анархии", 3000);
            }
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Автоматически перезаходит на анархию";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Перезаходит на анархию", "", "Использование:", "> rct <anarchy> - Заходит на <anarchy>", "> rct - Перезаходит на анархию где вы только что были"
      );
   }
}
