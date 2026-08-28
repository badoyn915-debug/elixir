package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.input.MoveUtil;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.simulate.Simulations;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public class NoWeb extends Module {
   public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода").value("Грим", "Грим новый");
   private SliderSettings speedy = new SliderSettings("Скорость Y", "Выберите скорость для стрейфа")
      .setValue(0.42F)
      .range(0.0F, 3.0F)
      .step(0.1F)
      .visible(() -> this.webMode.getSelected().equals("Грим"));
   private SliderSettings speedxz = new SliderSettings("Скорость XZ", "Выберите скорость для стрейфа")
      .setValue(0.64F)
      .range(0.0F, 1.0F)
      .step(0.01F)
      .visible(() -> this.webMode.getSelected().equals("Грим"));
   private final SliderSettings grimSpeed = new SliderSettings("Скорость XZ", "Скорость движения в паутине")
      .range(0.05F, 0.5F)
      .setValue(0.05F)
      .step(0.05F)
      .visible(() -> this.webMode.getSelected().equals("Грим новый"));
   private final SliderSettings grimSpeedY = new SliderSettings("Скорость Y", "Скорость движения в паутине")
      .range(0.05F, 0.8F)
      .setValue(0.65F)
      .step(0.05F)
      .visible(() -> this.webMode.getSelected().equals("Грим новый"));
   private int webTicks = 0;
   private boolean wasInWeb = false;
   private boolean grimFlag = false;
   private int packetDelay = 0;

   public static NoWeb getInstance() {
      return Instance.get(NoWeb.class);
   }

   public NoWeb() {
      super("NoWeb", "NoWeb", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.webMode, this.speedy, this.speedxz, this.grimSpeed, this.grimSpeedY});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      String mode = this.webMode.getSelected();
      if (mode.equals("Грим")) {
         if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = Simulations.calculateDirection(this.speedxz.getValue());
            mc.player.addVelocity(speed[0], 0.0, speed[1]);
            mc.player.velocity.y = mc.options.jumpKey.isPressed()
               ? this.speedy.getValue()
               : (mc.options.sneakKey.isPressed() ? -this.speedy.getValue() * 1.3 : 0.0);
         }
      } else if (mode.equals("Грим новый")) {
         if (mc.player == null || mc.world == null) {
            return;
         }

         boolean inWeb = this.isPlayerInWeb();
         if (inWeb) {
            if (!this.wasInWeb) {
               this.webTicks = 0;
               this.grimFlag = false;
               this.packetDelay = 0;
            }

            this.webTicks++;
            this.wasInWeb = true;
            this.handleWebBypass();
         } else {
            this.webTicks = 0;
            this.wasInWeb = false;
            this.grimFlag = false;
            this.packetDelay = 0;
         }

         if (this.packetDelay > 0) {
            this.packetDelay--;
         }
      }
   }

   private void handleWebBypass() {
      String mode = this.webMode.getSelected();
      if (mode.equals("Грим новый")) {
         this.handleGrimV2();
      }
   }

   private void handleGrimV2() {
      if (mc.player.getHungerManager().getFoodLevel() >= 20) {
         double baseSpeed = this.grimSpeed.getValue();
         if (this.webTicks == 0 && !this.grimFlag && !mc.player.isSwimming()) {
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
            this.grimFlag = true;
            this.packetDelay = 2;
         }

         if (this.webTicks > 0 && this.packetDelay == 0) {
            if (MoveUtil.isMoving()) {
               double[] motion = Simulations.forward(baseSpeed);
               mc.player.addVelocity(motion[0], 0.0, motion[1]);
            }

            if (!mc.player.isOnGround()) {
               if (mc.options.jumpKey.isPressed()) {
                  mc.player.velocity.y = this.grimSpeedY.getValue();
               } else if (mc.options.sneakKey.isPressed()) {
                  mc.player.velocity.y = -this.grimSpeedY.getValue();
               }
            }
         }
      }
   }

   private boolean isPlayerInWeb() {
      if (mc.player != null && mc.world != null) {
         Box box = mc.player.getBoundingBox();
         return PlayerInteractionHelper.isBoxInBlock(box, Blocks.COBWEB);
      } else {
         return false;
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.webTicks = 0;
      this.wasInWeb = false;
      this.grimFlag = false;
      this.packetDelay = 0;
   }
}
