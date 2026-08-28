package code.elixir.utils.display.gif;

import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Image;
import code.elixir.utils.math.frame.FrameRateCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class GifRender {
   private int currentFrame = 0;
   private float frameTime = 0.0F;
   private final float frameDuration = 0.0015F;
   private final String[] frames;
   private final Image image;

   public GifRender(String path, int frameCount) {
      this.frames = new String[frameCount];

      for (int i = 0; i < frameCount; i++) {
         this.frames[i] = String.format("minecraft:gif/backgrounds/mainmenutype1/%05d.png", i + 1);
      }

      this.image = new Image();
   }

   public void render(MatrixStack matrix, float x, float y, float width, float height) {
      if (MinecraftClient.getInstance().isWindowFocused()) {
         this.frameTime = this.frameTime + (FrameRateCounter.INSTANCE.getFps() > 0 ? 1.0F / FrameRateCounter.INSTANCE.getFps() : 0.006F);
         if (this.frameTime >= 0.0015F) {
            this.currentFrame = (this.currentFrame + 1) % this.frames.length;
            this.frameTime = 0.0F;
         }

         this.image.setTexture(this.frames[this.currentFrame]).render(ShapeProperties.create(matrix, x, y, width, height).color(-1).build());
      }
   }
}
