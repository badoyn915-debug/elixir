package code.elixir.features.impl.misc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import code.elixir.utils.math.script.Script;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class ElytraHelper extends Module {
   private final BindSetting elytraSetting = new BindSetting("Свап элитр", "Меняет нагрудник на элитры");
   private final BindSetting fireworkSetting = new BindSetting("Фейерверк", "Использует фейерверк на элитре");
   private final RadioSetting fireworkMode = new RadioSetting(
         "Режим фейерверка", "Способ использования фейерверка", new String[]{"Легитно", "Пакетно"}, "Легитно"
      )
      .visible(() -> this.fireworkSetting.getKey() != -1);
   private final BooleanSetting hotbarOnly = new BooleanSetting("Фейерверки только с хотбара", "Берёт фейерверк только из хотбара")
      .setValue(false)
      .visible(() -> this.fireworkSetting.getKey() != -1);
   private final SliderSettings hotbarSlot = new SliderSettings("Слот хотбара (1-9)", "Слот для перемещения фейерверка")
      .setValue(9.0F)
      .range(1.0F, 9.0F)
      .step(1.0F)
      .visible(() -> this.hotbarOnly.isValue() && this.fireworkSetting.getKey() != -1);
   private final SliderSettings swapDelay = new SliderSettings("Задержка свапа", "Задержка между свапами (тики)").setValue(0.0F).range(0.0F, 20.0F).step(1.0F);
   private final BooleanSetting removeOnLand = new BooleanSetting("Снимать элитры при приземлении", "Автоматически снимает элитру на земле")
      .setValue(false)
      .visible(() -> this.elytraSetting.getKey() != -1);
   private final BooleanSetting autoTakeoff = new BooleanSetting("Автоматически взлетать", "Взлетает при приземлении с элитрой").setValue(false);
   private final BooleanSetting fireworkOnTakeoff = new BooleanSetting("Фейерверк при взлёте", "Использует фейерверк при старте полёта").setValue(false);
   private final BooleanSetting startSetting = new BooleanSetting("Быстрый старт", "При свапе на элитру сразу взлетает и жжёт фейерверк").setValue(false);
   private final BooleanSetting equipInCT = new BooleanSetting("Надевать в кт", "Обход свапа в кт через инвентарь").setValue(false);
   private final Script script = new Script();
   private boolean swapQueued;
   private int swapCooldown;
   private boolean wasOnGround = true;
   private boolean ctSwapPending;
   private int ctSwapTicks;
   public static boolean start = false;

   public ElytraHelper() {
      super("ElytraHelper", "ElytraHelper", ModuleCategory.MISC);
      this.setup(
         new Setting[]{
            this.elytraSetting,
            this.fireworkSetting,
            this.fireworkMode,
            this.hotbarOnly,
            this.hotbarSlot,
            this.swapDelay,
            this.removeOnLand,
            this.autoTakeoff,
            this.fireworkOnTakeoff,
            this.startSetting,
            this.equipInCT
         }
      );
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.autoTakeoff.isValue() && mc.player != null) {
         if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            if (mc.player.isOnGround()) {
               event.setJumping(true);
            } else if (!mc.player.isGliding()) {
               PlayerInteractionHelper.startFallFlying();
               if (this.fireworkOnTakeoff.isValue()) {
                  this.useFirework();
               }
            }
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (this.script.isFinished() && mc.player != null) {
         if (event.isKeyDown(this.elytraSetting.getKey()) && this.swapCooldown == 0) {
            boolean hasElytra = InventoryTask.getSlot(Items.ELYTRA) != null;
            boolean hasChest = this.chestPlate() != null;
            if (!hasElytra && !hasChest) {
               return;
            }

            if (this.equipInCT.isValue()) {
               this.changeChestPlate();
            } else if (this.chestPlate() != null) {
               this.swapQueued = true;
            }
         } else if (event.isKeyDown(this.fireworkSetting.getKey()) && mc.player.isGliding()) {
            this.useFirework();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.script.update();
      if (mc.player != null) {
         if (this.swapCooldown > 0) {
            this.swapCooldown--;
         }

         if (this.ctSwapPending) {
            this.handleCtSwap();
         }

         if (this.removeOnLand.isValue()
            && !this.wasOnGround
            && mc.player.isOnGround()
            && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
            && this.chestPlate() != null
            && this.swapCooldown == 0) {
            this.performSwap();
         }

         this.wasOnGround = mc.player.isOnGround();
         if (this.swapQueued && this.swapCooldown == 0) {
            this.swapQueued = false;
            Slot slot = this.chestPlate();
            if (slot == null) {
               return;
            }

            boolean puttingElytra = slot.getStack().isOf(Items.ELYTRA);
            Slot firework = puttingElytra ? InventoryTask.getSlot(Items.FIREWORK_ROCKET) : null;
            this.performSwap();
            if (this.startSetting.isValue() && firework != null && puttingElytra) {
               this.script.cleanup().addTickStep(4, () -> {
                  if (mc.player.isOnGround()) {
                     mc.player.jump();
                  }
               }).addTickStep(3, () -> {
                  if (ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET) > 0.0F) {
                     this.notifyFireworkCooldown();
                  } else {
                     PlayerInteractionHelper.startFallFlying();
                     InventoryTask.swapAndUse(Items.FIREWORK_ROCKET, false);
                  }
               });
            }
         }
      }
   }

   private void performSwap() {
      Slot slot = this.chestPlate();
      if (slot != null) {
         InventoryTask.moveItem(slot, 6, !GuiMove.mode.isSelected("Reallyworld"), true);
         this.swapCooldown = Math.round(this.swapDelay.getValue());
      }
   }

   private void handleCtSwap() {
      this.ctSwapTicks++;
      if (this.ctSwapTicks >= 2) {
         GuiMove guiMove = GuiMove.getInstance();
         if (guiMove != null && guiMove.isState()) {
            guiMove.setState(false);
         }

         Slot slot;
         if ((slot = this.chestPlate()) != null) {
            InventoryTask.moveItem(slot, 6, false, true);
         }

         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         }

         mc.setScreen(null);
         if (guiMove != null) {
            guiMove.setState(true);
         }

         this.ctSwapPending = false;
         this.ctSwapTicks = 0;
         this.swapCooldown = Math.round(this.swapDelay.getValue());
      }
   }

   private Slot chestPlate() {
      return Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
         ? InventoryTask.getSlot(
            List.of(
               Items.NETHERITE_CHESTPLATE,
               Items.DIAMOND_CHESTPLATE,
               Items.CHAINMAIL_CHESTPLATE,
               Items.IRON_CHESTPLATE,
               Items.GOLDEN_CHESTPLATE,
               Items.LEATHER_CHESTPLATE
            )
         )
         : InventoryTask.getSlot(Items.ELYTRA);
   }

   public void changeChestPlate() {
      ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (chestStack != null && chestStack.getItem() == Items.ELYTRA) {
         int chestSlot = getChestPlateSlot();
         if (chestSlot == -1) {
            ChatMessage.brandmessage("Нет нагрудника!");
            return;
         }

         if (GuiMove.getInstance() != null && GuiMove.getInstance().isState()) {
            mc.setScreen(new InventoryScreen(mc.player));
            this.ctSwapPending = true;
            this.ctSwapTicks = 0;
            InventoryTask.clickSlot(chestSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на нагрудник (обошёл запрет свапа в кт)");
         } else {
            InventoryTask.clickSlot(chestSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на нагрудник");
         }
      } else {
         int elytraSlot = this.getItemSlot(Items.ELYTRA);
         if (elytraSlot == -1) {
            ChatMessage.brandmessage("Нету: Элитра");
            return;
         }

         if (GuiMove.getInstance() != null && GuiMove.getInstance().isState()) {
            mc.setScreen(new InventoryScreen(mc.player));
            this.ctSwapPending = true;
            this.ctSwapTicks = 0;
            InventoryTask.clickSlot(elytraSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на элитру");
         } else {
            InventoryTask.clickSlot(elytraSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на элитру");
         }
      }
   }

   private void useFirework() {
      if (mc.player != null && mc.player.isGliding()) {
         if (ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET) > 0.0F) {
            this.notifyFireworkCooldown();
         } else if ("Пакетно".equals(this.fireworkMode.get())) {
            this.useFireworkPacket();
         } else {
            this.useFireworkLegit();
         }
      }
   }

   private void useFireworkLegit() {
      if (this.hotbarOnly.isValue()) {
         int hotbarIndex = Math.round(this.hotbarSlot.getValue()) - 1;
         int rocketSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
         if (rocketSlot == -1) {
            int invSlot = InventoryTask.findInventorySlot(Items.FIREWORK_ROCKET);
            if (invSlot == -1) {
               ChatMessage.brandmessage("§cНе удалось переместить фейерверк в хотбар");
               return;
            }

            InventoryTask.clickSlot(invSlot, hotbarIndex, SlotActionType.SWAP, false, true);
            rocketSlot = hotbarIndex;
         }

         int previous = mc.player.getInventory().selectedSlot;
         InventoryTask.switchTo(rocketSlot);
         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryTask.switchTo(previous);
      } else if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
      } else if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
         PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
         mc.player.swingHand(Hand.OFF_HAND);
      } else {
         int rocketHotbarSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
         if (rocketHotbarSlot != -1) {
            InventoryTask.switchTo(rocketHotbarSlot);
            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            Slot slot = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
            if (slot != null) {
               InventoryTask.swapHand(slot, Hand.OFF_HAND, false);
               PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
               mc.player.swingHand(Hand.OFF_HAND);
            }
         }
      }
   }

   private void useFireworkPacket() {
      if (mc.getNetworkHandler() != null) {
         if (ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET) > 0.0F) {
            this.notifyFireworkCooldown();
         } else if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
            PlayerInteractionHelper.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, mc.player.getYaw(), mc.player.getPitch()));
         } else if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            PlayerInteractionHelper.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, mc.player.getYaw(), mc.player.getPitch()));
         } else {
            int rocketHotbarSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
            if (rocketHotbarSlot != -1) {
               int previousSlot = mc.player.getInventory().selectedSlot;
               if (rocketHotbarSlot != previousSlot) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(rocketHotbarSlot));
               }

               PlayerInteractionHelper.sendSequencedPacket(
                  seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, mc.player.getYaw(), mc.player.getPitch())
               );
               if (rocketHotbarSlot != previousSlot) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
               }
            } else {
               Slot slot = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
               if (slot == null) {
                  ChatMessage.brandmessage("§cНет фейерверка в инвентаре");
               } else {
                  InventoryTask.swapHand(slot, Hand.OFF_HAND, false);
                  PlayerInteractionHelper.sendSequencedPacket(
                     seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, mc.player.getYaw(), mc.player.getPitch())
                  );
                  InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
               }
            }
         }
      }
   }

   private void notifyFireworkCooldown() {
      String time = String.format("%.1f", ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET)) + "с";
      Notifications.getInstance().addList(Formatting.RED + "Фейерверк" + Formatting.RESET + " - в кд еще " + time, 2000);
   }

   private int getItemSlot(Item input) {
      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == input) {
            slot = i;
            break;
         }
      }

      if (slot < 9 && slot != -1) {
         slot += 36;
      }

      return slot;
   }

   public static int getChestPlateSlot() {
      for (Item item : new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.LEATHER_CHESTPLATE}) {
         for (int i = 0; i < 36; i++) {
            Item stack = mc.player.getInventory().getStack(i).getItem();
            if (stack == item) {
               if (i < 9) {
                  i += 36;
               }

               return i;
            }
         }
      }

      return -1;
   }
}
