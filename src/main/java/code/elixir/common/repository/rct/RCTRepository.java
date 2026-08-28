package code.elixir.common.repository.rct;

import code.elixir.display.hud.Notifications;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class RCTRepository implements QuickImports, QuickLogger {
   private final StopWatch stopWatch = new StopWatch();
   private final StopWatch hubDelayWatch = new StopWatch();
   private boolean lobby;
   private int anarchy;
   private boolean rwReconnecting;
   private boolean hubSent;
   private boolean anSent;
   private int anarchyBeforeHub;
   private boolean rwLeftWorld;
   private long rwStartTime;

   public RCTRepository(EventManager eventManager) {
      EventManager.register(this);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      GameMessageS2CPacket message;
      String text;
      Packet<?> class_25962;
      if (this.anarchy != 0
         && (class_25962 = e.getPacket()) instanceof GameMessageS2CPacket
         && !(text = (message = (GameMessageS2CPacket)class_25962).content().getString().toLowerCase()).contains("хаб")
         && text.contains("не удалось")) {
         Notifications.getInstance().addList("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти", 3000);
         this.anarchy = 0;
         this.rwReconnecting = false;
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.anarchy != 0) {
         if (Network.isReallyWorld()) {
            this.tickReallyWorld();
         } else if (!Network.isSpookyTime() && !Network.isFunTime()) {
            if (this.anarchy <= 0 || !this.hubSent && !this.rwReconnecting) {
               this.anarchy = 0;
               this.rwReconnecting = false;
               this.hubSent = false;
               this.anSent = false;
               this.anarchyBeforeHub = -1;
               this.hubDelayWatch.reset();
            } else {
               this.tickCopyTime();
            }
         } else {
            this.tickCopyTime();
         }
      }
   }

   private void tickReallyWorld() {
      int currentAnarchy = Network.getAnarchy();
      if (!this.rwReconnecting) {
         this.rwReconnecting = true;
         this.hubSent = false;
         this.rwLeftWorld = false;
         this.anarchyBeforeHub = currentAnarchy;
         this.rwStartTime = System.currentTimeMillis();
         this.hubDelayWatch.reset();
      }

      if (System.currentTimeMillis() - this.rwStartTime > 25000L) {
         Notifications.getInstance().addList("[RCT] Таймаут — не удалось перезайти на ГРИФ #" + Formatting.BLUE + this.anarchy + Formatting.RESET, 3500);
         this.anarchy = 0;
         this.rwReconnecting = false;
         this.hubSent = false;
         this.rwLeftWorld = false;
         this.anarchyBeforeHub = -1;
      } else if (!this.hubSent) {
         mc.player.networkHandler.sendChatCommand("hub");
         this.hubSent = true;
         this.hubDelayWatch.reset();
      } else {
         int nowAnarchy = Network.getAnarchy();
         if (!this.rwLeftWorld && nowAnarchy != this.anarchyBeforeHub) {
            this.rwLeftWorld = true;
         }

         if (this.rwLeftWorld) {
            if (nowAnarchy == this.anarchy) {
               int completed = this.anarchy;
               SoundManager.playSound(SoundManager.ENABLE_MODULE);
               Notifications.getInstance().addList("Перезаход на ГРИФ #" + Formatting.BLUE + completed + Formatting.RESET + " выполнен", 3000);
               this.anarchy = 0;
               this.rwReconnecting = false;
               this.hubSent = false;
               this.rwLeftWorld = false;
               this.anarchyBeforeHub = -1;
            } else {
               if (mc.currentScreen instanceof GenericContainerScreen screen) {
                  String title = screen.getTitle().getString();
                  if (title.contains("Выбор сервера")) {
                     for (int i = 0; i < ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().size(); i++) {
                        ItemStack stack = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().getStack(i);
                        String name = stack.getName().getString();
                        if (name.contains("ГРИФЕРСКОЕ ВЫЖИВАНИЕ") && stack.getItem() == Blocks.CRAFTING_TABLE.asItem()) {
                           InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                           return;
                        }
                     }
                  } else if (title.contains("Выбор мира грифа")) {
                     for (int ix = 0; ix < ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().size(); ix++) {
                        ItemStack stack = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().getStack(ix);
                        String name = stack.getName().getString();
                        if (name.contains("ГРИФ #" + this.anarchy) && stack.getItem() == Items.PLAYER_HEAD) {
                           InventoryTask.clickSlot(ix, 0, SlotActionType.PICKUP, false);
                           return;
                        }
                     }
                  }
               } else if (this.stopWatch.every(500.0)) {
                  InventoryTask.selectCompass();
                  mc.player
                     .networkHandler
                     .sendPacket(
                        new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch())
                     );
               }
            }
         }
      }
   }

   private void tickCopyTime() {
      String serverTitle = "null";
      int currentAnarchy = Network.getAnarchy();
      if (this.hubSent) {
         this.hubDelayWatch.elapsedTime();
      } else {
         long var10000 = 0L;
      }

      serverTitle = mc.world != null && mc.world.getScoreboard() != null
         ? (
            mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null
               ? mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR).getDisplayName().getString()
               : "null"
         )
         : "null";
      if (currentAnarchy == this.anarchy && this.anarchy > 0 && this.rwReconnecting && this.anSent && serverTitle.contains("Анархия-")) {
         int completedAnarchy = this.anarchy;
         SoundManager.playSound(SoundManager.ENABLE_MODULE);
         Notifications.getInstance().addList("Перезаход на анархию #" + Formatting.BLUE + completedAnarchy + Formatting.RESET + " выполнен", 3000);
         this.anarchy = 0;
         this.rwReconnecting = false;
         this.hubSent = false;
         this.anSent = false;
         this.anarchyBeforeHub = -1;
         this.hubDelayWatch.reset();
      } else {
         if (currentAnarchy == this.anarchy && this.anarchy > 0 && !this.rwReconnecting) {
            this.rwReconnecting = true;
            this.hubSent = false;
            this.anSent = false;
            this.hubDelayWatch.reset();
         }

         if (this.rwReconnecting) {
            if (!this.hubSent) {
               this.anarchyBeforeHub = currentAnarchy;
               mc.player.networkHandler.sendChatMessage("/hub");
               this.hubSent = true;
               this.anSent = false;
               this.hubDelayWatch.reset();
            } else if (!this.anSent) {
               long elapsed = this.hubDelayWatch.elapsedTime();
               boolean worldChanged = currentAnarchy != this.anarchyBeforeHub;
               if (elapsed >= 890L && worldChanged) {
                  mc.player.networkHandler.sendChatMessage("/an" + this.anarchy);
                  this.anSent = true;
               }
            }
         }
      }
   }

   public void reconnect(int anarchy) {
      if (Network.isReallyWorld()) {
         if (anarchy > 0) {
            this.anarchy = anarchy;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.rwLeftWorld = false;
            this.anarchyBeforeHub = -1;
            this.rwStartTime = 0L;
         } else {
            Notifications.getInstance().addList("[RCT] Неверный " + Formatting.RED + "гриф", 3000);
         }
      } else if (Network.isSpookyTime() || Network.isFunTime()) {
         if (anarchy > 0) {
            this.anarchy = anarchy;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.hubDelayWatch.reset();
         } else {
            int currentAnarchy = Network.getAnarchy();
            if (currentAnarchy > 0) {
               this.anarchy = currentAnarchy;
               this.rwReconnecting = false;
               this.hubSent = false;
               this.hubDelayWatch.reset();
            } else {
               Notifications.getInstance().addList("[RCT] Неверный номер " + Formatting.RED + "анархии", 3000);
            }
         }
      }
   }
}
