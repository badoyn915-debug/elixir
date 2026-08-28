package code.elixir.features.impl.combat;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.script.Script;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoTotem extends Module {
   private SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для экипировки тотема")
      .setValue(4.5F)
      .range(1.0F, 20.0F)
      .step(0.5F);
   private MultiSelectSetting settings = new MultiSelectSetting("Настройки", "Дополнительные условия для экипировки тотема")
      .value("Сохранять талики", "Возвращать предмет", "Не брать если шар", "Не брать если ешь")
      .selected("Сохранять талики", "Возвращать предмет");
   private MultiSelectSetting modes = new MultiSelectSetting("Учитывать", "Дополнительные условия для экипировки тотема")
      .value("Здоровье на элитре", "Падение", "Золотые серца", "Кристалы", "Обсидиан", "Булава", "Динамит")
      .selected("Здоровье на элитре", "Падение", "Золотые серца", "Кристалы", "Обсидиан", "Булава");
   private SliderSettings elytraHealth = new SliderSettings("Здоровье на элитре", "Здоровье для экипировки тотема при полёте на элитре")
      .setValue(8.5F)
      .range(1.0F, 20.0F)
      .visible(() -> this.modes.isSelected("Здоровье на элитре"));
   private SliderSettings crystalDistance = new SliderSettings("Дистанция до кристала", "Максимальная дистанция до кристалла для экипировки тотема")
      .setValue(4.0F)
      .range(1.0F, 6.0F)
      .visible(() -> this.modes.isSelected("Кристалы"));
   private SliderSettings obsidianDistance = new SliderSettings("Дистанция до обсидиана", "Максимальная дистанция до обсидиана для экипировки тотема")
      .setValue(4.0F)
      .range(1.0F, 6.0F)
      .visible(() -> this.modes.isSelected("Обсидиан"));
   private SliderSettings tntDistance = new SliderSettings("Дистанция до динамита", "Максимальная дистанция до динамита для экипировки тотема")
      .setValue(10.0F)
      .range(1.0F, 50.0F)
      .visible(() -> this.modes.isSelected("Динамит"));
   private BooleanSetting drawCounter = new BooleanSetting("Рендерить количество", "Отображать количество тотемов на экране").setValue(false);
   private final Script script = new Script();
   private int oldSlot = -1;
   private ItemStack backItemStack = ItemStack.EMPTY;
   private boolean totemIsUsed = false;
   private long lastTotemUseTime = 0L;
   private boolean returningItem = false;
   private boolean swappingTotem = false;
   private long lastSwapAttempt = 0L;
   private long lastSuccessfulSwap = 0L;

   public AutoTotem() {
      super("AutoTotem", "AutoTotem", ModuleCategory.COMBAT);
      this.setup(
         new Setting[]{
            this.healthThreshold, this.settings, this.modes, this.elytraHealth, this.crystalDistance, this.obsidianDistance, this.tntDistance, this.drawCounter
         }
      );
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         this.script.update();
         float health;
         float effectiveHealth = health = mc.player.getHealth();
         if (this.modes.isSelected("Золотые серца")) {
            effectiveHealth += mc.player.getAbsorptionAmount();
         }

         boolean shouldSwap = effectiveHealth <= this.healthThreshold.getValue()
            || this.totemIsUsed && this.getTotemCount() > 0 && System.currentTimeMillis() - this.lastTotemUseTime >= 500L;
         if (this.modes.isSelected("Здоровье на элитре") && mc.player.isGliding() && health <= this.elytraHealth.getValue()) {
            shouldSwap = true;
         }

         if (this.modes.isSelected("Падение") && mc.player.fallDistance > 10.0F) {
            shouldSwap = true;
         }

         if (this.modes.isSelected("Кристалы") && this.getClosestCrystalDistance() <= this.crystalDistance.getValue()) {
            shouldSwap = this.settings.isSelected("Не брать если шар") && this.isHoldingSkull() ? effectiveHealth <= this.healthThreshold.getValue() : true;
         }

         if (this.modes.isSelected("Обсидиан") && this.getClosestObsidianDistance() <= this.obsidianDistance.getValue()) {
            shouldSwap = this.settings.isSelected("Не брать если шар") && this.isHoldingSkull() ? effectiveHealth <= this.healthThreshold.getValue() : true;
         }

         if (this.modes.isSelected("Динамит") && this.getClosestTntDistance() <= this.tntDistance.getValue()) {
            shouldSwap = true;
         }

         if (this.modes.isSelected("Булава") && this.checkForMaceInEnemyHand()) {
            shouldSwap = true;
         }

         if (this.modes.isSelected("Не брать если ешь") && mc.player.isUsingItem() && mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            shouldSwap = false;
         }

         ItemStack offhandStack = mc.player.getOffHandStack();
         boolean isTotemInOffhand = this.isTotemInOffhand();
         boolean isEnchantedTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING && EnchantmentHelper.hasEnchantments(offhandStack);
         boolean isTotemOnCooldown = isTotemInOffhand && mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack());
         if (isTotemOnCooldown
            && this.backItemStack != ItemStack.EMPTY
            && this.oldSlot != -1
            && this.settings.isSelected("Возвращать предмет")
            && !this.returningItem) {
            Slot returnSlot = InventoryTask.slots().filter(s -> s.id == this.oldSlot).findFirst().orElse(null);
            if (returnSlot != null && !returnSlot.getStack().isEmpty() && returnSlot.getStack().getItem() == this.backItemStack.getItem()) {
               if (InventoryFlowManager.script.isFinished()) {
                  this.returningItem = true;
                  boolean instant = GuiMove.mode.isSelected("Reallyworld") || GuiMove.mode.isSelected("Vanilla");
                  InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, !instant, true);
                  this.backItemStack = ItemStack.EMPTY;
                  this.oldSlot = -1;
                  this.returningItem = false;
                  return;
               }
            } else {
               this.oldSlot = -1;
               this.backItemStack = ItemStack.EMPTY;
            }
         }

         boolean hasNormalTotem = this.isTotemInOffhand() && !isEnchantedTotemInOffhand;
         if (hasNormalTotem && !shouldSwap) {
            this.swappingTotem = false;
         }

         boolean totemWasUsed = this.totemIsUsed && !this.isTotemInOffhand() && System.currentTimeMillis() - this.lastTotemUseTime > 200L;
         boolean canReturnAfterUse = totemWasUsed && mc.player.isOnGround() && mc.player.fallDistance < 6.0F;
         boolean shouldReturn = !shouldSwap && this.isTotemInOffhand() || canReturnAfterUse;
         if (shouldReturn && this.oldSlot != -1 && this.settings.isSelected("Возвращать предмет")) {
            Slot returnSlot = InventoryTask.slots().filter(s -> s.id == this.oldSlot).findFirst().orElse(null);
            if (returnSlot != null && !returnSlot.getStack().isEmpty() && returnSlot.getStack().getItem() == this.backItemStack.getItem()) {
               if (InventoryFlowManager.script.isFinished() && !this.returningItem) {
                  this.returningItem = true;
                  boolean instant = GuiMove.mode.isSelected("Reallyworld") || GuiMove.mode.isSelected("Vanilla");
                  InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, !instant, true);
                  this.backItemStack = ItemStack.EMPTY;
                  this.oldSlot = -1;
                  this.totemIsUsed = false;
                  this.returningItem = false;
                  return;
               }
            } else {
               this.oldSlot = -1;
               this.backItemStack = ItemStack.EMPTY;
            }
         } else if (this.returningItem && !shouldReturn) {
            this.returningItem = false;
         }

         if (shouldSwap && (!hasNormalTotem || isEnchantedTotemInOffhand)) {
            if (hasNormalTotem && !isEnchantedTotemInOffhand) {
               this.swappingTotem = false;
               return;
            }

            if (this.swappingTotem) {
               return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastSwapAttempt < 100L || currentTime - this.lastSuccessfulSwap < 200L) {
               return;
            }

            if (!InventoryFlowManager.script.isFinished()) {
               return;
            }

            Slot totemSlot = this.findTotemSlot();
            if (totemSlot == null) {
               return;
            }

            if (mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack())) {
               if (this.backItemStack != ItemStack.EMPTY && this.oldSlot != -1 && this.settings.isSelected("Возвращать предмет") && !this.returningItem) {
                  Slot returnSlot = InventoryTask.slots().filter(s -> s.id == this.oldSlot).findFirst().orElse(null);
                  if (returnSlot != null && !returnSlot.getStack().isEmpty() && returnSlot.getStack().getItem() == this.backItemStack.getItem()) {
                     if (InventoryFlowManager.script.isFinished()) {
                        this.returningItem = true;
                        boolean instant = GuiMove.mode.isSelected("Reallyworld") || GuiMove.mode.isSelected("Vanilla");
                        InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, !instant, true);
                        this.backItemStack = ItemStack.EMPTY;
                        this.oldSlot = -1;
                        this.returningItem = false;
                        return;
                     }
                  } else {
                     this.oldSlot = -1;
                     this.backItemStack = ItemStack.EMPTY;
                  }
               }

               return;
            }

            if (this.isTotemInOffhand()) {
               ItemStack currentOffhand = mc.player.getOffHandStack();
               ItemStack foundTotem = totemSlot.getStack();
               if (currentOffhand.getItem() == foundTotem.getItem()) {
                  boolean currentEnchanted = EnchantmentHelper.hasEnchantments(currentOffhand);
                  boolean foundEnchanted;
                  if (currentEnchanted == (foundEnchanted = EnchantmentHelper.hasEnchantments(foundTotem))) {
                     return;
                  }

                  if (!currentEnchanted && foundEnchanted) {
                     return;
                  }
               }
            }

            this.lastSwapAttempt = currentTime;
            this.swappingTotem = true;
            if (!offhandStack.isEmpty() && this.oldSlot == -1 && this.settings.isSelected("Возвращать предмет")) {
               this.oldSlot = totemSlot.id;
               this.backItemStack = offhandStack.copy();
            }

            boolean instant = GuiMove.mode.isSelected("Reallyworld") || GuiMove.mode.isSelected("Vanilla");
            InventoryTask.swapHand(totemSlot, Hand.OFF_HAND, !instant, true);
            this.totemIsUsed = false;
            this.script.cleanup().addTickStep(2, () -> {
               if (this.isTotemInOffhand() && !EnchantmentHelper.hasEnchantments(mc.player.getOffHandStack())) {
                  this.lastSuccessfulSwap = System.currentTimeMillis();
               }

               this.swappingTotem = false;
            });
         } else if (this.swappingTotem && hasNormalTotem) {
            this.swappingTotem = false;
         }
      }
   }

   private boolean isHoldingSkull() {
      ItemStack mainHand = mc.player.getMainHandStack();
      ItemStack offHand = mc.player.getOffHandStack();
      return this.isSkull(mainHand) || this.isSkull(offHand);
   }

   private boolean isSkull(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : stack.getItem() == Items.SKELETON_SKULL
            || stack.getItem() == Items.WITHER_SKELETON_SKULL
            || stack.getItem() == Items.ZOMBIE_HEAD
            || stack.getItem() == Items.PLAYER_HEAD
            || stack.getItem() == Items.CREEPER_HEAD
            || stack.getItem() == Items.DRAGON_HEAD
            || stack.getItem() == Items.PIGLIN_HEAD;
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      Packet<?> class_25962 = event.getPacket();
      EntityStatusS2CPacket statusPacket;
      if (class_25962 instanceof EntityStatusS2CPacket
         && (statusPacket = (EntityStatusS2CPacket)class_25962).getStatus() == 35
         && statusPacket.getEntity(mc.world) == mc.player) {
         this.totemIsUsed = true;
         this.lastTotemUseTime = System.currentTimeMillis();
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (this.drawCounter.isValue() && mc.player != null && mc.world != null && !mc.player.isCreative()) {
         DrawContext context = e.getDrawContext();
         MatrixStack matrix = context.getMatrices();
         float x = mc.getWindow().getScaledWidth() / 2.0F + 120.0F;
         float y = mc.getWindow().getScaledHeight() - 17;
         int totemX = (int)(x - 128.0F);
         int totemY = (int)(y - 37.0F);
         ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
         RenderSystem.disableBlend();
         Render2D.defaultDrawStack(context, totemStack, totemX, totemY, false, false, 1.0F);
         RenderSystem.enableBlend();
         String count = String.valueOf(this.getTotemCount());
         int countWidth = mc.textRenderer.getWidth(count);
         matrix.push();
         matrix.translate(0.0F, 0.0F, 300.0F);
         context.drawText(mc.textRenderer, count, (int)(totemX + 16.5F - countWidth), totemY + 9, 16777215, true);
         matrix.pop();
      }
   }

   private boolean checkForMaceInEnemyHand() {
      if (!this.modes.isSelected("Булава")) {
         return false;
      } else {
         Box box = mc.player.getBoundingBox().expand(30.0);

         for (AbstractClientPlayerEntity enemy : mc.world.getEntitiesByClass(AbstractClientPlayerEntity.class, box, e -> true)) {
            if (enemy != mc.player) {
               ItemStack main = enemy.getMainHandStack();
               ItemStack off = enemy.getOffHandStack();
               if (main.getItem() == Items.MACE || off.getItem() == Items.MACE) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private double getClosestCrystalDistance() {
      double minDist = Double.MAX_VALUE;
      Vec3d playerPos = mc.player.getPos();
      Box box = mc.player.getBoundingBox().expand(this.crystalDistance.getValue());

      for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true)) {
         double dist = playerPos.distanceTo(crystal.getPos());
         if (dist < minDist) {
            minDist = dist;
         }
      }

      return minDist;
   }

   private double getClosestObsidianDistance() {
      double minDist = Double.MAX_VALUE;
      BlockPos playerBlockPos = mc.player.getBlockPos();
      int dist = (int)Math.ceil(this.obsidianDistance.getValue());

      for (int x = -dist; x <= dist; x++) {
         for (int y = -dist; y <= dist; y++) {
            for (int z = -dist; z <= dist; z++) {
               BlockPos pos = playerBlockPos.add(x, y, z);
               double d;
               if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && (d = MathHelper.sqrt((float)playerBlockPos.getSquaredDistance(pos))) < minDist) {
                  minDist = d;
               }
            }
         }
      }

      return minDist;
   }

   private double getClosestTntDistance() {
      double minDist = Double.MAX_VALUE;
      Vec3d playerPos = mc.player.getPos();
      Box box = mc.player.getBoundingBox().expand(this.tntDistance.getValue());

      for (TntEntity tnt : mc.world.getEntitiesByClass(TntEntity.class, box, e -> true)) {
         double dist = playerPos.distanceTo(tnt.getPos());
         if (dist < minDist) {
            minDist = dist;
         }
      }

      return minDist;
   }

   private boolean isTotemInOffhand() {
      ItemStack offhandStack = mc.player.getOffHandStack();
      return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
   }

   private Slot findTotemSlot() {
      if (this.settings.isSelected("Сохранять талики")) {
         Slot nonEnchantedSlot = null;
         Slot enchantedSlot = null;

         for (Slot slot : InventoryTask.slots().toList()) {
            ItemStack stack = slot.getStack();
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
               if (!EnchantmentHelper.hasEnchantments(stack)) {
                  nonEnchantedSlot = slot;
               } else {
                  enchantedSlot = slot;
               }
            }
         }

         if (nonEnchantedSlot != null) {
            return nonEnchantedSlot;
         } else {
            return enchantedSlot != null ? enchantedSlot : null;
         }
      } else {
         return InventoryTask.getSlot(Items.TOTEM_OF_UNDYING);
      }
   }

   private int getTotemCount() {
      return (int)mc.player.getInventory().main.stream().filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING).count();
   }

   @Override
   public void deactivate() {
      this.oldSlot = -1;
      this.backItemStack = ItemStack.EMPTY;
      this.totemIsUsed = false;
      this.lastTotemUseTime = 0L;
      this.returningItem = false;
      this.swappingTotem = false;
      this.lastSwapAttempt = 0L;
      this.lastSuccessfulSwap = 0L;
      this.script.cleanup();
      super.deactivate();
   }

   public SliderSettings getHealthThreshold() {
      return this.healthThreshold;
   }

   public MultiSelectSetting getSettings() {
      return this.settings;
   }

   public MultiSelectSetting getModes() {
      return this.modes;
   }

   public SliderSettings getElytraHealth() {
      return this.elytraHealth;
   }

   public SliderSettings getCrystalDistance() {
      return this.crystalDistance;
   }

   public SliderSettings getObsidianDistance() {
      return this.obsidianDistance;
   }

   public SliderSettings getTntDistance() {
      return this.tntDistance;
   }

   public BooleanSetting getDrawCounter() {
      return this.drawCounter;
   }

   public Script getScript() {
      return this.script;
   }

   public int getOldSlot() {
      return this.oldSlot;
   }

   public ItemStack getBackItemStack() {
      return this.backItemStack;
   }

   public boolean isTotemIsUsed() {
      return this.totemIsUsed;
   }

   public long getLastTotemUseTime() {
      return this.lastTotemUseTime;
   }

   public boolean isReturningItem() {
      return this.returningItem;
   }

   public boolean isSwappingTotem() {
      return this.swappingTotem;
   }

   public long getLastSwapAttempt() {
      return this.lastSwapAttempt;
   }

   public long getLastSuccessfulSwap() {
      return this.lastSuccessfulSwap;
   }
}
