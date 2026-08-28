package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.TimerUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

public class AutoDuel extends Module implements QuickImports {
   private final SelectSetting kits = new SelectSetting("Разрешенные киты", "auto_duel.kits.desc")
      .value("Щиты", "Шипы 3", "Лук", "Тотемы", "НоДебафф", "Шары", "Классик", "Читерский рай", "Без эндер-жемчуга")
      .selected("Шары");
   private final List<String> sent = new ArrayList<>();
   private final TimerUtil duelDelay = TimerUtil.create();
   private final TimerUtil clearSentDelay = TimerUtil.create();
   private final TimerUtil kitChoiceDelay = TimerUtil.create();
   private final TimerUtil duelSetupDelay = TimerUtil.create();
   static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

   public AutoDuel() {
      super("AutoDuel", ModuleCategory.MISC);
      this.setup(new Setting[]{this.kits});
   }

   @Override
   public void activate() {
      this.sent.clear();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         List<String> players = this.getOnlinePlayers();
         if (this.clearSentDelay.hasTimeElapsed(800L * Math.max(players.size(), 1))) {
            this.sent.clear();
            this.clearSentDelay.resetCounter();
         }

         for (String string : players) {
            if (!this.sent.contains(string) && !string.equals(mc.player.getGameProfile().getName())) {
               if (this.duelDelay.hasTimeElapsed(1000L)) {
                  mc.player.networkHandler.sendChatMessage("/duel " + string);
                  this.sent.add(string);
                  this.duelDelay.resetCounter();
               }
               break;
            }
         }

         Screen class_4372 = mc.currentScreen;
         if (mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)class_4372;
            String stringx = screen.getTitle().getString();
            if (stringx.contains("Выбор набора (1/1)") || stringx.contains("Kit selection")) {
               this.handleKitSelection();
            } else if (stringx.contains("Настройка поединка") || stringx.contains("Duel setup")) {
               this.handleDuelSetup();
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> class_25962 = e.getPacket();
      GameMessageS2CPacket packet;
      String text;
      if (class_25962 instanceof GameMessageS2CPacket
         && (
            (text = (packet = (GameMessageS2CPacket)class_25962).content().getString().toLowerCase()).contains("начало")
                  && text.contains("через")
                  && text.contains("секунд")
               || text.contains("дуэли » во время поединка запрещено использовать команды")
               || text.contains("duel") && text.contains("during") && text.contains("forbidden")
         )
         && this.isState()) {
         this.switchState();
      }
   }

   private List<String> getOnlinePlayers() {
      return mc.getNetworkHandler()
         .getPlayerList()
         .stream()
         .map(entry -> entry.getProfile().getName())
         .filter(name -> NAME_PATTERN.matcher(name).matches())
         .collect(Collectors.toList());
   }

   private void handleKitSelection() {
      List<String> list = this.kits.getList();
      if (list != null && !list.isEmpty()) {
         ArrayList<Integer> slots = new ArrayList<>();

         for (int i = 0; i < list.size(); i++) {
            if (this.kits.isSelected(list.get(i))) {
               slots.add(i);
            }
         }

         if (!slots.isEmpty()) {
            Collections.shuffle(slots);
            int slotId = slots.get(0);
            if (this.kitChoiceDelay.hasTimeElapsed(90L)) {
               InventoryTask.clickSlot(slotId, 0, SlotActionType.QUICK_MOVE, false);
               this.kitChoiceDelay.resetCounter();
            }
         }
      }
   }

   private void handleDuelSetup() {
      if (this.duelSetupDelay.hasTimeElapsed(90L)) {
         InventoryTask.clickSlot(0, 0, SlotActionType.QUICK_MOVE, false);
         this.duelSetupDelay.resetCounter();
      }
   }
}
