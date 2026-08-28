package code.elixir.utils.display.gif;

import code.elixir.utils.display.render.font.glyph.GlyphMap;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GifTextureRenderer {
   private final Identifier gifResource;
   private final List<Identifier> frameTextures = new ArrayList<>();
   private final List<Integer> frameDurationsMs = new ArrayList<>();
   private final List<GifTextureRenderer.PendingFrame> pendingFrames = new ArrayList<>();
   private final Object lock = new Object();
   private volatile boolean loading;
   private volatile boolean ready;
   private long animationStartTime;

   public GifTextureRenderer(Identifier gifResource) {
      this.gifResource = gifResource;
   }

   public void preload() {
      this.ensureLoaded();
   }

   public void preloadEarly() {
      this.ensureLoaded();
   }

   public void render(DrawContext context, float x, float y, float width, float height) {
      this.render(context, x, y, width, height, 1.0F);
   }

   public void render(DrawContext context, float x, float y, float width, float height, float alpha) {
      if (!(alpha <= 0.01F)) {
         this.ensureLoaded();
         this.flushPendingFrames();
         Object object = this.lock;
         Identifier frame;
         synchronized (object) {
            if (!this.ready || this.frameTextures.isEmpty()) {
               return;
            }

            frame = this.getCurrentFrame();
         }

         if (frame != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int alphaBits = (int)(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
            int tint = alphaBits << 24 | 16777215;
            context.drawTexture(
               RenderLayer::getGuiTextured, frame, (int)x, (int)y, 0.0F, 0.0F, (int)width, (int)height, (int)width, (int)height, (int)width, (int)height, tint
            );
         }
      }
   }

   private void ensureLoaded() {
      if (!this.ready || !this.pendingFrames.isEmpty()) {
         if (!this.loading) {
            Object object = this.lock;
            synchronized (object) {
               if (this.loading) {
                  return;
               }

               if (this.ready && this.pendingFrames.isEmpty()) {
                  return;
               }

               this.loading = true;
            }

            CompletableFuture.runAsync(this::decodeAndQueueFrames).whenComplete((ignored, error) -> {
               if (error != null) {
                  this.loading = false;
               }
            });
         }
      }
   }

   private void decodeAndQueueFrames() {
      try {
         try (InputStream input = this.openGifInputStream()) {
            if (input != null) {
               try (ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
                  Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                  if (readers.hasNext()) {
                     ImageReader reader = readers.next();

                     try {
                        reader.setInput(imageInput, false);
                        int frameCount = reader.getNumImages(true);
                        int canvasWidth = 0;
                        int canvasHeight = 0;
                        IIOMetadata streamMetadata = reader.getStreamMetadata();
                        String formatName;
                        if (streamMetadata != null && (formatName = streamMetadata.getNativeMetadataFormatName()) != null) {
                           Node streamRoot = streamMetadata.getAsTree(formatName);
                           Node descriptor = this.findNode(streamRoot, "LogicalScreenDescriptor");
                           canvasWidth = this.getIntAttribute(descriptor, "logicalScreenWidth", 0);
                           canvasHeight = this.getIntAttribute(descriptor, "logicalScreenHeight", 0);
                        }

                        BufferedImage canvas = null;
                        BufferedImage previousCanvas = null;
                        GifTextureRenderer.FrameInfo previousInfo = null;

                        for (int i = 0; i < frameCount; i++) {
                           BufferedImage rawFrame = reader.read(i);
                           GifTextureRenderer.FrameInfo info = this.readFrameInfo(reader.getImageMetadata(i), rawFrame);
                           if (canvas == null) {
                              int width = Math.max(canvasWidth, info.left + rawFrame.getWidth());
                              int height = Math.max(canvasHeight, info.top + rawFrame.getHeight());
                              canvas = new BufferedImage(Math.max(1, width), Math.max(1, height), 2);
                           }

                           this.applyDisposal(canvas, previousCanvas, previousInfo);
                           previousCanvas = "restoreToPrevious".equals(info.disposalMethod) ? this.deepCopy(canvas) : null;
                           Graphics2D graphics = canvas.createGraphics();
                           graphics.setComposite(AlphaComposite.SrcOver);
                           graphics.drawImage(rawFrame, info.left, info.top, null);
                           graphics.dispose();
                           boolean lastFrame = i == frameCount - 1;
                           this.queueFrameRegistration(i, this.deepCopy(canvas), info.delayMs, lastFrame);
                           previousInfo = info;
                        }

                        return;
                     } finally {
                        reader.dispose();
                     }
                  }

                  this.loading = false;
                  return;
               }
            }

            this.loading = false;
         }
      } catch (IOException var30) {
         this.loading = false;
      }
   }

   private void queueFrameRegistration(int index, BufferedImage frame, int durationMs, boolean lastFrame) {
      Runnable task = () -> {
         this.registerFrame(index, frame, durationMs);
         if (lastFrame) {
            this.loading = false;
         }
      };
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         mc.execute(task);
      } else {
         Object object = this.lock;
         synchronized (object) {
            this.pendingFrames.add(new GifTextureRenderer.PendingFrame(index, frame, durationMs, lastFrame));
         }
      }
   }

   private void flushPendingFrames() {
      Object object = this.lock;
      ArrayList<GifTextureRenderer.PendingFrame> toRegister;
      synchronized (object) {
         if (this.pendingFrames.isEmpty()) {
            return;
         }

         toRegister = new ArrayList<>(this.pendingFrames);
         this.pendingFrames.clear();
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null) {
         Object object2 = this.lock;
         synchronized (object2) {
            this.pendingFrames.addAll(0, toRegister);
         }
      } else {
         for (GifTextureRenderer.PendingFrame pending : toRegister) {
            this.registerFrame(pending.index(), pending.frame(), pending.durationMs());
            if (pending.lastFrame()) {
               this.loading = false;
            }
         }
      }
   }

   private void registerFrame(int index, BufferedImage frame, int durationMs) {
      Object object = this.lock;
      synchronized (object) {
         this.ensureCapacity(index + 1);
         Identifier textureId = Identifier.of("elixir", "dynamic/gif/" + this.sanitizePath(this.gifResource.getPath()) + "_" + index);
         GlyphMap.registerBufferedImageTexture(textureId, frame);
         this.frameTextures.set(index, textureId);
         this.frameDurationsMs.set(index, durationMs);
         if (!this.ready) {
            this.animationStartTime = System.currentTimeMillis();
            this.ready = true;
         }
      }
   }

   private void ensureCapacity(int size) {
      while (this.frameTextures.size() < size) {
         this.frameTextures.add(null);
         this.frameDurationsMs.add(0);
      }
   }

   private InputStream openGifInputStream() throws IOException {
      String classpathPath = "/assets/" + this.gifResource.getNamespace() + "/" + this.gifResource.getPath();
      InputStream classpathStream = GifTextureRenderer.class.getResourceAsStream(classpathPath);
      if (classpathStream != null) {
         return classpathStream;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc == null) {
            return null;
         } else {
            Optional resource = mc.getResourceManager().getResource(this.gifResource);
            return resource.isEmpty() ? null : ((Resource)resource.get()).getInputStream();
         }
      }
   }

   private void applyDisposal(BufferedImage canvas, BufferedImage previousCanvas, GifTextureRenderer.FrameInfo previousInfo) {
      if (canvas != null && previousInfo != null) {
         String var4 = previousInfo.disposalMethod;
         switch (var4) {
            case "restoreToBackgroundColor":
               Graphics2D graphics = canvas.createGraphics();
               graphics.setComposite(AlphaComposite.Clear);
               graphics.fillRect(previousInfo.left, previousInfo.top, previousInfo.width, previousInfo.height);
               graphics.dispose();
               break;
            case "restoreToPrevious":
               if (previousCanvas != null) {
                  Graphics2D graphicsx = canvas.createGraphics();
                  graphicsx.setComposite(AlphaComposite.Src);
                  graphicsx.drawImage(previousCanvas, 0, 0, null);
                  graphicsx.dispose();
               }
         }
      }
   }

   private Identifier getCurrentFrame() {
      Object object = this.lock;
      synchronized (object) {
         if (this.frameTextures.isEmpty()) {
            return null;
         } else {
            List<Identifier> available = this.frameTextures.stream().filter(id -> id != null).toList();
            if (available.isEmpty()) {
               return null;
            } else if (available.size() == 1) {
               return available.getFirst();
            } else {
               long elapsed = Math.max(0L, System.currentTimeMillis() - this.animationStartTime);
               long totalDuration = 0L;
               int registeredCount = 0;

               for (int i = 0; i < this.frameDurationsMs.size() && this.frameTextures.get(i) != null; i++) {
                  totalDuration += Math.max(20, this.frameDurationsMs.get(i));
                  registeredCount++;
               }

               if (totalDuration > 0L && registeredCount != 0) {
                  long loopTime = elapsed % totalDuration;
                  long accumulated = 0L;

                  for (int i = 0; i < registeredCount; i++) {
                     if (loopTime < (accumulated += Math.max(20, this.frameDurationsMs.get(i)))) {
                        return this.frameTextures.get(i);
                     }
                  }

                  return this.frameTextures.get(registeredCount - 1);
               } else {
                  return available.getFirst();
               }
            }
         }
      }
   }

   private GifTextureRenderer.FrameInfo readFrameInfo(IIOMetadata metadata, BufferedImage frame) {
      int left = 0;
      int top = 0;
      int delayMs = 100;
      String disposalMethod = "none";
      String formatName;
      if (metadata != null && (formatName = metadata.getNativeMetadataFormatName()) != null) {
         Node root = metadata.getAsTree(formatName);
         Node imageDescriptor = this.findNode(root, "ImageDescriptor");
         left = this.getIntAttribute(imageDescriptor, "imageLeftPosition", 0);
         top = this.getIntAttribute(imageDescriptor, "imageTopPosition", 0);
         Node graphicControl = this.findNode(root, "GraphicControlExtension");
         delayMs = Math.max(20, this.getIntAttribute(graphicControl, "delayTime", 10) * 10);
         disposalMethod = this.getStringAttribute(graphicControl, "disposalMethod", "none");
      }

      return new GifTextureRenderer.FrameInfo(left, top, frame.getWidth(), frame.getHeight(), delayMs, disposalMethod);
   }

   private int getIntAttribute(Node node, String attribute, int fallback) {
      String value = this.getStringAttribute(node, attribute, null);
      if (value == null) {
         return fallback;
      } else {
         try {
            return Integer.parseInt(value);
         } catch (NumberFormatException var6) {
            return fallback;
         }
      }
   }

   private String getStringAttribute(Node node, String attribute, String fallback) {
      if (node == null) {
         return fallback;
      } else {
         NamedNodeMap attributes = node.getAttributes();
         if (attributes == null) {
            return fallback;
         } else {
            Node value = attributes.getNamedItem(attribute);
            return value != null ? value.getNodeValue() : fallback;
         }
      }
   }

   private Node findNode(Node node, String name) {
      if (node == null) {
         return null;
      } else if (name.equals(node.getNodeName())) {
         return node;
      } else {
         NodeList children = node.getChildNodes();

         for (int i = 0; i < children.getLength(); i++) {
            Node found = this.findNode(children.item(i), name);
            if (found != null) {
               return found;
            }
         }

         return null;
      }
   }

   private BufferedImage deepCopy(BufferedImage image) {
      BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), 2);
      Graphics2D graphics = copy.createGraphics();
      graphics.setComposite(AlphaComposite.Src);
      graphics.drawImage(image, 0, 0, null);
      graphics.dispose();
      return copy;
   }

   private String sanitizePath(String path) {
      return path.replace('/', '_').replace('\\', '_').replace('.', '_');
   }

   private record FrameInfo(int left, int top, int width, int height, int delayMs, String disposalMethod) {
   }

   private record PendingFrame(int index, BufferedImage frame, int durationMs, boolean lastFrame) {
   }
}
