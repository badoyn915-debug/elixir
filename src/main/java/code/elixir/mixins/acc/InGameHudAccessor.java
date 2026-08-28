package code.elixir.mixins.acc;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({InGameHud.class})
public interface InGameHudAccessor {
   @Accessor("FOOD_FULL_TEXTURE")
   Identifier getFoodFullTexture();

   @Accessor("FOOD_HALF_TEXTURE")
   Identifier getFoodHalfTexture();

   @Accessor("FOOD_EMPTY_TEXTURE")
   Identifier getFoodEmptyTexture();

   @Accessor("FOOD_FULL_HUNGER_TEXTURE")
   Identifier getFoodFullHungerTexture();

   @Accessor("FOOD_HALF_HUNGER_TEXTURE")
   Identifier getFoodHalfHungerTexture();

   @Accessor("FOOD_EMPTY_HUNGER_TEXTURE")
   Identifier getFoodEmptyHungerTexture();
}
