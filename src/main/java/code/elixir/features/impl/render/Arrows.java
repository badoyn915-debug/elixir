package code.elixir.features.impl.render;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Arrows extends Module {
   private final Identifier iconId = Identifier.of("textures/arrow.png");
   private final Animation radiusAnim = new Decelerate().setMs(150).setValue(6.0);
   private final SliderSettings radiusSetting = new SliderSettings("Радиус", "Радиус стрелок").setValue(50.0F).range(30, 100);
   private final SliderSettings sizeSetting = new SliderSettings("Размер", "Размер стрелок").setValue(10.0F).range(8, 20);
   private final ColorSetting colorSetting = new ColorSetting("Цвет", "Цвет стрелок")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB());

   public Arrows() {
      super("Arrows", "Arrows", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.radiusSetting, this.sizeSetting, this.colorSetting});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrix = e.getDrawContext().getMatrices();
         List<AbstractClientPlayerEntity> players = mc.world
            .getPlayers()
            .stream()
            .filter(p -> p != mc.player)
            .filter(Entity::isAlive)
            .filter(p -> !p.isRemoved())
            .filter(p -> !p.isSpectator())
            .toList();
         float middleW = mc.getWindow().getScaledWidth() / 2.0F;
         float middleH = mc.getWindow().getScaledHeight() / 2.0F;
         float posY = middleH - this.radiusSetting.getValue() - this.radiusAnim.getOutput().floatValue();
         float size = this.sizeSetting.getValue();
         if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON) && !players.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShaderTexture(0, this.iconId);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            players.forEach(
               player -> {
                  int color = FriendUtils.isFriend(player) ? ColorAssist.getFriendColor() : this.colorSetting.getColor();
                  float yaw = getRotations(player) - mc.gameRenderer.getCamera().getYaw();
                  matrix.push();
                  matrix.translate(middleW, middleH, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                  matrix.translate(-middleW, -middleH, 0.0F);
                  Matrix4f matrix4f = matrix.peek().getPositionMatrix();
                  buffer.vertex(matrix4f, middleW - size / 2.0F, posY + size, 0.0F)
                     .texture(0.0F, 1.0F)
                     .color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                  buffer.vertex(matrix4f, middleW + size / 2.0F, posY + size, 0.0F)
                     .texture(1.0F, 1.0F)
                     .color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                  buffer.vertex(matrix4f, middleW + size / 2.0F, posY, 0.0F).texture(1.0F, 0.0F).color(color);
                  buffer.vertex(matrix4f, middleW - size / 2.0F, posY, 0.0F).texture(0.0F, 0.0F).color(color);
                  matrix.translate(middleW, middleH, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
                  matrix.translate(-middleW, -middleH, 0.0F);
                  matrix.pop();
               }
            );
            BufferDraw.draw(buffer);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   public static float getRotations(Entity entity) {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
      Vec3d vec3d = entity.getLerpedPos(tickDelta).subtract(mc.player.getLerpedPos(tickDelta));
      return (float)(-(Math.atan2(vec3d.x, vec3d.z) * (180.0 / Math.PI)));
   }
}
