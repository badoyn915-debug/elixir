package code.elixir.features.impl.misc.serverhelper;

import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class LonyGriefHandler extends ServerHelperHandler {
   private static final Pattern EQUATION_A = Pattern.compile("([xXхХ])\\s*([+\\-*/])\\s*([\\d.,]+)\\s*=\\s*([\\d.,]+)");
   private static final Pattern EQUATION_B = Pattern.compile("([\\d.,]+)\\s*([+\\-*/])\\s*([xXхХ])\\s*=\\s*([\\d.,]+)");
   private static final String[] NAMES = new String[]{"Обычная ливалка", "Уникальное перышко", "Ливалка с платформой", "Уникальная трапка"};
   private static final Item[] ITEMS = new Item[]{Items.FEATHER, Items.FEATHER, Items.FEATHER, Items.NETHERITE_SCRAP};
   private final List<BindSetting> binds;
   private final BooleanSupplier chatSolverEnabled;
   private boolean onlyHotbar;
   private String pendingName;
   private long lastChatSolve;

   public LonyGriefHandler(List<BindSetting> binds, BooleanSupplier chatSolverEnabled) {
      this.binds = binds;
      this.chatSolverEnabled = chatSolverEnabled;
   }

   @Override
   public String getModeId() {
      return "LonyGrief";
   }

   @Override
   public String getOverlayTitle() {
      return "Lony";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public void setOnlyHotbar(boolean onlyHotbar) {
      this.onlyHotbar = onlyHotbar;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();

      for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
         ServerHelperItemConfig config = ServerHelperItemRegistry.get(NAMES[i]);
         int slot = config == null ? -1 : findSlotByConfig(config);
         String status = slot == -1 ? "—" : (onlyBar && slot >= 9 ? "—" : cooldownStatus(ITEMS[i]));
         String shortName = NAMES[i].length() > 8 ? NAMES[i].substring(0, 8) : NAMES[i];
         items.add(new ServerHelperRenderItem(NAMES[i], shortName, status, ITEMS[i]).withBind(this.binds.get(i).getKey()));
      }

      return items;
   }

   @Override
   public boolean onKey(int keyCode) {
      for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
         if (this.binds.get(i).getKey() != -1 && this.binds.get(i).getKey() == keyCode) {
            this.queueUse(NAMES[i]);
            return true;
         }
      }

      return false;
   }

   private void queueUse(String itemName) {
      ServerHelperItemConfig config = ServerHelperItemRegistry.get(itemName);
      if (config == null) {
         notify("§c" + itemName + " не найден в конфигурации");
      } else {
         int slot = findSlotByConfig(config);
         if (slot == -1) {
            notifyMissing(itemName);
         } else if (this.onlyHotbar && slot >= 9) {
            notifyHotbarOnly(itemName);
         } else if (ItemTask.getCooldownProgress(config.item()) > 0.0F) {
            notifyCooldown(itemName);
         } else {
            this.pendingName = itemName;
         }
      }
   }

   @Override
   public void onPacketReceive(PacketEvent event) {
      if (!event.isSend() && this.chatSolverEnabled.getAsBoolean() && mc.player != null) {
         if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            if (System.currentTimeMillis() - this.lastChatSolve >= 400L) {
               String line = sanitizeChat(packet.content().getString());
               if (line.contains("=")) {
                  String lower = line.toLowerCase();
                  boolean looksLikeGame = lower.contains("найдите") || lower.contains("чат-игра") || lower.contains("решите уравнение");
                  if (looksLikeGame) {
                     String answer = solveEquation(line);
                     if (answer != null) {
                        this.lastChatSolve = System.currentTimeMillis();
                        mc.player.networkHandler.sendChatMessage(answer);
                     }
                  }
               }
            }
         }
      }
   }

   private static String sanitizeChat(String raw) {
      String s = raw.replaceAll("§[0-9a-fk-or]", "");
      s = s.replace("⎹", " ").replace("⎧", " ").replace("⎩", " ");
      s = s.replace("◂", " ").replace("▸", " ");
      s = s.replace(',', '.');
      return s.trim();
   }

   private static String solveEquation(String line) {
      Matcher m1 = EQUATION_A.matcher(line);
      if (m1.find()) {
         double x = solve(m1.group(2), Double.parseDouble(m1.group(3)), Double.parseDouble(m1.group(4)));
         return formatAnswer(x);
      } else {
         Matcher m2 = EQUATION_B.matcher(line);
         if (m2.find()) {
            double x = solveReverse(Double.parseDouble(m2.group(1)), m2.group(2), Double.parseDouble(m2.group(4)));
            return formatAnswer(x);
         } else {
            return null;
         }
      }
   }

   private static double solve(String op, double b, double c) {
      return switch (op) {
         case "+" -> c - b;
         case "-" -> c + b;
         case "*" -> c / b;
         case "/" -> c * b;
         default -> Double.NaN;
      };
   }

   private static double solveReverse(double a, String op, double c) {
      return switch (op) {
         case "+" -> c - a;
         case "-" -> a - c;
         case "*" -> c / a;
         case "/" -> a / c;
         default -> Double.NaN;
      };
   }

   private static String formatAnswer(double x) {
      if (!Double.isNaN(x) && !Double.isInfinite(x)) {
         long rounded = Math.round(x);
         return Math.abs(x - rounded) < 1.0E-4 ? Long.toString(rounded) : Double.toString(x);
      } else {
         return null;
      }
   }

   @Override
   public boolean hasPendingUse() {
      return this.pendingName != null;
   }

   @Override
   public int getPendingSlot() {
      if (this.pendingName == null) {
         return -1;
      } else {
         ServerHelperItemConfig config = ServerHelperItemRegistry.get(this.pendingName);
         return config == null ? -1 : findSlotByConfig(config);
      }
   }

   @Override
   public void clearPending() {
      this.pendingName = null;
   }
}
