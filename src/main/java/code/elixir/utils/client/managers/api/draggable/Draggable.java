package code.elixir.utils.client.managers.api.draggable;

import code.elixir.events.container.SetScreenEvent;
import code.elixir.events.packet.PacketEvent;
import net.minecraft.client.gui.DrawContext;

public interface Draggable {
   boolean visible();

   void tick();

   void render(DrawContext var1, int var2, int var3, float var4);

   void packet(PacketEvent var1);

   void setScreen(SetScreenEvent var1);

   boolean mouseClicked(double var1, double var3, int var5);

   boolean mouseReleased(double var1, double var3, int var5);

   boolean mouseScrolled(double var1, double var3, double var5);
}
