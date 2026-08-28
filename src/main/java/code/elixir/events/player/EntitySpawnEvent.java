package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.Entity;

public class EntitySpawnEvent extends EventCancellable {
   private Entity entity;

   public EntitySpawnEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }
}
