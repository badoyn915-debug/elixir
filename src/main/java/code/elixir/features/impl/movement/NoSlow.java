package code.elixir.features.impl.movement;

import code.elixir.events.player.EventNoSlow;
import code.elixir.events.player.MotionEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemUseHelper;
import code.elixir.utils.math.script.Script;
import code.elixir.utils.math.time.StopWatch;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class NoSlow extends Module {
   private final StopWatch notifWatch = new StopWatch();
   private final Script script = new Script();
   private boolean finish;
   public final SelectSetting itemMode = new SelectSetting("Режим", "Выберите режим обхода")
      .value("Новый Грим", "Старый Грим", "Грим", "Грим тики", "Грим2", "LonyGrief");
   public final BooleanSetting NoGepl = new BooleanSetting("Не работать на геплы и чарки", "").visible(() -> this.itemMode.isSelected("Грим"));
   private int ticks = 0;

   public static NoSlow getInstance() {
      return Instance.get(NoSlow.class);
   }

   public NoSlow() {
      super("NoSlow", "NoSlow", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.itemMode, this.NoGepl});
   }

   @EventHandler
   public void onUpdate(TickEvent event) {
      if (this.itemMode.isSelected("Грим")) {
         this.handleGrimLast();
      }

      if (this.itemMode.isSelected("LonyGrief")) {
         this.handleLonyGriefUpdate();
      }

      this.ticks = mc.player.getActiveHand() != Hand.MAIN_HAND && mc.player.getActiveHand() != Hand.OFF_HAND ? 0 : ++this.ticks;
   }

   @EventHandler
   public void onNoslow(EventNoSlow e) {
      if (mc.player != null && mc.player.isUsingItem()) {
         ItemStack activeStack = mc.player.getActiveItem();
         if (!ItemUseHelper.isPriorityUseItem(activeStack)) {
            Hand first = mc.player.getActiveHand();
            Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            String var6 = this.itemMode.getSelected();
            switch (var6) {
               case "Старый Грим":
                  if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                     PlayerInteractionHelper.interactItem(first);
                     PlayerInteractionHelper.interactItem(second);
                     e.cancel();
                  }
                  break;
               case "Новый Грим":
                  if (mc.player.getItemUseTime() < 7) {
                     InventoryTask.updateSlots();
                     InventoryTask.closeScreen(true);
                  } else {
                     e.cancel();
                  }
                  break;
               case "Грим тики":
                  if (this.NoGepl.isValue() && (activeStack.getItem() == Items.GOLDEN_APPLE || activeStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
                     return;
                  }

                  if (this.ticks > 1.0F && mc.player.getItemUseTime() > 1) {
                     e.cancel();
                     this.ticks = 0;
                  }
                  break;
               case "Грим":
                  if (this.NoGepl.isValue() && (activeStack.getItem() == Items.GOLDEN_APPLE || activeStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
                     return;
                  }

                  if (this.handleGrimLastNoslow()) {
                     e.cancel();
                  }
                  break;
               case "Грим2":
                  e.cancel();
                  break;
               case "LonyGrief":
                  this.handleLonyGriefNoSlow(e);
            }
         }
      }
   }

   private boolean handleGrimLastNoslow() {
      if (mc.player == null) {
         return false;
      } else {
         int useTime = mc.player.getItemUseTime();
         return useTime < 4 ? false : useTime > 4;
      }
   }

   private void handleGrimLast() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         ItemStack activeStack = mc.player.getActiveItem();
         if (!ItemUseHelper.isPriorityUseItem(activeStack)) {
            int useTime = mc.player.getItemUseTime();
            if (useTime == 4) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
            }
         }
      }
   }

   private void handleLonyGriefNoSlow(EventNoSlow event) {
      if (mc.player != null) {
         Hand activeHand = mc.player.getActiveHand();
         if (activeHand == Hand.OFF_HAND) {
            if (this.ticks % 2 == 0 && !mc.player.isSneaking()) {
               event.cancel();
            }
         } else {
            if (mc.player.getItemUseTime() > 0) {
               event.cancel();
            }
         }
      }
   }

   private void handleLonyGriefUpdate() {
      if (mc.player != null && mc.player.networkHandler != null) {
         if (mc.player.isUsingItem() && mc.player.getItemUseTime() == 0) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
         }
      }
   }

   @EventHandler
   public void onMotion(MotionEvent event) {
      if (mc.player.isUsingItem() && this.itemMode.isSelected("Грим2")) {
         if (!ItemUseHelper.isPriorityUseItem(mc.player.getActiveItem())) {
            Hand hand = mc.player.getActiveHand();
            if (hand == Hand.MAIN_HAND) {
               mc.player
                  .networkHandler
                  .sendPacket(
                     new PlayerInteractItemC2SPacket(
                        Hand.OFF_HAND, 0, TurnsConnection.INSTANCE.getRotation().getYaw(), TurnsConnection.INSTANCE.getRotation().getPitch()
                     )
                  );
            } else if (hand == Hand.OFF_HAND) {
               mc.player
                  .networkHandler
                  .sendPacket(
                     new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND, 0, TurnsConnection.INSTANCE.getRotation().getYaw(), TurnsConnection.INSTANCE.getRotation().getPitch()
                     )
                  );
            }
         }
      }
   }
}
