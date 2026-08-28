package code.elixir.features.impl.misc.serverhelper;

import code.elixir.events.chat.ChatEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.misc.ServerHelper;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;

public class ReallyworldHandler extends ServerHelperHandler {
   private static final Set<String> BAN_PATTERNS = new HashSet<>(
      Arrays.asList(
         "акриен(а|у|ом|е|чик)?",
         "рич(а|у|ом|ей|е)?",
         "ньюкод(ом|а|у|ами|ик|е)?",
         "экспенсив(ом|а|у|ами|е)?",
         "импакт(ом|а|у|ами|ик|е)?",
         "экселлент(ом|а|у|ами|ик|е)?",
         "экселент(ом|а|у|ами|ик)?",
         "катлаван(ом|а|у|ами|чик)?",
         "катлован(ом|а|у|ами|чик)?",
         "целестиал(ом|а|у|ами|е)?",
         "целк(ой|а|у|ами|очка|е)?",
         "матикс(ом|а|у|ами|е)?",
         "инерти(я|е|ей|ю|ями|е)?",
         "эксп(а|ой|ою|у|уличка|е)?",
         "флюгер(ом|а|у|ами)?",
         "рикер(а|у|ом|очек)?",
         "фанпей",
         "фантайм(е|а|у)?",
         "холиворлд",
         "wwe",
         "hvh",
         "rogalik",
         "thunderhack",
         "liquidbounce",
         "expensive",
         "celestial",
         "newcode",
         "arbuz",
         "akrien",
         "nursultan",
         "relake",
         "wild",
         "wurst",
         "catlovan",
         "excellent",
         "rockstar",
         "catlavan",
         "impact",
         "matix",
         "inertia",
         "wex",
         "wexside",
         "nurik",
         "nurlan",
         "rich",
         "funpay",
         "fluger",
         "riker",
         "funtime",
         "holyworld"
      )
   );
   private final List<BindSetting> binds;
   private final BooleanSetting closeMenu;
   private final BooleanSetting chatFilter;

   public ReallyworldHandler(List<BindSetting> binds, BooleanSetting closeMenu, BooleanSetting chatFilter) {
      this.binds = binds;
      this.closeMenu = closeMenu;
      this.chatFilter = chatFilter;
   }

   @Override
   public String getModeId() {
      return "Reallyworld";
   }

   @Override
   public String getOverlayTitle() {
      return "RW";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();
      items.add(
         new ServerHelperRenderItem("Анти полет", "AP", findSlotByItem(Items.FIREWORK_STAR) == -1 ? "—" : "✓", Items.FIREWORK_STAR)
            .withBind(this.binds.isEmpty() ? -1 : this.binds.get(0).getKey())
      );
      this.addPotion(items, "Зелье Гринча", "гринч", 1, ColorAssist.getColor(50, 205, 50, 200));
      items.add(new ServerHelperRenderItem("AutoShift", "H", "✓", Items.PLAYER_HEAD).withBind(this.binds.size() > 2 ? this.binds.get(2).getKey() : -1));
      this.addPotion(items, "Новогодний ужас", "новогодний ужас", 3, ColorAssist.getColor(220, 20, 60, 200));
      this.addPotion(items, "Эссенция кромешника", "эссенция кромешника", 4, ColorAssist.getColor(75, 0, 130, 200));
      this.addPotion(items, "Зелье Снежок", "снежок", 5, ColorAssist.getColor(135, 206, 250, 200));
      this.addPotion(items, "Ловушка", "ловуш", 6, ColorAssist.getColor(255, 69, 0, 200));
      return items;
   }

   private void addPotion(List<ServerHelperRenderItem> items, String name, String search, int bindIndex, int tint) {
      int slot = this.findPotionSlot(search);
      String shortName = name.length() > 8 ? name.substring(0, 8) : name;
      items.add(
         new ServerHelperRenderItem(name, shortName, slot == -1 ? "—" : "✓", Items.SPLASH_POTION)
            .withBind(this.binds.size() > bindIndex ? this.binds.get(bindIndex).getKey() : -1)
            .withTint(tint)
      );
   }

   @Override
   public boolean onKey(int keyCode) {
      if (this.binds.isEmpty()) {
         return false;
      } else if (this.binds.get(0).getKey() != -1 && this.binds.get(0).getKey() == keyCode) {
         useAntiFlight();
         return true;
      } else if (this.binds.size() > 1 && this.binds.get(1).getKey() != -1 && this.binds.get(1).getKey() == keyCode) {
         this.usePotion("гринч", "Зелье Гринча");
         return true;
      } else if (this.binds.size() > 2 && this.binds.get(2).getKey() != -1 && this.binds.get(2).getKey() == keyCode) {
         this.pulseShift();
         return true;
      } else if (this.binds.size() > 3 && this.binds.get(3).getKey() != -1 && this.binds.get(3).getKey() == keyCode) {
         this.usePotion("новогодний ужас", "Новогодний ужас");
         return true;
      } else if (this.binds.size() > 4 && this.binds.get(4).getKey() != -1 && this.binds.get(4).getKey() == keyCode) {
         this.usePotion("эссенция кромешника", "Эссенция кромешника");
         return true;
      } else if (this.binds.size() > 5 && this.binds.get(5).getKey() != -1 && this.binds.get(5).getKey() == keyCode) {
         this.usePotion("снежок", "Снежок");
         return true;
      } else if (this.binds.size() > 6 && this.binds.get(6).getKey() != -1 && this.binds.get(6).getKey() == keyCode) {
         this.usePotion("ловуш", "Ловушка");
         return true;
      } else {
         return false;
      }
   }

   private void usePotion(String search, String display) {
      int slot = this.findPotionSlot(search);
      if (slot == -1) {
         notifyMissing(display);
      } else {
         ServerHelper.getInstance().queueSlotUse(slot);
      }
   }

   private int findPotionSlot(String searchQuery) {
      if (mc.player == null) {
         return -1;
      } else {
         String needle = searchQuery.toLowerCase(Locale.ROOT);
         boolean trap = needle.contains("ловуш");
         String trapStem = trap ? "ловушк" : "";

         for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            String cleanName;
            if (!stack.isEmpty() && ((cleanName = cleanName(stack.getName().getString())).contains(needle) || trap && cleanName.contains(trapStem))) {
               return i;
            }
         }

         return -1;
      }
   }

   private void pulseShift() {
      if (mc.options != null) {
         mc.options.sneakKey.setPressed(true);
         new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
               if (QuickImports.mc.options != null) {
                  QuickImports.mc.options.sneakKey.setPressed(false);
               }
            }
         }, 150L);
      }
   }

   @Override
   public void onPacketReceive(PacketEvent event) {
      if (!event.isSend() && mc.player != null) {
         OpenScreenS2CPacket screen;
         Packet<?> class_25962;
         if (this.closeMenu.isValue()
            && (class_25962 = event.getPacket()) instanceof OpenScreenS2CPacket
            && (screen = (OpenScreenS2CPacket)class_25962).getName().getString().contains("ꈁꀀꈂꌁꈂꀁ")
            && mc.player.age < 100) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(screen.getSyncId()));
            event.cancel();
         }
      }
   }

   @Override
   public void onChatSend(ChatEvent event) {
      if (this.chatFilter.isValue()) {
         String message = event.getMessage().toLowerCase(Locale.ROOT);

         for (String pattern : BAN_PATTERNS) {
            if (message.matches(".*" + pattern + ".*")) {
               event.cancel();
               notify("В вашем сообщении было найдено запрещенное слово, отправка сообщения отменена!");
               return;
            }
         }
      }
   }

   @Override
   public boolean hasPendingUse() {
      return false;
   }

   @Override
   public int getPendingSlot() {
      return -1;
   }

   @Override
   public void clearPending() {
   }
}
