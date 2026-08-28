package code.elixir.features.impl.render.jumpCircle.data;

import code.elixir.common.animation.implement.OutBack;
import code.elixir.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class JumpCircleStorage {
   private final List<JumpCircleLayer> jumpCircleLayers = new CopyOnWriteArrayList<>();
   private final JumpCircleModule module;

   public JumpCircleStorage(JumpCircleModule module) {
      this.module = module;
   }

   public void add(Vec3d vec3d) {
      int lifeTime = (int)this.module.getLifeTime().getValue();
      this.jumpCircleLayers.add(new JumpCircleLayer(vec3d, new OutBack().setMs(lifeTime).setValue(1.0)));
   }

   public void add(PlayerEntity player) {
      this.add(player.getPos().add(0.0, 0.01F, 0.0));
   }

   public boolean isPresent() {
      return !this.jumpCircleLayers.isEmpty();
   }

   public List<JumpCircleLayer> getJumpCircleLayers() {
      return this.jumpCircleLayers;
   }

   public JumpCircleModule getModule() {
      return this.module;
   }
}
