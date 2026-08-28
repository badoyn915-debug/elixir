package code.elixir.display.screens.clickgui.animation;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.events.render.DrawEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import net.minecraft.client.MinecraftClient;

public class CloseAnimationOverlay {
   private static CloseAnimationOverlay INSTANCE;
   private final CloseAnimationController controller = new CloseAnimationController();
   private boolean active = false;

   private CloseAnimationOverlay() {
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   public static CloseAnimationOverlay getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new CloseAnimationOverlay();
      }

      return INSTANCE;
   }

   public void start() {
      MinecraftClient mc = MinecraftClient.getInstance();
      float yaw = mc.player != null ? mc.player.getYaw() : 0.0F;
      float pitch = mc.player != null ? mc.player.getPitch() : 0.0F;
      this.controller.reset();
      this.controller.startClosing(yaw, pitch);
      this.active = true;
   }

   public void stop() {
      this.active = false;
      this.controller.reset();
   }

   public void reset() {
      this.stop();
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (this.active) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null) {
            this.controller.updateCamera(mc.player.getYaw(), mc.player.getPitch());
         }

         this.controller.update();
         if (this.controller.isFinished()) {
            this.active = false;
            this.controller.reset();
         } else {
            MenuScreen.getInstance().render(event.getDrawContext(), -100, -100, event.getPartialTicks());
         }
      }
   }

   public CloseAnimationController getController() {
      return this.controller;
   }

   public boolean isActive() {
      return this.active;
   }
}
