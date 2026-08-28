package code.elixir.features.impl.render.zenisesp;

import net.minecraft.util.math.Vec3d;

public class StickSegment {
   public Vec3d pos = Vec3d.ZERO;
   public Vec3d prevPos = Vec3d.ZERO;
   public Vec3d renderPos = Vec3d.ZERO;
   public Vec3d prevRenderPos = Vec3d.ZERO;
   public Vec3d velocity = Vec3d.ZERO;

   public StickSegment(Vec3d initialPos) {
      this.pos = initialPos;
      this.prevPos = initialPos;
      this.renderPos = initialPos;
      this.prevRenderPos = initialPos;
   }

   public void set(Vec3d newPos) {
      this.prevPos = this.pos;
      this.pos = newPos;
   }

   public void syncRender() {
      this.prevRenderPos = this.renderPos;
      this.renderPos = this.pos;
   }

   public Vec3d getInterpolated(float partialTicks) {
      return new Vec3d(
         this.prevRenderPos.x + (this.renderPos.x - this.prevRenderPos.x) * partialTicks,
         this.prevRenderPos.y + (this.renderPos.y - this.prevRenderPos.y) * partialTicks,
         this.prevRenderPos.z + (this.renderPos.z - this.prevRenderPos.z) * partialTicks
      );
   }
}
