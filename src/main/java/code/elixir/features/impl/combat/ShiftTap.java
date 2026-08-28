package code.elixir.features.impl.combat;

import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.client.MinecraftClient;

public class ShiftTap extends Module {
   private long shiftTapEndTime = 0L;
   private boolean isModuleControllingSneak = false;
   private int shiftTapDuration = 100;
   private final MinecraftClient mc = MinecraftClient.getInstance();

   public ShiftTap() {
      super("ShiftTap", "ShiftTap", ModuleCategory.COMBAT);
   }

   private void startShiftTap() {
      this.shiftTapEndTime = System.currentTimeMillis() + this.shiftTapDuration;
      if (!this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(true);
         this.isModuleControllingSneak = true;
      }
   }

   private void stopShiftTap() {
      if (this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(false);
         this.isModuleControllingSneak = false;
      }
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (this.mc.player != null) {
         this.startShiftTap();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.mc.player != null && !this.mc.player.isSpectator()) {
         long currentTime = System.currentTimeMillis();
         if (this.isModuleControllingSneak && currentTime > this.shiftTapEndTime) {
            this.stopShiftTap();
         }
      } else {
         this.stopShiftTap();
      }
   }
}
