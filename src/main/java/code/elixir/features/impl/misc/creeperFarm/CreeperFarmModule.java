package code.elixir.features.impl.misc.creeperFarm;

import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;

public class CreeperFarmModule extends Module {
   private final CreeperFarmController controller = new CreeperFarmController();

   public CreeperFarmModule() {
      super("Creeper Farm", ModuleCategory.MISC);
   }

   @EventHandler
   public void tickEvent(TickEvent tickEvent) {
      this.controller.onTick();
   }

   @EventHandler
   public void rotationUpdateEvent(RotationUpdateEvent rotationUpdateEvent) {
      this.controller.onRotationUpdate();
   }

   public CreeperFarmController getController() {
      return this.controller;
   }
}
