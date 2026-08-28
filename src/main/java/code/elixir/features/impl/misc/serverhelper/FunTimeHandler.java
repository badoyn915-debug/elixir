package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class FunTimeHandler extends ServerHelperHandler {
   private static final String[] NAMES = new String[]{"Трапа", "Дезорент", "Пласт", "Явная пыль", "Божья аура", "Огнен смерч", "Снежок"};
   private static final Item[] ITEMS = new Item[]{
      Items.NETHERITE_SCRAP, Items.ENDER_EYE, Items.DRIED_KELP, Items.SUGAR, Items.PHANTOM_MEMBRANE, Items.FIRE_CHARGE, Items.SNOWBALL
   };
   private final List<BindSetting> binds;
   private boolean onlyHotbar;
   private Item pendingItem;

   public FunTimeHandler(List<BindSetting> binds) {
      this.binds = binds;
   }

   @Override
   public String getModeId() {
      return "FunTime";
   }

   @Override
   public String getOverlayTitle() {
      return "FunTime";
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
         int slot = findSlotByItem(ITEMS[i]);
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
            this.queueUse(NAMES[i], ITEMS[i]);
            return true;
         }
      }

      return false;
   }

   private void queueUse(String name, Item item) {
      int slot = findSlotByItem(item);
      if (slot == -1) {
         notifyMissing(name);
      } else if (this.onlyHotbar && slot >= 9) {
         notifyHotbarOnly(name);
      } else if (ItemTask.getCooldownProgress(item) > 0.0F) {
         notifyCooldown(name);
      } else {
         this.pendingItem = item;
      }
   }

   @Override
   public boolean hasPendingUse() {
      return this.pendingItem != null;
   }

   @Override
   public int getPendingSlot() {
      return this.pendingItem == null ? -1 : findSlotByItem(this.pendingItem);
   }

   @Override
   public void clearPending() {
      this.pendingItem = null;
   }
}
