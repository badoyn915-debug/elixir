package code.elixir.features.impl.misc;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoBrew extends Module {
   private static final int INGREDIENT_SLOT = 0;
   private static final int FUEL_SLOT = 1;
   private static final int BOTTLE_SLOT_START = 2;
   private static final int BOTTLE_SLOT_END = 4;
   private static final int PLAYER_SLOT_MIN = 5;
   private final SelectSetting potionType = new SelectSetting("Зелье", "Какое зелье варить")
      .value("Огнестойкость", "Невидимость", "Сила", "Скорость", "Исцеление")
      .selected("Огнестойкость");
   private final BooleanSetting redstoneExtend = new BooleanSetting("Удлинение", "Добавляет редстоун для увеличения длительности").setValue(true);
   private final BooleanSetting glowstoneUpgrade = new BooleanSetting("Усиление II", "Добавляет светопыль для уровня II").setValue(false);
   private final BooleanSetting infiniteMode = new BooleanSetting("Бесконечно", "Повторять варку, пока модуль включён").setValue(true);
   private final BooleanSetting searchChest = new BooleanSetting("Искать в сундуке", "Перед варкой пытается взять ресурсы из сундука").setValue(true);
   private final SliderSettings brewTime = new SliderSettings("Время варки", "Секунд ожидания варки").setValue(20.0F).range(5.0F, 60.0F).step(1.0F);
   private final SliderSettings clickDelay = new SliderSettings("Задержка кликов", "Миллисекунд между кликами")
      .setValue(250.0F)
      .range(50.0F, 1000.0F)
      .step(50.0F);
   private final StopWatch actionTimer = new StopWatch();
   private final StopWatch brewTimer = new StopWatch();
   private AutoBrew.BrewState state = AutoBrew.BrewState.IDLE;
   private int cycles;
   private BlockPos standPos;
   private BlockPos chestPos;
   private AutoBrew.BrewRecipe recipe;
   private AutoBrew.BrewStage stage;
   private List<Item> neededItems = new ArrayList<>();
   private int lootItemIndex;
   private boolean awkwardBrewDone;
   private boolean waitingSync;

   public AutoBrew() {
      super("AutoBrew", "Auto Brew", ModuleCategory.MISC);
      this.setup(
         new Setting[]{this.potionType, this.redstoneExtend, this.glowstoneUpgrade, this.infiniteMode, this.searchChest, this.brewTime, this.clickDelay}
      );
   }

   @Override
   public void activate() {
      this.cycles = 0;
      this.reset();
      this.state = AutoBrew.BrewState.PREPARE;
      ChatMessage.brandmessage("AutoBrew: запуск — " + this.potionType.getSelected());
   }

   @Override
   public void deactivate() {
      this.reset();
      super.deactivate();
   }

   private void reset() {
      this.state = AutoBrew.BrewState.IDLE;
      this.standPos = null;
      this.chestPos = null;
      this.recipe = null;
      this.stage = AutoBrew.BrewStage.MAIN;
      this.neededItems.clear();
      this.lootItemIndex = 0;
      this.awkwardBrewDone = false;
      this.waitingSync = false;
      this.actionTimer.reset();
      this.brewTimer.reset();
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         switch (this.state) {
            case IDLE:
            default:
               break;
            case PREPARE:
               this.prepareCycle();
               break;
            case SEARCH_CHEST:
               this.searchChest();
               break;
            case LOOT_CHEST:
               this.lootChest();
               break;
            case FIND_STAND:
               this.findStand();
               break;
            case OPEN_STAND:
               this.openStand();
               break;
            case PLACE_INGREDIENT:
               this.placeIngredient();
               break;
            case WAIT_BREW:
               this.waitBrew();
               break;
            case COLLECT:
               this.collectPotions();
               break;
            case FINISH:
               this.finishCycle();
         }
      }
   }

   private void prepareCycle() {
      this.recipe = AutoBrew.BrewRecipe.fromLabel(this.potionType.getSelected());
      if (this.recipe == null) {
         ChatMessage.brandmessage("§cAutoBrew: неизвестный тип зелья");
         this.setState(AutoBrew.BrewState.IDLE);
         this.deactivate();
      } else {
         this.stage = this.recipe.isTwoStage() ? AutoBrew.BrewStage.PRE : AutoBrew.BrewStage.MAIN;
         this.awkwardBrewDone = false;
         this.waitingSync = false;
         this.neededItems = this.recipe.itemsForStage(this.stage, this.redstoneExtend.isValue(), this.glowstoneUpgrade.isValue());
         this.lootItemIndex = 0;
         if (this.searchChest.isValue() && !this.hasItems(this.neededItems)) {
            this.setState(AutoBrew.BrewState.SEARCH_CHEST);
         } else {
            this.setState(AutoBrew.BrewState.FIND_STAND);
         }
      }
   }

   private void searchChest() {
      if (this.actionTimer.finished((long)this.clickDelay.getValue())) {
         this.chestPos = this.findNearbyBlock(10, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL);
         if (this.chestPos == null) {
            ChatMessage.brandmessage("§cAutoBrew: сундук с ресурсами не найден");
            this.setState(AutoBrew.BrewState.FIND_STAND);
         } else {
            this.interactBlock(this.chestPos);
            this.actionTimer.reset();
            this.setState(AutoBrew.BrewState.LOOT_CHEST);
         }
      }
   }

   private void lootChest() {
      if (mc.currentScreen instanceof GenericContainerScreen && mc.player.currentScreenHandler != null) {
         if (this.actionTimer.finished((long)this.clickDelay.getValue())) {
            if (this.lootItemIndex >= this.neededItems.size()) {
               mc.player.closeHandledScreen();
               this.actionTimer.reset();
               this.setState(AutoBrew.BrewState.FIND_STAND);
            } else {
               Item wanted = this.neededItems.get(this.lootItemIndex);
               Slot slot = this.findPlayerItem(wanted);
               if (slot != null) {
                  InventoryTask.clickSlot(slot.id, 0, SlotActionType.QUICK_MOVE, false, false);
                  this.actionTimer.reset();
               }

               this.lootItemIndex++;
            }
         }
      } else {
         if (this.actionTimer.finished(500.0)) {
            this.setState(AutoBrew.BrewState.FIND_STAND);
         }
      }
   }

   private void findStand() {
      this.standPos = this.findNearbyBlock(5, Blocks.BREWING_STAND);
      if (this.standPos == null) {
         ChatMessage.brandmessage("§cAutoBrew: варочная стойка не найдена");
         this.finishCycle();
      } else {
         this.setState(AutoBrew.BrewState.OPEN_STAND);
      }
   }

   private void openStand() {
      if (this.actionTimer.finished((long)this.clickDelay.getValue())) {
         if (!(mc.currentScreen instanceof BrewingStandScreen)) {
            this.interactBlock(this.standPos);
            this.actionTimer.reset();
         } else {
            this.waitingSync = false;
            this.setState(AutoBrew.BrewState.PLACE_INGREDIENT);
         }
      }
   }

   private void placeIngredient() {
      if (mc.currentScreen instanceof BrewingStandScreen && mc.player.currentScreenHandler != null) {
         if (this.actionTimer.finished((long)this.clickDelay.getValue())) {
            if (this.waitingSync) {
               this.waitingSync = false;
               this.actionTimer.reset();
            } else {
               ScreenHandler handler = mc.player.currentScreenHandler;
               if (!handler.getCursorStack().isEmpty()) {
                  Slot empty = this.findEmptyPlayerSlot();
                  if (empty != null) {
                     InventoryTask.clickSlot(empty.id, 0, SlotActionType.PICKUP, false, false);
                     this.waitingSync = true;
                     this.actionTimer.reset();
                  }
               } else if (!this.isFuelReady(handler)) {
                  Slot fuel = this.findPlayerItem(Items.BLAZE_POWDER);
                  if (fuel != null) {
                     InventoryTask.clickSlot(fuel.id, 0, SlotActionType.QUICK_MOVE, false, false);
                     this.waitingSync = true;
                     this.actionTimer.reset();
                  } else {
                     ChatMessage.brandmessage("§cAutoBrew: нет порошка ифрита");
                     mc.player.closeHandledScreen();
                     this.finishCycle();
                  }
               } else {
                  int emptyBottleSlot = this.findEmptyBottleSlot(handler);
                  if (emptyBottleSlot >= 0) {
                     Slot bottle = this.findBottleSlot();
                     if (bottle != null) {
                        InventoryTask.clickSlot(bottle.id, 0, SlotActionType.QUICK_MOVE, false, false);
                        this.waitingSync = true;
                        this.actionTimer.reset();
                     } else {
                        ChatMessage.brandmessage("§cAutoBrew: нет подходящих бутылок");
                        mc.player.closeHandledScreen();
                        this.finishCycle();
                     }
                  } else {
                     Item ingredient = this.activeIngredient();
                     if (ingredient == null) {
                        this.brewTimer.reset();
                        this.setState(AutoBrew.BrewState.WAIT_BREW);
                     } else {
                        ItemStack ingredientStack = handler.getSlot(0).getStack();
                        if (ingredientStack.isOf(ingredient)) {
                           this.brewTimer.reset();
                           this.setState(AutoBrew.BrewState.WAIT_BREW);
                        } else if (!ingredientStack.isEmpty()) {
                           InventoryTask.clickSlot(0, 0, SlotActionType.QUICK_MOVE, false, false);
                           this.waitingSync = true;
                           this.actionTimer.reset();
                        } else {
                           Slot ingredientSlot = this.findPlayerItem(ingredient);
                           if (ingredientSlot != null) {
                              InventoryTask.clickSlot(ingredientSlot.id, 0, SlotActionType.QUICK_MOVE, false, false);
                              this.waitingSync = true;
                              this.actionTimer.reset();
                           } else {
                              ChatMessage.brandmessage("§cAutoBrew: не найден " + ingredient.getName().getString());
                              mc.player.closeHandledScreen();
                              this.finishCycle();
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.setState(AutoBrew.BrewState.OPEN_STAND);
      }
   }

   private boolean isFuelReady(ScreenHandler handler) {
      ItemStack fuelStack = handler.getSlot(1).getStack();
      return !fuelStack.isEmpty() && fuelStack.isOf(Items.BLAZE_POWDER) ? true : this.findPlayerItem(Items.BLAZE_POWDER) == null;
   }

   private int findEmptyBottleSlot(ScreenHandler handler) {
      for (int bottleSlot = 2; bottleSlot <= 4; bottleSlot++) {
         if (handler.getSlot(bottleSlot).getStack().isEmpty()) {
            return bottleSlot;
         }
      }

      return -1;
   }

   private Slot findEmptyPlayerSlot() {
      return InventoryTask.slots()
         .filter(slot -> slot.inventory instanceof PlayerInventory)
         .filter(slot -> slot.id >= 5)
         .filter(slot -> slot.getStack().isEmpty())
         .findFirst()
         .orElse(null);
   }

   private void waitBrew() {
      if (this.brewTimer.finished((long)(this.brewTime.getValue() * 1000.0F))) {
         this.setState(AutoBrew.BrewState.COLLECT);
      }
   }

   private void collectPotions() {
      if (mc.currentScreen instanceof BrewingStandScreen && mc.player.currentScreenHandler != null) {
         if (this.actionTimer.finished((long)this.clickDelay.getValue())) {
            for (int slot = 2; slot <= 4; slot++) {
               if (!mc.player.currentScreenHandler.getSlot(slot).getStack().isEmpty()) {
                  InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false, false);
               }
            }

            mc.player.closeHandledScreen();
            this.actionTimer.reset();
            if (this.needsAwkwardBrewFirst()) {
               this.awkwardBrewDone = true;
               this.setState(AutoBrew.BrewState.OPEN_STAND);
            } else if (this.recipe.isTwoStage() && this.stage == AutoBrew.BrewStage.PRE) {
               this.stage = AutoBrew.BrewStage.MAIN;
               this.neededItems = this.recipe.itemsForStage(this.stage, this.redstoneExtend.isValue(), this.glowstoneUpgrade.isValue());
               this.setState(AutoBrew.BrewState.PREPARE);
            } else {
               Item modifier = this.currentModifier();
               if (modifier != null && this.stage != AutoBrew.BrewStage.MODIFIER) {
                  this.stage = AutoBrew.BrewStage.MODIFIER;
                  this.setState(AutoBrew.BrewState.OPEN_STAND);
               } else {
                  this.cycles++;
                  ChatMessage.brandmessage("§aAutoBrew: цикл #" + this.cycles + " завершён");
                  this.setState(AutoBrew.BrewState.FINISH);
               }
            }
         }
      } else {
         this.setState(AutoBrew.BrewState.OPEN_STAND);
      }
   }

   private void finishCycle() {
      if (!this.infiniteMode.isValue()) {
         ChatMessage.brandmessage("§aAutoBrew: готово. Циклов: " + this.cycles);
         this.deactivate();
      } else {
         this.setState(AutoBrew.BrewState.PREPARE);
      }
   }

   private Item activeIngredient() {
      if (this.stage == AutoBrew.BrewStage.MODIFIER) {
         return this.currentModifier();
      } else {
         return this.needsAwkwardBrewFirst() ? Items.NETHER_WART : this.recipe.ingredientFor(this.stage);
      }
   }

   private boolean needsAwkwardBrewFirst() {
      return this.recipe != null && this.recipe.needsAwkwardBase() && this.stage == AutoBrew.BrewStage.MAIN && !this.awkwardBrewDone;
   }

   private Slot findPlayerItem(Item item) {
      return InventoryTask.slots()
         .filter(slot -> slot.inventory instanceof PlayerInventory)
         .filter(slot -> slot.id >= 5)
         .filter(slot -> slot.getStack().isOf(item))
         .findFirst()
         .orElse(null);
   }

   private Slot findBottleSlot() {
      Slot nightVision;
      if (this.recipe.isTwoStage()
         && this.stage == AutoBrew.BrewStage.MAIN
         && (nightVision = InventoryTask.getPotion(StatusEffects.NIGHT_VISION)) != null
         && nightVision.inventory instanceof PlayerInventory
         && nightVision.id >= 5) {
         return nightVision;
      } else {
         if (this.needsAwkwardBrewFirst()) {
            Slot water = this.findWaterBottle();
            if (water != null) {
               return water;
            }
         } else if (this.recipe.needsAwkwardBase() && this.awkwardBrewDone) {
            Slot awkward = this.findAwkwardPotion();
            if (awkward != null) {
               return awkward;
            }
         } else {
            Slot water = this.findWaterBottle();
            if (water != null) {
               return water;
            }
         }

         return InventoryTask.slots()
            .filter(slot -> slot.inventory instanceof PlayerInventory)
            .filter(slot -> slot.id >= 5)
            .filter(slot -> slot.getStack().isOf(Items.POTION))
            .findFirst()
            .orElse(null);
      }
   }

   private Slot findWaterBottle() {
      return InventoryTask.slots()
         .filter(slot -> slot.inventory instanceof PlayerInventory)
         .filter(slot -> slot.id >= 5)
         .filter(slot -> this.isWaterBottle(slot.getStack()))
         .findFirst()
         .orElse(null);
   }

   private Slot findAwkwardPotion() {
      return InventoryTask.slots()
         .filter(slot -> slot.inventory instanceof PlayerInventory)
         .filter(slot -> slot.id >= 5)
         .filter(slot -> this.isAwkwardPotion(slot.getStack()))
         .findFirst()
         .orElse(null);
   }

   private boolean isWaterBottle(ItemStack stack) {
      if (!stack.isOf(Items.POTION)) {
         return false;
      } else if (this.isAwkwardPotion(stack)) {
         return false;
      } else {
         PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (contents == null) {
            return true;
         } else if (contents.getEffects().iterator().hasNext()) {
            return false;
         } else {
            return contents.potion().isEmpty() ? true : this.hasPotionId(contents, "water");
         }
      }
   }

   private boolean isAwkwardPotion(ItemStack stack) {
      if (!stack.isOf(Items.POTION)) {
         return false;
      } else {
         PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (contents == null || contents.potion().isEmpty()) {
            return false;
         } else {
            return contents.getEffects().iterator().hasNext() ? false : this.hasPotionId(contents, "awkward");
         }
      }
   }

   private boolean hasPotionId(PotionContentsComponent contents, String id) {
      return contents.potion().map(entry -> entry.getKey().map(key -> key.getValue().equals(Identifier.ofVanilla(id))).orElse(false)).orElse(false);
   }

   private Item currentIngredient() {
      return this.activeIngredient();
   }

   private Item currentModifier() {
      if (this.glowstoneUpgrade.isValue()) {
         return Items.GLOWSTONE_DUST;
      } else {
         return this.redstoneExtend.isValue() ? Items.REDSTONE : null;
      }
   }

   private boolean hasItems(List<Item> items) {
      for (Item item : items) {
         if (this.findPlayerItem(item) == null) {
            return false;
         }
      }

      return true;
   }

   private void setState(AutoBrew.BrewState next) {
      this.state = next;
      this.actionTimer.reset();
   }

   private BlockPos findNearbyBlock(int radius, Block... blocks) {
      BlockPos origin = mc.player.getBlockPos();

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos pos = origin.add(x, y, z);
               Block block = mc.world.getBlockState(pos).getBlock();

               for (Block target : blocks) {
                  if (block == target) {
                     return pos;
                  }
               }
            }
         }
      }

      return null;
   }

   private void interactBlock(BlockPos pos) {
      Vec3d hit = Vec3d.ofCenter(pos);
      BlockHitResult result = new BlockHitResult(hit, Direction.UP, pos, false);
      if (mc.interactionManager != null) {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private static enum BrewRecipe {
      FIRE_RESISTANCE("Огнестойкость", Items.MAGMA_CREAM, null, null, false),
      INVISIBILITY("Невидимость", Items.FERMENTED_SPIDER_EYE, Items.GOLDEN_CARROT, StatusEffects.NIGHT_VISION, true),
      STRENGTH("Сила", Items.BLAZE_POWDER, Items.NETHER_WART, StatusEffects.STRENGTH, false),
      SPEED("Скорость", Items.SUGAR, Items.NETHER_WART, StatusEffects.SPEED, false),
      HEALING("Исцеление", Items.GLISTERING_MELON_SLICE, Items.NETHER_WART, StatusEffects.INSTANT_HEALTH, false);

      private final String label;
      private final Item mainIngredient;
      private final Item preIngredient;
      private final RegistryEntry<StatusEffect> resultEffect;
      private final boolean twoStage;

      static AutoBrew.BrewRecipe fromLabel(String label) {
         for (AutoBrew.BrewRecipe recipe : values()) {
            if (recipe.label.equals(label)) {
               return recipe;
            }
         }

         return null;
      }

      Item ingredientFor(AutoBrew.BrewStage stage) {
         return this.twoStage && stage == AutoBrew.BrewStage.PRE ? this.preIngredient : this.mainIngredient;
      }

      List<Item> itemsForStage(AutoBrew.BrewStage stage, boolean redstone, boolean glowstone) {
         ArrayList<Item> items = new ArrayList<>();
         if (this.twoStage && stage == AutoBrew.BrewStage.PRE) {
            items.add(this.preIngredient);
            items.add(Items.POTION);
         } else if (this.twoStage && stage == AutoBrew.BrewStage.MAIN) {
            items.add(this.mainIngredient);
         } else if (!this.twoStage && this.needsAwkwardBase()) {
            items.add(this.mainIngredient);
            items.add(Items.NETHER_WART);
            items.add(Items.POTION);
         } else {
            items.add(this.mainIngredient);
            items.add(Items.POTION);
         }

         if (redstone) {
            items.add(Items.REDSTONE);
         }

         if (glowstone) {
            items.add(Items.GLOWSTONE_DUST);
         }

         items.add(Items.BLAZE_POWDER);
         return items;
      }

      boolean needsAwkwardBase() {
         return this == STRENGTH || this == SPEED || this == HEALING;
      }

      public String getLabel() {
         return this.label;
      }

      public Item getMainIngredient() {
         return this.mainIngredient;
      }

      public Item getPreIngredient() {
         return this.preIngredient;
      }

      public RegistryEntry<StatusEffect> getResultEffect() {
         return this.resultEffect;
      }

      public boolean isTwoStage() {
         return this.twoStage;
      }

      private BrewRecipe(String label, Item mainIngredient, Item preIngredient, RegistryEntry<StatusEffect> resultEffect, boolean twoStage) {
         this.label = label;
         this.mainIngredient = mainIngredient;
         this.preIngredient = preIngredient;
         this.resultEffect = resultEffect;
         this.twoStage = twoStage;
      }
   }

   private static enum BrewStage {
      PRE,
      MAIN,
      MODIFIER;
   }

   private static enum BrewState {
      IDLE,
      PREPARE,
      SEARCH_CHEST,
      LOOT_CHEST,
      FIND_STAND,
      OPEN_STAND,
      PLACE_INGREDIENT,
      WAIT_BREW,
      COLLECT,
      FINISH;
   }
}
