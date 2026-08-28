package code.elixir.utils.display.scissor;

public record ClipBounds(int x, int y, int width, int height) {
   public float bottom() {
      return this.y + this.height;
   }

   public float right() {
      return this.x + this.width;
   }
}
