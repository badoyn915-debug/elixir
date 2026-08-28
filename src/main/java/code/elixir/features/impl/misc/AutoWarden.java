package code.elixir.features.impl.misc;

import code.elixir.Elixir;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.features.impl.render.WardenHelper;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoWarden extends Module {
   private final TextSetting signName = new TextSetting("Табличка", "Название таблички на сундуке с припасами").setText("warden");
   private final TextSetting wardenHomeName = new TextSetting("Хом Вардена", "Название clan home к вардену").setText("warden");
   private final TextSetting resourceHomeName = new TextSetting("Хом Ресурсов", "Название clan home к сундуку ресурсов").setText("base");
   private final TextSetting anarchyNumber = new TextSetting("Анархия", "Номер анархии (an1, an2...)").setText("1");
   private final SliderSettings foodAmount = new SliderSettings("Кол-во еды", "Сколько золотых морковок брать с собой").setValue(8.0F).range(4, 16);
   private final SliderSettings maxChestTimer = new SliderSettings("Макс. таймер", "Максимальный таймер сундука (сек)").setValue(90.0F).range(30, 180);
   private final BooleanSetting autoRestart = new BooleanSetting("Авто рестарт", "Перезапуск после смерти").setValue(true);
   private AutoWarden.State currentState = AutoWarden.State.IDLE;
   private final StopWatch stateTimer = new StopWatch();
   private final StopWatch commandCooldown = new StopWatch();
   private final StopWatch lootDelay = new StopWatch();
   private final StopWatch dumpDelay = new StopWatch();
   private final StopWatch scanTimer = new StopWatch();
   private final StopWatch moveTimer = new StopWatch();
   private final StopWatch supplyCooldown = new StopWatch();
   private final StopWatch invisCooldown = new StopWatch();
   private final StopWatch eatCooldown = new StopWatch();
   private boolean pendingInvisUse = false;
   private boolean pendingFoodUse = false;
   private Vec3d preTpPosition = null;
   private static final double TP_DISTANCE_THRESHOLD = 10.0;
   private static final long COMMAND_COOLDOWN_MS = 2200L;
   private static final long TP_WAIT_MS = 5000L;
   private static final long TP_VERIFY_TIMEOUT_MS = 10000L;
   private static final long DARENA_WAIT_MS = 3000L;
   private static final long LOOT_DELAY_MS = 80L;
   private static final long DEAD_RESPAWN_MS = 3000L;
   private static final long HUB_WAIT_MS = 5000L;
   private static final long REJOIN_WAIT_MS = 7000L;
   private static final long MOVE_COOLDOWN_MS = 12000L;
   private static final int MIN_DARENA_TIMER_SEC = 15;
   private static final String TEMP_HOME_NAME = "wdtmp";
   private BlockPos targetChestPos = null;
   private int targetChestRemainingSeconds = 0;
   private long targetChestFoundTime = 0L;
   private boolean baritoneWalking = false;
   private boolean darenaItemFound = false;
   private boolean suppliesCollected = false;
   private int lootedSlots = 0;
   private int totalItemsLooted = 0;
   private int cyclesCompleted = 0;
   private int supplyTakeCount = 0;
   private BlockPos patrolOrigin = null;
   private int patrolIndex = 0;
   private static final int PATROL_RADIUS = 25;
   private static final int[][] PATROL_OFFSETS = new int[][]{{25, 0}, {25, 25}, {0, 25}, {-25, 25}, {-25, 0}, {-25, -25}, {0, -25}, {25, -25}};

   public AutoWarden() {
      super("AutoWarden", "AutoWarden", ModuleCategory.MISC);
      this.setup(
         new Setting[]{this.signName, this.wardenHomeName, this.resourceHomeName, this.anarchyNumber, this.foodAmount, this.maxChestTimer, this.autoRestart}
      );
   }

   @Override
   public void activate() {
      super.activate();
      this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
      this.resetAll();
      ChatMessage.brandmessage("§a[AutoWarden] Запущен. Иду к сундуку припасов...");
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.stopBaritone();
      this.currentState = AutoWarden.State.IDLE;
      ChatMessage.brandmessage("§c[AutoWarden] Остановлен.");
   }

   private void resetAll() {
      this.stateTimer.reset();
      this.commandCooldown.reset();
      this.lootDelay.reset();
      this.dumpDelay.reset();
      this.scanTimer.reset();
      this.moveTimer.reset();
      this.supplyCooldown.reset();
      this.invisCooldown.reset();
      this.eatCooldown.reset();
      this.pendingInvisUse = false;
      this.pendingFoodUse = false;
      this.targetChestPos = null;
      this.targetChestRemainingSeconds = 0;
      this.targetChestFoundTime = 0L;
      this.preTpPosition = null;
      this.baritoneWalking = false;
      this.darenaItemFound = false;
      this.suppliesCollected = false;
      this.lootedSlots = 0;
      this.totalItemsLooted = 0;
      this.cyclesCompleted = 0;
      this.supplyTakeCount = 0;
      this.patrolOrigin = null;
      this.patrolIndex = 0;
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.getHealth() <= 0.0F) {
            this.handleDeath();
         } else {
            if (mc.currentScreen == null) {
               this.tickInvisAndEat();
            }

            switch (this.currentState) {
               case IDLE:
               default:
                  break;
               case GOING_TO_SUPPLY_CHEST:
                  this.handleGoingToSupplyChest();
                  break;
               case OPENING_SUPPLY_CHEST:
                  this.handleOpeningSupplyChest();
                  break;
               case TAKING_SUPPLIES:
                  this.handleTakingSupplies();
                  break;
               case DRINKING_INVIS:
                  this.handleDrinkingInvis();
                  break;
               case TELEPORTING_TO_WARDEN:
                  this.handleTeleportToWarden();
                  break;
               case WAITING_WARDEN_TP:
                  this.handleWaitingWardenTp();
                  break;
               case SEARCHING_CHESTS:
                  this.handleSearchingChests();
                  break;
               case WALKING_TO_CHEST:
                  this.handleWalkingToChest();
                  break;
               case SETTING_HOME_AT_CHEST:
                  this.handleSettingHomeAtChest();
                  break;
               case TELEPORTING_TO_DARENA:
                  this.handleTeleportToDarena();
                  break;
               case WAITING_DARENA:
                  this.handleWaitingDarena();
                  break;
               case BROWSING_DARENA:
                  this.handleBrowsingDarena();
                  break;
               case WAITING_TIMER:
                  this.handleWaitingTimer();
                  break;
               case TELEPORTING_BACK_TO_CHEST:
                  this.handleTeleportBackToChest();
                  break;
               case WAITING_CHEST_TP:
                  this.handleWaitingChestTp();
                  break;
               case LOOTING_CHEST:
                  this.handleLootingChest();
                  break;
               case TELEPORTING_TO_BASE:
                  this.handleTeleportToBase();
                  break;
               case WAITING_BASE_TP:
                  this.handleWaitingBaseTp();
                  break;
               case DUMPING_LOOT:
                  this.handleDumpingLoot();
                  break;
               case DELETING_TEMP_HOME:
                  this.handleDeletingTempHome();
                  break;
               case FLEEING_WARDEN:
                  this.handleFleeingWarden();
                  break;
               case WAITING_HUB:
                  this.handleWaitingHub();
                  break;
               case REJOINING_ANARCHY:
                  this.handleRejoiningAnarchy();
                  break;
               case WAITING_REJOIN:
                  this.handleWaitingRejoin();
                  break;
               case DEAD_WAITING:
                  this.handleDeadWaiting();
                  break;
               case WAITING_AT_CHEST:
                  this.handleWaitingAtChest();
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getType() == PacketEvent.Type.RECEIVE) {
         if (e.getPacket() instanceof GameMessageS2CPacket gameMessage) {
            String msg = gameMessage.content().getString().toLowerCase();
            if (msg.contains("варден")
               && (msg.contains("агр") || msg.contains("обнаруж") || msg.contains("напал"))
               && this.currentState != AutoWarden.State.FLEEING_WARDEN
               && this.currentState != AutoWarden.State.WAITING_HUB
               && this.currentState != AutoWarden.State.REJOINING_ANARCHY
               && this.currentState != AutoWarden.State.IDLE) {
               ChatMessage.brandmessage("§c[AutoWarden] Варден заагрился! Бегу в хаб...");
               this.stopBaritone();
               this.currentState = AutoWarden.State.FLEEING_WARDEN;
               this.stateTimer.reset();
            }

            if (msg.contains("[baritone]") && (msg.contains("goal reached") || msg.contains("done"))) {
               this.baritoneWalking = false;
            }
         }
      }
   }

   private void handleDeath() {
      if (this.currentState != AutoWarden.State.DEAD_WAITING) {
         ChatMessage.brandmessage("§c[AutoWarden] Умер! Жду респавн...");
         this.stopBaritone();
         this.currentState = AutoWarden.State.DEAD_WAITING;
         this.stateTimer.reset();
      }
   }

   private void handleDeadWaiting() {
      if (mc.player.getHealth() > 0.0F && this.stateTimer.finished(3000.0)) {
         if (this.autoRestart.isValue()) {
            ChatMessage.brandmessage("§a[AutoWarden] Респавн. Иду к припасам...");
            this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
            this.stateTimer.reset();
         } else {
            this.setState(false);
         }
      }
   }

   private void handleGoingToSupplyChest() {
      BlockPos supplyChest = this.findSupplyChest();
      if (supplyChest != null) {
         double dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(supplyChest));
         if (dist <= 9.0) {
            this.stopBaritone();
            this.baritoneWalking = false;
            this.currentState = AutoWarden.State.OPENING_SUPPLY_CHEST;
            this.stateTimer.reset();
         } else if (!this.baritoneWalking) {
            this.baritoneGoto(supplyChest);
            this.baritoneWalking = true;
         }
      } else if (this.supplyCooldown.every(5000.0)) {
         this.sendCommand("/clan home " + this.resourceHomeName.getText());
         this.stateTimer.reset();
      }
   }

   private void handleOpeningSupplyChest() {
      BlockPos supplyChest = this.findSupplyChest();
      if (supplyChest == null) {
         this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
      } else if (mc.currentScreen instanceof GenericContainerScreen) {
         this.currentState = AutoWarden.State.TAKING_SUPPLIES;
         this.suppliesCollected = false;
         this.supplyTakeCount = 0;
         this.stateTimer.reset();
      } else if (!this.hasLineOfSight(supplyChest)) {
         ChatMessage.brandmessage("§c[AutoWarden] Сундук за стеной, ищу снова...");
         this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
      } else {
         if (this.commandCooldown.every(600.0)) {
            this.interactWithBlock(supplyChest);
         }
      }
   }

   private void handleTakingSupplies() {
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         if (this.suppliesCollected) {
            this.currentState = AutoWarden.State.DRINKING_INVIS;
            this.stateTimer.reset();
         } else {
            this.currentState = AutoWarden.State.OPENING_SUPPLY_CHEST;
            this.stateTimer.reset();
         }
      } else if (this.stateTimer.finished(300.0)) {
         int invInvisCount = this.countInvisInInventory();
         int invFoodCount = this.countFoodInInventory();
         boolean needInvis = invInvisCount < 1;
         boolean needFood = invFoodCount < (int)this.foodAmount.getValue();
         if (!needInvis && !needFood) {
            mc.player.closeHandledScreen();
            this.suppliesCollected = true;
            this.currentState = AutoWarden.State.DRINKING_INVIS;
            this.stateTimer.reset();
         } else if (this.lootDelay.finished(120.0)) {
            GenericContainerScreenHandler handler = (GenericContainerScreenHandler)screen.getScreenHandler();

            for (Slot slot : handler.slots) {
               if (slot.inventory != mc.player.getInventory() && slot.hasStack()) {
                  ItemStack stack = slot.getStack();
                  if (needInvis && this.isInvisPotion(stack)) {
                     InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
                     this.lootDelay.reset();
                     this.supplyTakeCount++;
                     return;
                  }

                  if (needFood && stack.getItem() == Items.GOLDEN_CARROT) {
                     InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
                     this.lootDelay.reset();
                     this.supplyTakeCount++;
                     return;
                  }
               }
            }

            mc.player.closeHandledScreen();
            this.suppliesCollected = true;
            this.currentState = AutoWarden.State.DRINKING_INVIS;
            this.stateTimer.reset();
         }
      }
   }

   private void handleDrinkingInvis() {
      if (mc.player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
         this.currentState = AutoWarden.State.TELEPORTING_TO_WARDEN;
         this.stateTimer.reset();
      } else if (this.commandCooldown.finished(600.0)) {
         int slot = this.findInvisPotionHotbar();
         int invSlot;
         if (slot == -1 && (invSlot = this.findInvisPotionInventory()) != -1) {
            InventoryTask.swapSlots(invSlot, 0);
            this.commandCooldown.reset();
         } else {
            if (slot != -1) {
               mc.player.getInventory().selectedSlot = slot;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               this.commandCooldown.reset();
            } else {
               ChatMessage.brandmessage("§c[AutoWarden] Нет зелья невидимости!");
               this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
               this.stateTimer.reset();
            }
         }
      }
   }

   private void handleTeleportToWarden() {
      if (this.commandCooldown.finished(2200.0)) {
         this.preTpPosition = mc.player.getPos();
         this.sendCommand("/clan home " + this.wardenHomeName.getText());
         this.currentState = AutoWarden.State.WAITING_WARDEN_TP;
         this.stateTimer.reset();
      }
   }

   private void handleWaitingWardenTp() {
      if (this.preTpPosition != null && mc.player != null && mc.player.getPos().squaredDistanceTo(this.preTpPosition) >= 100.0) {
         this.preTpPosition = null;
         this.currentState = AutoWarden.State.SEARCHING_CHESTS;
         this.stateTimer.reset();
         ChatMessage.brandmessage("§a[AutoWarden] Прибыл к вардену. Ищу сундуки...");
      } else {
         if (this.stateTimer.finished(10000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] ТП не сработал, повторяю...");
            this.currentState = AutoWarden.State.TELEPORTING_TO_WARDEN;
            this.stateTimer.reset();
         }
      }
   }

   private void handleSearchingChests() {
      WardenHelper wardenHelper = this.getWardenHelper();
      if (wardenHelper != null && wardenHelper.isState()) {
         if (this.patrolOrigin == null) {
            this.patrolOrigin = mc.player.getBlockPos();
            this.patrolIndex = 0;
         }

         Optional<WardenHelper.ChestSnapshot> best;
         if ((best = wardenHelper.getClosestChestUnder((int)this.maxChestTimer.getValue())).isPresent()) {
            WardenHelper.ChestSnapshot snapshot = best.get();
            this.targetChestPos = snapshot.pos();
            this.targetChestRemainingSeconds = snapshot.remainingSeconds();
            this.targetChestFoundTime = System.currentTimeMillis();
            this.stopBaritone();
            this.baritoneWalking = false;
            ChatMessage.brandmessage("§a[AutoWarden] Найден сундук! Таймер: " + this.formatTime(this.targetChestRemainingSeconds) + " | Иду к нему...");
            this.currentState = AutoWarden.State.WALKING_TO_CHEST;
            this.stateTimer.reset();
         } else {
            if (!this.baritoneWalking || this.stateTimer.finished(12000.0)) {
               this.stopBaritone();
               this.baritoneWalking = false;
               int[] offset = PATROL_OFFSETS[this.patrolIndex % PATROL_OFFSETS.length];
               int rx = offset[0] + (int)(Math.random() * 6.0 - 3.0);
               int rz = offset[1] + (int)(Math.random() * 6.0 - 3.0);
               BlockPos patrolTarget = this.patrolOrigin.add(rx, 0, rz);
               this.patrolIndex++;
               this.baritoneGoto(patrolTarget);
               this.baritoneWalking = true;
               this.stateTimer.reset();
            }
         }
      } else {
         if (this.stateTimer.finished(3000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] WardenHelper не активен!");
            this.stateTimer.reset();
         }
      }
   }

   private void handleWalkingToChest() {
      if (this.targetChestPos == null) {
         this.currentState = AutoWarden.State.SEARCHING_CHESTS;
      } else {
         WardenHelper wardenHelper = this.getWardenHelper();
         Optional<WardenHelper.ChestSnapshot> better;
         if (wardenHelper != null && wardenHelper.isState() && (better = wardenHelper.getClosestChestUnder((int)this.maxChestTimer.getValue())).isPresent()) {
            WardenHelper.ChestSnapshot snap = better.get();
            if (!snap.pos().equals(this.targetChestPos)) {
               double currentDist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.targetChestPos));
               double newDist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(snap.pos()));
               boolean betterTimer = snap.remainingSeconds() < this.targetChestRemainingSeconds;
               boolean muchCloser = newDist < currentDist * 0.4;
               if (betterTimer || muchCloser) {
                  this.targetChestPos = snap.pos();
                  this.targetChestRemainingSeconds = snap.remainingSeconds();
                  this.targetChestFoundTime = System.currentTimeMillis();
                  this.stopBaritone();
                  this.baritoneWalking = false;
                  ChatMessage.brandmessage("§e[AutoWarden] Нашёл лучший сундук! Таймер: " + this.formatTime(this.targetChestRemainingSeconds));
               }
            } else {
               Optional<WardenHelper.ChestSnapshot> updatedSnap = wardenHelper.getChestSnapshot(this.targetChestPos);
               updatedSnap.ifPresent(s -> {
                  this.targetChestRemainingSeconds = s.remainingSeconds();
                  this.targetChestFoundTime = System.currentTimeMillis();
               });
            }
         }

         if (mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.targetChestPos)) <= 6.25) {
            this.stopBaritone();
            this.baritoneWalking = false;
            this.currentState = AutoWarden.State.SETTING_HOME_AT_CHEST;
            this.stateTimer.reset();
         } else if (!this.baritoneWalking) {
            this.baritoneGoto(this.targetChestPos);
            this.baritoneWalking = true;
         }

         if (this.stateTimer.finished(35000.0)) {
            this.stopBaritone();
            this.baritoneWalking = false;
            this.currentState = AutoWarden.State.SEARCHING_CHESTS;
            this.stateTimer.reset();
            this.targetChestPos = null;
            ChatMessage.brandmessage("§c[AutoWarden] Не смог дойти до сундука. Ищу дальше...");
         }
      }
   }

   private void handleSettingHomeAtChest() {
      int remaining = this.getCurrentChestRemaining();
      if (remaining < 15 && remaining > 0) {
         ChatMessage.brandmessage("§e[AutoWarden] Таймер " + this.formatTime(remaining) + " — жду у сундука...");
         this.stopBaritone();
         this.baritoneWalking = false;
         this.currentState = AutoWarden.State.WAITING_AT_CHEST;
         this.stateTimer.reset();
      } else if (remaining <= 0) {
         this.currentState = AutoWarden.State.LOOTING_CHEST;
         this.lootedSlots = 0;
         this.stateTimer.reset();
      } else if (this.commandCooldown.finished(2200.0)) {
         this.sendCommand("/clan sethome wdtmp");
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.TELEPORTING_TO_DARENA;
         this.stateTimer.reset();
         ChatMessage.brandmessage("§a[AutoWarden] Хом установлен. Лечу на дарену...");
      }
   }

   private void handleWaitingAtChest() {
      if (this.targetChestPos == null) {
         this.currentState = AutoWarden.State.SEARCHING_CHESTS;
      } else {
         int remaining = this.getCurrentChestRemaining();
         if (remaining <= 0) {
            this.currentState = AutoWarden.State.LOOTING_CHEST;
            this.lootedSlots = 0;
            this.stateTimer.reset();
            ChatMessage.brandmessage("§a[AutoWarden] Таймер истёк! Лутаю сундук...");
         } else {
            double dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.targetChestPos));
            if (dist > 6.25 && !this.baritoneWalking) {
               this.baritoneGoto(this.targetChestPos);
               this.baritoneWalking = true;
            } else if (dist <= 6.25) {
               this.stopBaritone();
               this.baritoneWalking = false;
            }
         }
      }
   }

   private void handleTeleportToDarena() {
      if (this.commandCooldown.finished(2200.0)) {
         this.sendCommand("/darena");
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.WAITING_DARENA;
         this.stateTimer.reset();
      }
   }

   private void handleWaitingDarena() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.currentState = AutoWarden.State.BROWSING_DARENA;
         this.darenaItemFound = false;
         this.stateTimer.reset();
      } else {
         if (this.stateTimer.finished(8000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] /darena не открылся. Повторяю...");
            this.currentState = AutoWarden.State.TELEPORTING_TO_DARENA;
            this.stateTimer.reset();
         }
      }
   }

   private void handleBrowsingDarena() {
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         if (this.darenaItemFound) {
            this.currentState = AutoWarden.State.WAITING_TIMER;
            this.stateTimer.reset();
            ChatMessage.brandmessage("§a[AutoWarden] На смотровой. Жду таймер сундука...");
         } else if (this.stateTimer.finished(3000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] Иглобрюх не найден. Повторяю /darena...");
            this.currentState = AutoWarden.State.TELEPORTING_TO_DARENA;
            this.stateTimer.reset();
         }
      } else if (this.stateTimer.finished(400.0)) {
         GenericContainerScreenHandler handler = (GenericContainerScreenHandler)screen.getScreenHandler();

         for (Slot slot : handler.slots) {
            ItemStack stack;
            if (slot.inventory != mc.player.getInventory()
               && slot.hasStack()
               && (stack = slot.getStack()).getItem() == Items.PUFFERFISH
               && stack.getName().getString().toLowerCase().contains("смотр")) {
               InventoryTask.clickSlot(slot, 0, SlotActionType.PICKUP, true);
               this.darenaItemFound = true;
               this.stateTimer.reset();
               ChatMessage.brandmessage("§a[AutoWarden] Нажал на иглобрюх! Телепортируюсь на смотровую...");
               return;
            }
         }

         if (this.stateTimer.finished(5000.0)) {
            mc.player.closeHandledScreen();
            ChatMessage.brandmessage("§c[AutoWarden] Иглобрюх не найден в /darena!");
            this.currentState = AutoWarden.State.TELEPORTING_TO_DARENA;
            this.stateTimer.reset();
         }
      }
   }

   private void handleWaitingTimer() {
      int remaining = this.getCurrentChestRemaining();
      if (remaining <= 8 && remaining > 0) {
         this.currentState = AutoWarden.State.TELEPORTING_BACK_TO_CHEST;
         this.stateTimer.reset();
         ChatMessage.brandmessage("§a[AutoWarden] Таймер ~8с! Телепортируюсь к сундуку...");
      } else {
         if (remaining <= 0 && this.stateTimer.finished(10000.0)) {
            this.currentState = AutoWarden.State.TELEPORTING_BACK_TO_CHEST;
            this.stateTimer.reset();
         }
      }
   }

   private void handleTeleportBackToChest() {
      if (this.commandCooldown.finished(2200.0)) {
         this.preTpPosition = mc.player.getPos();
         this.sendCommand("/clan home wdtmp");
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.WAITING_CHEST_TP;
         this.stateTimer.reset();
      }
   }

   private void handleWaitingChestTp() {
      if (this.preTpPosition != null && mc.player != null && mc.player.getPos().squaredDistanceTo(this.preTpPosition) >= 100.0) {
         this.preTpPosition = null;
         if (this.stateTimer.finished(800.0)) {
            this.currentState = AutoWarden.State.LOOTING_CHEST;
            this.stateTimer.reset();
            this.lootedSlots = 0;
         }
      } else {
         if (this.stateTimer.finished(10000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] ТП к сундуку не сработал!");
            this.currentState = AutoWarden.State.TELEPORTING_BACK_TO_CHEST;
            this.stateTimer.reset();
         }
      }
   }

   private void handleLootingChest() {
      if (this.targetChestPos == null) {
         this.currentState = AutoWarden.State.SEARCHING_CHESTS;
         this.stateTimer.reset();
      } else {
         if (mc.currentScreen instanceof GenericContainerScreen screen) {
            if (!this.lootDelay.finished(80.0)) {
               return;
            }

            GenericContainerScreenHandler handler = (GenericContainerScreenHandler)screen.getScreenHandler();

            for (Slot slot : handler.slots) {
               if (slot.inventory != mc.player.getInventory() && slot.hasStack()) {
                  InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
                  this.lootedSlots++;
                  this.totalItemsLooted++;
                  this.lootDelay.reset();
                  return;
               }
            }

            mc.player.closeHandledScreen();
            if (this.lootedSlots == 0) {
               ChatMessage.brandmessage("§e[AutoWarden] Сундук пуст. Продолжаю патрулирование...");
               this.targetChestPos = null;
               this.currentState = AutoWarden.State.SEARCHING_CHESTS;
               this.stateTimer.reset();
            } else {
               ChatMessage.brandmessage("§a[AutoWarden] Сундук залутан! (" + this.lootedSlots + " предметов)");
               this.currentState = AutoWarden.State.TELEPORTING_TO_BASE;
               this.stateTimer.reset();
            }
         } else {
            if (!this.hasLineOfSight(this.targetChestPos)) {
               double dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.targetChestPos));
               if (dist > 9.0 && !this.baritoneWalking) {
                  this.baritoneGoto(this.targetChestPos);
                  this.baritoneWalking = true;
               }

               return;
            }

            if (this.stateTimer.finished(500.0)) {
               this.interactWithBlock(this.targetChestPos);
               this.stateTimer.reset();
            }
         }
      }
   }

   private void handleTeleportToBase() {
      if (this.commandCooldown.finished(2200.0)) {
         this.preTpPosition = mc.player.getPos();
         this.sendCommand("/clan home " + this.resourceHomeName.getText());
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.WAITING_BASE_TP;
         this.stateTimer.reset();
      }
   }

   private void handleWaitingBaseTp() {
      if (this.preTpPosition != null && mc.player != null && mc.player.getPos().squaredDistanceTo(this.preTpPosition) >= 100.0) {
         this.preTpPosition = null;
         this.currentState = AutoWarden.State.DUMPING_LOOT;
         this.stateTimer.reset();
      } else {
         if (this.stateTimer.finished(10000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] ТП на базу не сработал, повторяю...");
            this.currentState = AutoWarden.State.TELEPORTING_TO_BASE;
            this.stateTimer.reset();
         }
      }
   }

   private void handleDumpingLoot() {
      BlockPos dumpChest = this.findDumpChest();
      if (dumpChest == null) {
         if (this.stateTimer.finished(5000.0)) {
            ChatMessage.brandmessage("§c[AutoWarden] Не найден сундук для сброса лута!");
            this.currentState = AutoWarden.State.DELETING_TEMP_HOME;
            this.stateTimer.reset();
         }
      } else {
         double dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(dumpChest));
         if (dist > 9.0) {
            if (!this.baritoneWalking) {
               this.baritoneGoto(dumpChest);
               this.baritoneWalking = true;
            }
         } else {
            this.stopBaritone();
            this.baritoneWalking = false;
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
               if (!this.dumpDelay.finished(100.0)) {
                  return;
               }

               GenericContainerScreenHandler handler = (GenericContainerScreenHandler)screen.getScreenHandler();
               boolean dumpedSomething = false;

               for (Slot slot : handler.slots) {
                  ItemStack stack;
                  if (slot.inventory == mc.player.getInventory()
                     && slot.hasStack()
                     && (stack = slot.getStack()).getItem() != Items.GOLDEN_CARROT
                     && !this.isInvisPotion(stack)) {
                     InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
                     this.dumpDelay.reset();
                     dumpedSomething = true;
                     break;
                  }
               }

               if (!dumpedSomething) {
                  mc.player.closeHandledScreen();
                  this.cyclesCompleted++;
                  this.currentState = AutoWarden.State.DELETING_TEMP_HOME;
                  this.stateTimer.reset();
                  ChatMessage.brandmessage("§a[AutoWarden] Лут сброшен! Цикл " + this.cyclesCompleted + " завершён.");
               }
            } else {
               if (!this.hasLineOfSight(dumpChest)) {
                  this.baritoneGoto(dumpChest);
                  this.baritoneWalking = true;
                  return;
               }

               if (this.commandCooldown.every(700.0)) {
                  this.interactWithBlock(dumpChest);
               }
            }
         }
      }
   }

   private void handleDeletingTempHome() {
      if (this.commandCooldown.finished(2200.0)) {
         this.sendCommand("/clan delhome wdtmp");
         this.commandCooldown.reset();
         this.targetChestPos = null;
         this.patrolOrigin = null;
         this.patrolIndex = 0;
         this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
         this.stateTimer.reset();
         ChatMessage.brandmessage("§a[AutoWarden] Цикл завершён. Начинаю заново...");
      }
   }

   private void handleFleeingWarden() {
      if (this.commandCooldown.finished(2200.0)) {
         this.sendCommand("/hub");
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.WAITING_HUB;
         this.stateTimer.reset();
      }
   }

   private void handleWaitingHub() {
      if (this.stateTimer.finished(5000.0)) {
         this.currentState = AutoWarden.State.REJOINING_ANARCHY;
         this.stateTimer.reset();
      }
   }

   private void handleRejoiningAnarchy() {
      if (this.commandCooldown.finished(2200.0)) {
         this.sendCommand("/an" + this.anarchyNumber.getText());
         this.commandCooldown.reset();
         this.currentState = AutoWarden.State.WAITING_REJOIN;
         this.stateTimer.reset();
         ChatMessage.brandmessage("§a[AutoWarden] Перезахожу на анархию " + this.anarchyNumber.getText());
      }
   }

   private void handleWaitingRejoin() {
      if (this.stateTimer.finished(7000.0)) {
         this.currentState = AutoWarden.State.GOING_TO_SUPPLY_CHEST;
         this.stateTimer.reset();
      }
   }

   private WardenHelper getWardenHelper() {
      return Elixir.getInstance().getModuleProvider().get(WardenHelper.class);
   }

   private int getCurrentChestRemaining() {
      if (this.targetChestPos == null) {
         return 0;
      } else {
         long elapsed = (System.currentTimeMillis() - this.targetChestFoundTime) / 1000L;
         int remaining = (int)(this.targetChestRemainingSeconds - elapsed);
         WardenHelper helper = this.getWardenHelper();
         Optional<WardenHelper.ChestSnapshot> snap;
         if (helper != null && helper.isState() && (snap = helper.getChestSnapshot(this.targetChestPos)).isPresent()) {
            remaining = snap.get().remainingSeconds();
         }

         return Math.max(0, remaining);
      }
   }

   private void tickInvisAndEat() {
      if (mc.player != null) {
         boolean needsInvis = this.needsInvisPotion();
         if (this.pendingInvisUse) {
            this.pendingInvisUse = false;
            int slot = this.findInvisPotionHotbar();
            if (slot != -1) {
               mc.player.getInventory().selectedSlot = slot;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               this.invisCooldown.reset();
            }
         } else if (needsInvis && this.invisCooldown.finished(1500.0)) {
            int slot = this.findInvisPotionHotbar();
            if (slot != -1) {
               mc.player.getInventory().selectedSlot = slot;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               this.invisCooldown.reset();
            } else {
               int invSlot = this.findInvisPotionInventory();
               if (invSlot != -1) {
                  InventoryTask.swapSlots(invSlot, 0);
                  this.pendingInvisUse = true;
               }
            }
         } else if (this.pendingFoodUse) {
            this.pendingFoodUse = false;
            int slot = this.findFoodHotbar();
            if (slot != -1) {
               mc.player.getInventory().selectedSlot = slot;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               this.eatCooldown.reset();
            }
         } else {
            int foodLevel = mc.player.getHungerManager().getFoodLevel();
            if (foodLevel <= 17 && this.eatCooldown.finished(2000.0)) {
               int slot = this.findFoodHotbar();
               if (slot != -1) {
                  mc.player.getInventory().selectedSlot = slot;
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  this.eatCooldown.reset();
               } else {
                  int invSlot = this.findFoodInventory();
                  if (invSlot != -1) {
                     InventoryTask.swapSlots(invSlot, 0);
                     this.pendingFoodUse = true;
                  }
               }
            }
         }
      }
   }

   private boolean needsInvisPotion() {
      if (this.currentState != AutoWarden.State.IDLE
         && this.currentState != AutoWarden.State.DEAD_WAITING
         && this.currentState != AutoWarden.State.GOING_TO_SUPPLY_CHEST
         && this.currentState != AutoWarden.State.OPENING_SUPPLY_CHEST
         && this.currentState != AutoWarden.State.TAKING_SUPPLIES
         && this.currentState != AutoWarden.State.DRINKING_INVIS
         && this.currentState != AutoWarden.State.FLEEING_WARDEN
         && this.currentState != AutoWarden.State.WAITING_HUB
         && this.currentState != AutoWarden.State.REJOINING_ANARCHY
         && this.currentState != AutoWarden.State.WAITING_REJOIN) {
         StatusEffectInstance invis = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
         return invis == null || invis.getDuration() < 1200;
      } else {
         return false;
      }
   }

   private boolean hasLineOfSight(BlockPos pos) {
      if (mc.player != null && mc.world != null) {
         Vec3d eyes = mc.player.getEyePos();
         BlockHitResult result = mc.world.raycast(new RaycastContext(eyes, Vec3d.ofCenter(pos), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (result == null) {
            return true;
         } else {
            BlockPos hitPos = result.getBlockPos();
            return hitPos.equals(pos);
         }
      } else {
         return false;
      }
   }

   private BlockPos findSupplyChest() {
      BlockPos playerPos = mc.player.getBlockPos();
      String targetSign = this.signName.getText() != null ? this.signName.getText().toLowerCase() : "warden";
      BlockPos closest = null;
      double closestDist = Double.MAX_VALUE;

      for (int x = -8; x <= 8; x++) {
         for (int y = -4; y <= 4; y++) {
            for (int z = -8; z <= 8; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               double dist;
               if (mc.world.getBlockState(pos).isOf(Blocks.CHEST)
                  && this.hasAdjacentSign(pos, targetSign)
                  && (dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))) < closestDist) {
                  closestDist = dist;
                  closest = pos;
               }
            }
         }
      }

      return closest;
   }

   private boolean hasAdjacentSign(BlockPos chestPos, String text) {
      for (BlockPos signPos : new BlockPos[]{
         chestPos.up(),
         chestPos.north(),
         chestPos.south(),
         chestPos.east(),
         chestPos.west(),
         chestPos.up().north(),
         chestPos.up().south(),
         chestPos.up().east(),
         chestPos.up().west()
      }) {
         BlockEntity be = mc.world.getBlockEntity(signPos);
         SignBlockEntity sign;
         if (be instanceof SignBlockEntity && this.signContainsText(sign = (SignBlockEntity)be, text)) {
            return true;
         }
      }

      return false;
   }

   private boolean signContainsText(SignBlockEntity sign, String text) {
      SignText front = sign.getFrontText();

      for (int i = 0; i < 4; i++) {
         if (front.getMessage(i, false).getString().toLowerCase().contains(text)) {
            return true;
         }
      }

      SignText back = sign.getBackText();

      for (int ix = 0; ix < 4; ix++) {
         if (back.getMessage(ix, false).getString().toLowerCase().contains(text)) {
            return true;
         }
      }

      return false;
   }

   private BlockPos findDumpChest() {
      BlockPos playerPos = mc.player.getBlockPos();
      String targetSign = this.signName.getText() != null ? this.signName.getText().toLowerCase() : "warden";
      BlockPos closest = null;
      double closestDist = Double.MAX_VALUE;

      for (int x = -8; x <= 8; x++) {
         for (int y = -4; y <= 4; y++) {
            for (int z = -8; z <= 8; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               double dist;
               if (mc.world.getBlockState(pos).isOf(Blocks.CHEST)
                  && !this.hasAdjacentSign(pos, targetSign)
                  && (dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))) < closestDist) {
                  closestDist = dist;
                  closest = pos;
               }
            }
         }
      }

      return closest;
   }

   private boolean isInvisPotion(ItemStack stack) {
      if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) {
         return false;
      } else {
         String name = stack.getName().getString().toLowerCase();
         return name.contains("невидим") || name.contains("invisib");
      }
   }

   private int countInvisInInventory() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         if (this.isInvisPotion(mc.player.getInventory().getStack(i))) {
            count++;
         }
      }

      return count;
   }

   private int countFoodInInventory() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.GOLDEN_CARROT) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private int findInvisPotionHotbar() {
      for (int i = 0; i < 9; i++) {
         if (this.isInvisPotion(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private int findInvisPotionInventory() {
      for (int i = 9; i < 36; i++) {
         if (this.isInvisPotion(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private int findFoodHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.GOLDEN_CARROT) {
            return i;
         }
      }

      return -1;
   }

   private int findFoodInventory() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.GOLDEN_CARROT) {
            return i;
         }
      }

      return -1;
   }

   private void interactWithBlock(BlockPos pos) {
      if (mc.interactionManager != null && mc.player != null) {
         Vec3d center = Vec3d.ofCenter(pos).add(0.2, 0.375, 0.2);
         Direction side = Direction.getFacing((float)(center.x - mc.player.getX()), (float)(center.y - mc.player.getY()), (float)(center.z - mc.player.getZ()))
            .getOpposite();
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, pos, false));
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void baritoneGoto(BlockPos pos) {
      if (this.moveTimer.finished(800.0)) {
         this.sendCommand("#goto " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
         this.moveTimer.reset();
      }
   }

   private void stopBaritone() {
      this.sendCommand("#stop");
   }

   private void sendCommand(String command) {
      if (mc.player != null && mc.player.networkHandler != null) {
         if (!Network.isPvp()) {
            mc.player.networkHandler.sendChatMessage(command);
         }
      }
   }

   private String formatTime(int seconds) {
      return String.format("%d:%02d", seconds / 60, seconds % 60);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (mc.player != null && mc.world != null && this.currentState != AutoWarden.State.IDLE) {
         DrawContext context = e.getDrawContext();
         MatrixStack matrix = context.getMatrices();
         FontRenderer font = Fonts.getSize(11);
         FontRenderer fontBold = Fonts.getSize(12);
         float x = 4.0F;
         float y = mc.getWindow().getScaledHeight() / 2.0F - 60.0F;
         float padding = 4.0F;
         float lineHeight = 12.0F;
         String stateName = this.getStateDisplayName();
         String timerText = this.targetChestPos != null ? this.formatTime(this.getCurrentChestRemaining()) : "--:--";
         String itemsText = String.valueOf(this.totalItemsLooted);
         String cyclesText = String.valueOf(this.cyclesCompleted);
         String lootText = String.valueOf(this.lootedSlots);
         String[] lines = new String[]{
            "§fAutoWarden",
            "§7Стадия: §e" + stateName,
            "§7Таймер: §a" + timerText,
            "§7Лут (сейчас): §b" + lootText,
            "§7Лут (всего): §b" + itemsText,
            "§7Циклы: §d" + cyclesText
         };
         float maxWidth = 0.0F;

         for (String line : lines) {
            float w = font.getStringWidth(line);
            if (w > maxWidth) {
               maxWidth = w;
            }
         }

         float boxWidth = maxWidth + padding * 2.0F;
         float boxHeight = lines.length * lineHeight + padding * 2.0F;
         rectangle.render(ShapeProperties.create(matrix, x, y, boxWidth, boxHeight).round(5.0F).color(-1442840576).build());
         float textY = y + padding;

         for (int i = 0; i < lines.length; i++) {
            FontRenderer f = i == 0 ? fontBold : font;
            f.drawString(matrix, lines[i], x + padding, textY, -1);
            textY += lineHeight;
         }
      }
   }

   private String getStateDisplayName() {
      return switch (this.currentState) {
         case IDLE -> "Ожидание";
         case GOING_TO_SUPPLY_CHEST -> "Иду к припасам";
         case OPENING_SUPPLY_CHEST -> "Открываю сундук";
         case TAKING_SUPPLIES -> "Беру припасы";
         case DRINKING_INVIS -> "Пью невидимость";
         case TELEPORTING_TO_WARDEN -> "ТП к вардену";
         case WAITING_WARDEN_TP -> "Жду ТП";
         case SEARCHING_CHESTS -> "Патрулирование";
         case WALKING_TO_CHEST -> "Иду к сундуку";
         case SETTING_HOME_AT_CHEST -> "Ставлю хом";
         case TELEPORTING_TO_DARENA -> "ТП на /darena";
         case WAITING_DARENA -> "Жду /darena";
         case BROWSING_DARENA -> "Ищу иглобрюх";
         case WAITING_TIMER -> "Жду таймер";
         case TELEPORTING_BACK_TO_CHEST -> "ТП к сундуку";
         case WAITING_CHEST_TP -> "Жду ТП";
         case LOOTING_CHEST -> "Лутаю сундук";
         case TELEPORTING_TO_BASE -> "ТП на базу";
         case WAITING_BASE_TP -> "Жду ТП";
         case DUMPING_LOOT -> "Скидываю лут";
         case DELETING_TEMP_HOME -> "Удаляю хом";
         case FLEEING_WARDEN -> "Бегу в хаб!";
         case WAITING_HUB -> "Жду в хабе";
         case REJOINING_ANARCHY -> "Захожу на анархию";
         case WAITING_REJOIN -> "Жду подключения";
         case DEAD_WAITING -> "Умер, жду...";
         case WAITING_AT_CHEST -> "Жду у сундука " + this.formatTime(this.getCurrentChestRemaining());
      };
   }

   private static enum State {
      IDLE,
      GOING_TO_SUPPLY_CHEST,
      OPENING_SUPPLY_CHEST,
      TAKING_SUPPLIES,
      DRINKING_INVIS,
      TELEPORTING_TO_WARDEN,
      WAITING_WARDEN_TP,
      SEARCHING_CHESTS,
      WALKING_TO_CHEST,
      SETTING_HOME_AT_CHEST,
      TELEPORTING_TO_DARENA,
      WAITING_DARENA,
      BROWSING_DARENA,
      WAITING_TIMER,
      TELEPORTING_BACK_TO_CHEST,
      WAITING_CHEST_TP,
      LOOTING_CHEST,
      TELEPORTING_TO_BASE,
      WAITING_BASE_TP,
      DUMPING_LOOT,
      DELETING_TEMP_HOME,
      FLEEING_WARDEN,
      WAITING_HUB,
      REJOINING_ANARCHY,
      WAITING_REJOIN,
      DEAD_WAITING,
      WAITING_AT_CHEST;
   }
}
