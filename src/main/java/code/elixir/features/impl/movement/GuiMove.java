package code.elixir.features.impl.movement;

import code.elixir.events.container.CloseScreenEvent;
import code.elixir.events.item.ClickSlotEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.math.time.StopWatch;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

public class GuiMove extends Module {
   public static final SelectSetting mode = new SelectSetting("Обход", "Режим обхода при движении в GUI")
      .value("Vanilla", "Reallyworld", "Funtime", "Holyworld", "SpookyTime", "LonyGrief", "Легит")
      .selected("SpookyTime");
   private final List<Packet<?>> pendingPackets = new ArrayList<>();
   private final Queue<ClickSlotC2SPacket> spookyQueue = new LinkedList<>();
   private final StopWatch holyTimer = new StopWatch();
   private final StopWatch funtimeTimer = new StopWatch();
   private final StopWatch lonyTimer = new StopWatch();
   private int reallyworldTicks;
   private int holyDelay;
   private boolean holyClosing;
   private int spookyClickDelay;
   private boolean spookyMovementLock;
   private boolean spookyHadCursor;
   public int movementBlockTicks;
   public boolean bypassActive;
   private long spookyLastClickMs;

   public static GuiMove getInstance() {
      return Instance.get(GuiMove.class);
   }

   public GuiMove() {
      super("GuiMove", "GuiMove", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{mode});
   }

   public boolean isReallyworld() {
      return this.isState() && mode.isSelected("Reallyworld");
   }

   public boolean isFuntime() {
      return this.isState() && mode.isSelected("Funtime");
   }

   public boolean isHolyworld() {
      return this.isState() && mode.isSelected("Holyworld");
   }

   public boolean isSpookyTime() {
      return this.isState() && mode.isSelected("SpookyTime");
   }

   public boolean isLonyGrief() {
      return this.isState() && mode.isSelected("LonyGrief");
   }

   public boolean isLegit() {
      return this.isState() && mode.isSelected("Легит");
   }

   public boolean shouldBlockMovement() {
      return !this.isState() || this.movementBlockTicks > 0;
   }

   public void notifySpookyClick() {
      if (this.isSpookyTime()) {
         this.spookyClickDelay = this.resolveSpookyDelay(null) + 1;
      }
   }

   public void setBypassActive(boolean active) {
      this.bypassActive = active;
   }

   public void beginBypassForSwap() {
      this.beginBypassForSwap(mode.getSelected());
   }

   public void beginBypassForSwap(String bypassMode) {
      this.bypassActive = true;
      switch (bypassMode) {
         case "Funtime":
            this.funtimeTimer.reset();
            break;
         case "Holyworld":
            this.holyDelay = 2;
            this.holyClosing = true;
            this.holyTimer.reset();
            break;
         case "Reallyworld":
            this.reallyworldTicks = 3;
            break;
         case "LonyGrief":
            this.lonyTimer.reset();
            break;
         case "SpookyTime":
            this.notifySpookyClick();
      }
   }

   public boolean isBypassMode(String bypassMode) {
      return bypassMode != null && bypassMode.equals(mode.getSelected());
   }

   public void flushSpookyQueue() {
      if (!this.spookyQueue.isEmpty() && mc.player != null) {
         while (!this.spookyQueue.isEmpty()) {
            ClickSlotC2SPacket packet = this.spookyQueue.poll();
            if (packet != null) {
               mc.getNetworkHandler().sendPacket(packet);
            }
         }

         this.movementBlockTicks = 0;
      }
   }

   @Override
   public void deactivate() {
      this.flushSpookyQueue();
      this.pendingPackets.clear();
      this.movementBlockTicks = 1;
      this.reallyworldTicks = 0;
      this.holyDelay = 0;
      this.spookyClickDelay = 0;
      this.spookyMovementLock = false;
      this.bypassActive = false;
      this.holyClosing = false;
      super.deactivate();
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.isState() && mc.player != null && !this.isLegit()) {
         if (this.isLonyGrief()) {
            Packet<?> class_25963;
            if (!this.bypassActive && event.isSend() && (class_25963 = event.getPacket()) instanceof ClickSlotC2SPacket) {
               ClickSlotC2SPacket click = (ClickSlotC2SPacket)class_25963;
               if (mc.currentScreen instanceof InventoryScreen && !this.spookyMovementLock) {
                  this.pendingPackets.add(click);
                  event.cancel();
               }
            }
         } else if (this.isSpookyTime()) {
            if (!event.isSend() && event.getPacket() instanceof CloseScreenS2CPacket) {
               event.cancel();
            } else {
               Packet<?> class_25964;
               if (event.isSend() && (class_25964 = event.getPacket()) instanceof ClickSlotC2SPacket) {
                  ClickSlotC2SPacket click = (ClickSlotC2SPacket)class_25964;
                  this.spookyLastClickMs = System.currentTimeMillis();
                  if (!this.bypassActive && this.isPlayerMoving() && click.getSlot() != -1) {
                     this.notifySpookyClick(click.getActionType());
                     if (!this.spookyMovementLock && this.enqueueSpooky(click)) {
                        this.movementBlockTicks = 0;
                        event.cancel();
                     }
                  }
               }
            }
         } else if (!this.usesBypass()) {
            Packet<?> class_25965;
            CloseScreenS2CPacket screen;
            if (!event.isSend()
               && (class_25965 = event.getPacket()) instanceof CloseScreenS2CPacket
               && (screen = (CloseScreenS2CPacket)class_25965).getSyncId() == 0) {
               event.cancel();
            }
         } else {
            Packet<?> class_25962;
            if (event.isSend() && (class_25962 = event.getPacket()) instanceof ClickSlotC2SPacket) {
               ClickSlotC2SPacket click = (ClickSlotC2SPacket)class_25962;
               if ((!this.pendingPackets.isEmpty() || Simulations.hasPlayerMovement()) && InventoryFlowManager.shouldSkipExecution()) {
                  this.pendingPackets.add(click);
                  event.cancel();
               }
            }
         }
      }
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent event) {
      if (this.isState() && mc.player != null) {
         if (this.isLegit()) {
            if (Simulations.hasPlayerMovement()) {
               event.cancel();
            }
         } else if (this.usesBypass() && !this.isSpookyTime() && !this.isLonyGrief()) {
            SlotActionType actionType = event.getActionType();
            if ((!this.pendingPackets.isEmpty() || Simulations.hasPlayerMovement())
               && (event.getButton() == 1 && actionType != SlotActionType.SWAP && actionType != SlotActionType.THROW || actionType == SlotActionType.PICKUP_ALL)
               )
             {
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   public void onCloseScreen(CloseScreenEvent event) {
      if (this.isState() && !this.isLegit() && this.usesBypass() && !this.pendingPackets.isEmpty()) {
         if (!this.isSpookyTime() && !this.isLonyGrief()) {
            InventoryFlowManager.addTask(() -> {
               this.pendingPackets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
               this.pendingPackets.clear();
               InventoryTask.updateSlots();
            });
         }
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.isState() && mc.player != null && !this.isLegit()) {
         if (this.isReallyworld() && this.reallyworldTicks > 0) {
            event.inputNone();
         } else if (this.isFuntime() && !this.funtimeTimer.finished(190.0)) {
            event.inputNone();
         } else if (this.isHolyworld() && this.holyDelay > 0 && !this.holyTimer.finished(222.0)) {
            event.inputNone();
         } else if (this.isSpookyTime()) {
            if (System.currentTimeMillis() - this.spookyLastClickMs <= 170L) {
               event.inputNone();
            } else if (this.spookyMovementLock) {
               event.inputNone();
            }
         } else {
            if (this.isLonyGrief() && this.bypassActive && !this.lonyTimer.finished(50.0)) {
               event.inputNone();
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.isState() && mc.player != null) {
         if (this.isLegit()) {
            if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution()) {
               InventoryFlowManager.updateMoveKeys();
            }
         } else {
            if (this.reallyworldTicks > 0) {
               this.reallyworldTicks--;
            }

            if (this.holyDelay > 0) {
               this.holyDelay--;
            }

            if (this.isSpookyTime()) {
               if (this.spookyHadCursor && this.spookyMovementLock && this.spookyClickDelay > 0) {
                  this.flushSpookyQueue();
               }

               this.spookyHadCursor = this.spookyMovementLock;
               if (this.shouldFreezeSpookyClick()) {
                  this.notifySpookyClick(null);
               } else if (this.spookyClickDelay > 0) {
                  this.spookyClickDelay--;
               }

               this.spookyMovementLock = this.spookyClickDelay > 0;
               this.movementBlockTicks++;
               if (this.spookyMovementLock) {
                  this.movementBlockTicks = 0;
               }
            }

            if (!this.isBlockedScreen()) {
               if (!this.shouldPauseMovement()) {
                  this.updateMovementKeys();
                  if (this.bypassActive && this.isReallyworld()) {
                     this.sendPendingAndClose();
                     this.bypassActive = false;
                  }

                  if (this.bypassActive && this.isFuntime()) {
                     mc.setScreen(new InventoryScreen(mc.player));
                     this.sendPendingAndClose();
                     this.bypassActive = false;
                     mc.setScreen(null);
                  }

                  if (this.holyClosing && this.isHolyworld() && this.holyDelay == 0) {
                     this.sendPendingAndClose();
                     this.holyClosing = false;
                     this.bypassActive = false;
                  }

                  if (this.bypassActive && this.isLonyGrief() && this.lonyTimer.finished(50.0)) {
                     this.sendPendingAndClose();
                     this.bypassActive = false;
                     if (mc.currentScreen != null) {
                        mc.setScreen(null);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean usesBypass() {
      return this.isReallyworld() || this.isFuntime() || this.isHolyworld() || this.isSpookyTime() || this.isLonyGrief();
   }

   private boolean isBlockedScreen() {
      return mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof SignEditScreen
         || mc.currentScreen instanceof AnvilScreen
         || mc.currentScreen instanceof StructureBlockScreen;
   }

   private boolean shouldPauseMovement() {
      if (this.isReallyworld() && this.reallyworldTicks > 0) {
         return true;
      } else if (this.isFuntime() && !this.funtimeTimer.finished(100.0)) {
         return true;
      } else if (this.isHolyworld() && this.holyDelay > 0 && !this.holyTimer.finished(125.0)) {
         return true;
      } else {
         return this.isSpookyTime() && this.spookyMovementLock ? true : this.isLonyGrief() && this.bypassActive && !this.lonyTimer.finished(50.0);
      }
   }

   private void updateMovementKeys() {
      for (KeyBinding key : new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey}) {
         key.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getDefaultKey().getCode()));
      }
   }

   private void sendPendingAndClose() {
      this.pendingPackets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
      this.pendingPackets.clear();
      if (mc.player != null) {
         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }

   private boolean isPlayerMoving() {
      return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F || this.spookyMovementLock;
   }

   private boolean shouldFreezeSpookyClick() {
      if (!(mc.currentScreen instanceof InventoryScreen)) {
         return false;
      } else {
         return !this.isPlayerMoving() ? false : !mc.player.currentScreenHandler.getCursorStack().isEmpty();
      }
   }

   private int resolveSpookyDelay(SlotActionType type) {
      if (type == null) {
         return 2;
      } else if (type == SlotActionType.PICKUP) {
         return 1;
      } else {
         return this.spookyClickDelay > 1 ? 2 : 3;
      }
   }

   private void notifySpookyClick(SlotActionType type) {
      this.spookyClickDelay = this.resolveSpookyDelay(type) + 1;
   }

   private boolean enqueueSpooky(ClickSlotC2SPacket packet) {
      return !this.spookyQueue.contains(packet) && this.spookyQueue.add(packet);
   }

   public List<Packet<?>> getPendingPackets() {
      return this.pendingPackets;
   }

   public Queue<ClickSlotC2SPacket> getSpookyQueue() {
      return this.spookyQueue;
   }

   public StopWatch getHolyTimer() {
      return this.holyTimer;
   }

   public StopWatch getFuntimeTimer() {
      return this.funtimeTimer;
   }

   public StopWatch getLonyTimer() {
      return this.lonyTimer;
   }

   public int getReallyworldTicks() {
      return this.reallyworldTicks;
   }

   public int getHolyDelay() {
      return this.holyDelay;
   }

   public boolean isHolyClosing() {
      return this.holyClosing;
   }

   public int getSpookyClickDelay() {
      return this.spookyClickDelay;
   }

   public boolean isSpookyMovementLock() {
      return this.spookyMovementLock;
   }

   public boolean isSpookyHadCursor() {
      return this.spookyHadCursor;
   }

   public int getMovementBlockTicks() {
      return this.movementBlockTicks;
   }

   public boolean isBypassActive() {
      return this.bypassActive;
   }

   public long getSpookyLastClickMs() {
      return this.spookyLastClickMs;
   }
}
