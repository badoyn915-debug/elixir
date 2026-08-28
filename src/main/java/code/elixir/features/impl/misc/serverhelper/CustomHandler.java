package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.impl.misc.ServerHelper;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;

public class CustomHandler extends ServerHelperHandler {
   private final BindSetting openGuiBind = new BindSetting("Открыть GUI", "Открыть редактор предметов")
      .visible(() -> ServerHelper.getInstance() != null && ServerHelper.getInstance().isCustomMode());
   private final Map<CustomHandler.CustomItemEntry, BindSetting> bindMap = new HashMap<>();
   private final List<CustomHandler.CustomItemEntry> entries = new ArrayList<>();
   private boolean onlyHotbar;
   private Item pendingItem;

   public CustomHandler() {
      this.refreshBinds(List.of());
   }

   public void setEntries(List<CustomHandler.CustomItemEntry> configs) {
      this.entries.clear();
      this.entries.addAll(configs);
      this.refreshBinds(configs);
   }

   private void refreshBinds(List<CustomHandler.CustomItemEntry> configs) {
      HashMap<CustomHandler.CustomItemEntry, BindSetting> old = new HashMap<>(this.bindMap);
      this.bindMap.clear();

      for (CustomHandler.CustomItemEntry entry : configs) {
         if (entry.bindKey() != -1) {
            BindSetting bind = old.get(entry);
            if (bind == null) {
               bind = new BindSetting(entry.displayName(), "Пользовательский предмет")
                  .visible(() -> ServerHelper.getInstance() != null && ServerHelper.getInstance().isCustomMode());
            }

            bind.setKey(entry.bindKey());
            this.bindMap.put(entry, bind);
         }
      }
   }

   public List<BindSetting> getDynamicBinds() {
      ArrayList<BindSetting> binds = new ArrayList<>();
      binds.add(this.openGuiBind);
      binds.addAll(this.bindMap.values());
      return binds;
   }

   public BindSetting getOpenGuiBind() {
      return this.openGuiBind;
   }

   @Override
   public String getModeId() {
      return "Custom";
   }

   @Override
   public String getOverlayTitle() {
      return "C";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.getDynamicBinds();
   }

   @Override
   public void setOnlyHotbar(boolean onlyHotbar) {
      this.onlyHotbar = onlyHotbar;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();

      for (CustomHandler.CustomItemEntry entry : this.entries) {
         if (entry.bindKey() != -1) {
            int slot = this.resolveSlot(entry);
            String status = slot == -1
               ? "—"
               : (onlyBar && slot >= 9 ? "—" : (ItemTask.getCooldownProgress(entry.item()) > 0.0F ? cooldownStatus(entry.item()) : "✓"));
            String shortName = entry.displayName().length() > 8 ? entry.displayName().substring(0, 8) : entry.displayName();
            items.add(new ServerHelperRenderItem(entry.displayName(), shortName, status, entry.item()).withBind(entry.bindKey()));
         }
      }

      return items;
   }

   private int resolveSlot(CustomHandler.CustomItemEntry entry) {
      int slot;
      ServerHelperItemConfig config;
      if (!entry.potionName().isEmpty() && (config = ServerHelperItemRegistry.get(entry.potionName())) != null && (slot = findSlotByConfig(config)) != -1) {
         return slot;
      } else {
         return InventoryTask.findHotbarSlot(entry.item()) != -1 ? InventoryTask.findHotbarSlot(entry.item()) : InventoryTask.findInventorySlot(entry.item());
      }
   }

   @Override
   public boolean onKey(int keyCode) {
      if (this.openGuiBind.getKey() != -1 && this.openGuiBind.getKey() == keyCode) {
         notify("§eРедактор Custom пока не подключён — добавьте предметы в конфиг.");
         return true;
      } else {
         for (CustomHandler.CustomItemEntry entry : this.entries) {
            if (entry.bindKey() != -1 && entry.bindKey() == keyCode) {
               this.useEntry(entry);
               return true;
            }
         }

         if (mc.player != null && mc.player.isSneaking() && keyCode == 344) {
            for (CustomHandler.CustomItemEntry entryx : this.entries) {
               if (entryx.activateOnShift()) {
                  this.useEntry(entryx);
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void useEntry(CustomHandler.CustomItemEntry entry) {
      int slot = this.resolveSlot(entry);
      if (slot == -1) {
         notifyMissing(entry.displayName());
      } else if (this.onlyHotbar && slot >= 9) {
         notifyHotbarOnly(entry.displayName());
      } else if (ItemTask.getCooldownProgress(entry.item()) > 0.0F) {
         notifyCooldown(entry.displayName());
      } else if (entry.useServerHelperQueue()) {
         ServerHelper.getInstance().queueSlotUse(slot);
      } else {
         this.pendingItem = entry.item();
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

   public record CustomItemEntry(Item item, String displayName, int bindKey, String potionName, boolean useServerHelperQueue, boolean activateOnShift) {
   }
}
