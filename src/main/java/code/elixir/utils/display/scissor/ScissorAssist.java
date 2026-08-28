package code.elixir.utils.display.scissor;

import code.elixir.utils.display.interfaces.QuickImports;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Stack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ScissorAssist implements QuickImports {
   private final Pool<ScissorAssist.Scissor> scissorPool = new Pool<>(ScissorAssist.Scissor::new);
   private final Stack<ScissorAssist.Scissor> scissorStack = new Stack<>();

   public ClipBounds getCurrentBounds() {
      if (this.scissorStack.isEmpty()) {
         return null;
      } else {
         ScissorAssist.Scissor scissor = this.scissorStack.peek();
         return new ClipBounds(scissor.x, scissor.y, scissor.width, scissor.height);
      }
   }

   public void push(Matrix4f matrix4f, float x, float y, float width, float height) {
      ScissorAssist.Scissor newScissor = this.scissorPool.get().copy();
      Vector3f pos = matrix4f.transformPosition(x, y, 0.0F, new Vector3f());
      Vector3f size = matrix4f.getScale(new Vector3f()).mul(width, height, 0.0F);
      newScissor.set(pos.x, pos.y, size.x, size.y);
      if (!this.scissorStack.isEmpty()) {
         ScissorAssist.Scissor currentScissor = this.scissorStack.peek().copy();
         int newX = Math.max(currentScissor.x, newScissor.x);
         int newY = Math.max(currentScissor.y, newScissor.y);
         int newWidth = Math.min(currentScissor.x + currentScissor.width, newScissor.x + newScissor.width) - newX;
         int newHeight = Math.min(currentScissor.y + currentScissor.height, newScissor.y + newScissor.height) - newY;
         if (newWidth > 0 && newHeight > 0) {
            newScissor.set(newX, newY, newWidth, newHeight);
         } else {
            newScissor.set(0.0, 0.0, 0.0, 0.0);
         }
      }

      this.scissorStack.push(newScissor);
      this.setScissor(newScissor);
   }

   public void pop() {
      if (!this.scissorStack.isEmpty()) {
         this.scissorPool.free(this.scissorStack.pop());
         if (this.scissorStack.isEmpty()) {
            RenderSystem.disableScissor();
         } else {
            this.setScissor(this.scissorStack.peek());
         }
      }
   }

   private void setScissor(ScissorAssist.Scissor scissor) {
      int scaleFactor = (int)window.getScaleFactor();
      int x = scissor.x * scaleFactor;
      int width = Math.max(0, scissor.width * scaleFactor);
      int height = Math.max(0, scissor.height * scaleFactor);
      int y = window.getHeight() - (scissor.y * scaleFactor + height);
      RenderSystem.enableScissor(x, y, width, height);
   }

   private static class Scissor {
      public int x;
      public int y;
      public int width;
      public int height;

      public void set(double x, double y, double width, double height) {
         this.x = Math.max(0, (int)Math.round(x));
         this.y = Math.max(0, (int)Math.round(y));
         this.width = Math.max(0, (int)Math.round(width));
         this.height = Math.max(0, (int)Math.round(height));
      }

      ScissorAssist.Scissor copy() {
         ScissorAssist.Scissor newScissor = new ScissorAssist.Scissor();
         newScissor.set(this.x, this.y, this.width, this.height);
         return newScissor;
      }
   }
}
