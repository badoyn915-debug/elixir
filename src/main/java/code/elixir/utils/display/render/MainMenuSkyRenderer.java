package code.elixir.utils.display.render;

import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shader.SkyShaderUtil;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class MainMenuSkyRenderer {
   private static ShaderProgramKey SHADER = null;
   private static boolean shaderInitialized = false;
   private static long startMillis = -1L;
   private static long lastFrameMillis = -1L;

   // Smooth mouse FOV / Parallax with heavy cinematic damping
   private static float smoothFovX = 0.0F;
   private static float smoothFovY = 0.0F;
   private static boolean fovInitialized = false;

   public static void interactBubble(float nx, float ny, boolean isSpawn) {
   }

   public static void render(DrawContext context, int width, int height) {
      render(context, width, height, 1.0F);
   }

   public static void render(DrawContext context, int width, int height, float alpha) {
      if (!shaderInitialized) {
         try {
            SHADER = new ShaderProgramKey(
               SkyShaderUtil.getShader("post/mainmenu/mainmenu_sky"),
               VertexFormats.POSITION_TEXTURE_COLOR,
               Defines.EMPTY
            );
         } catch (Exception e) {
            System.err.println("Failed to load procedural marble background shader: " + e.getMessage());
         }
         shaderInitialized = true;
      }

      long now = System.currentTimeMillis();
      if (startMillis < 0L) {
         startMillis = now;
         lastFrameMillis = now;
      }

      float time = (float)(now - startMillis) / 1000.0F;
      float dt = MathHelper.clamp((float)(now - lastFrameMillis) / 1000.0F, 0.001F, 0.05F);
      lastFrameMillis = now;

      MinecraftClient mc = MinecraftClient.getInstance();
      float fw = (float)mc.getWindow().getFramebufferWidth();
      float fh = (float)mc.getWindow().getFramebufferHeight();
      double mouseRawX = mc.mouse.getX();
      double mouseRawY = mc.mouse.getY();

      // Normalize mouse to [-0.5, 0.5]
      float targetNormX = MathHelper.clamp((float)(mouseRawX / Math.max(1.0, fw)) - 0.5F, -0.5F, 0.5F);
      float targetNormY = MathHelper.clamp((float)(mouseRawY / Math.max(1.0, fh)) - 0.5F, -0.5F, 0.5F);

      if (!fovInitialized) {
         smoothFovX = targetNormX * 2.0F;
         smoothFovY = targetNormY * 2.0F;
         fovInitialized = true;
      }

      // Smooth exponential damping (completely eliminates jitter, gives heavy liquid inertia)
      float fovRate = 1.0F - (float)Math.exp(-2.2 * dt);
      smoothFovX = MathHelper.lerp(fovRate, smoothFovX, targetNormX * 2.0F);
      smoothFovY = MathHelper.lerp(fovRate, smoothFovY, targetNormY * 2.0F);

      boolean shaderRendered = false;

      if (SHADER != null) {
         MatrixStack matrices = context.getMatrices();
         Entry entry = matrices.peek();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);

         try {
            ShaderProgram shader = RenderSystem.setShader(SHADER);
            if (shader != null) {
               if (shader.getUniform("uTime") != null) {
                  shader.getUniform("uTime").set(time);
               }
               if (shader.getUniform("uResolution") != null) {
                  shader.getUniform("uResolution").set(fw, fh);
               }
               if (shader.getUniform("uMouseFov") != null) {
                  shader.getUniform("uMouseFov").set(smoothFovX, smoothFovY);
               }

               BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               int color = -1;
               float fWidth = (float)width;
               float fHeight = (float)height;
               builder.vertex(entry, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(color);
               builder.vertex(entry, 0.0F, fHeight, 0.0F).texture(0.0F, 1.0F).color(color);
               builder.vertex(entry, fWidth, fHeight, 0.0F).texture(1.0F, 1.0F).color(color);
               builder.vertex(entry, fWidth, 0.0F, 0.0F).texture(1.0F, 0.0F).color(color);
               BufferDraw.draw(builder);

               shaderRendered = true;
            }
         } catch (Throwable ignored) {
         } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      if (!shaderRendered) {
         renderFallbackGradient(context, (float)width, (float)height);
      }
   }

   private static void renderFallbackGradient(DrawContext context, float fWidth, float fHeight) {
      MatrixStack matrices = context.getMatrices();
      Matrix4f mat = matrices.peek().getPositionMatrix();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder bgBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      int topColor = 0xFF0A0A0D;
      int midColor = 0xFF141418;
      int botColor = 0xFF050507;

      bgBuffer.vertex(mat, 0.0F, 0.0F, 0.0F).color(topColor);
      bgBuffer.vertex(mat, 0.0F, fHeight * 0.55F, 0.0F).color(midColor);
      bgBuffer.vertex(mat, fWidth, fHeight * 0.55F, 0.0F).color(midColor);
      bgBuffer.vertex(mat, fWidth, 0.0F, 0.0F).color(topColor);

      bgBuffer.vertex(mat, 0.0F, fHeight * 0.55F, 0.0F).color(midColor);
      bgBuffer.vertex(mat, 0.0F, fHeight, 0.0F).color(botColor);
      bgBuffer.vertex(mat, fWidth, fHeight, 0.0F).color(botColor);
      bgBuffer.vertex(mat, fWidth, fHeight * 0.55F, 0.0F).color(midColor);
      BufferDraw.draw(bgBuffer);

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }
}
