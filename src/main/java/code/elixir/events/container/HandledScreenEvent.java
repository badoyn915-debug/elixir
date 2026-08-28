package code.elixir.events.container;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public class HandledScreenEvent implements Event {
   private DrawContext drawContext;
   private Slot slotHover;
   private int backgroundWidth;
   private int backgroundHeight;
   private int mouseX;
   private int mouseY;
   private float tickDelta;

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public Slot getSlotHover() {
      return this.slotHover;
   }

   public int getBackgroundWidth() {
      return this.backgroundWidth;
   }

   public int getBackgroundHeight() {
      return this.backgroundHeight;
   }

   public int getMouseX() {
      return this.mouseX;
   }

   public int getMouseY() {
      return this.mouseY;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public HandledScreenEvent(DrawContext drawContext, Slot slotHover, int backgroundWidth, int backgroundHeight, int mouseX, int mouseY, float tickDelta) {
      this.drawContext = drawContext;
      this.slotHover = slotHover;
      this.backgroundWidth = backgroundWidth;
      this.backgroundHeight = backgroundHeight;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.tickDelta = tickDelta;
   }
}
