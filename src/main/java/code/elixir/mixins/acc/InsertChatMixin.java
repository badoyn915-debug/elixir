package code.elixir.mixins.acc;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ChatHud.class})
public interface InsertChatMixin {
   @Invoker("addMessage")
   void invokeAddMessage(ChatHudLine var1);

   @Invoker("addVisibleMessage")
   void invokeAddVisibleMessage(ChatHudLine var1);
}
