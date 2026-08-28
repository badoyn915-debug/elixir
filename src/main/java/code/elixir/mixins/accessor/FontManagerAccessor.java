package code.elixir.mixins.accessor;

import java.util.Map;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({FontManager.class})
public interface FontManagerAccessor {
   @Accessor("fontStorages")
   Map<Identifier, FontStorage> elixir$getFontStorages();
}
