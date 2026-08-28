package code.elixir.features.impl.movement;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;

public class HighJump extends Module {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Режим прыжка")
      .value("Лодка", "Экран шалкера", "Буст слизи", "ФанТайм песок душ")
      .selected("Лодка");
   private final SliderSettings timer2 = new SliderSettings("Скорость буста", "")
      .setValue(2.4F)
      .range(1.0F, 6.0F)
      .visible(() -> this.modeSetting.isSelected("Экран шалкера"));
   private boolean wasInShulkerScreen = false;
   private boolean wasOnSlimeBlock = false;

   public HighJump() {
      super("HighJump", "HighJump", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.modeSetting, this.timer2});
   }

   @EventHandler
   private void tickEvent(TickEvent event) {
      if (this.modeSetting.isSelected("Экран шалкера") && mc.currentScreen instanceof ShulkerBoxScreen) {
         mc.player.addVelocity(0.0, this.timer2.getValue(), 0.0);
      }

      if (this.modeSetting.isSelected("ФанТайм песок душ") && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
         mc.player.addVelocity(0.0, 0.56, 0.0);
      }

      if (this.modeSetting.isSelected("Лодка")) {
         if (mc.currentScreen instanceof ShulkerBoxScreen) {
            float yaw = (float)Math.toRadians(mc.player.getYaw());
            double x = -Math.sin(yaw) * 1.0;
            double z = Math.cos(yaw) * 1.0;
            mc.player.addVelocity(0.0, 1.0, 0.0);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.24, mc.player.getZ());
         }

         if (mc.currentScreen instanceof ShulkerBoxScreen) {
            this.wasInShulkerScreen = true;
         } else if (this.wasInShulkerScreen && mc.currentScreen == null && this.isNearShulkerBox()) {
            this.wasInShulkerScreen = false;
         }
      }

      if (this.modeSetting.isSelected("Буст слизи")) {
         if (mc.player.isOnGround() && this.isOnSlimeBlock()) {
            this.wasOnSlimeBlock = true;
         } else if (this.wasOnSlimeBlock && !mc.player.isOnGround() && mc.player.getVelocity().getY() > 0.0) {
            mc.player.addVelocity(0.0, 1.35, 0.0);
            this.wasOnSlimeBlock = false;
         } else if (!this.isOnSlimeBlock()) {
            this.wasOnSlimeBlock = false;
         }
      }
   }

   private boolean isNearShulkerBox() {
      BlockPos playerPos = mc.player.getBlockPos();

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isOnSlimeBlock() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos belowPos = playerPos.down();
      return mc.world.getBlockState(belowPos).getBlock() instanceof SlimeBlock;
   }

   public SelectSetting getModeSetting() {
      return this.modeSetting;
   }

   public SliderSettings getTimer2() {
      return this.timer2;
   }

   public boolean isWasInShulkerScreen() {
      return this.wasInShulkerScreen;
   }

   public boolean isWasOnSlimeBlock() {
      return this.wasOnSlimeBlock;
   }
}
