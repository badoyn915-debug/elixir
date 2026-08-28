package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class MetaHvHHandler extends ServerHelperHandler {
   private final List<BindSetting> binds;
   private final SelectSetting helmetType;
   private final SliderSettings swapDelay;
   private final BooleanSetting autoBootsWater;
   private final SliderSettings ecRadius;
   private final StopWatch swapTimer = new StopWatch();
   private boolean autoEcEnabled;
   private BlockPos miningPos = BlockPos.ORIGIN;

   public MetaHvHHandler(List<BindSetting> binds, SelectSetting helmetType, SliderSettings swapDelay, BooleanSetting autoBootsWater, SliderSettings ecRadius) {
      this.binds = binds;
      this.helmetType = helmetType;
      this.swapDelay = swapDelay;
      this.autoBootsWater = autoBootsWater;
      this.ecRadius = ecRadius;
   }

   @Override
   public String getModeId() {
      return "MetaHvH";
   }

   @Override
   public String getOverlayTitle() {
      return "Meta";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();
      boolean hasSanta = this.hasSantaHead();
      items.add(
         new ServerHelperRenderItem("Свап головы", "H", hasSanta ? "✓" : "—", Items.PLAYER_HEAD)
            .withBind(this.binds.isEmpty() ? -1 : this.binds.get(0).getKey())
      );
      boolean hasBoots = findSlotByItem(Items.GOLDEN_BOOTS) != -1 || findSlotByItem(Items.NETHERITE_BOOTS) != -1 || findSlotByItem(Items.DIAMOND_BOOTS) != -1;
      items.add(
         new ServerHelperRenderItem("Свап ботинок", "B", hasBoots ? "✓" : "—", Items.GOLDEN_BOOTS)
            .withBind(this.binds.size() > 1 ? this.binds.get(1).getKey() : -1)
      );
      items.add(
         new ServerHelperRenderItem("Авто EC", "EC", this.autoEcEnabled ? "ON" : "OFF", Items.ENDER_CHEST)
            .withBind(this.binds.size() > 2 ? this.binds.get(2).getKey() : -1)
      );
      return items;
   }

   @Override
   public boolean onKey(int keyCode) {
      if (!this.binds.isEmpty() && mc.player != null) {
         if (this.binds.get(0).getKey() != -1 && this.binds.get(0).getKey() == keyCode) {
            if (this.swapTimer.finished((long)(this.swapDelay.getValue() * 1000.0F))) {
               this.swapHead();
               this.swapTimer.reset();
            }

            return true;
         } else if (this.binds.size() > 1 && this.binds.get(1).getKey() != -1 && this.binds.get(1).getKey() == keyCode) {
            this.swapBoots();
            return true;
         } else if (this.binds.size() > 2 && this.binds.get(2).getKey() != -1 && this.binds.get(2).getKey() == keyCode) {
            this.autoEcEnabled = !this.autoEcEnabled;
            notify(this.autoEcEnabled ? "§aАвто EC включен" : "§cАвто EC выключен");
            if (!this.autoEcEnabled) {
               this.miningPos = BlockPos.ORIGIN;
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      if (this.autoBootsWater.isValue()) {
         this.handleWaterBoots();
      }

      if (this.autoEcEnabled) {
         this.handleAutoEc();
      }
   }

   private void handleWaterBoots() {
      if (mc.player != null) {
         ItemStack boots = mc.player.getEquippedStack(EquipmentSlot.FEET);
         int goldSlot = findSlotByItem(Items.GOLDEN_BOOTS);
         Item normal = findSlotByItem(Items.NETHERITE_BOOTS) != -1 ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS;
         int normalSlot = findSlotByItem(normal);
         boolean inWater = mc.player.isTouchingWater();
         if (inWater && normalSlot != -1 && !boots.isOf(normal)) {
            this.swapArmor(normalSlot, 8);
         } else if (!inWater && goldSlot != -1 && !boots.isOf(Items.GOLDEN_BOOTS)) {
            this.swapArmor(goldSlot, 8);
         }
      }
   }

   private void handleAutoEc() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         int pickSlot = findSlotByItem(Items.NETHERITE_PICKAXE);
         if (pickSlot == -1) {
            pickSlot = findSlotByItem(Items.DIAMOND_PICKAXE);
         }

         if (pickSlot != -1 && !mc.player.isUsingItem()) {
            for (BlockPos pos : this.findEcBlocks()) {
               if (mc.world.getBlockState(pos).isOf(Blocks.ENDER_CHEST)) {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  if (pickSlot < 9) {
                     mc.player.getInventory().selectedSlot = pickSlot;
                     this.syncSlot();
                  } else {
                     int from = pickSlot < 9 ? pickSlot + 36 : pickSlot;
                     InventoryTask.clickSlot(from, oldSlot, SlotActionType.SWAP, false);
                  }

                  mc.interactionManager.attackBlock(pos, Direction.UP);
                  this.miningPos = pos;
                  if (pickSlot >= 9) {
                     int from = pickSlot < 9 ? pickSlot + 36 : pickSlot;
                     InventoryTask.clickSlot(from, oldSlot, SlotActionType.SWAP, false);
                  }

                  mc.player.getInventory().selectedSlot = oldSlot;
                  this.syncSlot();
                  break;
               }
            }
         }
      }
   }

   private CopyOnWriteArrayList<BlockPos> findEcBlocks() {
      CopyOnWriteArrayList<BlockPos> blocks = new CopyOnWriteArrayList<>();
      if (mc.player != null && mc.world != null) {
         BlockPos start = mc.player.getBlockPos();
         int radius = this.ecRadius.getInt();

         for (int y = 3; y >= 0; y--) {
            for (int x = radius; x >= -radius; x--) {
               for (int z = radius; z >= -radius; z--) {
                  BlockPos pos = start.add(x, y, z);
                  if (pos.getY() > 0 && mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                     blocks.add(pos);
                  }
               }
            }
         }

         return blocks;
      } else {
         return blocks;
      }
   }

   private void swapHead() {
      Item helmetItem = this.helmetType.isSelected("Незеритовый") ? Items.NETHERITE_HELMET : Items.GOLDEN_HELMET;
      ItemStack current = mc.player.getEquippedStack(EquipmentSlot.HEAD);
      int helmetSlot = findSlotByItem(helmetItem);
      boolean wearingHelmet = current.isOf(helmetItem);
      boolean wearingSanta = current.isOf(Items.PLAYER_HEAD);
      if (wearingHelmet) {
         this.equipSantaHead();
      } else if (wearingSanta && helmetSlot != -1) {
         this.swapArmor(helmetSlot, 5);
      } else {
         notify("§cНе найден шлем или голова для свапа!");
      }
   }

   private void equipSantaHead() {
      for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.isOf(Items.PLAYER_HEAD) && this.isSantaHead(stack)) {
            this.swapArmor(slot, 5);
            return;
         }
      }

      notify("§cГолова санты не найдена в инвентаре!");
   }

   private boolean hasSantaHead() {
      for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.isOf(Items.PLAYER_HEAD) && this.isSantaHead(stack)) {
            return true;
         }
      }

      return mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.PLAYER_HEAD);
   }

   private boolean isSantaHead(ItemStack stack) {
      return stack.isOf(Items.PLAYER_HEAD);
   }

   private void swapBoots() {
      ItemStack current = mc.player.getEquippedStack(EquipmentSlot.FEET);
      int goldSlot = findSlotByItem(Items.GOLDEN_BOOTS);
      Item normal = findSlotByItem(Items.NETHERITE_BOOTS) != -1 ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS;
      int normalSlot = findSlotByItem(normal);
      if (current.isOf(Items.GOLDEN_BOOTS) && normalSlot != -1) {
         this.swapArmor(normalSlot, 8);
      } else if (goldSlot != -1) {
         this.swapArmor(goldSlot, 8);
      } else {
         notify("§cНе найдены ботинки для свапа!");
      }
   }

   private void swapArmor(int fromSlot, int armorSlot) {
      int invFrom = fromSlot < 9 ? fromSlot + 36 : fromSlot;
      InventoryTask.clickSlot(invFrom, 0, SlotActionType.PICKUP, false);
      InventoryTask.clickSlot(armorSlot, 0, SlotActionType.PICKUP, false);
      InventoryTask.clickSlot(invFrom, 0, SlotActionType.PICKUP, false);
   }

   private void syncSlot() {
      if (mc.interactionManager != null) {
         mc.interactionManager.syncSelectedSlot();
      }
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
      this.autoEcEnabled = false;
      this.miningPos = BlockPos.ORIGIN;
   }
}
