package code.elixir.mixins;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPlayerEntity.class})
public interface ClientPlayerEntityAccessor {
   @Accessor("lastSprinting")
   boolean getLastSprinting();

   @Accessor("lastSprinting")
   void setLastSprinting(boolean var1);
}
