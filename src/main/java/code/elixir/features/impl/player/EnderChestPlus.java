package code.elixir.features.impl.player;

import code.elixir.display.hud.Notifications;
import code.elixir.events.container.CloseScreenEvent;
import code.elixir.events.container.SetScreenEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.inv.InventoryTask;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class EnderChestPlus extends Module {
   private HandledScreen<?> screen;
   private final BindSetting bindSetting = new BindSetting("Сложить", "Помещает все предметы в эндер-сундук");

   public EnderChestPlus() {
      super("EnderChestPlus", "Ender-ChestPlus", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.bindSetting});
   }

   @Override
   public void deactivate() {
      if (this.screen != null) {
         this.screen = null;
         InventoryTask.closeScreen(true);
         Notifications.getInstance().addList("Ender Chest - " + Formatting.RED + "закрыт", 5000);
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.bindSetting.getKey()) && this.screen != null) {
         DefaultedList<Slot> slots = mc.player.currentScreenHandler.slots;
         slots.stream().filter(slot -> lambda$onKey$0((List)slots, slot)).findFirst().ifPresent(s -> InventoryTask.swapHand(s, Hand.OFF_HAND, false));
         slots.stream().filter(slot -> lambda$onKey$2((List)slots, slot)).forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, false));
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.screen != null && mc.player != null) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof GameJoinS2CPacket || packet instanceof OpenScreenS2CPacket || packet instanceof CloseScreenS2CPacket || packet instanceof PlayerRespawnS2CPacket) {
            this.deactivate();
         } else if (packet instanceof CloseHandledScreenC2SPacket) {
            e.cancel();
         } else if (packet instanceof PlayerActionC2SPacket player && player.getAction().equals(Action.SWAP_ITEM_WITH_OFFHAND)) {
            InventoryTask.swapHand(InventoryTask.mainHandSlot(), Hand.OFF_HAND, false);
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onSetScreen(SetScreenEvent e) {
      if (e.getScreen() instanceof InventoryScreen && this.screen != null) {
         e.setScreen(this.screen);
      }
   }

   @EventHandler
   public void onCloseScreen(CloseScreenEvent e) {
      Screen class_4372 = e.getScreen();
      GenericContainerScreen scr;
      if (class_4372 instanceof GenericContainerScreen
         && (scr = (GenericContainerScreen)class_4372).getTitle().getString().contains(Text.translatable("container.enderchest").getString())) {
         this.screen = scr;
      }

      if (this.screen != null) {
         mc.setScreen(null);
         e.cancel();
      }
   }

   private static boolean lambda$onKey$2(List slots, Slot s) {
      return s.id >= slots.size() - 36 && !s.getStack().isEmpty();
   }

   private static boolean lambda$onKey$0(List slots, Slot s) {
      return s.id < slots.size() - 36 && s.getStack().isEmpty();
   }
}
