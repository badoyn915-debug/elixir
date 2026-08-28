package code.elixir.mixins.accessor;

import java.util.List;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({FontStorage.class})
public interface FontStorageAccessor {
   @Accessor("glyphAtlases")
   List<GlyphAtlasTexture> elixir$getGlyphAtlases();
}
