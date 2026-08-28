package code.elixir.utils.interactions.inv;

import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class InventoryToolkit implements QuickImports {
   private static int cachedSlot = -1;

   public static int getItemSlot(Item input) {
      for (ItemStack stack : mc.player.getInventory().armor) {
         if (stack.getItem() == input && stack.getDamage() < 430) {
            return -2;
         }
      }

      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stackx = mc.player.getInventory().getStack(i);
         if (stackx.getItem() == input && stackx.getDamage() < 430) {
            slot = i;
            break;
         }
      }

      if (slot < 9 && slot != -1) {
         slot += 36;
      }

      return slot;
   }

   public static void moveTo(int syncId, ItemStack stack, int slot) {
      if (!Objects.isNull(stack)) {
         mc.interactionManager.clickSlot(syncId, mc.player.getInventory().getSlotWithStack(stack), 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static InventoryResult findInInventory(InventoryToolkit.Searcher searcher) {
      if (mc.player != null) {
         for (ItemStack stack : mc.player.getInventory().armor) {
            if (searcher.isValid(stack)) {
               return new InventoryResult(-2, true, stack);
            }
         }

         for (int i = 40; i >= 0; i--) {
            ItemStack stackx = mc.player.getInventory().getStack(i);
            if (searcher.isValid(stackx)) {
               if (i < 9) {
                  i += 36;
               }

               return new InventoryResult(i, true, stackx);
            }
         }
      }

      return InventoryResult.notFound();
   }

   public static InventoryResult findItemInInventory(List<Item> items) {
      return findInInventory(stack -> items.contains(stack.getItem()));
   }

   public static InventoryResult findItemInInventory(Item... items) {
      return findItemInInventory(Arrays.asList(items));
   }

   public static int getAxe() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof AxeItem) {
            return i;
         }
      }

      return -1;
   }

   public static InventoryResult findItemInHotBar(Item item) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      } else {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && stack.getDamage() < 430) {
               return new InventoryResult(i, true, stack);
            }
         }

         return InventoryResult.notFound();
      }
   }

   public static InventoryResult findItemInInventory(Item item) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      } else {
         for (ItemStack stack : mc.player.getInventory().armor) {
            if (stack.getItem() == item && stack.getDamage() < 430) {
               return new InventoryResult(-2, true, stack);
            }
         }

         for (int i = 36; i >= 0; i--) {
            ItemStack stackx = mc.player.getInventory().getStack(i);
            if (stackx.getItem() == item && stackx.getDamage() < 430) {
               if (i < 9) {
                  i += 36;
               }

               return new InventoryResult(i, true, stackx);
            }
         }

         return InventoryResult.notFound();
      }
   }

   public static InventoryResult findInHotBar(InventoryToolkit.Searcher searcher) {
      if (mc.player != null) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (searcher.isValid(stack) && stack.getDamage() < 430) {
               return new InventoryResult(i, true, stack);
            }
         }
      }

      return InventoryResult.notFound();
   }

   public static InventoryResult findItemInHotBar(List<Item> items) {
      return findInHotBar(stack -> items.contains(stack.getItem()));
   }

   public static InventoryResult findItemInHotBar(Item... items) {
      return findItemInHotBar(Arrays.asList(items));
   }

   public static void saveSlot() {
      cachedSlot = mc.player.getInventory().selectedSlot;
   }

   public static void returnSlot() {
      if (cachedSlot != -1) {
         switchTo(cachedSlot);
      }

      cachedSlot = -1;
   }

   public static void use(int slot) {
      if (slot == 1 || slot == 4 || slot == 21) {
         if (slot < 9) {
            int n3 = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot - 1;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot - 1));
            InventoryTick.schelude(() -> {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
            }, 2);
            InventoryTick.schelude(() -> {
               mc.player.getInventory().selectedSlot = n3;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(n3));
            }, 2);
         } else {
            mc.player.setSprinting(false);
            InventoryTick.schelude(
               () -> mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player),
               2
            );
            InventoryTick.schelude(() -> {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
            }, 2);
            InventoryTick.schelude(
               () -> mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player),
               4
            );
            InventoryTick.schelude(() -> mc.getNetworkHandler().sendPacket(new CloseScreenS2CPacket(mc.player.currentScreenHandler.syncId)), 2);
         }
      }
   }

   public static void switchTo(int slot) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         if (mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
         }
      }
   }

   public static void switchToSilent(int slot) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.getNetworkHandler() != null && mc.world != null) {
         mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
      }
   }

   public static void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }

   public static void clickSlot(int id) {
      if (id != -1 && mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static void clickSlot(int id, SlotActionType type) {
      if (id != -1 && mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 0, type, mc.player);
      }
   }

   public static void clickSlot(int id, int button, SlotActionType type) {
      if (id != -1 && mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
      }
   }

   public static ItemStack byItem(Item item) {
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.getItem().equals(item) && itemStack.getDamage() < 430) {
            return itemStack;
         }
      }

      return null;
   }

   public static boolean quickMoveFromTo(int from, int to) {
      if (from != -1 && to != -1 && mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
         return true;
      } else {
         return false;
      }
   }

   public static int getSlotWithStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (ItemStack.areEqual(invStack, stack)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public interface Searcher {
      boolean isValid(ItemStack var1);
   }
}
