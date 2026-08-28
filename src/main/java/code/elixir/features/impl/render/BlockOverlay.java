package code.elixir.features.impl.render;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.EntityDeathEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.display.render.geometry.ShaderBox3D;
import java.awt.Color;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public class BlockOverlay extends Module {
   private final SelectSetting mode = new SelectSetting("Режим", "Тип отрисовки блока").value("Обычный", "Шейдер").selected("Шейдер");
   private final SelectSetting style = new SelectSetting("Стиль", "Стиль шейдера блока")
      .value("Энергия", "Плазма", "Соты")
      .selected("Энергия")
      .visible(() -> !this.mode.isSelected("Обычный"));
   private final ColorSetting color = new ColorSetting("Цвет", "Тонировка шейдера и контура")
      .value(new Color(150, 130, 255, 175).getRGB())
      .presets(
         new Color(150, 130, 255, 175).getRGB(),
         new Color(120, 180, 255, 175).getRGB(),
         new Color(235, 120, 220, 175).getRGB(),
         new Color(255, 255, 255, 175).getRGB()
      );
   private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации шейдера")
      .range(0.35F, 2.75F)
      .step(0.05F)
      .setValue(1.0F)
      .visible(() -> !this.mode.isSelected("Обычный"));
   private final SliderSettings opacity = new SliderSettings("Прозрачность", "Непрозрачность космического шейдера")
      .range(0.1F, 1.0F)
      .step(0.05F)
      .setValue(0.9F)
      .visible(() -> !this.mode.isSelected("Обычный"));
   private final BooleanSetting outline = new BooleanSetting("Контур", "Рисовать контур поверх шейдера").setValue(true);
   private final SliderSettings outlineWidth = new SliderSettings("Толщина контура", "Толщина обводки блока")
      .range(1.0F, 4.0F)
      .step(0.25F)
      .setValue(2.0F)
      .visible(this.outline::isValue);
   private final SliderSettings smoothness = new SliderSettings("Плавность", "Плавность перемещения оверлея").range(1, 10).step(1.0F).setValue(5.0F);
   private float smoothX;
   private float smoothY;
   private float smoothZ;
   private boolean smoothInitialized;

   public static BlockOverlay getInstance() {
      return Instance.get(BlockOverlay.class);
   }

   public BlockOverlay() {
      super("BlockOverlay", "BlockOverlay", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.mode, this.style, this.color, this.speed, this.opacity, this.outline, this.outlineWidth, this.smoothness});
   }

   @Override
   public void deactivate() {
      this.resetSmoothState();
      super.deactivate();
   }

   private void resetSmoothState() {
      this.smoothInitialized = false;
      this.smoothZ = 0.0F;
      this.smoothY = 0.0F;
      this.smoothX = 0.0F;
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      if (mc.player != null && event.getEntity() == mc.player) {
         this.resetSmoothState();
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.getPacket() instanceof PlayerRespawnS2CPacket) {
         this.resetSmoothState();
      }
   }

   @EventHandler(4)
   public void onWorldRender(WorldRenderEvent e) {
      this.renderOverlay(e);
   }

   public void renderOverlay(WorldRenderEvent e) {
      if (mc.player != null && mc.world != null && mc.player.isAlive() && !(mc.player.getHealth() <= 0.0F)) {
         if (!(mc.currentScreen instanceof DeathScreen)) {
            HitResult class_2392 = mc.crosshairTarget;
            BlockHitResult result;
            if (class_2392 instanceof BlockHitResult && (result = (BlockHitResult)class_2392).getType() == Type.BLOCK) {
               BlockPos pos = result.getBlockPos();
               VoxelShape localShape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
               if (!localShape.isEmpty()) {
                  float targetX = pos.getX();
                  float targetY = pos.getY();
                  float targetZ = pos.getZ();
                  if (this.smoothInitialized && !(this.smoothness.getValue() <= 1.0F)) {
                     float t = MathHelper.clamp((11.0F - this.smoothness.getValue()) / 10.0F, 0.05F, 1.0F);
                     float step = t * t * 0.85F + 0.008F;
                     step = MathHelper.clamp(step, 0.008F, 1.0F);
                     step = 1.0F - (float)Math.pow(1.0F - step, e.getPartialTicks());
                     this.smoothX = MathHelper.lerp(step, this.smoothX, targetX);
                     this.smoothY = MathHelper.lerp(step, this.smoothY, targetY);
                     this.smoothZ = MathHelper.lerp(step, this.smoothZ, targetZ);
                  } else {
                     this.smoothX = targetX;
                     this.smoothY = targetY;
                     this.smoothZ = targetZ;
                     this.smoothInitialized = true;
                  }

                  BlockPos renderPos = BlockPos.ofFloored(this.smoothX, this.smoothY, this.smoothZ);
                  float offsetX = this.smoothX - renderPos.getX();
                  float offsetY = this.smoothY - renderPos.getY();
                  float offsetZ = this.smoothZ - renderPos.getZ();
                  VoxelShape offsetShape = localShape.offset(offsetX, offsetY, offsetZ);
                  VoxelShape worldShape = localShape.offset(this.smoothX, this.smoothY, this.smoothZ);
                  int overlayColor = this.color.getColor();
                  boolean shaderDrawn = false;
                  if (!this.mode.isSelected("Обычный")) {
                     float time = ((float)(mc.world.getTime() % 1200L) + e.getPartialTicks()) * 0.045F * this.speed.getValue();
                     ShaderBox3D.ShaderType shaderType = this.style.isSelected("Плазма")
                        ? ShaderBox3D.ShaderType.PLASMA
                        : (this.style.isSelected("Соты") ? ShaderBox3D.ShaderType.HEX : ShaderBox3D.ShaderType.SHADER);
                     shaderDrawn = ShaderBox3D.drawShaderShape(
                        offsetShape, renderPos, shaderType, overlayColor, time, this.opacity.getValue(), e.getStack().peek()
                     );
                  }

                  if (!shaderDrawn) {
                     Render3D.drawShapeAlternative(renderPos, offsetShape, overlayColor, this.outlineWidth.getValue(), true, this.outline.isValue(), true);
                  } else if (this.outline.isValue()) {
                     int outlineColor = ColorAssist.setAlpha(overlayColor, Math.max(140, ColorAssist.getAlpha(overlayColor)));
                     Render3D.drawShapeAlternative(renderPos, offsetShape, outlineColor, this.outlineWidth.getValue(), false, true, true);
                  }
               }
            }
         }
      }
   }
}
