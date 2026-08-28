package code.elixir.features.impl.misc;

import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.utils.RaycastAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.task.TaskPriority;
import code.elixir.utils.math.time.StopWatch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

public class ChestStealer extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final StopWatch openDelayWatch = new StopWatch();
   private final Map<String, Item> itemMap = new HashMap<>();
   private final SelectSetting modeSetting = new SelectSetting("Тип", "Выбирает тип стила").value("ФанТайм", "Избранные предметы", "Обычный");
   private final SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между кликами по слоту")
      .setValue(100.0F)
      .range(0, 1000)
      .visible(() -> {
         String mode = this.modeSetting.getSelected();
         return mode.equals("Избранные предметы") || mode.equals("Обычный");
      });
   private final BooleanSetting autoOpen = new BooleanSetting("Авто открытие", "Наводится на сундук и открывает его сам")
      .visible(() -> this.modeSetting.getSelected().equals("Обычный"));
   private final MultiSelectSetting itemSettings = new MultiSelectSetting("Предметы", "Выберите предметы, которые вор будет подбирать")
      .value(
         "Голова игрока",
         "Тотем бессмертия",
         "Элитры",
         "Незеритовый меч",
         "Незеритовый шлем",
         "Незеритовый нагрудник",
         "Незеритовые поножи",
         "Незеритовые ботинки",
         "Незеритовый слиток",
         "Незеритовый лом"
      )
      .visible(() -> this.modeSetting.getSelected().equals("Избранные предметы"));

   public ChestStealer() {
      super("ChestStealer", "ChestStealer", ModuleCategory.MISC);
      this.setup(new Setting[]{this.modeSetting, this.delaySetting, this.autoOpen, this.itemSettings});
      this.initializeItemMap();
   }

   private void initializeItemMap() {
      this.itemMap.put("Голова игрока", Items.PLAYER_HEAD);
      this.itemMap.put("Тотем бессмертия", Items.TOTEM_OF_UNDYING);
      this.itemMap.put("Элитры", Items.ELYTRA);
      this.itemMap.put("Незеритовый меч", Items.NETHERITE_SWORD);
      this.itemMap.put("Незеритовый шлем", Items.NETHERITE_HELMET);
      this.itemMap.put("Незеритовый нагрудник", Items.NETHERITE_CHESTPLATE);
      this.itemMap.put("Незеритовые поножи", Items.NETHERITE_LEGGINGS);
      this.itemMap.put("Незеритовые ботинки", Items.NETHERITE_BOOTS);
      this.itemMap.put("Незеритовый слиток", Items.NETHERITE_INGOT);
      this.itemMap.put("Незеритовый лом", Items.NETHERITE_PICKAXE);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         String mode;
         String var3 = mode = this.modeSetting.getSelected();
         switch (var3) {
            case "ФанТайм":
               this.handleFunTimeMode();
               break;
            case "Избранные предметы":
            case "Обычный":
               this.handleDefaultModes(mode);
         }
      }
   }

   private void handleFunTimeMode() {
      Screen class_4372 = mc.currentScreen;
      GenericContainerScreen sh;
      if (class_4372 instanceof GenericContainerScreen
         && (sh = (GenericContainerScreen)class_4372).getTitle().getString().toLowerCase().contains("мистический")
         && !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {
         ((GenericContainerScreenHandler)sh.getScreenHandler())
            .slots
            .stream()
            .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && this.stopWatch.every(150.0))
            .forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));
      }
   }

   private void handleDefaultModes(String mode) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) {
         sh.slots
            .forEach(
               s -> {
                  boolean isDefaultMode = mode.equals("Обычный");
                  if (s.hasStack()
                     && !s.inventory.equals(mc.player.getInventory())
                     && (isDefaultMode || this.whiteList(s.getStack().getItem()))
                     && this.stopWatch.every(this.delaySetting.getValue())) {
                     InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true);
                  }
               }
            );
      }

      if (mode.equals("Обычный") && this.autoOpen.isValue() && mc.currentScreen == null) {
         this.tryOpenChest(this.findNearestChest(6, 200.25), false);
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0 && mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (this.modeSetting.getSelected().equals("Обычный") && this.autoOpen.isValue()) {
            BlockPos chestPos = this.findNearestChest(6, 200.25);
            if (chestPos != null) {
               Turns target = MathAngle.calculateAngle(this.getChestInteractVec(chestPos));
               TurnsConnection.INSTANCE.rotateTo(target, TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
         }
      }
   }

   private BlockPos findNearestChest(int range, double maxDistanceSq) {
      BlockPos playerPos = mc.player.getBlockPos();
      Mutable nearest = new Mutable();
      double nearestDistSq = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.iterate(playerPos.add(-range, -range, -range), playerPos.add(range, range, range))) {
         BlockState state = mc.world.getBlockState(pos);
         double distSq;
         if ((state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST))
            && (distSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))) < nearestDistSq
            && distSq <= maxDistanceSq) {
            nearestDistSq = distSq;
            nearest.set(pos);
         }
      }

      return nearestDistSq < Double.MAX_VALUE ? nearest.toImmutable() : null;
   }

   private Vec3d getChestInteractVec(BlockPos pos) {
      Vec3d eye = mc.player.getEyePos();
      Vec3d center = Vec3d.ofCenter(pos);
      double dx = eye.x - center.x;
      double dy = eye.y - center.y;
      double dz = eye.z - center.z;
      double absX = Math.abs(dx);
      double absY = Math.abs(dy);
      double absZ = Math.abs(dz);
      if (absX >= absY && absX >= absZ) {
         return new Vec3d(
            center.x + (dx > 0.0 ? 0.45 : -0.45),
            MathHelper.clamp(eye.y, center.y - 0.4, center.y + 0.4),
            MathHelper.clamp(eye.z, center.z - 0.45, center.z + 0.45)
         );
      } else {
         return absZ >= absY
            ? new Vec3d(
               MathHelper.clamp(eye.x, center.x - 0.45, center.x + 0.45),
               MathHelper.clamp(eye.y, center.y - 0.4, center.y + 0.4),
               center.z + (dz > 0.0 ? 0.45 : -0.45)
            )
            : new Vec3d(
               MathHelper.clamp(eye.x, center.x - 0.45, center.x + 0.45),
               center.y + (dy > 0.0 ? 0.45 : -0.45),
               MathHelper.clamp(eye.z, center.z - 0.45, center.z + 0.45)
            );
      }
   }

   private boolean canInteractWithChest(BlockPos chestPos, double reach) {
      Turns rotation = TurnsConnection.INSTANCE.getRotation();
      if (rotation == null) {
         rotation = new Turns(mc.player.getYaw(), mc.player.getPitch());
      }

      BlockHitResult hit;
      if ((hit = RaycastAngle.raycast(reach, rotation, false)) != null && hit.getType() == Type.BLOCK) {
         BlockPos hitPos = hit.getBlockPos();
         if (hitPos.equals(chestPos)) {
            return true;
         } else {
            BlockPos partner = this.getDoubleChestPartner(chestPos);
            return partner != null && hitPos.equals(partner);
         }
      } else {
         return false;
      }
   }

   private void tryOpenChest(BlockPos chestPos, boolean ignored) {
      if (chestPos != null && mc.currentScreen == null) {
         if (!(mc.player.squaredDistanceTo(Vec3d.ofCenter(chestPos)) > 200.25) && this.openDelayWatch.finished(200.0)) {
            BlockHitResult hit;
            if (this.canInteractWithChest(chestPos, 6.0)
               && (hit = RaycastAngle.raycast(6.0, TurnsConnection.INSTANCE.getRotation(), false)) != null
               && hit.getType() == Type.BLOCK) {
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
               mc.player.swingHand(Hand.MAIN_HAND);
               this.openDelayWatch.reset();
            }
         }
      }
   }

   private BlockPos getDoubleChestPartner(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
         return null;
      } else {
         ChestType type = (ChestType)state.get(ChestBlock.CHEST_TYPE);
         if (type == ChestType.SINGLE) {
            return null;
         } else {
            Direction facing = (Direction)state.get(ChestBlock.FACING);
            BlockPos partner = pos.offset(type == ChestType.LEFT ? facing.rotateYCounterclockwise() : facing.rotateYClockwise());
            return mc.world.getBlockState(partner).getBlock() instanceof ChestBlock ? partner.toImmutable() : null;
         }
      }
   }

   @Override
   public void deactivate() {
      TurnsConnection.INSTANCE.clear();
      TurnsConnection.INSTANCE.setRotation(null);
      super.deactivate();
   }

   private boolean whiteList(Item item) {
      List<String> selected = this.itemSettings.getSelected();
      if (selected != null && !selected.isEmpty()) {
         for (String selectedName : selected) {
            Item mappedItem = this.itemMap.get(selectedName);
            if (mappedItem != null && mappedItem.equals(item)) {
               return true;
            }
         }

         if (mc.world != null && mc.player != null) {
            ItemStack stack = item.getDefaultStack();
            String itemNameLower = stack.getName().getString().toLowerCase();

            for (String selectedNamex : selected) {
               String selectedLower = selectedNamex.toLowerCase();
               if (itemNameLower.contains(selectedLower) || selectedLower.contains(itemNameLower)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
