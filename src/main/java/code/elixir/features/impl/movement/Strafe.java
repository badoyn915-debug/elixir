package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.math.task.TaskPriority;
import net.minecraft.client.MinecraftClient;

public class Strafe extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public SelectSetting mode = new SelectSetting("Режим", "Выберите тип стрейфов").value("Матрикс", "Грим").selected("Матрикс");
   SliderSettings speed = new SliderSettings("Скорость", "Выберите скорость для стрейфа").setValue(0.42F).range(0.0F, 1.0F);
   private float lastYaw;
   private float lastPitch;
   private final Turns rot = new Turns(0.0F, 0.0F);

   public Strafe() {
      super("Strafe", "Strafe", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.mode, this.speed});
   }

   public static Strafe getInstance() {
      return Instance.get(Strafe.class);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         boolean moving = Simulations.hasPlayerMovement();
         float yaw = mc.player.getYaw();
         String modeVal = this.mode.getSelected();
         if (modeVal.equals("Матрикс")) {
            if (moving) {
               yaw = Simulations.moveYaw(mc.player.getYaw());
               double motion = this.speed.getValue() * 1.5F;
               Simulations.setVelocity(motion);
            } else {
               Simulations.setVelocity(0.0);
            }

            mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
         } else if (modeVal.equals("Грим") && moving) {
            TurnsConfig.freeCorrection = true;
            yaw = Simulations.moveYaw(mc.player.getYaw());
            this.rot.setYaw(yaw);
            this.rot.setPitch(mc.player.getPitch());
            TurnsConnection.INSTANCE.rotateTo(this.rot, TurnsConfig.DEFAULT, TaskPriority.LOW_PRIORITY, this);
         }

         this.lastYaw = yaw;
         this.lastPitch = 0.0F;
      }
   }

   @Override
   public void activate() {
      super.activate();
      this.lastYaw = mc.player != null ? mc.player.getYaw() : 0.0F;
      this.lastPitch = mc.player != null ? mc.player.getPitch() : 0.0F;
   }
}
