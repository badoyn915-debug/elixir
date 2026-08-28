package code.elixir.utils.display.render.geometry;

import code.elixir.utils.display.atlasfont.providers.ResourceProvider;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class ShaderBox3D {
   private static final float UV_SCALE = 4.0F;
   private static final ShaderProgramKey SHADER_SHADER = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("block_overlay_shader"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );
   private static final ShaderProgramKey PLASMA_SHADER = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("block_plasma"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );
   private static final ShaderProgramKey HEX_SHADER = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("block_hex"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );

   private ShaderBox3D() {
   }

   public static boolean drawShaderShape(VoxelShape shape, ShaderBox3D.ShaderType shaderType, int color, float time, float opacity) {
      return drawShaderShape(shape, null, shaderType, color, time, opacity, Render3D.lastWorldSpaceMatrix);
   }

   public static boolean drawShaderShape(VoxelShape shape, ShaderBox3D.ShaderType shaderType, int color, float time, float opacity, Entry matrixEntry) {
      return drawShaderShape(shape, null, shaderType, color, time, opacity, matrixEntry);
   }

   public static boolean drawShaderShape(
      VoxelShape shape, BlockPos origin, ShaderBox3D.ShaderType shaderType, int color, float time, float opacity, Entry matrixEntry
   ) {
      if (shape != null && !shape.isEmpty() && matrixEntry != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         ShaderProgram shader = RenderSystem.setShader(shaderType.shaderKey);
         if (shader == null) {
            restoreRenderState();
            return false;
         } else {
            if (shader.getUniform("Time") != null) {
               shader.getUniform("Time").set(time);
            }

            if (shader.getUniform("Opacity") != null) {
               shader.getUniform("Opacity").set(opacity);
            }

            double ox = origin != null ? origin.getX() : 0.0;
            double oy = origin != null ? origin.getY() : 0.0;
            double oz = origin != null ? origin.getZ() : 0.0;
            BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Box box : shape.getBoundingBoxes()) {
               Box worldBox = box.offset(ox, oy, oz).expand(0.0025);
               addBox(builder, matrixEntry, worldBox, color);
            }

            BufferDraw.draw(builder);
            restoreRenderState();
            return true;
         }
      } else {
         return false;
      }
   }

   private static void restoreRenderState() {
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void addBox(BufferBuilder builder, Entry entry, Box box, int color) {
      addQuad(builder, entry, 0, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
      addQuad(builder, entry, 0, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
      addQuad(builder, entry, 1, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color);
      addQuad(builder, entry, 1, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, color);
      addQuad(builder, entry, 2, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, box.minX, box.minY, box.minZ, color);
      addQuad(builder, entry, 2, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
   }

   private static void addQuad(
      BufferBuilder builder,
      Entry entry,
      int plane,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      double x3,
      double y3,
      double z3,
      double x4,
      double y4,
      double z4,
      int color
   ) {
      builder.vertex(entry, (float)x1, (float)y1, (float)z1).texture(planeU(plane, x1, y1, z1), planeV(plane, x1, y1, z1)).color(color);
      builder.vertex(entry, (float)x2, (float)y2, (float)z2).texture(planeU(plane, x2, y2, z2), planeV(plane, x2, y2, z2)).color(color);
      builder.vertex(entry, (float)x3, (float)y3, (float)z3).texture(planeU(plane, x3, y3, z3), planeV(plane, x3, y3, z3)).color(color);
      builder.vertex(entry, (float)x4, (float)y4, (float)z4).texture(planeU(plane, x4, y4, z4), planeV(plane, x4, y4, z4)).color(color);
   }

   private static float planeU(int plane, double x, double y, double z) {
      return plane == 2 ? (float)(z * 4.0) : (float)(x * 4.0);
   }

   private static float planeV(int plane, double x, double y, double z) {
      return plane == 0 ? (float)(z * 4.0) : (float)(y * 4.0);
   }

   public static enum ShaderType {
      SHADER(ShaderBox3D.SHADER_SHADER),
      PLASMA(ShaderBox3D.PLASMA_SHADER),
      HEX(ShaderBox3D.HEX_SHADER);

      private final ShaderProgramKey shaderKey;

      private ShaderType(ShaderProgramKey shaderKey) {
         this.shaderKey = shaderKey;
      }
   }
}
