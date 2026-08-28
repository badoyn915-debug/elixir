package code.elixir.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import org.lwjgl.opengl.GL11;

public class GlowHelper {
   public static final HashMap<Integer, Integer> glowCache = new HashMap<>();

   public static void drawGlow(double x, double y, int width, int height, int glowRadius, Color color) {
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 1.0E-4F);
      GlStateManager._bindTexture(getGlowTexture(width, height, glowRadius));
      width += glowRadius * 2;
      applyColor(color);
      GL11.glBegin(7);
      GL11.glTexCoord2d(0.0, 1.0);
      double var8;
      double var9;
      int var11;
      GL11.glVertex2d(var8 = x - glowRadius, var9 = y - ((var11 = height + glowRadius * 2) - glowRadius));
      GL11.glTexCoord2d(0.0, 0.0);
      GL11.glVertex2d(var8, var9 + var11);
      GL11.glTexCoord2d(1.0, 0.0);
      GL11.glVertex2d(var8 + width, var9 + var11);
      GL11.glTexCoord2d(1.0, 1.0);
      GL11.glVertex2d(var8 + width, var9);
      GL11.glEnd();
      GlStateManager._bindTexture(0);
      GL11.glDisable(3008);
      GlStateManager._disableBlend();
   }

   public static int getGlowTexture(int width, int height, int blurRadius) {
      int identifier = (width * 401 + height) * 407 + blurRadius;
      int texId = glowCache.getOrDefault(identifier, -1);
      if (texId == -1) {
         BufferedImage original = new BufferedImage(width + blurRadius * 2, height + blurRadius * 2, 3);
         Graphics g = original.getGraphics();
         g.setColor(Color.WHITE);
         g.fillRect(blurRadius, blurRadius, width, height);
         g.dispose();
         GlowFilter glow = new GlowFilter(blurRadius);
         BufferedImage blurred = glow.filter(original, null);

         try {
            texId = loadTexture(blurred);
            glowCache.put(identifier, texId);
         } catch (Exception var10) {
            var10.printStackTrace();
         }
      }

      return texId;
   }

   public static void applyColor(Color color) {
      GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
   }

   private static int loadTexture(BufferedImage image) {
      return (int)(System.currentTimeMillis() % 10000L);
   }
}
