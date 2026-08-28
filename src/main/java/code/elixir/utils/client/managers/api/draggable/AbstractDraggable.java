package code.elixir.utils.client.managers.api.draggable;

import code.elixir.Elixir;
import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.events.container.SetScreenEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.file.impl.ElementsFile;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
   private String name;
   private int x;
   private int y;
   private int width;
   private int height;
   private boolean dragging;
   private boolean canDrag;
   private int dragX;
   private int dragY;
   private boolean resizeModeActive = false;
   private long lastClickTime = 0L;
   private static final long DOUBLE_CLICK_DELAY_MS = 300L;
   private static AbstractDraggable currentlyEditing = null;
   private float scaleFactor = 1.0F;
   private float renderX;
   private float renderY;
   private boolean renderInit = false;
   private float swayAngle = 0.0F;
   private long lastMotionTime = 0L;
   public Animation scaleAnimation = new Decelerate().setValue(1.0).setMs(100);

   public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.canDrag = canDrag;
   }

   @Override
   public boolean visible() {
      return true;
   }

   @Override
   public void tick() {
      this.validPosition();
   }

   @Override
   public void packet(PacketEvent e) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.dragging) {
         this.dragX = 0;
         this.dragY = 0;
      }

      float mouseDragX = mouseX + this.dragX;
      float mouseDragY = mouseY + this.dragY;
      if (this.resizeModeActive && this.isHovered(mouseX, mouseY)) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221191));
      }

      if (this.dragging) {
         this.x = (int)mouseDragX;
         this.y = (int)mouseDragY;
         this.renderX = this.x;
         this.renderY = this.y;
         this.clampToScreen();
      }
   }

   @Override
   public void setScreen(SetScreenEvent e) {
      if (!PlayerInteractionHelper.isChat(e.getScreen())) {
         this.dragging = false;
         this.resizeModeActive = false;
         this.dragX = 0;
         this.dragY = 0;
         if (currentlyEditing == this) {
            currentlyEditing = null;
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return button == 0 && this.beginDrag(mouseX, mouseY);
   }

   public boolean beginDrag(double mouseX, double mouseY) {
      if (this.canDrag && this.isHovered(mouseX, mouseY)) {
         this.dragging = true;
         this.dragX = this.x - (int)mouseX;
         this.dragY = this.y - (int)mouseY;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging) {
         try {
            Elixir.getInstance().getFileController().saveFile(ElementsFile.class);
         } catch (Exception var7) {
            var7.printStackTrace();
         }
      }

      this.dragging = false;
      this.dragX = 0;
      this.dragY = 0;
      this.clampToScreen();
      GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
      return false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      return false;
   }

   public void increaseScale() {
      this.scaleFactor = Math.min(2.0F, Math.round((this.scaleFactor + 0.05F) * 100.0F) / 100.0F);
      this.clampToScreen();
   }

   public void decreaseScale() {
      this.scaleFactor = Math.max(0.4F, Math.round((this.scaleFactor - 0.05F) * 100.0F) / 100.0F);
      this.clampToScreen();
   }

   public float getScaleFactor() {
      return this.scaleFactor;
   }

   public void setScaleFactor(float scale) {
      this.scaleFactor = scale;
      this.clampToScreen();
   }

   public void updateEditorMotion() {
      long now = System.currentTimeMillis();
      float dt = this.lastMotionTime == 0L ? 0.0F : (float)(now - this.lastMotionTime) / 1000.0F;
      this.lastMotionTime = now;
      dt = MathHelper.clamp(dt, 0.0F, 0.05F);
      if (!this.renderInit) {
         this.renderX = this.x;
         this.renderY = this.y;
         this.renderInit = true;
      }

      float follow = MathHelper.clamp(dt * 16.0F, 0.0F, 1.0F);
      float prevRenderX = this.renderX;
      this.renderX = this.renderX + (this.x - this.renderX) * follow;
      this.renderY = this.renderY + (this.y - this.renderY) * follow;
      float velocityX = dt > 0.0F ? (this.renderX - prevRenderX) / dt : 0.0F;
      float swayTarget = this.dragging ? MathHelper.clamp(velocityX * 0.011F, -10.0F, 10.0F) : 0.0F;
      float swaySpeed = MathHelper.clamp(dt * 11.0F, 0.0F, 1.0F);
      this.swayAngle = this.swayAngle + (swayTarget - this.swayAngle) * swaySpeed;
      this.clampRenderPosition();
   }

   public void clampToScreen() {
   }

   private void clampRenderPosition() {
   }

   public abstract void drawDraggable(DrawContext var1);

   public void drawRect(float x, float y, float width, float height) {
      Render2D.drawQuad(x, y, width, height, ColorAssist.getText(0.5F));
   }

   public void stopAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   public void startAnimation() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public void validPosition() {
      this.clampToScreen();
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width + 1 && mouseY >= this.y && mouseY <= this.y + this.height + 1;
   }

   public boolean isCloseAnimationFinished() {
      return this.scaleAnimation.isFinished(Direction.BACKWARDS);
   }

   public boolean canDraw(Hud hud, AbstractDraggable draggable) {
      return hud != null && hud.isState() && this.visible();
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   public void setCanDrag(boolean canDrag) {
      this.canDrag = canDrag;
   }

   public void setDragX(int dragX) {
      this.dragX = dragX;
   }

   public void setDragY(int dragY) {
      this.dragY = dragY;
   }

   public void setResizeModeActive(boolean resizeModeActive) {
      this.resizeModeActive = resizeModeActive;
   }

   public void setLastClickTime(long lastClickTime) {
      this.lastClickTime = lastClickTime;
   }

   public void setRenderX(float renderX) {
      this.renderX = renderX;
   }

   public void setRenderY(float renderY) {
      this.renderY = renderY;
   }

   public void setRenderInit(boolean renderInit) {
      this.renderInit = renderInit;
   }

   public void setSwayAngle(float swayAngle) {
      this.swayAngle = swayAngle;
   }

   public void setLastMotionTime(long lastMotionTime) {
      this.lastMotionTime = lastMotionTime;
   }

   public void setScaleAnimation(Animation scaleAnimation) {
      this.scaleAnimation = scaleAnimation;
   }

   public String getName() {
      return this.name;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public boolean isCanDrag() {
      return this.canDrag;
   }

   public int getDragX() {
      return this.dragX;
   }

   public int getDragY() {
      return this.dragY;
   }

   public boolean isResizeModeActive() {
      return this.resizeModeActive;
   }

   public long getLastClickTime() {
      return this.lastClickTime;
   }

   public float getRenderX() {
      return this.renderX;
   }

   public float getRenderY() {
      return this.renderY;
   }

   public boolean isRenderInit() {
      return this.renderInit;
   }

   public float getSwayAngle() {
      return this.swayAngle;
   }

   public long getLastMotionTime() {
      return this.lastMotionTime;
   }

   public Animation getScaleAnimation() {
      return this.scaleAnimation;
   }
}
