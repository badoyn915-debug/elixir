package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;

public class AntiAFK extends Module {
   private final SliderSettings moveDelay = new SliderSettings("Задержка перед движением", "Задержка в секундах").setValue(59.0F).range(1.0F, 60.0F).step(0.5F);
   private long lastSpecialMoveTime = 0L;
   private boolean performingSpecialMove = false;
   private long specialMoveStartTime = 0L;
   private int specialMovePhase = 0;

   public AntiAFK() {
      super("AntiAFK", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.moveDelay});
   }

   @Override
   public void activate() {
      this.lastSpecialMoveTime = System.currentTimeMillis();
      this.performingSpecialMove = false;
      this.specialMovePhase = 0;
      super.activate();
   }

   @Override
   public void deactivate() {
      this.resetAllKeys();
      super.deactivate();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.handleMovementMode(System.currentTimeMillis());
      }
   }

   private void handleMovementMode(long currentTime) {
      if (this.performingSpecialMove) {
         long specialMoveElapsed = currentTime - this.specialMoveStartTime;
         switch (this.specialMovePhase) {
            case 0:
               this.resetAllKeys();
               mc.options.forwardKey.setPressed(true);
               if (specialMoveElapsed >= 280L) {
                  this.specialMovePhase = 1;
                  this.specialMoveStartTime = currentTime;
                  this.resetAllKeys();
               }
               break;
            case 1:
               this.resetAllKeys();
               mc.options.backKey.setPressed(true);
               if (specialMoveElapsed >= 350L) {
                  this.specialMovePhase = 2;
                  this.performingSpecialMove = false;
                  this.resetAllKeys();
               }
         }
      } else {
         long delayMs = (long)(this.moveDelay.getValue() * 1000.0F);
         if (currentTime - this.lastSpecialMoveTime >= delayMs) {
            this.performingSpecialMove = true;
            this.specialMoveStartTime = currentTime;
            this.specialMovePhase = 0;
            this.lastSpecialMoveTime = currentTime;
            this.resetAllKeys();
         }
      }
   }

   private void resetAllKeys() {
      mc.options.forwardKey.setPressed(false);
      mc.options.backKey.setPressed(false);
      mc.options.leftKey.setPressed(false);
      mc.options.rightKey.setPressed(false);
   }
}
