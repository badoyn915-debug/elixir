package code.elixir.features.impl.movement;

import code.elixir.events.player.PostTickEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.simulate.PlayerSimulation;
import code.elixir.utils.math.script.Script;
import code.elixir.utils.math.task.TaskPriority;
import code.elixir.utils.math.time.StopWatch;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Spider extends Module {
   private static final String FT_HAY = "Сено";
   private static final String FT_KELP = "Ламинария";
   private static final String FT_LILY_PAD = "Кувшинка";
   private static final String FT_RAILS = "Рельсы";
   private static final String FT_PRESSURE_PLATES = "Нажимные плиты";
   private static final String FT_SEA_PICKLE = "Морской огурец";
   private static final String FT_CANDLES = "Свечи";
   private static final String FT_TURTLE_EGG = "Черепашье яйцо";
   private static final String FT_MUSHROOMS = "Грибы";
   private static final String FT_FLOWERS = "Цветы";
   private static final String FT_SIGNS = "Таблички";
   private Script script = new Script();
   private StopWatch stopWatch = new StopWatch();
   private SelectSetting mode = new SelectSetting("Режим", "Выбирает режим").value("Блоки", "СпукиТайм", "ФанТайм", "SpookyNEW").selected("Блоки");
   private MultiSelectSetting funTimeBlocks = new MultiSelectSetting("Выбор блоков", "Какие предметы использовать в режиме ФанТайм")
      .value("Сено", "Ламинария", "Кувшинка", "Рельсы", "Нажимные плиты", "Морской огурец", "Свечи", "Черепашье яйцо", "Грибы", "Цветы", "Таблички")
      .selected("Сено", "Ламинария", "Кувшинка")
      .visible(() -> this.mode.isSelected("ФанТайм"));
   private SliderSettings delay = new SliderSettings("Задержка", "Задержка использования ведра")
      .range(0.1F, 1.0F)
      .step(0.001F)
      .setValue(0.4F)
      .visible(() -> this.mode.isSelected("WaterBucket"));
   private SliderSettings funTimeDelay = new SliderSettings("Задержка ФанТайм", "Задержка между установкой блоков (мс)")
      .range(50.0F, 500.0F)
      .step(10.0F)
      .setValue(130.0F)
      .visible(() -> this.mode.isSelected("ФанТайм"));
   private SliderSettings funTimePitch = new SliderSettings("Угол ФанТайм", "Угол наклона при установке блоков")
      .range(60.0F, 90.0F)
      .step(1.0F)
      .setValue(75.0F)
      .visible(() -> this.mode.isSelected("ФанТайм"));
   private BooleanSetting holdShift = new BooleanSetting("Зажимать шифт", "Зажимает шифт при лазании")
      .setValue(true)
      .visible(() -> this.mode.isSelected("WaterBucket"));
   private BooleanSetting silentUse = new BooleanSetting("Брать ведро невидимо", "Меняет слот невидимо")
      .setValue(true)
      .visible(() -> this.mode.isSelected("WaterBucket"));
   private BooleanSetting holdSpace = new BooleanSetting("Прыгать в начале", "Прыгает в начале лазания")
      .setValue(false)
      .visible(() -> this.mode.isSelected("WaterBucket"));
   private Timer spookyTimer = new Timer();
   private boolean canUseSpooky = true;
   private long lastWallJumpMs = 0L;
   static final long WALL_JUMP_COOLDOWN_MS = 250L;
   private final Random random = new Random();
   private boolean canUseWaterBucket = true;
   private StopWatch useCooldownWaterBucket = new StopWatch();
   private boolean canUseWaterBucket2 = true;
   private StopWatch useCooldownWaterBucket2 = new StopWatch();
   private int prevSlotSpookyNEW = -1;
   private int useItemSequence = 0;
   private double lastWaterY = 0.0;
   private long lastWaterPlaceTime = 0L;

   public Spider() {
      super("Spider", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.funTimeBlocks, this.delay, this.funTimeDelay, this.funTimePitch, this.holdShift, this.silentUse, this.holdSpace});
   }

   @Override
   public void deactivate() {
      if (mc.options != null) {
         mc.options.sneakKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }

      if (this.mode.isSelected("Ведро лавы")) {
         this.spookyTimer.cancel();
         this.spookyTimer = new Timer();
         this.canUseSpooky = true;
      }

      if (this.mode.isSelected("WaterBucket")) {
         this.canUseWaterBucket = true;
         this.useCooldownWaterBucket.reset();
      }

      if (this.mode.isSelected("СпукиТайм")) {
         this.canUseWaterBucket2 = true;
         this.useCooldownWaterBucket2.reset();
      }

      if (this.mode.isSelected("SpookyNEW")) {
         if (mc.player != null && this.prevSlotSpookyNEW != -1 && this.prevSlotSpookyNEW != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = this.prevSlotSpookyNEW;
         }

         this.prevSlotSpookyNEW = -1;
      }
   }

   @EventHandler
   public void onPostTick(PostTickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.mode.isSelected("ФанТайм")) {
            if (mc.player.horizontalCollision) {
               mc.options.jumpKey.setPressed(true);
               mc.player.setPitch(this.funTimePitch.getValue());
               if (this.stopWatch.finished(this.funTimeDelay.getValue())) {
                  int blockSlot = this.findFunTimeBlockSlot();
                  if (blockSlot == -1) {
                     ChatMessage.brandmessage("Нужен выбранный блок ФанТайм в хотбаре");
                     this.setState(false);
                     return;
                  }

                  mc.player.setOnGround(true);
                  mc.player.jump();
                  this.placeFunTimeBlockAtFeet(blockSlot);
                  mc.player.fallDistance = 0.0F;
                  this.stopWatch.reset();
               }
            } else {
               mc.options.jumpKey.setPressed(false);
            }
         }

         if (this.mode.isSelected("FixedPitch")) {
            if (mc.player.horizontalCollision) {
               mc.options.forwardKey.setPressed(false);
            }

            int waterBucketSlot = InventoryTask.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() == Items.WATER_BUCKET);
            boolean hasWaterBucketInMain = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET;
            if (waterBucketSlot >= 0 && waterBucketSlot <= 8) {
               mc.options.jumpKey.setPressed(true);
               mc.options.sneakKey.setPressed(true);
               mc.player.getInventory().setSelectedSlot(waterBucketSlot);
            } else {
               ChatMessage.brandmessage("Нужно ведро воды в хотбаре");
            }

            BlockPos placeablePos;
            if (hasWaterBucketInMain && (placeablePos = this.getPlaceableWaterBlock()) != null && this.stopWatch.finished(340.0)) {
               mc.player.setPitch(75.0F);
               Vec3d vec = placeablePos.toCenterPos();
               Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
               PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
               mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               this.stopWatch.reset();
               mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
            }
         }

         if (this.mode.isSelected("Ведро лавы")) {
            this.handleLavaBucketMode();
         }

         if (this.mode.isSelected("WaterBucket")) {
            this.handleWaterBucketMode();
         }

         if (this.mode.isSelected("СпукиТайм")) {
            this.handleWaterBucketMode2();
         }

         if (this.mode.isSelected("SpookyNEW")) {
            this.handleSpookyNEWMode();
         }
      }
   }

   private void handleLavaBucketMode() {
      if (mc.player != null && !mc.player.isTouchingWater()) {
         mc.player.setPitch(68.0F);
         if (!mc.player.horizontalCollision) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
         } else {
            int lavaSlot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack != null && stack.getItem() == Items.LAVA_BUCKET) {
                  lavaSlot = i;
                  break;
               }
            }

            if (lavaSlot != -1) {
               long now;
               if (mc.player.isOnGround() && (now = System.currentTimeMillis()) - this.lastWallJumpMs > 250L) {
                  mc.player.jump();
                  this.lastWallJumpMs = now;
               }

               mc.options.jumpKey.setPressed(true);
               if (this.canUseSpooky) {
                  int currentSlot = mc.player.getInventory().selectedSlot;
                  if (lavaSlot != currentSlot) {
                     mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lavaSlot));
                  }

                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  if (lavaSlot != currentSlot) {
                     mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
                  }

                  double yBoost = 0.4 + this.random.nextDouble() * 0.03;
                  mc.player.setVelocity(mc.player.getVelocity().x, yBoost, mc.player.getVelocity().z);
                  this.canUseSpooky = false;
                  int delayMs = 450 + this.random.nextInt(60) - 30;
                  this.spookyTimer.schedule(new TimerTask() {
                     @Override
                     public void run() {
                        Spider.this.canUseSpooky = true;
                     }
                  }, Math.max(150, delayMs));
               }

               mc.options.sneakKey.setPressed(true);
            }
         }
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0 && this.mode.isSelected("Блоки")) {
         boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
         int slotId = InventoryTask.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem);
         BlockPos blockPos = this.findPos();
         if (this.script.isFinished() && (offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
            ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
            Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            Vec3d vec = blockPos.toCenterPos();
            Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
            Turns angle = MathAngle.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.1F)));
            Turns.VecRotation vecRotation = new Turns.VecRotation(angle, angle.toVector());
            TurnsConnection.INSTANCE
               .rotateTo(vecRotation, mc.player, 1, new TurnsConfig(new LinearConstructor(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);
            if (this.canPlace(stack)) {
               int prev = mc.player.inventory.selectedSlot;
               if (!offHand) {
                  mc.player.inventory.selectedSlot = slotId;
               }

               mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
               mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
               if (!offHand) {
                  mc.player.inventory.selectedSlot = prev;
               }
            }
         }
      }
   }

   private boolean canPlace(ItemStack stack) {
      BlockPos blockPos = this.getBlockPos();
      if (blockPos.getY() >= mc.player.getBlockY()) {
         return false;
      } else {
         BlockItem blockItem = (BlockItem)stack.getItem();
         VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
         if (shape.isEmpty()) {
            return false;
         } else {
            Box box = shape.getBoundingBox().offset(blockPos);
            return !box.intersects(mc.player.getBoundingBox()) && box.intersects(PlayerSimulation.simulateLocalPlayer(4).boundingBox);
         }
      }
   }

   private BlockPos findPos() {
      BlockPos blockPos = this.getBlockPos();
      return mc.world.getBlockState(blockPos).isSolid()
         ? BlockPos.ORIGIN
         : Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
            .filter(pos -> mc.world.getBlockState(pos).isSolid())
            .findFirst()
            .orElse(BlockPos.ORIGIN);
   }

   private BlockPos getPlaceableWaterBlock() {
      BlockPos below = BlockPos.ofFloored(mc.player.getPos().add(0.0, -1.3, 0.0));
      if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
         return below;
      } else {
         for (Direction dir : Direction.values()) {
            BlockPos side = below.offset(dir);
            if (mc.world.getBlockState(side).isSolidBlock(mc.world, side)) {
               return side;
            }
         }

         return null;
      }
   }

   private BlockPos getBlockPos() {
      return BlockPos.ofFloored(PlayerSimulation.simulateLocalPlayer(1).pos.add(0.0, -0.001, 0.0));
   }

   private int findFunTimeBlockSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && this.isAllowedFunTimeItem(stack.getItem())) {
            return i;
         }
      }

      return -1;
   }

   private boolean isAllowedFunTimeItem(Item item) {
      return this.funTimeBlocks.isSelected("Сено") && item == Items.WHEAT
         || this.funTimeBlocks.isSelected("Ламинария") && item == Items.KELP
         || this.funTimeBlocks.isSelected("Кувшинка") && item == Items.LILY_PAD
         || this.funTimeBlocks.isSelected("Рельсы") && this.isRail(item)
         || this.funTimeBlocks.isSelected("Нажимные плиты") && this.isPressurePlate(item)
         || this.funTimeBlocks.isSelected("Морской огурец") && item == Items.SEA_PICKLE
         || this.funTimeBlocks.isSelected("Свечи") && this.isCandle(item)
         || this.funTimeBlocks.isSelected("Черепашье яйцо") && item == Items.TURTLE_EGG
         || this.funTimeBlocks.isSelected("Грибы") && this.isMushroom(item)
         || this.funTimeBlocks.isSelected("Цветы") && this.isFlower(item)
         || this.funTimeBlocks.isSelected("Таблички") && this.isSign(item);
   }

   private boolean isRail(Item item) {
      return item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.DETECTOR_RAIL || item == Items.ACTIVATOR_RAIL;
   }

   private boolean isPressurePlate(Item item) {
      return item == Items.STONE_PRESSURE_PLATE
         || item == Items.OAK_PRESSURE_PLATE
         || item == Items.SPRUCE_PRESSURE_PLATE
         || item == Items.BIRCH_PRESSURE_PLATE
         || item == Items.JUNGLE_PRESSURE_PLATE
         || item == Items.ACACIA_PRESSURE_PLATE
         || item == Items.DARK_OAK_PRESSURE_PLATE
         || item == Items.MANGROVE_PRESSURE_PLATE
         || item == Items.CHERRY_PRESSURE_PLATE
         || item == Items.BAMBOO_PRESSURE_PLATE
         || item == Items.CRIMSON_PRESSURE_PLATE
         || item == Items.WARPED_PRESSURE_PLATE
         || item == Items.POLISHED_BLACKSTONE_PRESSURE_PLATE
         || item == Items.LIGHT_WEIGHTED_PRESSURE_PLATE
         || item == Items.HEAVY_WEIGHTED_PRESSURE_PLATE;
   }

   private boolean isCandle(Item item) {
      return item == Items.CANDLE
         || item == Items.WHITE_CANDLE
         || item == Items.ORANGE_CANDLE
         || item == Items.MAGENTA_CANDLE
         || item == Items.LIGHT_BLUE_CANDLE
         || item == Items.YELLOW_CANDLE
         || item == Items.LIME_CANDLE
         || item == Items.PINK_CANDLE
         || item == Items.GRAY_CANDLE
         || item == Items.LIGHT_GRAY_CANDLE
         || item == Items.CYAN_CANDLE
         || item == Items.PURPLE_CANDLE
         || item == Items.BLUE_CANDLE
         || item == Items.BROWN_CANDLE
         || item == Items.GREEN_CANDLE
         || item == Items.RED_CANDLE
         || item == Items.BLACK_CANDLE;
   }

   private boolean isMushroom(Item item) {
      return item == Items.BROWN_MUSHROOM || item == Items.RED_MUSHROOM;
   }

   private boolean isFlower(Item item) {
      return item == Items.DANDELION
         || item == Items.POPPY
         || item == Items.BLUE_ORCHID
         || item == Items.ALLIUM
         || item == Items.AZURE_BLUET
         || item == Items.RED_TULIP
         || item == Items.ORANGE_TULIP
         || item == Items.WHITE_TULIP
         || item == Items.PINK_TULIP
         || item == Items.OXEYE_DAISY
         || item == Items.CORNFLOWER
         || item == Items.LILY_OF_THE_VALLEY
         || item == Items.WITHER_ROSE
         || item == Items.SUNFLOWER
         || item == Items.LILAC
         || item == Items.ROSE_BUSH
         || item == Items.PEONY
         || item == Items.TORCHFLOWER
         || item == Items.PITCHER_PLANT
         || item == Items.SPORE_BLOSSOM
         || item == Items.FLOWERING_AZALEA
         || item == Items.FLOWERING_AZALEA_LEAVES;
   }

   private boolean isSign(Item item) {
      return item == Items.OAK_SIGN
         || item == Items.SPRUCE_SIGN
         || item == Items.BIRCH_SIGN
         || item == Items.JUNGLE_SIGN
         || item == Items.ACACIA_SIGN
         || item == Items.DARK_OAK_SIGN
         || item == Items.MANGROVE_SIGN
         || item == Items.CHERRY_SIGN
         || item == Items.BAMBOO_SIGN
         || item == Items.CRIMSON_SIGN
         || item == Items.WARPED_SIGN;
   }

   private void placeFunTimeBlockAtFeet(int slot) {
      int previousSlot = mc.player.getInventory().selectedSlot;
      float previousPitch = mc.player.getPitch();
      mc.player.getInventory().selectedSlot = slot;
      mc.player.setPitch(this.funTimePitch.getValue());
      BlockHitResult hitResult = mc.world
         .raycast(
            new RaycastContext(
               mc.player.getEyePos(),
               mc.player.getEyePos().add(Vec3d.fromPolar(this.funTimePitch.getValue(), mc.player.getYaw()).normalize().multiply(4.5)),
               ShapeType.OUTLINE,
               FluidHandling.NONE,
               mc.player
            )
         );
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.setPitch(previousPitch);
      mc.player.getInventory().selectedSlot = previousSlot;
   }

   private void handleWaterBucketMode() {
      if (mc.player != null) {
         if (!mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            if (!this.canUseWaterBucket && this.useCooldownWaterBucket.finished(this.getAppliedDelayMs())) {
               this.canUseWaterBucket = true;
            }

            if (this.silentUse.isValue()) {
               this.handleSilentWaterBucket();
            } else {
               this.handleVisibleWaterBucket();
            }

            if (!mc.player.horizontalCollision) {
               if (mc.options.sneakKey.isPressed() && this.holdShift.isValue()) {
                  mc.options.sneakKey.setPressed(false);
               }

               if (mc.options.jumpKey.isPressed() && this.holdSpace.isValue()) {
                  mc.options.jumpKey.setPressed(false);
               }
            }
         } else {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.45, mc.player.getVelocity().z);
         }
      }
   }

   private void handleSilentWaterBucket() {
      int waterSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != null && stack.getItem() == Items.WATER_BUCKET) {
            waterSlot = i;
            break;
         }
      }

      if (waterSlot != -1 && mc.player.horizontalCollision) {
         this.handleWallClimb(waterSlot);
      }
   }

   private void handleVisibleWaterBucket() {
      if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
         this.handleWallClimb(mc.player.getInventory().selectedSlot);
      }
   }

   private void handleWallClimb(int waterSlot) {
      if (this.holdSpace.isValue()) {
         long now;
         if (mc.player.isOnGround() && (now = System.currentTimeMillis()) - this.lastWallJumpMs > 250L) {
            mc.player.jump();
            this.lastWallJumpMs = now;
         }

         mc.options.jumpKey.setPressed(true);
      }

      if (this.canUseWaterBucket) {
         int clientSlot = mc.player.getInventory().selectedSlot;
         mc.player.setPitch(75.0F);
         if (this.silentUse.isValue() && waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(waterSlot));
            mc.player.getInventory().selectedSlot = waterSlot;
         }

         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         mc.player.setVelocity(mc.player.getVelocity().x, 0.43, mc.player.getVelocity().z);
         if (this.silentUse.isValue() && waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(clientSlot));
            mc.player.getInventory().selectedSlot = clientSlot;
         }

         this.canUseWaterBucket = false;
         this.useCooldownWaterBucket.reset();
      }

      if (this.holdShift.isValue()) {
         mc.options.sneakKey.setPressed(true);
      }
   }

   private int getAppliedDelayMs() {
      float base = this.delay.getValue();
      return (int)(base * 1000.0F);
   }

   private void handleWaterBucketMode2() {
      if (mc.player != null) {
         if (!mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            if (!this.canUseWaterBucket2 && this.useCooldownWaterBucket2.finished(this.getAppliedDelayMs2())) {
               this.canUseWaterBucket2 = true;
            }

            this.handleSilentWaterBucket2();
            if (!mc.player.horizontalCollision) {
               if (mc.options.sneakKey.isPressed()) {
                  mc.options.sneakKey.setPressed(false);
               }

               if (mc.options.jumpKey.isPressed()) {
                  mc.options.jumpKey.setPressed(false);
               }
            }
         } else {
            mc.player.velocity.y = 0.46;
         }
      }
   }

   private void handleSilentWaterBucket2() {
      int waterSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != null && stack.getItem() == Items.WATER_BUCKET) {
            waterSlot = i;
            break;
         }
      }

      if (waterSlot != -1 && mc.player.horizontalCollision) {
         this.handleWallClimb2(waterSlot);
      }
   }

   private void handleWallClimb2(int waterSlot) {
      long now;
      if (mc.player.isOnGround() && (now = System.currentTimeMillis()) - this.lastWallJumpMs > 250L) {
         mc.player.jump();
         this.lastWallJumpMs = now;
      }

      mc.options.jumpKey.setPressed(true);
      if (this.canUseWaterBucket2) {
         int clientSlot = mc.player.getInventory().selectedSlot;
         mc.player.setPitch(77.0F);
         if (waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(waterSlot));
         }

         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         mc.player.velocity.y = 0.46;
         if (waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(clientSlot));
         }

         this.canUseWaterBucket2 = false;
         this.useCooldownWaterBucket2.reset();
      }

      mc.options.sneakKey.setPressed(true);
   }

   private int getAppliedDelayMs2() {
      float base = 0.405F;
      return (int)(base * 1000.0F);
   }

   private void handleSpookyNEWMode() {
      if (mc.player != null && mc.world != null) {
         int bucketSlot = this.findWaterBucketSlotSpookyNEW();
         if (bucketSlot != -1) {
            if (mc.player.horizontalCollision) {
               this.prevSlotSpookyNEW = mc.player.getInventory().selectedSlot;
               mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(bucketSlot));
               mc.player.getInventory().selectedSlot = bucketSlot;
               if (mc.interactionManager != null) {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
               if (this.prevSlotSpookyNEW != bucketSlot) {
                  mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.prevSlotSpookyNEW));
                  mc.player.getInventory().selectedSlot = this.prevSlotSpookyNEW;
               }
            }
         }
      }
   }

   private int findWaterBucketSlotSpookyNEW() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
               return i;
            }
         }

         return -1;
      }
   }

   public void setScript(Script script) {
      this.script = script;
   }

   public void setStopWatch(StopWatch stopWatch) {
      this.stopWatch = stopWatch;
   }

   public void setMode(SelectSetting mode) {
      this.mode = mode;
   }

   public void setFunTimeBlocks(MultiSelectSetting funTimeBlocks) {
      this.funTimeBlocks = funTimeBlocks;
   }

   public void setDelay(SliderSettings delay) {
      this.delay = delay;
   }

   public void setFunTimeDelay(SliderSettings funTimeDelay) {
      this.funTimeDelay = funTimeDelay;
   }

   public void setFunTimePitch(SliderSettings funTimePitch) {
      this.funTimePitch = funTimePitch;
   }

   public void setHoldShift(BooleanSetting holdShift) {
      this.holdShift = holdShift;
   }

   public void setSilentUse(BooleanSetting silentUse) {
      this.silentUse = silentUse;
   }

   public void setHoldSpace(BooleanSetting holdSpace) {
      this.holdSpace = holdSpace;
   }

   public void setSpookyTimer(Timer spookyTimer) {
      this.spookyTimer = spookyTimer;
   }

   public void setCanUseSpooky(boolean canUseSpooky) {
      this.canUseSpooky = canUseSpooky;
   }

   public void setLastWallJumpMs(long lastWallJumpMs) {
      this.lastWallJumpMs = lastWallJumpMs;
   }

   public void setCanUseWaterBucket(boolean canUseWaterBucket) {
      this.canUseWaterBucket = canUseWaterBucket;
   }

   public void setUseCooldownWaterBucket(StopWatch useCooldownWaterBucket) {
      this.useCooldownWaterBucket = useCooldownWaterBucket;
   }

   public void setCanUseWaterBucket2(boolean canUseWaterBucket2) {
      this.canUseWaterBucket2 = canUseWaterBucket2;
   }

   public void setUseCooldownWaterBucket2(StopWatch useCooldownWaterBucket2) {
      this.useCooldownWaterBucket2 = useCooldownWaterBucket2;
   }

   public void setPrevSlotSpookyNEW(int prevSlotSpookyNEW) {
      this.prevSlotSpookyNEW = prevSlotSpookyNEW;
   }

   public void setUseItemSequence(int useItemSequence) {
      this.useItemSequence = useItemSequence;
   }

   public void setLastWaterY(double lastWaterY) {
      this.lastWaterY = lastWaterY;
   }

   public void setLastWaterPlaceTime(long lastWaterPlaceTime) {
      this.lastWaterPlaceTime = lastWaterPlaceTime;
   }

   public Script getScript() {
      return this.script;
   }

   public StopWatch getStopWatch() {
      return this.stopWatch;
   }

   public SelectSetting getMode() {
      return this.mode;
   }

   public MultiSelectSetting getFunTimeBlocks() {
      return this.funTimeBlocks;
   }

   public SliderSettings getDelay() {
      return this.delay;
   }

   public SliderSettings getFunTimeDelay() {
      return this.funTimeDelay;
   }

   public SliderSettings getFunTimePitch() {
      return this.funTimePitch;
   }

   public BooleanSetting getHoldShift() {
      return this.holdShift;
   }

   public BooleanSetting getSilentUse() {
      return this.silentUse;
   }

   public BooleanSetting getHoldSpace() {
      return this.holdSpace;
   }

   public Timer getSpookyTimer() {
      return this.spookyTimer;
   }

   public boolean isCanUseSpooky() {
      return this.canUseSpooky;
   }

   public long getLastWallJumpMs() {
      return this.lastWallJumpMs;
   }

   public Random getRandom() {
      return this.random;
   }

   public boolean isCanUseWaterBucket() {
      return this.canUseWaterBucket;
   }

   public StopWatch getUseCooldownWaterBucket() {
      return this.useCooldownWaterBucket;
   }

   public boolean isCanUseWaterBucket2() {
      return this.canUseWaterBucket2;
   }

   public StopWatch getUseCooldownWaterBucket2() {
      return this.useCooldownWaterBucket2;
   }

   public int getPrevSlotSpookyNEW() {
      return this.prevSlotSpookyNEW;
   }

   public int getUseItemSequence() {
      return this.useItemSequence;
   }

   public double getLastWaterY() {
      return this.lastWaterY;
   }

   public long getLastWaterPlaceTime() {
      return this.lastWaterPlaceTime;
   }
}
