package code.elixir.features.impl.misc;

import code.elixir.events.container.HandledScreenEvent;
import code.elixir.events.item.ClickSlotEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.math.time.StopWatch;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ItemScroller extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final SliderSettings scrollerSetting = new SliderSettings("Задержка", "Выберите задержку прокрутки предметов").setValue(100.0F).range(0, 200);

   public ItemScroller() {
      super("ItemScroller", "ItemScroller", ModuleCategory.MISC);
      this.setup(new Setting[]{this.scrollerSetting});
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      Slot hoverSlot = e.getSlotHover();
      SlotActionType actionType = PlayerInteractionHelper.isKey(mc.options.dropKey)
         ? SlotActionType.THROW
         : (PlayerInteractionHelper.isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null);
      if (PlayerInteractionHelper.isKey(mc.options.sneakKey)
         && !PlayerInteractionHelper.isKey(mc.options.sprintKey)
         && hoverSlot != null
         && hoverSlot.hasStack()
         && actionType != null
         && this.stopWatch.every(this.scrollerSetting.getValue())) {
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType, mc.player);
      }
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent e) {
      int slotId = e.getSlotId();
      if (slotId >= 0 && slotId <= mc.player.currentScreenHandler.slots.size()) {
         Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
         Item item = slot.getStack().getItem();
         if (item != null
            && PlayerInteractionHelper.isKey(mc.options.sneakKey)
            && PlayerInteractionHelper.isKey(mc.options.sprintKey)
            && this.stopWatch.every(50.0)) {
            InventoryTask.slots()
               .filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
               .forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
         }
      }
   }
}
