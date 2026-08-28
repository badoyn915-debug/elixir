package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class SunriseHandler extends ServerHelperHandler {
   private static final String[] NAMES = new String[]{
      "Обычная трапка",
      "Хорошая трапка",
      "Отличная трапка",
      "Эпическая трапка",
      "Легендарная трапка",
      "Ливалка",
      "Амулет телепортации",
      "Скопление света",
      "Волна огня",
      "Порошок возвращения",
      "Порошок дезорганизации",
      "Разрушитель Стоек"
   };
   private static final String[] SHORT = new String[]{"ОТ", "ХТ", "ОтТ", "ЭТ", "ЛТ", "Лив", "АТ", "СС", "ВО", "ПВ", "ПД", "РС"};
   private static final Item[] ICONS = new Item[]{
      Items.NETHERITE_SCRAP,
      Items.GOLDEN_APPLE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.TOTEM_OF_UNDYING,
      Items.NETHER_STAR,
      Items.FEATHER,
      Items.ENDER_PEARL,
      Items.GLOWSTONE_DUST,
      Items.FIRE_CHARGE,
      Items.GUNPOWDER,
      Items.SUGAR,
      Items.IRON_AXE
   };
   private final List<BindSetting> binds;
   private final int[] cachedSlots = new int[NAMES.length];
   private long lastScan;
   private boolean onlyHotbar;
   private String pendingName;
   private List<ServerHelperRenderItem> cachedRender;
   private boolean cachedOnlyHotbar;

   public SunriseHandler(List<BindSetting> binds) {
      this.binds = binds;

      for (int i = 0; i < this.cachedSlots.length; i++) {
         this.cachedSlots[i] = -1;
      }
   }

   @Override
   public String getModeId() {
      return "Sunrise";
   }

   @Override
   public String getOverlayTitle() {
      return "Sunrise";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public void setOnlyHotbar(boolean onlyHotbar) {
      this.onlyHotbar = onlyHotbar;
   }

   private void scanSlots() {
      long now = System.currentTimeMillis();
      if (now - this.lastScan >= 100L) {
         this.lastScan = now;
         if (mc.player != null) {
            for (int i = 0; i < this.cachedSlots.length; i++) {
               this.cachedSlots[i] = -1;
            }

            for (int slot = 0; slot < 36; slot++) {
               String cleanName = cleanName(mc.player.getInventory().getStack(slot).getName().getString());
               if (!cleanName.isEmpty()) {
                  for (int i = 0; i < NAMES.length; i++) {
                     if (this.cachedSlots[i] == -1 && cleanName.contains(NAMES[i].toLowerCase())) {
                        this.cachedSlots[i] = slot;
                     }
                  }
               }
            }

            this.cachedRender = null;
         }
      }
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      this.scanSlots();
      if (this.cachedRender != null && this.cachedOnlyHotbar == onlyBar) {
         return this.cachedRender;
      } else {
         this.cachedOnlyHotbar = onlyBar;
         ArrayList<ServerHelperRenderItem> items = new ArrayList<>();

         for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
            int slot = this.cachedSlots[i];
            String status = slot == -1 ? "—" : (onlyBar && slot >= 9 ? "—" : "✓");
            items.add(new ServerHelperRenderItem(NAMES[i], SHORT[i], status, ICONS[i]).withBind(this.binds.get(i).getKey()));
         }

         this.cachedRender = items;
         return items;
      }
   }

   @Override
   public boolean onKey(int keyCode) {
      this.scanSlots();

      for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
         if (this.binds.get(i).getKey() != -1 && this.binds.get(i).getKey() == keyCode) {
            int slot = this.cachedSlots[i];
            if (slot == -1) {
               notifyMissing(NAMES[i]);
               return true;
            }

            if (this.onlyHotbar && slot >= 9) {
               notifyHotbarOnly(NAMES[i]);
               return true;
            }

            this.pendingName = NAMES[i];
            return true;
         }
      }

      return false;
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
         this.scanSlots();

         for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(this.pendingName)) {
               return this.cachedSlots[i];
            }
         }

         return -1;
      }
   }

   @Override
   public void clearPending() {
      this.pendingName = null;
   }
}
