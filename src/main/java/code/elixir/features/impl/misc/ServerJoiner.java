package code.elixir.features.impl.misc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.interactions.inv.InventoryTask;
import java.util.function.Predicate;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ServerJoiner extends Module {
   private final SelectSetting serverSelection = new SelectSetting("Сервер", "Выберите целевой сервер")
      .value("РиллиВорлд", "СпукиТайм дуэли")
      .selected("РиллиВорлд");
   private final SliderSettings griefSelection = new SliderSettings("Номер грифа", "Выберите номер сервера для грифа")
      .setValue(1.0F)
      .range(1, 54)
      .visible(() -> this.serverSelection.isSelected("РиллиВорлд"));
   private final TextSetting spookyCompassName = new TextSetting("Название компаса", "Название компаса для SpookyTime")
      .setText("выберите режим")
      .setMin(1)
      .setMax(64)
      .visible(() -> this.serverSelection.isSelected("СпукиТайм дуэли"));
   private long lastActionTime;
   private boolean isToggling;
   private boolean retryDuels;

   public ServerJoiner() {
      super("ServerJoiner", "ServerJoiner", ModuleCategory.MISC);
      this.setup(new Setting[]{this.serverSelection, this.griefSelection, this.spookyCompassName});
   }

   public static ServerJoiner getInstance() {
      return Instance.get(ServerJoiner.class);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.state) {
         if (GLFW.glfwGetKey(mc.getWindow().getHandle(), 260) == 1) {
            this.setState(false);
         } else if (mc.currentScreen == null && mc.player != null) {
            if (this.serverSelection.isSelected("СпукиТайм дуэли")) {
               if (System.currentTimeMillis() - this.lastActionTime > 250L) {
                  this.useSpookyCompassOrDisable();
               }
            } else if (mc.player.age < 5) {
               InventoryTask.selectCompass();
               mc.player
                  .networkHandler
                  .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
            }
         } else if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < ((GenericContainerScreenHandler)container.getScreenHandler()).slots.size(); i++) {
               String slotName = ((Slot)((GenericContainerScreenHandler)container.getScreenHandler()).slots.get(i))
                  .getStack()
                  .getName()
                  .getString()
                  .toLowerCase();
               if (this.serverSelection.isSelected("РиллиВорлд") && Network.isReallyWorld()) {
                  if (slotName.contains("гриферское выживание") && this.canClick()) {
                     InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                     this.lastActionTime = System.currentTimeMillis();
                  }

                  if (slotName.contains("гриф #" + (int)this.griefSelection.getValue()) && this.canClick()) {
                     InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                     this.lastActionTime = System.currentTimeMillis();
                  }
               } else if (this.serverSelection.isSelected("СпукиТайм дуэли")
                  && container.getTitle().getString().toLowerCase().contains("выберите режим")
                  && this.canClick()) {
                  InventoryTask.clickSlot(14, 0, SlotActionType.QUICK_MOVE, false);
                  this.lastActionTime = System.currentTimeMillis();
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.state && event.getType() == PacketEvent.Type.RECEIVE) {
         Packet<?> class_25962 = event.getPacket();
         DisconnectS2CPacket packet;
         String message;
         if (class_25962 instanceof DisconnectS2CPacket
            && (
               (message = (packet = (DisconnectS2CPacket)class_25962).reason().getString().toLowerCase()).contains("к сожалению сервер переполнен")
                  || message.contains("подождите 20 секунд!")
                  || message.contains("вы уже подключены на этот сервер!")
                  || message.contains("подождите несколько секунд перед повторным подключением!")
                  || message.contains("вы были кикнуты с сервера 1duels:")
                  || message.contains("вы были кикнуты")
                  || message.contains("большой поток игроков")
                  || message.contains("сервер заполнен!")
            )) {
            if (this.serverSelection.isSelected("СпукиТайм дуэли")) {
               this.useSpookyCompassOrDisable();
            } else {
               InventoryTask.selectCompass();
               mc.player
                  .networkHandler
                  .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
            }
         }
      }
   }

   @Override
   public void activate() {
      super.activate();
      this.isToggling = false;
      this.retryDuels = false;
      this.lastActionTime = 0L;
      if (this.serverSelection.isSelected("СпукиТайм дуэли")) {
         this.useSpookyCompassOrDisable();
      } else if (mc.player != null && mc.player.networkHandler != null) {
         InventoryTask.selectCompass();
         mc.player
            .networkHandler
            .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.lastActionTime = 0L;
      this.isToggling = false;
      this.retryDuels = false;
   }

   private boolean canClick() {
      return System.currentTimeMillis() - this.lastActionTime > 50L;
   }

   private void useSpookyCompassOrDisable() {
      if (mc.player != null && mc.player.networkHandler != null) {
         Slot compassSlot = this.findSpookyCompassSlot();
         if (compassSlot == null) {
            Notifications.getInstance().addList("Компас с названием " + this.spookyCompassName.getText() + " не найден", 2500);
            this.setState(false);
         } else {
            InventoryTask.swapHand(compassSlot, Hand.MAIN_HAND, false, true);
            mc.player
               .networkHandler
               .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
            this.lastActionTime = System.currentTimeMillis();
         }
      }
   }

   private Slot findSpookyCompassSlot() {
      String searchName = normalize(this.spookyCompassName.getText());
      return InventoryTask.getSlot((Predicate<Slot>)(slot -> {
         if (!slot.getStack().isOf(Items.COMPASS)) {
            return false;
         } else {
            return searchName.isEmpty() ? true : normalize(InventoryTask.getCleanName(slot.getStack().getName())).contains(searchName);
         }
      }));
   }

   private static String normalize(String text) {
      return text == null ? "" : text.toLowerCase().trim();
   }
}
