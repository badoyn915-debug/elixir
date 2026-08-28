package code.elixir.events.item;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotEvent extends EventCancellable {
   private int windowId;
   private int slotId;
   private int button;
   private SlotActionType actionType;

   public int getWindowId() {
      return this.windowId;
   }

   public int getSlotId() {
      return this.slotId;
   }

   public int getButton() {
      return this.button;
   }

   public SlotActionType getActionType() {
      return this.actionType;
   }

   public void setWindowId(int windowId) {
      this.windowId = windowId;
   }

   public void setSlotId(int slotId) {
      this.slotId = slotId;
   }

   public void setButton(int button) {
      this.button = button;
   }

   public void setActionType(SlotActionType actionType) {
      this.actionType = actionType;
   }

   public ClickSlotEvent(int windowId, int slotId, int button, SlotActionType actionType) {
      this.windowId = windowId;
      this.slotId = slotId;
      this.button = button;
      this.actionType = actionType;
   }
}
