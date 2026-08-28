package code.elixir.features.impl.misc.serverhelper;

import code.elixir.display.hud.Notifications;
import code.elixir.events.chat.ChatEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.misc.ServerHelper;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public abstract class ServerHelperHandler implements QuickImports {
   public static void useAntiFlight() {
      int slot = InventoryTask.findHotbarSlot(Items.FIREWORK_STAR);
      if (slot == -1) {
         slot = InventoryTask.findInventorySlot(Items.FIREWORK_STAR);
      }

      if (slot == -1) {
         notify("§cУ вас отсутствует анти полёт!");
      } else {
         ServerHelper.getInstance().queueSlotUse(slot);
      }
   }

   public abstract String getModeId();

   public abstract String getOverlayTitle();

   public abstract List<BindSetting> getBinds();

   public abstract List<ServerHelperRenderItem> buildRenderItems(boolean var1);

   public abstract boolean onKey(int var1);

   public abstract boolean hasPendingUse();

   public abstract int getPendingSlot();

   public abstract void clearPending();

   public BooleanSetting getAntiCrashSetting() {
      return null;
   }

   public void setOnlyHotbar(boolean onlyHotbar) {
   }

   public void tick() {
   }

   public void onPacketReceive(PacketEvent event) {
   }

   public void onChatSend(ChatEvent event) {
   }

   protected static String cleanName(String value) {
      String stripped = stripFormatting(value == null ? "" : value);
      return normalizeSpaces(stripped).toLowerCase(Locale.ROOT);
   }

   protected static String stripFormatting(String value) {
      if (value != null && !value.isEmpty()) {
         StringBuilder result = new StringBuilder(value.length());
         boolean lastWasSpace = false;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != 171 && c != 187 && c != '"' && c != '\'' && c != '`') {
               if (Character.isWhitespace(c)) {
                  if (!lastWasSpace && !result.isEmpty()) {
                     result.append(' ');
                     lastWasSpace = true;
                  }
               } else {
                  result.append(c);
                  lastWasSpace = false;
               }
            }
         }

         int length = result.length();
         if (length > 0 && result.charAt(length - 1) == ' ') {
            result.setLength(length - 1);
         }

         return result.toString();
      } else {
         return "";
      }
   }

   private static String normalizeSpaces(String value) {
      return value.replaceAll("§[0-9a-fk-or]", "").trim();
   }

   protected static int findSlotByName(String searchQuery) {
      if (mc.player != null && searchQuery != null && !searchQuery.isEmpty()) {
         String needle = searchQuery.toLowerCase(Locale.ROOT);

         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && cleanName(stack.getName().getString()).contains(needle)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static int findSlotByItem(Item item) {
      if (mc.player != null && item != null) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static int findSlotByConfig(ServerHelperItemConfig config) {
      if (mc.player != null && config != null) {
         String cleanItemName = cleanName(config.displayName());

         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
               String cleanDisplayName = cleanName(stack.getName().getString());
               boolean nameMatches = cleanDisplayName.contains(cleanItemName)
                  || cleanItemName.contains(cleanDisplayName)
                  || cleanDisplayName.equals(cleanItemName);
               if (nameMatches && (config.loreParams().isEmpty() || matchesLore(stack, config.loreParams()))) {
                  return i;
               }

               if (stack.isOf(config.item()) && (config.loreParams().isEmpty() || matchesLore(stack, config.loreParams()))) {
                  return i;
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static boolean matchesLore(ItemStack stack, List<String> requiredParams) {
      if (requiredParams.isEmpty()) {
         return true;
      } else {
         String loreText = InventoryTask.getCleanName(stack.getName());

         for (String param : requiredParams) {
            if (!loreText.contains(param.toLowerCase(Locale.ROOT))) {
               return false;
            }
         }

         return true;
      }
   }

   protected static String cooldownStatus(Item item) {
      float cooldown = ItemTask.getCooldownProgress(item);
      return cooldown > 0.0F ? String.format("%.0fс", cooldown * 20.0F) : "✓";
   }

   protected static void notify(String message) {
      Notifications.getInstance().addList(Text.literal(message), 3000);
   }

   protected static void notifyMissing(String itemName) {
      notify("§c" + itemName + " не найден в инвентаре");
   }

   protected static void notifyHotbarOnly(String itemName) {
      notify("§c" + itemName + " не в хотбаре");
   }

   protected static void notifyCooldown(String itemName) {
      notify("§c" + itemName + " на кулдауне");
   }
}
