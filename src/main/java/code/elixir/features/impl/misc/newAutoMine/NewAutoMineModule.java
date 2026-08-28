package code.elixir.features.impl.misc.newAutoMine;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;

public class NewAutoMineModule extends Module {
   AutoMineController controller = new AutoMineController();

   public NewAutoMineModule() {
      super("Test Auto Mine", ModuleCategory.MISC);
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      this.controller.onTick();
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
      this.controller.onRotationUpdate(rotationUpdateEvent);
   }

   @EventHandler
   public void onInput(InputEvent inputEvent) {
      this.controller.onInput(inputEvent);
   }
}
