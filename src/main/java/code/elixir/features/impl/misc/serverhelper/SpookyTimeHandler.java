package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class SpookyTimeHandler extends ServerHelperHandler {
   private static final String[] NAMES = new String[]{
      "Дезориентация", "Явная пыль", "Пласт", "Трапка Spooky", "Огненный смерч", "Снежок заморозка", "Божья аура"
   };
   private static final String[] SEARCH = new String[]{"дезориент", "явная пыль", "пласт", "трапка", "смерч", "заморозка", "божья аура"};
   private static final Item[] ITEMS = new Item[]{
      Items.ENDER_EYE, Items.SUGAR, Items.DRIED_KELP, Items.NETHERITE_SCRAP, Items.FIRE_CHARGE, Items.SNOWBALL, Items.PHANTOM_MEMBRANE
   };
   private final List<BindSetting> binds;
   private boolean onlyHotbar;
   private String pendingSearch;
   private Item pendingItem;

   public SpookyTimeHandler(List<BindSetting> binds) {
      this.binds = binds;
   }

   @Override
   public String getModeId() {
      return "SpookyTime";
   }

   @Override
   public String getOverlayTitle() {
      return "Spooky";
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
         int slot = findSlotByName(SEARCH[i]);
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
            this.queueUse(NAMES[i], SEARCH[i], ITEMS[i]);
            return true;
         }
      }

      return false;
   }

   private void queueUse(String itemName, String search, Item item) {
      int slot = findSlotByName(search);
      if (slot == -1) {
         notifyMissing(itemName);
      } else if (this.onlyHotbar && slot >= 9) {
         notifyHotbarOnly(itemName);
      } else if (ItemTask.getCooldownProgress(item) > 0.0F) {
         notifyCooldown(itemName);
      } else {
         this.pendingSearch = search;
         this.pendingItem = item;
      }
   }

   @Override
   public boolean hasPendingUse() {
      return this.pendingSearch != null;
   }

   @Override
   public int getPendingSlot() {
      return this.pendingSearch == null ? -1 : findSlotByName(this.pendingSearch);
   }

   @Override
   public void clearPending() {
      this.pendingSearch = null;
      this.pendingItem = null;
   }
}
