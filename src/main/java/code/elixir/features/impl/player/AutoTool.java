package code.elixir.features.impl.player;

import code.elixir.events.block.BlockBreakingEvent;
import code.elixir.events.keyboard.HotBarScrollEvent;
import code.elixir.events.player.HotBarUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.ItemRendererEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.script.Script;
import code.elixir.utils.math.time.StopWatch;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoTool extends Module {
   private final StopWatch swap = new StopWatch();
   private final StopWatch breaking = new StopWatch();
   private final Script script = new Script();
   private final Script swapBackScript = new Script();
   private final RadioSetting mode = new RadioSetting("Режим", "Режим AutoTool", new String[]{"Обычный", "Пакетный"}, "Обычный");
   private ItemStack renderStack;
   private BlockPos lastBreakPos;
   private int previousSelectedSlot = -1;
   private long lastPacketBreakAt = 0L;

   public AutoTool() {
      super("AutoTool", "AutoTool", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.mode});
   }

   @EventHandler
   public void onItemRenderer(ItemRendererEvent e) {
      if (this.renderStack != null && e.getHand().equals(Hand.MAIN_HAND) && Objects.equals(mc.player, e.getPlayer())) {
         e.setStack(this.renderStack);
      }
   }

   @EventHandler
   public void onHotBarUpdate(HotBarUpdateEvent e) {
      if (!this.swapBackScript.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent e) {
      if (!this.swapBackScript.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onBlockBreaking(BlockBreakingEvent e) {
      this.breaking.reset();
      this.lastBreakPos = e.blockPos();
      if ("Пакетный".equals(this.mode.get())) {
         this.tryPacketBreak(e.blockPos(), e.direction());
      } else {
         Slot currentBestSlot;
         if (!mc.player.isCreative()
            && this.swapBackScript.isFinished()
            && this.swap.finished(350.0)
            && (currentBestSlot = this.findBestTool(this.lastBreakPos)) != null
            && currentBestSlot != InventoryTask.mainHandSlot()) {
            int hotbarSlot = this.getHotbarSlotFromSlot(currentBestSlot);
            if (hotbarSlot != -1) {
               if (this.previousSelectedSlot == -1) {
                  this.previousSelectedSlot = mc.player.getInventory().selectedSlot;
               }

               InventoryTask.switchTo(hotbarSlot);
               this.swapBackScript.cleanup().addTickStep(0, () -> {
                  if (this.previousSelectedSlot != -1) {
                     InventoryTask.switchTo(this.previousSelectedSlot);
                     this.previousSelectedSlot = -1;
                  }
               });
            } else {
               this.renderStack = mc.player.getMainHandStack();
               InventoryTask.swapHand(currentBestSlot, Hand.MAIN_HAND, true);
               this.swapBackScript.cleanup().addTickStep(0, () -> InventoryTask.swapHand(currentBestSlot, Hand.MAIN_HAND, true, true));
            }

            this.swap.reset();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.script.update();
      Slot currentBestSlot;
      if (!this.swapBackScript.isFinished()
         && this.swap.finished(350.0)
         && ((currentBestSlot = this.findBestTool(this.lastBreakPos)) != InventoryTask.mainHandSlot() || this.breaking.finished(100.0))) {
         if ((currentBestSlot == InventoryTask.mainHandSlot() || this.breaking.finished(100.0)) && this.previousSelectedSlot != -1) {
            InventoryTask.switchTo(this.previousSelectedSlot);
            this.previousSelectedSlot = -1;
         }

         this.script.cleanup().addTickStep(4, () -> this.renderStack = null);
         this.swapBackScript.update();
         this.swap.reset();
      }
   }

   private Slot findBestTool(BlockPos blockPos) {
      BlockState state = mc.world.getBlockState(blockPos);
      if (PlayerInteractionHelper.isAir(state)) {
         return InventoryTask.mainHandSlot();
      } else {
         Slot bestHotbarTool = null;
         double bestHotbarSpeed = 0.0;

         for (int i = 0; i < 9; i++) {
            int savedSelectedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = i;
            Slot hotbarSlot = InventoryTask.mainHandSlot();
            mc.player.getInventory().selectedSlot = savedSelectedSlot;
            double speed;
            if (hotbarSlot != null
               && !hotbarSlot.getStack().isEmpty()
               && (speed = hotbarSlot.getStack().getMiningSpeedMultiplier(state)) != 1.0
               && speed > bestHotbarSpeed) {
               bestHotbarSpeed = speed;
               bestHotbarTool = hotbarSlot;
            }
         }

         return bestHotbarTool != null && bestHotbarSpeed > 1.0
            ? bestHotbarTool
            : InventoryTask.slots()
               .sorted(Comparator.comparing(slot -> slot.equals(InventoryTask.mainHandSlot())))
               .filter(s -> s.getStack().getMiningSpeedMultiplier(state) != 1.0F)
               .max(Comparator.comparingDouble(s -> s.getStack().getMiningSpeedMultiplier(state)))
               .orElse(null);
      }
   }

   private void tryPacketBreak(BlockPos pos, Direction direction) {
      if (mc.player != null && mc.getNetworkHandler() != null && !mc.player.isCreative()) {
         long now = System.currentTimeMillis();
         if (now - this.lastPacketBreakAt >= 75L) {
            Slot bestSlot = this.findBestTool(pos);
            int hotbarSlot = this.getHotbarSlotFromSlot(bestSlot);
            if (hotbarSlot != -1) {
               int currentSlot = mc.player.getInventory().selectedSlot;
               if (hotbarSlot != currentSlot) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                  mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction));
                  mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction));
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
                  this.lastPacketBreakAt = now;
               }
            }
         }
      }
   }

   private int getHotbarSlotFromSlot(Slot slot) {
      if (slot != null && mc.player != null) {
         int i = 0;

         while (i < 9) {
            int savedSelectedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = i++;
            Slot hotbarSlot = InventoryTask.mainHandSlot();
            mc.player.getInventory().selectedSlot = savedSelectedSlot;
            if (slot.equals(hotbarSlot)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }
}
