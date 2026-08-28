package code.elixir.features.impl.combat;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.events.item.ClickSlotEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.features.impl.combat.autoswapUtil.AutoSwapEngine;
import code.elixir.features.impl.combat.autoswapUtil.AutoSwapWheelScreen;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.script.Script;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class AutoSwap extends Module implements QuickImports {
   private BindSetting bind = new BindSetting("Кнопка свапа", "Использует элемент при нажатии");
   private RadioSetting mode = new RadioSetting("Режим", "Выберите режим работы.", new String[]{"Двойной", "Тройной"}, "Двойной");
   private SelectSetting bypass = new SelectSetting("Обход", "Обход античита при свапе")
      .value("Vanilla", "Reallyworld", "Funtime", "Holyworld", "SpookyTime", "LonyGrief")
      .selected("SpookyTime");
   private RadioSetting firstItem = new RadioSetting(
         "Основной предмет", "Выберите первый предмет для обмена.", new String[]{"Тотем", "Сфера", "Гепл", "Щит"}, "Тотем"
      )
      .visible(() -> "Двойной".equals(this.mode.get()));
   private RadioSetting secondItem = new RadioSetting(
         "Вторичный предмет", "Выберите второй предмет для обмена.", new String[]{"Тотем", "Сфера", "Гепл", "Щит"}, "Тотем"
      )
      .visible(() -> "Двойной".equals(this.mode.get()));
   private BooleanSetting autoDamage = new BooleanSetting("Авто урон", "Автоматически свапает на предмет с макс уроном");
   private MultiSelectSetting autoDamageConditions = new MultiSelectSetting("Условия", "Условия для авто урона")
      .value("Таргет с малым HP", "У таргета нет меча")
      .selected("Таргет с малым HP")
      .visible(() -> this.autoDamage.isValue());
   private SliderSettings lowHpThreshold = new SliderSettings("Порог HP", "Порог низкого HP таргета")
      .setValue(10.0F)
      .range(1.0F, 20.0F)
      .step(0.5F)
      .visible(() -> this.autoDamage.isValue() && this.autoDamageConditions.isSelected("Таргет с малым HP"));
   private ItemStack savedOffhandItem = ItemStack.EMPTY;
   private boolean autoDamageActive = false;
   private Text lastSwapDisplayName = null;
   private long lastSwapTimeMs = 0L;
   private Animation swapAnim = new Decelerate().setMs(250).setValue(1.0);
   private final Script script = new Script();
   static final long SWAP_TOAST_DURATION_MS = 1800L;
   static final long SWAP_FADE_MS = 250L;
   private AutoSwap.WheelSlotItem[] wheelSlots = new AutoSwap.WheelSlotItem[3];
   private Integer selectingSlotIndex = null;
   private boolean awaitingInventoryPick = false;

   public AutoSwap() {
      super("AutoSwap", "AutoSwap", ModuleCategory.COMBAT);
      this.setup(
         new Setting[]{this.mode, this.bypass, this.firstItem, this.secondItem, this.bind, this.autoDamage, this.autoDamageConditions, this.lowHpThreshold}
      );
   }

   @Override
   public void deactivate() {
      AutoSwapEngine.get().reset();
      super.deactivate();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.script.update();
      AutoSwapEngine.get().onTick(e);
      if (this.awaitingInventoryPick && this.selectingSlotIndex != null && mc != null && !(mc.currentScreen instanceof InventoryScreen)) {
         this.awaitingInventoryPick = false;
         this.selectingSlotIndex = null;
         if (!(mc.currentScreen instanceof AutoSwapWheelScreen)) {
            mc.setScreen(new AutoSwapWheelScreen(this));
         }
      }

      if (this.autoDamage.isValue()) {
         this.handleAutoDamage();
      }
   }

   @EventHandler
   public void onInput(InputEvent e) {
      AutoSwapEngine.get().onInput(e);
   }

   private void swapOffhand(Slot slot, Runnable onComplete) {
      if (slot != null) {
         AutoSwapEngine.get().requestSwap(slot, this.bypass.getSelected());
         if (onComplete != null) {
            this.script.cleanup().addTickStep(2, onComplete::run);
         }
      }
   }

   private void handleAutoDamage() {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = AttackAura.getInstance() != null ? AttackAura.getInstance().getTarget() : null;
         boolean shouldSwapToDamage = false;
         if (target != null && target.isAlive()) {
            boolean lowHpCondition = false;
            boolean noSwordCondition = false;
            if (this.autoDamageConditions.isSelected("Таргет с малым HP")) {
               boolean hpBelowThreshold = target.getHealth() <= this.lowHpThreshold.getValue();
               boolean hpLessThanMine = target.getHealth() < mc.player.getHealth();
               lowHpCondition = hpBelowThreshold && hpLessThanMine;
            }

            if (this.autoDamageConditions.isSelected("У таргета нет меча") && target instanceof PlayerEntity player) {
               ItemStack mainHand = player.getMainHandStack();
               noSwordCondition = !(mainHand.getItem() instanceof SwordItem);
            }

            if (this.autoDamageConditions.getSelected().isEmpty()) {
               shouldSwapToDamage = true;
            } else {
               shouldSwapToDamage = lowHpCondition || noSwordCondition;
            }
         }

         if (shouldSwapToDamage && !this.autoDamageActive) {
            this.savedOffhandItem = mc.player.getOffHandStack().copy();
            ItemStack bestDamageItem = this.findBestDamageItem();
            if (!bestDamageItem.isEmpty()) {
               this.swapToItemStack(bestDamageItem);
               this.autoDamageActive = true;
            }
         } else if (!shouldSwapToDamage && this.autoDamageActive) {
            if (!this.savedOffhandItem.isEmpty()) {
               this.swapToItemStack(this.savedOffhandItem);
            }

            this.autoDamageActive = false;
            this.savedOffhandItem = ItemStack.EMPTY;
         }
      }
   }

   private ItemStack findBestDamageItem() {
      if (mc.player == null) {
         return ItemStack.EMPTY;
      } else {
         ArrayList<ItemStack> damageItems = new ArrayList<>();

         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.TOTEM_OF_UNDYING) && this.getAttackDamage(stack) > 0.0) {
               damageItems.add(stack);
            }
         }

         return damageItems.isEmpty() ? ItemStack.EMPTY : damageItems.stream().max(Comparator.comparingDouble(this::getAttackDamage)).orElse(ItemStack.EMPTY);
      }
   }

   private double getAttackDamage(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0;
      } else {
         AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
         if (modifiers == null) {
            return 0.0;
         } else {
            double[] totalDamage = new double[]{0.0};
            modifiers.modifiers().forEach(entry -> {
               if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
                  totalDamage[0] += entry.modifier().value();
               }
            });
            return totalDamage[0];
         }
      }
   }

   private void swapToItemStack(ItemStack targetStack) {
      if (targetStack != null && !targetStack.isEmpty()) {
         Item item = targetStack.getItem();
         String itemName = targetStack.getName().getString();
         Slot foundSlot = null;

         for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isEmpty() && slotStack.getItem() == item && slotStack.getName().getString().equals(itemName)) {
               foundSlot = slot;
               break;
            }
         }

         if (foundSlot != null) {
            Slot slotToSwap = foundSlot;
            this.swapOffhand(slotToSwap, () -> this.triggerSwapToast(slotToSwap.getStack()));
         }
      }
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent e) {
      if (this.selectingSlotIndex != null && mc.currentScreen instanceof InventoryScreen) {
         if (mc.player != null && mc.player.currentScreenHandler != null) {
            Slot slot = null;

            for (Slot s : mc.player.currentScreenHandler.slots) {
               if (s.id == e.getSlotId()) {
                  slot = s;
                  break;
               }
            }

            if (slot != null && slot.hasStack() && slot.inventory instanceof PlayerInventory) {
               ItemStack stack = slot.getStack();
               if (!stack.isEmpty()) {
                  int slotIndex = this.selectingSlotIndex;
                  this.setWheelSlotItem(slotIndex, stack.getItem(), stack.getName().getString());
                  this.selectingSlotIndex = null;
                  this.awaitingInventoryPick = false;
                  e.setCancelled(true);
                  mc.setScreen(new AutoSwapWheelScreen(this));
               }
            }
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.bind.getKey()) && mc.player != null) {
         if (this.mode.get().equals("Тройной")) {
            if (mc.currentScreen == null) {
               mc.setScreen(new AutoSwapWheelScreen(this));
            }
         } else {
            Slot first = InventoryTask.getSlot(
               this.getItemByType(this.firstItem.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45
            );
            Slot second = InventoryTask.getSlot(
               this.getItemByType(this.secondItem.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45
            );
            if (first != null || second != null) {
               Item offhandItem = mc.player.getOffHandStack().getItem();
               Slot validSlot;
               if (first != null && offhandItem != first.getStack().getItem()) {
                  validSlot = first;
               } else if (second != null && offhandItem != second.getStack().getItem()) {
                  validSlot = second;
               } else {
                  validSlot = first != null ? first : second;
               }

               if (validSlot != null) {
                  ItemStack stackToSwap = validSlot.getStack();
                  this.swapOffhand(validSlot, () -> this.triggerSwapToast(stackToSwap));
               }
            }
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (this.lastSwapTimeMs != 0L) {
         if (mc != null && mc.getWindow() != null && mc.textRenderer != null) {
            long elapsed = System.currentTimeMillis() - this.lastSwapTimeMs;
            if (elapsed > 1800L) {
               this.lastSwapTimeMs = 0L;
               this.lastSwapDisplayName = null;
            } else {
               boolean fadingIn = elapsed < 250L;
               boolean fadingOut = elapsed > 1550L;
               float anim = 1.0F;
               if (fadingIn) {
                  anim = Math.min(1.0F, (float)elapsed / 250.0F);
               } else if (fadingOut) {
                  anim = Math.max(0.0F, (float)(1800L - elapsed) / 250.0F);
               }

               if (!(anim <= 0.01F)) {
                  DrawContext context = e.getDrawContext();
                  if (context != null) {
                     MatrixStack matrix = context.getMatrices();
                     String prefixStr = "Свапнул на ";
                     int screenW = mc.getWindow().getScaledWidth();
                     int screenH = mc.getWindow().getScaledHeight();
                     float y = screenH - 80;
                     float prefixW = mc.textRenderer.getWidth("Свапнул на ");
                     float nameW = 0.0F;
                     if (this.lastSwapDisplayName != null) {
                        nameW = mc.textRenderer.getWidth(this.lastSwapDisplayName);
                     }

                     float totalW = prefixW + nameW;
                     float x = (screenW - totalW) / 2.0F;
                     float animFinal = anim;
                     int alpha = (int)(255.0F * animFinal);
                     int prefixColor = ColorAssist.rgba(255, 255, 255, alpha);
                     Text lastSwapDisplayNameFinal = this.lastSwapDisplayName;
                     Calculate.setAlpha(animFinal, () -> Calculate.scale(matrix, x + totalW / 2.0F, y + 6.0F, animFinal, () -> {
                        context.drawText(mc.textRenderer, "Свапнул на ", (int)x, (int)y, prefixColor, false);
                        if (lastSwapDisplayNameFinal != null) {
                           context.drawText(mc.textRenderer, lastSwapDisplayNameFinal, (int)(x + prefixW), (int)y, 16777215 | alpha << 24, false);
                        }
                     }));
                  }
               }
            }
         }
      }
   }

   private void triggerSwapToast(ItemStack swappedTo) {
      try {
         this.lastSwapTimeMs = System.currentTimeMillis();
         this.lastSwapDisplayName = swappedTo.getName();
         this.swapAnim.setDirection(Direction.FORWARDS);
         this.swapAnim.reset();
      } catch (Exception var3) {
      }
   }

   public Item getItemByType(String itemType) {
      return switch (itemType) {
         case "Тотем" -> Items.TOTEM_OF_UNDYING;
         case "Сфера" -> Items.PLAYER_HEAD;
         case "Гепл" -> Items.GOLDEN_APPLE;
         case "Щит" -> Items.SHIELD;
         default -> Items.AIR;
      };
   }

   public void setWheelSlotItem(int index, Item item, String itemName) {
      if (index >= 0 && index < this.wheelSlots.length) {
         this.wheelSlots[index] = new AutoSwap.WheelSlotItem(item, itemName);
      }
   }

   public void clearWheelSlot(int index) {
      if (index >= 0 && index < this.wheelSlots.length) {
         this.wheelSlots[index] = null;
      }
   }

   public boolean isWheelSlotEmpty(int index) {
      if (index >= 0 && index < this.wheelSlots.length) {
         AutoSwap.WheelSlotItem slotItem = this.wheelSlots[index];
         return slotItem == null || slotItem.item == null || slotItem.item == Items.AIR;
      } else {
         return true;
      }
   }

   public void startSelectingItem(int wheelSlotIndex) {
      this.selectingSlotIndex = wheelSlotIndex;
      this.awaitingInventoryPick = true;
      if (mc != null && mc.player != null) {
         mc.setScreen(new InventoryScreen(mc.player));
      }
   }

   public Item getWheelSlotItem(int index) {
      return index >= 0 && index < this.wheelSlots.length && this.wheelSlots[index] != null ? this.wheelSlots[index].item : null;
   }

   public ItemStack getWheelSlotStack(int index) {
      if (index >= 0 && index < this.wheelSlots.length) {
         AutoSwap.WheelSlotItem slotItem = this.wheelSlots[index];
         if (slotItem != null && slotItem.item != null && slotItem.item != Items.AIR) {
            if (mc != null && mc.player != null) {
               PlayerInventory inv = mc.player.getInventory();

               for (int i = 0; i < inv.size(); i++) {
                  ItemStack stack = inv.getStack(i);
                  if (!stack.isEmpty() && stack.getItem() == slotItem.item && stack.getName().getString().equals(slotItem.itemName)) {
                     Logger.info(
                        "[AutoSwap] getWheelSlotStack: найден стек для слота "
                           + index
                           + ", item="
                           + slotItem.item
                           + ", name="
                           + slotItem.itemName
                           + ", в инвентаре слот "
                           + i
                     );
                     return stack;
                  }
               }

               Logger.warn(
                  "[AutoSwap] getWheelSlotStack: предмет "
                     + slotItem.item
                     + " с названием '"
                     + slotItem.itemName
                     + "' не найден в инвентаре для слота "
                     + index
               );
               return ItemStack.EMPTY;
            } else {
               Logger.warn("[AutoSwap] getWheelSlotStack: mc или player null");
               return ItemStack.EMPTY;
            }
         } else {
            Logger.info("[AutoSwap] getWheelSlotStack: слот " + index + " пуст");
            return ItemStack.EMPTY;
         }
      } else {
         Logger.warn("[AutoSwap] getWheelSlotStack: невалидный индекс " + index);
         return ItemStack.EMPTY;
      }
   }

   public void startSwapToItem(Item item) {
      Logger.info("[AutoSwap] startSwapToItem вызван: item=" + (item != null ? item.toString() : "null"));
      if (mc != null && mc.player != null && item != null && item != Items.AIR) {
         Logger.info("[AutoSwap] Ищем слот для предмета: " + item);
         Slot slot = InventoryTask.getSlot(item, Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
         if (slot != null) {
            ItemStack stackToSwap = slot.getStack();
            this.swapOffhand(slot, () -> this.triggerSwapToast(stackToSwap));
         } else {
            Logger.warn("[AutoSwap] Слот не найден для предмета: " + item);
         }
      } else {
         Logger.warn("[AutoSwap] startSwapToItem отменен: mc=" + (mc != null) + ", player=" + (mc != null && mc.player != null) + ", item=" + item);
      }
   }

   public void startSwapToItemStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         String itemName = stack.getName().getString();
         Logger.info("[AutoSwap] startSwapToItemStack: item=" + item + ", name=" + itemName);
         if (mc != null && mc.player != null && item != null && item != Items.AIR) {
            Slot foundSlot = null;

            for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
               ItemStack slotStack = slot.getStack();
               if (!slotStack.isEmpty() && slotStack.getItem() == item && slotStack.getName().getString().equals(itemName)) {
                  foundSlot = slot;
                  break;
               }
            }

            if (foundSlot != null) {
               ItemStack stackToSwap = foundSlot.getStack();
               this.swapOffhand(foundSlot, () -> this.triggerSwapToast(stackToSwap));
            } else {
               Logger.warn("[AutoSwap] Слот не найден для предмета: " + item + " с названием '" + itemName + "'");
            }
         } else {
            Logger.warn("[AutoSwap] startSwapToItemStack отменен");
         }
      } else {
         Logger.warn("[AutoSwap] startSwapToItemStack: стек пуст");
      }
   }

   @Override
   public BindSetting getBind() {
      return this.bind;
   }

   public RadioSetting getMode() {
      return this.mode;
   }

   public SelectSetting getBypass() {
      return this.bypass;
   }

   public RadioSetting getFirstItem() {
      return this.firstItem;
   }

   public RadioSetting getSecondItem() {
      return this.secondItem;
   }

   public BooleanSetting getAutoDamage() {
      return this.autoDamage;
   }

   public MultiSelectSetting getAutoDamageConditions() {
      return this.autoDamageConditions;
   }

   public SliderSettings getLowHpThreshold() {
      return this.lowHpThreshold;
   }

   public ItemStack getSavedOffhandItem() {
      return this.savedOffhandItem;
   }

   public boolean isAutoDamageActive() {
      return this.autoDamageActive;
   }

   public Text getLastSwapDisplayName() {
      return this.lastSwapDisplayName;
   }

   public long getLastSwapTimeMs() {
      return this.lastSwapTimeMs;
   }

   public Animation getSwapAnim() {
      return this.swapAnim;
   }

   public Script getScript() {
      return this.script;
   }

   public AutoSwap.WheelSlotItem[] getWheelSlots() {
      return this.wheelSlots;
   }

   public Integer getSelectingSlotIndex() {
      return this.selectingSlotIndex;
   }

   public boolean isAwaitingInventoryPick() {
      return this.awaitingInventoryPick;
   }

   private static class WheelSlotItem {
      final Item item;
      final String itemName;

      WheelSlotItem(Item item, String itemName) {
         this.item = item;
         this.itemName = itemName;
      }
   }
}
