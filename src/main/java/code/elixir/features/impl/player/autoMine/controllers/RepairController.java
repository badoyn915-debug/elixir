package code.elixir.features.impl.player.autoMine.controllers;

import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryResult;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.inv.InventoryToolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class RepairController implements QuickImports {
   static final int DURABILITY_THRESHOLD_PERCENT = 15;
   static final int MAX_XP_STACK_PRICE = 100000;
   static final int XP_STACKS_TO_BUY = 3;
   static final int MIN_STACK_SIZE = 64;
   static final long WAITING_AFTER_BUY_MS = 2500L;
   static final int SLOTS_TO_CLEAR = 3;
   static final long AUCTION_ACTION_DELAY = 350L;
   static final long POST_PURCHASE_DELAY_MS = 1000L;
   static final long THROW_DELAY = 0L;
   static final long POST_SWAP_DELAY_MS = 250L;
   static final long POST_COMMAND_DELAY_MS = 2000L;
   static final long AUCTION_LOAD_DELAY = 3000L;
   static final long BUY_TIMEOUT = 30000L;
   static final Pattern PRICE_PATTERN = Pattern.compile("(?:\\$\\s*([\\d,.\\s]+)|([\\d,.\\s]+)\\s*\\$|(?:Цена|Price)[:\\s]*([\\d,.\\s]+))");
   private RepairController.RepairPhase phase = RepairController.RepairPhase.IDLE;
   private long phaseStartTime = 0L;
   private long lastActionTime = 0L;
   private long lastPurchaseTime = 0L;
   private int stacksBought = 0;
   private boolean auctionOpened = false;
   private int refreshCount = 0;
   private int throwTickCounter = 0;
   private long lastThrowTime = 0L;
   private boolean needReleaseUseKey = false;
   private int dropActionIndex = 0;
   private List<Integer> slotsToDrop = new ArrayList<>();
   private int consolidateTicks = 0;
   private int consolidateTargetFree = 0;
   private int noItemsRetryCount = 0;

   public boolean needsRepair() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack pickaxe = this.findPickaxe();
         if (pickaxe == null || pickaxe.isEmpty()) {
            return false;
         } else if (!pickaxe.isDamageable()) {
            return false;
         } else if (!this.hasMending(pickaxe)) {
            return false;
         } else {
            int maxDurability = pickaxe.getMaxDamage();
            int currentDamage = pickaxe.getDamage();
            int remaining = maxDurability - currentDamage;
            int percent = maxDurability > 0 ? remaining * 100 / maxDurability : 100;
            return percent <= 15;
         }
      }
   }

   private ItemStack findPickaxe() {
      if (mc.player == null) {
         return null;
      } else {
         ItemStack mainHand = mc.player.getMainHandStack();
         if (mainHand.getItem() instanceof PickaxeItem) {
            return mainHand;
         } else {
            InventoryResult result = InventoryToolkit.findInInventory(stack -> stack.getItem() instanceof PickaxeItem);
            return result != null && result.slot() != -1 ? result.stack() : null;
         }
      }
   }

   private boolean hasMending(ItemStack stack) {
      ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchantments != null && !enchantments.isEmpty()) {
         for (RegistryEntry entry : enchantments.getEnchantments()) {
            String id = entry.getIdAsString();
            if (id != null && id.toLowerCase().contains("mending")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isPickaxeFullyRepaired() {
      if (mc.player == null) {
         return true;
      } else {
         ItemStack pickaxe = this.findPickaxe();
         if (pickaxe == null || pickaxe.isEmpty()) {
            return true;
         } else {
            return !pickaxe.isDamageable() ? true : pickaxe.getDamage() <= 5;
         }
      }
   }

   public int getPickaxeDurabilityPercent() {
      ItemStack pickaxe = this.findPickaxe();
      if (pickaxe != null && pickaxe.isDamageable()) {
         int max = pickaxe.getMaxDamage();
         int dmg = pickaxe.getDamage();
         return max > 0 ? (max - dmg) * 100 / max : 100;
      } else {
         return 100;
      }
   }

   public void startRepair() {
      ChatMessage.brandmessage("[AutoMine Repair] Начинаю починку кирки");
      this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
      this.phaseStartTime = System.currentTimeMillis();
      this.lastActionTime = 0L;
      this.stacksBought = 0;
      this.auctionOpened = false;
      this.refreshCount = 0;
      this.throwTickCounter = 0;
      this.lastThrowTime = 0L;
      this.dropActionIndex = 0;
      this.slotsToDrop.clear();
      this.consolidateTicks = 0;
      this.consolidateTargetFree = 0;
      this.noItemsRetryCount = 0;
   }

   public boolean tickRepair() {
      if (mc.player == null) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         if (this.phase == RepairController.RepairPhase.DROPPING_ITEMS || this.phase == RepairController.RepairPhase.CONSOLIDATING_DIAMONDS) {
            mc.player.setPitch(0.0F);
         } else if (this.phase == RepairController.RepairPhase.SWAPPING_PICKAXE_AND_XP
            || this.phase == RepairController.RepairPhase.THROWING_XP
            || this.phase == RepairController.RepairPhase.RETURNING_PICKAXE) {
            mc.player.setPitch(90.0F);
         }

         switch (this.phase) {
            case CLEARING_INVENTORY:
               this.tickClearingInventory(now);
               break;
            case DROPPING_ITEMS:
               this.tickDroppingItems(now);
               break;
            case CONSOLIDATING_DIAMONDS:
               this.tickConsolidatingDiamonds(now);
               break;
            case OPENING_AUCTION:
            case WAITING_AUCTION:
            case SCANNING_BUYING:
            case DONE_BUYING:
               this.tickBuying();
               break;
            case WAITING_AFTER_BUY:
               if (now - this.phaseStartTime >= 2500L) {
                  ChatMessage.brandmessage("[AutoMine Repair] Начинаю починку");
                  this.phase = RepairController.RepairPhase.SWAPPING_PICKAXE_AND_XP;
               }
               break;
            case SWAPPING_PICKAXE_AND_XP:
               this.tickSwappingPickaxeAndXp(now);
               break;
            case THROWING_XP:
               this.tickThrowingXp(now);
               break;
            case RETURNING_PICKAXE:
               this.tickReturningPickaxe(now);
               break;
            case DONE_REPAIR:
               return true;
            default:
               this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
         }

         return this.phase == RepairController.RepairPhase.DONE_REPAIR;
      }
   }

   private void tickClearingInventory(long now) {
      if (this.hasAnyXpBottles() && this.findPickaxeInvIndex() >= 0) {
         ChatMessage.brandmessage("[AutoMine Repair] Есть опыт и кирка — чиню");
         this.phase = RepairController.RepairPhase.SWAPPING_PICKAXE_AND_XP;
      } else {
         int freeSlots = this.countFreeSlots();
         if (freeSlots >= 3) {
            ChatMessage.brandmessage("[AutoMine Repair] Достаточно свободных слотов: " + freeSlots);
            this.phase = RepairController.RepairPhase.OPENING_AUCTION;
            this.startBuying();
         } else {
            this.slotsToDrop.clear();

            for (int i = 0; i < 36; i++) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (!stack.isEmpty() && !this.isKeepItem(stack)) {
                  this.slotsToDrop.add(i);
               }
            }

            if (!this.slotsToDrop.isEmpty()) {
               ChatMessage.brandmessage("[AutoMine Repair] Выкидываю " + this.slotsToDrop.size() + " предметов");
               this.phase = RepairController.RepairPhase.DROPPING_ITEMS;
               this.dropActionIndex = 0;
            } else {
               int slotsToFree = 3 - freeSlots;
               if (slotsToFree > 0 && this.canConsolidateDiamonds()) {
                  ChatMessage.brandmessage("[AutoMine Repair] Нет места — консолидирую до " + slotsToFree + " слотов");
                  this.phase = RepairController.RepairPhase.CONSOLIDATING_DIAMONDS;
                  this.consolidateTicks = 0;
                  this.consolidateTargetFree = 3;
               } else {
                  ChatMessage.brandmessage("[AutoMine Repair] Не удалось освободить слоты, продолжаю");
                  this.phase = RepairController.RepairPhase.OPENING_AUCTION;
                  this.startBuying();
               }
            }
         }
      }
   }

   private void tickDroppingItems(long now) {
      if (this.canAct(now)) {
         if (this.dropActionIndex >= this.slotsToDrop.size()) {
            this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
         } else {
            int invIdx = this.slotsToDrop.get(this.dropActionIndex);
            if (mc.player.getInventory().getStack(invIdx).isEmpty()) {
               this.dropActionIndex++;
            } else {
               Slot slot = this.getSlotFromInvIndex(invIdx);
               if (slot != null) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
               }

               this.dropActionIndex++;
               this.lastActionTime = now;
            }
         }
      }
   }

   private void tickConsolidatingDiamonds(long now) {
      if (this.canAct(now)) {
         if (this.countFreeSlots() >= this.consolidateTargetFree) {
            this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
         } else {
            this.consolidateTicks++;
            if (this.consolidateTicks > 30) {
               this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
            } else {
               Slot singleSlot = this.findSingleDiamondSlot();
               Slot stackSlot = this.findDiamondStackWithSpace();
               if (singleSlot != null && stackSlot != null && singleSlot.id != stackSlot.id) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, singleSlot.id, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, stackSlot.id, 0, SlotActionType.PICKUP, mc.player);
                  if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, singleSlot.id, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.lastActionTime = now;
               } else {
                  this.phase = RepairController.RepairPhase.CLEARING_INVENTORY;
               }
            }
         }
      }
   }

   private int countFreeSlots() {
      int free = 0;

      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            free++;
         }
      }

      return free;
   }

   private boolean hasAnyXpBottles() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
            return true;
         }
      }

      return false;
   }

   private int findPickaxeInvIndex() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
            return i;
         }
      }

      return -1;
   }

   private Slot getSlotFromInvIndex(int invIndex) {
      if (mc.player.currentScreenHandler != null && invIndex >= 0 && invIndex < 36) {
         PlayerInventory inv = mc.player.getInventory();

         for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.inventory == inv && slot.getIndex() == invIndex) {
               return slot;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean isKeepItem(ItemStack stack) {
      if (stack.getItem() == Items.DIAMOND) {
         return true;
      } else {
         return stack.getItem() instanceof PickaxeItem ? true : stack.getItem() == Items.EXPERIENCE_BOTTLE;
      }
   }

   private boolean canConsolidateDiamonds() {
      return this.findSingleDiamondSlot() != null && this.findDiamondStackWithSpace() != null;
   }

   private Slot findSingleDiamondSlot() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Slot slot;
         if (stack.getItem() == Items.DIAMOND && stack.getCount() == 1 && (slot = this.getSlotFromInvIndex(i)) != null) {
            return slot;
         }
      }

      return null;
   }

   private Slot findDiamondStackWithSpace() {
      Slot best = null;
      int bestCount = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Slot slot;
         if (stack.getItem() == Items.DIAMOND && stack.getCount() < 64 && stack.getCount() > bestCount && (slot = this.getSlotFromInvIndex(i)) != null) {
            best = slot;
            bestCount = stack.getCount();
         }
      }

      return best;
   }

   private void startBuying() {
      ChatMessage.brandmessage("[AutoMine Repair] Старт покупки пузырьков опыта");
      this.phase = RepairController.RepairPhase.OPENING_AUCTION;
      this.phaseStartTime = System.currentTimeMillis();
      this.lastActionTime = 0L;
      this.lastPurchaseTime = 0L;
      this.stacksBought = 0;
      this.auctionOpened = false;
      this.refreshCount = 0;
   }

   public boolean tickBuying() {
      if (mc.player == null) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         if (now - this.phaseStartTime > 30000L && this.phase != RepairController.RepairPhase.DONE_BUYING) {
            ChatMessage.brandmessage("[AutoMine Repair] Покупка: таймаут 30 сек");
            this.phase = RepairController.RepairPhase.DONE_BUYING;
         }

         switch (this.phase) {
            case OPENING_AUCTION:
               if (this.canAct(now)) {
                  if (mc.currentScreen != null) {
                     mc.player.closeHandledScreen();
                     this.lastActionTime = now;
                     return false;
                  }

                  ChatMessage.brandmessage("[AutoMine Repair] Открываю аукцион: /ah search Пузырёк опыта");
                  this.sendCommand("/ah search Пузырёк опыта");
                  this.phase = RepairController.RepairPhase.WAITING_AUCTION;
                  this.phaseStartTime = now;
                  this.lastActionTime = now;
               }
               break;
            case WAITING_AUCTION:
               long elapsed = now - this.phaseStartTime;
               if (mc.currentScreen instanceof GenericContainerScreen && elapsed >= 2000L) {
                  this.auctionOpened = true;
                  ChatMessage.brandmessage("[AutoMine Repair] Аукцион открыт, ищу пузырьки");
                  this.phase = RepairController.RepairPhase.SCANNING_BUYING;
                  this.lastActionTime = now;
               } else if (elapsed > 3000L) {
                  ChatMessage.brandmessage("[AutoMine Repair] Аукцион не открылся, повтор");
                  this.phase = RepairController.RepairPhase.OPENING_AUCTION;
                  this.phaseStartTime = now;
               }
               break;
            case SCANNING_BUYING:
               if (!(mc.currentScreen instanceof GenericContainerScreen container)) {
                  this.phase = RepairController.RepairPhase.OPENING_AUCTION;
                  return false;
               }

               if (this.stacksBought >= 3) {
                  ChatMessage.brandmessage("[AutoMine Repair] Куплено стаков: " + this.stacksBought);
                  this.phase = RepairController.RepairPhase.DONE_BUYING;
                  return false;
               }

               if (!this.canAct(now)) {
                  return false;
               }

               if (this.lastPurchaseTime > 0L && now - this.lastPurchaseTime < 1000L) {
                  return false;
               }

               DefaultedList slots = ((GenericContainerScreenHandler)container.getScreenHandler()).slots;
               int syncId = ((GenericContainerScreenHandler)container.getScreenHandler()).syncId;
               int bestSlot = -1;
               int bestPrice = Integer.MAX_VALUE;
               int maxSlot = Math.min(45, slots.size());

               for (int i = 0; i < maxSlot; i++) {
                  Slot slot = (Slot)slots.get(i);
                  int price;
                  ItemStack stack;
                  if (slot != null
                     && (stack = slot.getStack()) != null
                     && !stack.isEmpty()
                     && stack.getItem() == Items.EXPERIENCE_BOTTLE
                     && stack.getCount() >= 64
                     && (price = this.extractPrice(stack)) > 0
                     && price <= 100000
                     && price < bestPrice) {
                     bestPrice = price;
                     bestSlot = i;
                  }
               }

               if (bestSlot >= 0) {
                  mc.interactionManager.clickSlot(syncId, bestSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                  this.stacksBought++;
                  this.lastPurchaseTime = now;
                  ChatMessage.brandmessage("[AutoMine Repair] Куплен стак " + this.stacksBought + "/3, цена " + bestPrice + ", жду 1 сек");
                  this.lastActionTime = now;
               } else if (this.refreshCount < 5) {
                  int containerSize = slots.size() - 36;
                  int refreshSlot = containerSize > 0 ? containerSize - 5 : 49;
                  mc.interactionManager.clickSlot(syncId, refreshSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                  this.refreshCount++;
                  ChatMessage.brandmessage("[AutoMine Repair] Обновляю аукцион, попытка " + this.refreshCount);
                  this.lastActionTime = now;
               } else {
                  ChatMessage.brandmessage("[AutoMine Repair] Не найдено подходящих стаков, куплено " + this.stacksBought);
                  this.phase = RepairController.RepairPhase.DONE_BUYING;
               }
               break;
            case DONE_BUYING:
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               ChatMessage.brandmessage("[AutoMine Repair] Покупка завершена, стаков: " + this.stacksBought + ", жду синхронизацию...");
               this.phase = RepairController.RepairPhase.WAITING_AFTER_BUY;
               this.phaseStartTime = now;
               return false;
            default:
               return true;
         }

         return false;
      }
   }

   private int extractPrice(ItemStack stack) {
      LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
      if (lore != null && !lore.lines().isEmpty()) {
         for (Text line : lore.lines()) {
            String text = line.getString();
            Matcher matcher = PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
               for (int g = 1; g <= 3; g++) {
                  String priceStr = matcher.group(g);
                  if (priceStr != null) {
                     try {
                        String clean = priceStr.replaceAll("[\\s,.]", "");
                        return Integer.parseInt(clean);
                     } catch (NumberFormatException var13) {
                     }
                  }
               }
            }
         }
      }

      Matcher m;
      if ((m = PRICE_PATTERN.matcher(stack.getName().getString())).find()) {
         for (int gx = 1; gx <= 3; gx++) {
            String priceStr = m.group(gx);
            if (priceStr != null) {
               try {
                  String clean = priceStr.replaceAll("[\\s,.]", "");
                  return Integer.parseInt(clean);
               } catch (NumberFormatException var12) {
               }
            }
         }
      }

      return -1;
   }

   private void tickSwappingPickaxeAndXp(long now) {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
         this.lastActionTime = now;
      } else if (this.canAct(now)) {
         boolean pickaxeInOffhand = mc.player.getOffHandStack().getItem() instanceof PickaxeItem;
         int pickaxeInv = this.findPickaxeInvIndex();
         int xpInv = this.findXpBottleSlot();
         Slot pickaxeSlot = pickaxeInv >= 0 ? this.getSlotFromInvIndex(pickaxeInv) : null;
         Slot xpSlot = xpInv >= 0 ? this.getSlotFromInvIndex(xpInv) : null;
         boolean hasPickaxe = pickaxeInOffhand || pickaxeSlot != null;
         if (hasPickaxe && xpSlot != null) {
            this.noItemsRetryCount = 0;
            boolean xpInMainhand = mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
            if (pickaxeInOffhand && xpInMainhand) {
               ChatMessage.brandmessage("[AutoMine Repair] Кирка в левой, опыт в правой — кидаю");
               this.phase = RepairController.RepairPhase.THROWING_XP;
               this.lastThrowTime = now;
            } else if (!pickaxeInOffhand) {
               InventoryTask.swapHand(pickaxeSlot, Hand.OFF_HAND, false, true);
               this.lastActionTime = now;
            } else {
               if (!xpInMainhand) {
                  if (xpInv < 9) {
                     InventoryTask.switchTo(xpInv);
                  } else {
                     int sel = mc.player.getInventory().selectedSlot;
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, xpSlot.id, sel, SlotActionType.SWAP, mc.player);
                  }

                  this.lastActionTime = now;
               }
            }
         } else {
            if (this.noItemsRetryCount < 2 && this.stacksBought > 0) {
               this.noItemsRetryCount++;
               ChatMessage.brandmessage("[AutoMine Repair] Жду предметы, повтор " + this.noItemsRetryCount);
               this.phase = RepairController.RepairPhase.WAITING_AFTER_BUY;
               this.phaseStartTime = now;
            } else {
               ChatMessage.brandmessage("[AutoMine Repair] Нет кирки или пузырьков");
               this.phase = RepairController.RepairPhase.DONE_REPAIR;
            }
         }
      }
   }

   private void tickThrowingXp(long now) {
      if (this.needReleaseUseKey) {
         mc.options.useKey.setPressed(false);
         this.needReleaseUseKey = false;
      }

      if (this.isPickaxeFullyRepaired()) {
         ChatMessage.brandmessage("[AutoMine Repair] Кирка починена");
         this.phase = RepairController.RepairPhase.RETURNING_PICKAXE;
      } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
         int nextXp = this.findXpBottleSlot();
         if (nextXp == -1) {
            ChatMessage.brandmessage("[AutoMine Repair] Пузырьки закончились");
            this.phase = RepairController.RepairPhase.RETURNING_PICKAXE;
         } else {
            this.phase = RepairController.RepairPhase.SWAPPING_PICKAXE_AND_XP;
         }
      } else {
         long delay = this.throwTickCounter == 0 ? 250L : 0L;
         if (now - this.lastThrowTime >= delay) {
            mc.options.useKey.setPressed(true);
            this.needReleaseUseKey = true;
            this.lastThrowTime = now;
            this.throwTickCounter++;
         }
      }
   }

   private void tickReturningPickaxe(long now) {
      if (this.canAct(now)) {
         if (mc.player.getOffHandStack().getItem() instanceof PickaxeItem) {
            DefaultedList slots = mc.player.currentScreenHandler.slots;
            Slot offhandSlot = slots.size() >= 46 ? (Slot)slots.get(45) : null;
            if (offhandSlot != null) {
               InventoryTask.swapHand(offhandSlot, Hand.MAIN_HAND, false, true);
               ChatMessage.brandmessage("[AutoMine Repair] Кирка в основную руку, продолжаю майнить");
            }
         }

         this.phase = RepairController.RepairPhase.DONE_REPAIR;
      }
   }

   private int findXpBottleSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.EXPERIENCE_BOTTLE && !stack.isEmpty()) {
               return i;
            }
         }

         return -1;
      }
   }

   public boolean hasXpBottles() {
      return this.findXpBottleSlot() != -1;
   }

   public void reset() {
      this.phase = RepairController.RepairPhase.IDLE;
      this.stacksBought = 0;
      this.auctionOpened = false;
      this.refreshCount = 0;
      this.throwTickCounter = 0;
      this.lastThrowTime = 0L;
   }

   private boolean canAct(long now) {
      return now - this.lastActionTime >= 350L;
   }

   private void sendCommand(String command) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         String cmd = command.startsWith("/") ? command.substring(1) : command;
         mc.getNetworkHandler().sendCommand(cmd);
      }
   }

   public RepairController.RepairPhase getPhase() {
      return this.phase;
   }

   public long getPhaseStartTime() {
      return this.phaseStartTime;
   }

   public long getLastActionTime() {
      return this.lastActionTime;
   }

   public long getLastPurchaseTime() {
      return this.lastPurchaseTime;
   }

   public int getStacksBought() {
      return this.stacksBought;
   }

   public boolean isAuctionOpened() {
      return this.auctionOpened;
   }

   public int getRefreshCount() {
      return this.refreshCount;
   }

   public int getThrowTickCounter() {
      return this.throwTickCounter;
   }

   public long getLastThrowTime() {
      return this.lastThrowTime;
   }

   public boolean isNeedReleaseUseKey() {
      return this.needReleaseUseKey;
   }

   public int getDropActionIndex() {
      return this.dropActionIndex;
   }

   public List<Integer> getSlotsToDrop() {
      return this.slotsToDrop;
   }

   public int getConsolidateTicks() {
      return this.consolidateTicks;
   }

   public int getConsolidateTargetFree() {
      return this.consolidateTargetFree;
   }

   public int getNoItemsRetryCount() {
      return this.noItemsRetryCount;
   }

   public static enum RepairPhase {
      IDLE,
      CLEARING_INVENTORY,
      DROPPING_ITEMS,
      CONSOLIDATING_DIAMONDS,
      OPENING_AUCTION,
      WAITING_AUCTION,
      SCANNING_BUYING,
      DONE_BUYING,
      WAITING_AFTER_BUY,
      SWAPPING_PICKAXE_AND_XP,
      THROWING_XP,
      RETURNING_PICKAXE,
      DONE_REPAIR;
   }
}
