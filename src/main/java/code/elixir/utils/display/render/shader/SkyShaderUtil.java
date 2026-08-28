package code.elixir.utils.display.render.shader;

import net.minecraft.util.Identifier;

public class SkyShaderUtil {
   public static Identifier getShader(String name) {
      return Identifier.of("elixir", "core/" + name);
   }
}
