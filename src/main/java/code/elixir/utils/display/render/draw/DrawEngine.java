package code.elixir.utils.display.render.draw;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix4f;
import org.joml.Vector4i;

public interface DrawEngine {
   void quad(Matrix4f var1, BufferBuilder var2, float var3, float var4, float var5, float var6);

   void quad(Matrix4f var1, BufferBuilder var2, float var3, float var4, float var5, float var6, int var7);

   void quadTexture(Entry var1, BufferBuilder var2, float var3, float var4, float var5, float var6, Vector4i var7);

   void quad(Matrix4f var1, float var2, float var3, float var4, float var5, int var6);
}
