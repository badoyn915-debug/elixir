package code.elixir.features.impl.movement.autoMystGlider.handers;

import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.elixir.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.elixir.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.elixir.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.elixir.features.impl.movement.autoMystGlider.states.StealState;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.task.TaskPriority;
import java.util.Stack;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class StealHandler implements QuickImports {
   final AutoMystGliderModule module;
   final Stack<StealState> deque = new Stack<>();
   StealState stealState;
   long lastClickTime = -1L;
   long lastQuickMoveTime = -1L;
   long reconnectTime;
   boolean finished = false;

   public StealHandler(AutoMystGliderModule module) {
      this.module = module;
   }

   public void onTick() {
      if (this.deque.isEmpty() && !this.finished && !SpookyParser.isAnarchy(-1)) {
         this.updateChests(4);
      }

      if (!this.finished && this.allChestWasEmpty() && System.currentTimeMillis() - this.reconnectTime > 5000L) {
         this.finished = true;
         ConnectionState connection = this.module.getAutoGliderController().getConnectionHandler().getConnection();
         connection.getStealHandler().onDisconnect();
         connection.hub(GlidePhase.HUB_TO_HOME);
      } else if (!this.deque.isEmpty()) {
         this.applyState();
         if (this.stealState != null) {
            if (!this.isGeneric(this.stealState.getBlockPos())) {
               this.deque.remove(this.stealState);
               this.stealState = null;
            } else {
               if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
                  this.stealGenericHandler(handler);
                  if (this.isEmpty(handler)) {
                     this.stealState.setEmpty(true);
                     this.stealState = null;
                     mc.currentScreen.close();
                  }
               } else if (System.currentTimeMillis() - this.lastClickTime > 800L) {
                  this.openCurrentState();
                  this.lastClickTime = System.currentTimeMillis();
               }
            }
         }
      }
   }

   private void stealGenericHandler(GenericContainerScreenHandler handler) {
      for (Slot slot : handler.slots) {
         if (slot.inventory != mc.player.getInventory() && slot.hasStack() && System.currentTimeMillis() - this.lastQuickMoveTime > 150L) {
            InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
            this.lastQuickMoveTime = System.currentTimeMillis();
            break;
         }
      }
   }

   private void openCurrentState() {
      BlockPos pos = this.stealState.getBlockPos();
      Vec3d center = Vec3d.ofCenter(pos).add(0.2, 0.375, 0.2);
      Direction side = Direction.getFacing(center.x - mc.player.getX(), center.y - mc.player.getY(), center.z - mc.player.getZ()).getOpposite();
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, pos, false));
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public void onRotationUpdate(RotationUpdateEvent event) {
      if (this.stealState != null) {
         if (!this.isGeneric(this.stealState.getBlockPos())) {
            this.deque.remove(this.stealState);
            this.stealState = null;
         } else {
            BlockPos pos = this.stealState.getBlockPos();
            Turns current = TurnsConnection.INSTANCE.getRotation();
            Turns target = MathAngle.calculateAngle(Vec3d.ofCenter(pos).add(0.2, 0.375, 0.2));
            Turns delta = MathAngle.calculateDelta(current, target);
            Turns end = new Turns(current.getYaw() + delta.getYaw(), current.getPitch() + delta.getPitch());
            TurnsConnection.INSTANCE.rotateTo(end, new TurnsConfig(new LinearConstructor(), true, false), TaskPriority.HIGH_IMPORTANCE_1, this.module);
         }
      }
   }

   private boolean allChestWasEmpty() {
      if (this.deque.isEmpty()) {
         return false;
      } else {
         for (StealState state : this.deque) {
            if (!state.isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   private void updateChests(int range) {
      BlockPos base = mc.player.getBlockPos();

      for (BlockPos pos : BlockPos.iterate(base.add(-range, -range, -range), base.add(range, range, range))) {
         BlockState state = mc.world.getBlockState(pos);
         if ((state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST))
            && this.deque.stream().noneMatch(s -> s.getBlockPos().equals(pos))) {
            this.deque.add(new StealState(pos.toImmutable()));
         }
      }
   }

   private void applyState() {
      for (StealState state : this.deque) {
         if (!state.isEmpty()) {
            this.stealState = state;
            return;
         }
      }

      this.stealState = null;
   }

   private boolean isGeneric(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST);
   }

   private boolean isEmpty(GenericContainerScreenHandler handler) {
      return handler.slots.stream().filter(s -> s.inventory != mc.player.getInventory()).noneMatch(Slot::hasStack);
   }

   public void onConnect() {
      this.reconnectTime = System.currentTimeMillis();
   }

   public void onDisconnect() {
      this.reload();
   }

   public void reload() {
      this.deque.clear();
      this.stealState = null;
      this.finished = false;
   }
}
