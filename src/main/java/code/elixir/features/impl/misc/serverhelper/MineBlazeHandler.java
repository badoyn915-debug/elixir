package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.impl.misc.ServerHelper;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.display.color.ColorAssist;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Items;

public class MineBlazeHandler extends ServerHelperHandler {
   private static final String[] SEARCH = new String[]{"фитиль", "переставляющее зелье", "взрывное зелье"};
   private static final int[] TINTS = new int[]{
      ColorAssist.getColor(255, 165, 0, 200), ColorAssist.getColor(138, 43, 226, 200), ColorAssist.getColor(220, 20, 60, 200)
   };
   private final List<BindSetting> binds;
   private final Map<String, Integer> slotCache = new HashMap<>();
   private long lastScan;
   private boolean onlyHotbar;

   public MineBlazeHandler(List<BindSetting> binds) {
      this.binds = binds;
   }

   @Override
   public String getModeId() {
      return "MineBlaze";
   }

   @Override
   public String getOverlayTitle() {
      return "MB";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public void setOnlyHotbar(boolean onlyHotbar) {
      this.onlyHotbar = onlyHotbar;
   }

   private void scan() {
      long now = System.currentTimeMillis();
      if (now - this.lastScan >= 100L) {
         this.lastScan = now;
         this.slotCache.clear();
         if (mc.player != null) {
            int max = this.onlyHotbar ? 9 : 36;

            for (int i = 0; i < max; i++) {
               String name = cleanName(mc.player.getInventory().getStack(i).getName().getString());

               for (String search : SEARCH) {
                  if (name.contains(search) && !this.slotCache.containsKey(search)) {
                     this.slotCache.put(search, i);
                  }
               }
            }
         }
      }
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      this.scan();
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();
      String[] labels = new String[]{"Фитиль", "Переставляющее", "Взрывное"};
      String[] shorts = new String[]{"Ф", "ПЗ", "ВЗ"};

      for (int i = 0; i < SEARCH.length; i++) {
         int slot = this.slotCache.getOrDefault(SEARCH[i], -1);
         items.add(
            new ServerHelperRenderItem(labels[i], shorts[i], slot == -1 ? "—" : "✓", Items.SPLASH_POTION)
               .withBind(this.binds.size() > i ? this.binds.get(i).getKey() : -1)
               .withTint(TINTS[i])
         );
      }

      return items;
   }

   @Override
   public boolean onKey(int keyCode) {
      this.scan();

      for (int i = 0; i < SEARCH.length && i < this.binds.size(); i++) {
         if (this.binds.get(i).getKey() != -1 && this.binds.get(i).getKey() == keyCode) {
            int slot = this.slotCache.getOrDefault(SEARCH[i], -1);
            if (slot == -1) {
               notifyMissing(SEARCH[i]);
               return true;
            }

            ServerHelper.getInstance().queueSlotUse(slot);
            return true;
         }
      }

      return false;
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
