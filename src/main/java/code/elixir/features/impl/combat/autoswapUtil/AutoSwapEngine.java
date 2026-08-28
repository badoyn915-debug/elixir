package code.elixir.features.impl.combat.autoswapUtil;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class AutoSwapEngine implements QuickImports {
   private static final AutoSwapEngine INSTANCE = new AutoSwapEngine();
   private final StopWatch funtimeTimer = new StopWatch();
   private Slot pendingSlot;
   private boolean swapQueued;
   private int delayTicks;
   private int stage;
   private boolean funtimeQueued;
   private String bypassMode = "Vanilla";

   private AutoSwapEngine() {
   }

   public static AutoSwapEngine get() {
      return INSTANCE;
   }

   public void requestSwap(Slot slot, String bypassMode) {
      if (mc.player != null && slot != null && slot.id != -1) {
         this.bypassMode = bypassMode != null ? bypassMode : "Vanilla";
         this.pendingSlot = slot;
         this.swapQueued = true;
      } else {
         this.reset();
      }
   }

   public void onTick(TickEvent event) {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.delayTicks > 0) {
            this.delayTicks--;
         }

         if (this.funtimeQueued && this.funtimeTimer.finished(100.0) && this.pendingSlot != null) {
            this.executeFuntimeSwap(this.pendingSlot);
            this.reset();
         } else if (this.stage == 1 && this.pendingSlot != null && this.delayTicks <= 0) {
            this.executeDelayedSwap(this.pendingSlot);
            this.reset();
         } else if (this.swapQueued && this.pendingSlot != null) {
            this.swapQueued = false;
            Slot slot = this.pendingSlot;
            if (this.isFuntimeBypass()) {
               this.funtimeQueued = true;
               this.funtimeTimer.reset();
            } else if (this.shouldDelaySwap()) {
               this.delayTicks = 2;
               this.stage = 1;
            } else {
               this.executeInstantSwap(slot);
               this.pendingSlot = null;
            }
         }
      }
   }

   public void onInput(InputEvent event) {
      if (this.isBusy()) {
         if (this.delayTicks > 0 || this.funtimeQueued) {
            event.inputNone();
         }

         if (this.funtimeQueued && !this.funtimeTimer.finished(350.0)) {
            event.setSneak(false);
         }
      }
   }

   public boolean isBusy() {
      return this.swapQueued || this.funtimeQueued || this.stage > 0 || this.delayTicks > 0 || this.pendingSlot != null;
   }

   public void reset() {
      this.pendingSlot = null;
      this.swapQueued = false;
      this.delayTicks = 0;
      this.stage = 0;
      this.funtimeQueued = false;
      this.bypassMode = "Vanilla";
   }

   private boolean isFuntimeBypass() {
      return "Funtime".equals(this.bypassMode);
   }

   private boolean shouldDelaySwap() {
      return "SpookyTime".equals(this.bypassMode)
         || "Reallyworld".equals(this.bypassMode)
         || "Holyworld".equals(this.bypassMode)
         || "LonyGrief".equals(this.bypassMode);
   }

   private boolean usesInventoryTask() {
      return !"Vanilla".equals(this.bypassMode) && !"Reallyworld".equals(this.bypassMode);
   }

   private void executeInstantSwap(Slot slot) {
      this.withGuiMoveBypass(false, () -> {
         if (this.usesInventoryTask()) {
            InventoryTask.swapHand(slot, Hand.OFF_HAND, true, true);
         } else {
            this.clickOffhand(slot);
         }
      });
   }

   private void executeDelayedSwap(Slot slot) {
      this.withGuiMoveBypass(true, () -> this.clickOffhand(slot));
   }

   private void executeFuntimeSwap(Slot slot) {
      if (mc.player != null) {
         mc.setScreen(new InventoryScreen(mc.player));
         this.withGuiMoveBypass(false, () -> this.clickOffhand(slot));
         mc.setScreen(null);
      }
   }

   private void withGuiMoveBypass(boolean delayedSwap, Runnable action) {
      GuiMove guiMove = GuiMove.getInstance();
      boolean previousBypass = guiMove != null && guiMove.isBypassActive();
      if (guiMove != null && guiMove.isState()) {
         if (delayedSwap && "SpookyTime".equals(this.bypassMode)) {
            guiMove.setBypassActive(true);
         } else {
            guiMove.beginBypassForSwap(this.bypassMode);
         }

         if ("Funtime".equals(this.bypassMode)) {
            guiMove.notifySpookyClick();
         }
      }

      try {
         action.run();
      } finally {
         if (guiMove != null && delayedSwap && "SpookyTime".equals(this.bypassMode)) {
            guiMove.setBypassActive(previousBypass);
         }
      }
   }

   private void clickOffhand(Slot slot) {
      if (mc.player != null && mc.interactionManager != null && slot != null) {
         InventoryTask.clickSlot(slot, 40, SlotActionType.SWAP, false);
         if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         }

         InventoryFlowManager.enableMoveKeys();
         mc.interactionManager.syncSelectedSlot();
      }
   }
}
