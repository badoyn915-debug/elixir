package code.elixir.features.impl.misc.serverhelper;

import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class BravoHvHHandler extends ServerHelperHandler {
   private final List<BindSetting> binds;
   private final SliderSettings lowHpThreshold;
   private final BooleanSetting autoMedic;
   private final StopWatch medicTimer = new StopWatch();

   public BravoHvHHandler(List<BindSetting> binds, SliderSettings lowHpThreshold, BooleanSetting autoMedic) {
      this.binds = binds;
      this.lowHpThreshold = lowHpThreshold;
      this.autoMedic = autoMedic;
   }

   @Override
   public String getModeId() {
      return "BravoHvH";
   }

   @Override
   public String getOverlayTitle() {
      return "Bravo";
   }

   @Override
   public List<BindSetting> getBinds() {
      return this.binds;
   }

   @Override
   public List<ServerHelperRenderItem> buildRenderItems(boolean onlyBar) {
      ArrayList<ServerHelperRenderItem> items = new ArrayList<>();
      items.add(
         new ServerHelperRenderItem("Зелье киллера", "K", findSlotByName("киллер") == -1 ? "—" : "✓", Items.SPLASH_POTION)
            .withBind(this.binds.isEmpty() ? -1 : this.binds.get(0).getKey())
      );
      items.add(
         new ServerHelperRenderItem("Зелье медика", "M", findSlotByName("медик") == -1 ? "—" : "✓", Items.SPLASH_POTION)
            .withBind(this.binds.size() > 1 ? this.binds.get(1).getKey() : -1)
      );
      items.add(new ServerHelperRenderItem("Позвать помощь", "H", "✓", Items.SPONGE).withBind(this.binds.size() > 2 ? this.binds.get(2).getKey() : -1));
      return items;
   }

   @Override
   public boolean onKey(int keyCode) {
      if (!this.binds.isEmpty() && mc.player != null) {
         if (this.binds.get(0).getKey() != -1 && this.binds.get(0).getKey() == keyCode) {
            this.usePotion("киллер", "Зелье киллера", false);
            return true;
         } else if (this.binds.size() > 1 && this.binds.get(1).getKey() != -1 && this.binds.get(1).getKey() == keyCode) {
            this.usePotion("медик", "Зелье медика", true);
            return true;
         } else if (this.binds.size() > 2 && this.binds.get(2).getKey() != -1 && this.binds.get(2).getKey() == keyCode) {
            this.sendHelp();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      if (this.autoMedic.isValue() && mc.player != null) {
         int slot;
         if (mc.player.getHealth() <= this.lowHpThreshold.getValue() && this.medicTimer.finished(1000.0) && (slot = findSlotByName("медик")) != -1) {
            this.usePotionSlot(slot, true);
            this.medicTimer.reset();
         }
      }
   }

   private void usePotion(String search, String display, boolean splashDown) {
      int slot = findSlotByName(search);
      if (slot == -1) {
         notify("§c" + display + " не найдено!");
      } else {
         this.usePotionSlot(slot, splashDown);
      }
   }

   private void usePotionSlot(int slot, boolean splashDown) {
      if (mc.player != null && mc.interactionManager != null) {
         float oldPitch = mc.player.getPitch();
         if (splashDown) {
            mc.player.setPitch(90.0F);
         }

         int current = mc.player.getInventory().selectedSlot;
         if (slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = current;
         } else {
            InventoryTask.clickSlot(slot, current, SlotActionType.SWAP, false);
            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            InventoryTask.clickSlot(slot, current, SlotActionType.SWAP, false);
         }

         if (splashDown) {
            mc.player.setPitch(oldPitch);
         }
      }
   }

   private void sendHelp() {
      if (mc.player != null) {
         BlockPos pos = mc.player.getBlockPos();
         mc.player.networkHandler.sendChatMessage("! Помогите, " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
         notify("§aКоординаты отправлены в чат!");
      }
   }

   @Override
   public boolean hasPendingUse() {
      return false;
   }

   @Override
   public int getPendingSlot() {
      return -1;
   }

   @Override
   public void clearPending() {
   }
}
