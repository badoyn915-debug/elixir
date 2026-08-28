package code.elixir.features.impl.render.jumpCircle.updater;

import code.elixir.common.animation.Direction;
import code.elixir.features.impl.render.jumpCircle.JumpCircleUtility;
import code.elixir.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.elixir.features.impl.render.jumpCircle.data.JumpCircleStorage;
import code.elixir.features.impl.render.jumpCircle.receiver.JumpCircleModule;

public record JumpCircleUpdater(JumpCircleModule module) {
   public void update() {
      JumpCircleStorage storage = this.module.getStorage();

      for (JumpCircleLayer jumpCircleLayer : storage.getJumpCircleLayers()) {
         if (JumpCircleUtility.isExpired(jumpCircleLayer.startTime(), this.module.getLifeTime().getValue())) {
            jumpCircleLayer.animation().setDirection(Direction.BACKWARDS);
         }
      }

      storage.getJumpCircleLayers().removeIf(layer -> layer.animation().isFinished(Direction.BACKWARDS));
   }
}
