package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class HolyWorldHandler extends ServerHelperHandler {
   private static final String[] NAMES = new String[]{"Пузырёк опыта", "Трапка", "Взрывная трапка", "Прощальный гул", "Взрывная штучка", "Стан", "Ком снега"};
   private static final Item[] ITEMS = new Item[]{
      Items.EXPERIENCE_BOTTLE, Items.POPPED_CHORUS_FRUIT, Items.PRISMARINE_SHARD, Items.JACK_O_LANTERN, Items.FIRE_CHARGE, Items.NETHER_STAR, Items.SNOWBALL
   };
   private final List<BindSetting> binds;
   private final BooleanSetting antiCrash;
   private final BindSetting backpackBind;
   private boolean onlyHotbar;
   private String pendingName;
   private Item pendingItem;
   private boolean backpackActive;
   private int backpackSlot = -1;
   private int originalSlot = -1;
   private int backpackTicks;

   public HolyWorldHandler(List<BindSetting> binds, BooleanSetting antiCrash, BindSetting backpackBind) {
      this.binds = binds;
      this.antiCrash = antiCrash;
      this.backpackBind = backpackBind;
   }

   @Override
   public BooleanSetting getAntiCrashSetting() {
      return this.antiCrash;
   }

   @Override
   public String getModeId() {
      return "HolyWorld";
   }

   @Override
   public String getOverlayTitle() {
      return "Holy";
   }

   @Override
   public List<BindSetting> getBinds() {
      ArrayList<BindSetting> all = new ArrayList<>(this.binds);
      all.add(this.backpackBind);
      return all;
   }

   @Override
   public void setOnlyHotbar(boolean onlyHotbar) {
      this.onlyHotbar = onlyHotbar;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();

      for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
         int slot = this.resolveSlot(NAMES[i], ITEMS[i]);
         String status = this.formatStatus(slot, onlyBar, ITEMS[i]);
         String shortName = NAMES[i].length() > 8 ? NAMES[i].substring(0, 8) : NAMES[i];
         items.add(new ServerHelperRenderItem(NAMES[i], shortName, status, ITEMS[i]).withBind(this.binds.get(i).getKey()));
      }

      return items;
   }

   private int resolveSlot(String name, Item fallback) {
      ServerHelperItemConfig config = ServerHelperItemRegistry.get(name);
      int slot;
      return config != null && (slot = findSlotByConfig(config)) != -1 ? slot : findSlotByItem(fallback);
   }

   private String formatStatus(int slot, boolean onlyBar, Item item) {
      if (slot == -1) {
         return "—";
      } else {
         return onlyBar && slot >= 9 ? "—" : cooldownStatus(item);
      }
   }

   @Override
   public boolean onKey(int keyCode) {
      if (this.backpackBind.getKey() != -1 && this.backpackBind.getKey() == keyCode) {
         this.openBackpack();
         return true;
      } else {
         for (int i = 0; i < NAMES.length && i < this.binds.size(); i++) {
            if (this.binds.get(i).getKey() != -1 && this.binds.get(i).getKey() == keyCode) {
               this.queueUse(NAMES[i], ITEMS[i]);
               return true;
            }
         }

         return false;
      }
   }

   private void queueUse(String itemName, Item item) {
      int slot = this.resolveSlot(itemName, item);
      if (slot == -1) {
         notifyMissing(itemName);
      } else if (this.onlyHotbar && slot >= 9) {
         notifyHotbarOnly(itemName);
      } else if (ItemTask.getCooldownProgress(item) > 0.0F) {
         notifyCooldown(itemName);
      } else {
         this.pendingName = itemName;
         this.pendingItem = item;
      }
   }

   private void openBackpack() {
      int slot = this.findBackpackSlot();
      if (slot == -1) {
         notify("§cРюкзак не найден в инвентаре");
      } else {
         this.backpackSlot = slot;
         this.backpackActive = true;
      }
   }

   private int findBackpackSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
               String translationKey = stack.getItem().getTranslationKey();
               if (translationKey.contains("shulker_box")) {
                  return i;
               }

               String displayName = stack.getItem().getName().getString().toLowerCase(Locale.ROOT);
               if (displayName.contains("шалкер")) {
                  return i;
               }

               String cleanName = cleanName(stack.getName().getString());
               if (cleanName.contains("рюкзак")) {
                  return i;
               }
            }
         }

         return -1;
      }
   }

   @Override
   public void tick() {
      if (this.backpackActive || this.backpackTicks > 0) {
         if (this.backpackActive) {
            this.originalSlot = mc.player.getInventory().selectedSlot;
            if (this.backpackSlot < 9) {
               mc.player.getInventory().selectedSlot = this.backpackSlot;
               this.syncSlot();
            } else {
               InventoryTask.clickSlot(
                  this.backpackSlot < 9 ? this.backpackSlot + 36 : this.backpackSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, false
               );
               this.syncSlot();
            }

            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            this.backpackActive = false;
            this.backpackTicks = 0;
         } else {
            if (this.backpackTicks >= 0 && mc.currentScreen == null) {
               this.backpackTicks++;
               if (this.backpackTicks >= 3 && mc.player.currentScreenHandler.syncId == 0) {
                  if (this.backpackSlot >= 9) {
                     InventoryTask.clickSlot(this.backpackSlot, this.originalSlot, SlotActionType.SWAP, false);
                  }

                  mc.player.getInventory().selectedSlot = this.originalSlot;
                  this.syncSlot();
                  this.backpackSlot = -1;
                  this.originalSlot = -1;
                  this.backpackTicks = -1;
               }
            }
         }
      }
   }

   private void syncSlot() {
      if (mc.interactionManager != null) {
         mc.interactionManager.syncSelectedSlot();
      }
   }

   @Override
   public boolean hasPendingUse() {
      return this.pendingName != null;
   }

   @Override
   public int getPendingSlot() {
      return this.pendingName == null ? -1 : this.resolveSlot(this.pendingName, this.pendingItem);
   }

   @Override
   public void clearPending() {
      this.pendingName = null;
      this.pendingItem = null;
   }

   public boolean isBackpackBusy() {
      return this.backpackActive || this.backpackTicks >= 0;
   }
}
