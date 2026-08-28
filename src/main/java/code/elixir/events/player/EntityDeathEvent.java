package code.elixir.events.player;

import code.elixir.utils.client.managers.event.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;

public class EntityDeathEvent implements Event {
   private final Entity entity;
   private final DamageSource source;

   public EntityDeathEvent(Entity entity, DamageSource source) {
      this.entity = entity;
      this.source = source;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public DamageSource getSource() {
      return this.source;
   }
}
