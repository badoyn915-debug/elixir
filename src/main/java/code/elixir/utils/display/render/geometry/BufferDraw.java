package code.elixir.utils.display.render.geometry;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;

public final class BufferDraw {
   public static void draw(BufferBuilder buffer) {
      BuiltBuffer builtBuffer = buffer.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   private BufferDraw() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
