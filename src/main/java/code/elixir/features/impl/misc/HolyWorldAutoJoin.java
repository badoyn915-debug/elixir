package code.elixir.features.impl.misc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.player.TickEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.Iterator;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.StringUtils;

public class HolyWorldAutoJoin implements QuickImports {
   private int targetAnarchy = -1;
   private boolean isJoining = false;
   private StopWatch hubDelayWatch = new StopWatch();
   private StopWatch actionDelayWatch = new StopWatch();
   private int currentStep = 0;
   private boolean completed = false;
   private boolean active = false;

   @EventHandler
   public void onTick(TickEvent event) {
      if (!this.active) {
         if (this.isJoining || this.completed) {
            this.reset();
         }
      } else if (!Network.isHolyWorld()) {
         if (this.isJoining || this.completed) {
            this.reset();
         }
      } else if (!this.completed) {
         if (this.currentStep == 0 && !this.isJoining) {
            if (this.targetAnarchy <= 0) {
               int anarchy = Network.getAnarchy();
               if (anarchy <= 0) {
                  return;
               }

               this.targetAnarchy = anarchy;
            }

            this.isJoining = true;
            this.currentStep = 1;
            this.hubDelayWatch.reset();
            mc.player.networkHandler.sendChatCommand("hub");
         } else if (this.currentStep == 1) {
            if (this.hubDelayWatch.elapsedTime() >= 50L) {
               InventoryTask.selectCompass();
               this.actionDelayWatch.reset();
               this.currentStep = 2;
            }
         } else if (this.currentStep == 2) {
            ItemStack mainHandStack = mc.player.getMainHandStack();
            if (mainHandStack.getItem() == Items.COMPASS) {
               if (this.actionDelayWatch.elapsedTime() >= 50L) {
                  mc.player
                     .networkHandler
                     .sendPacket(
                        new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch())
                     );
                  this.actionDelayWatch.reset();
                  this.currentStep = 3;
               }
            } else if (this.actionDelayWatch.elapsedTime() >= 50L) {
               InventoryTask.selectCompass();
               this.actionDelayWatch.reset();
            }
         } else if (this.currentStep == 3) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
               if (this.actionDelayWatch.elapsedTime() >= 250L) {
                  this.currentStep = 4;
                  this.actionDelayWatch.reset();
               }
            } else if (this.actionDelayWatch.elapsedTime() >= 250L) {
               ItemStack mainHandStack = mc.player.getMainHandStack();
               if (mainHandStack.getItem() == Items.COMPASS) {
                  mc.player
                     .networkHandler
                     .sendPacket(
                        new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch())
                     );
                  this.actionDelayWatch.reset();
               } else {
                  InventoryTask.selectCompass();
                  this.currentStep = 2;
                  this.actionDelayWatch.reset();
               }
            }
         } else if (this.currentStep == 4) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
               if (this.actionDelayWatch.elapsedTime() >= 500L) {
                  InventoryTask.clickSlot(12, 0, SlotActionType.PICKUP, true);
                  this.currentStep = 5;
                  this.actionDelayWatch.reset();
               }
            } else {
               this.reset();
            }
         } else if (this.currentStep == 5) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
               if (this.actionDelayWatch.elapsedTime() >= 500L) {
                  int categorySlot = -1;
                  if (this.targetAnarchy <= 16) {
                     categorySlot = 0;
                  } else if (this.targetAnarchy <= 37) {
                     categorySlot = 1;
                  } else if (this.targetAnarchy <= 53) {
                     categorySlot = 2;
                  } else if (this.targetAnarchy >= 54) {
                     categorySlot = 3;
                  }

                  if (categorySlot != -1) {
                     InventoryTask.clickSlot(categorySlot, 0, SlotActionType.PICKUP, true);
                     this.currentStep = 6;
                     this.actionDelayWatch.reset();
                  }
               }
            } else {
               this.reset();
            }
         } else if (this.currentStep == 6) {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
               if (this.actionDelayWatch.elapsedTime() >= 500L) {
                  boolean found = false;
                  DefaultedList slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
                  String title = screen.getTitle().getString();
                  boolean isLightAnarchy = title != null && (title.toLowerCase().contains("лайт") || title.contains("1") && title.contains("64"));

                  for (int i = 0; i < slots.size(); i++) {
                     Slot slot = (Slot)slots.get(i);
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty()) {
                        Integer anarchyNumber = null;
                        LoreComponent lore;
                        if (stack.getItem() == Items.PLAYER_HEAD
                           && (lore = (LoreComponent)stack.get(DataComponentTypes.LORE)) != null
                           && !lore.lines().isEmpty()) {
                           for (Text line : lore.lines()) {
                              String loreText = line.getString();
                              String numberAfterHash;
                              if (loreText.contains("#") && (numberAfterHash = StringUtils.substringAfter(loreText, "#")) != null && !numberAfterHash.isEmpty()
                                 )
                               {
                                 anarchyNumber = parseLeadingInt(numberAfterHash.trim());
                                 break;
                              }
                           }
                        }

                        if (anarchyNumber == null
                           && (isLightAnarchy || this.targetAnarchy >= 1 && this.targetAnarchy <= 64)
                           && (anarchyNumber = parseLeadingInt(stack.getName().getString().replaceAll("§[0-9a-fk-or]", "").replaceAll("§.", "").trim()))
                              == null
                           && stack.get(DataComponentTypes.LORE) != null) {
                           Iterator iterator = ((LoreComponent)stack.get(DataComponentTypes.LORE)).lines().iterator();

                           while (
                              iterator.hasNext()
                                 && (anarchyNumber = parseLeadingInt(((Text)iterator.next()).getString().replaceAll("§[0-9a-fk-or]", "").trim())) == null
                           ) {
                           }
                        }

                        if (anarchyNumber != null && anarchyNumber == this.targetAnarchy) {
                           InventoryTask.clickSlot(slot.id, 0, SlotActionType.PICKUP, false);
                           found = true;
                           mc.player.closeHandledScreen();
                           SoundManager.playSound(SoundManager.ENABLE_MODULE);
                           Notifications.getInstance()
                              .addList("Перезаход на анархию #" + Formatting.BLUE + anarchyNumber + Formatting.RESET + " выполнен", 3000);
                           this.reset();
                           this.active = false;
                           break;
                        }
                     }
                  }

                  if (!found && this.actionDelayWatch.elapsedTime() >= 1000L) {
                     this.reset();
                  }
               }
            } else if (!this.completed) {
               this.reset();
            }
         }
      }
   }

   private static Integer parseLeadingInt(String s) {
      if (s != null && !s.isEmpty()) {
         StringBuilder num = new StringBuilder();

         for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) {
               break;
            }

            num.append(c);
         }

         if (num.length() == 0) {
            return null;
         } else {
            try {
               return Integer.parseInt(num.toString());
            } catch (NumberFormatException var6) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public void reset() {
      this.targetAnarchy = -1;
      this.isJoining = false;
      this.currentStep = 0;
      this.completed = false;
      this.hubDelayWatch.reset();
      this.actionDelayWatch.reset();
   }

   public void setTargetAnarchy(int anarchy) {
      this.targetAnarchy = anarchy;
   }

   public void setActive(boolean active) {
      this.active = active;
      if (!active) {
         this.reset();
      }
   }

   public boolean isActive() {
      return this.active;
   }
}
