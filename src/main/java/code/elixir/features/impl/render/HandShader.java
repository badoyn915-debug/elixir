package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.render.shader.HandBlurRenderer;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;

public class HandShader extends Module {
   public final SelectSetting mode = new SelectSetting("Режим", "Режим эффекта шейдера").value("Полный", "Неполный", "Веб-шейдер", "Плазма");
   public final ColorSetting color = new ColorSetting("Цвет", "Цвет шейдера")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB());
   public final SliderSettings blurRadius = new SliderSettings("Радиус размытия", "Радиус свечения вокруг руки")
      .setValue(18.0F)
      .range(1.0F, 50.0F)
      .visible(() -> this.mode.isSelected("Неполный"));
   public final SliderSettings opacity = new SliderSettings("Непрозрачность", "Прозрачность эффекта").setValue(0.6F).range(0.0F, 1.0F);
   public final SliderSettings threshold = new SliderSettings("Порог", "Минимальная яркость свечения")
      .setValue(0.03F)
      .range(0.005F, 0.15F)
      .visible(() -> this.mode.isSelected("Неполный"));
   public final SliderSettings shaderSpeed = new SliderSettings("Скорость", "Скорость анимации шейдера")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(() -> this.mode.isSelected("Веб-шейдер") || this.mode.isSelected("Плазма"));
   public final SliderSettings shaderScale = new SliderSettings("Масштаб", "Приближение или отдаление узора")
      .setValue(1.0F)
      .range(0.25F, 4.0F)
      .visible(() -> this.mode.isSelected("Веб-шейдер") || this.mode.isSelected("Плазма"));
   private Framebuffer savedBefore = null;
   private Framebuffer savedAfter = null;
   private SimpleFramebuffer outputFbo = null;
   private boolean captureReady = false;

   public static HandShader getInstance() {
      return Instance.get(HandShader.class);
   }

   public HandShader() {
      super("Hand Shader", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.mode, this.color, this.blurRadius, this.opacity, this.threshold, this.shaderSpeed, this.shaderScale});
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.invalidateCapture();
   }

   public void invalidateCapture() {
      this.savedBefore = null;
      this.savedAfter = null;
      this.outputFbo = null;
      this.captureReady = false;
   }

   public void beginHandCapture() {
      this.captureReady = false;
   }

   public boolean shouldRender() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (this.isState() && mc.player != null) {
         if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            return false;
         } else {
            Camera camera = mc.gameRenderer.getCamera();
            if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
               return false;
            } else {
               return !mc.player.isSubmergedInWater() && !mc.player.isInLava() ? !mc.player.isSwimming() || !mc.player.isTouchingWater() : false;
            }
         }
      } else {
         return false;
      }
   }

   public void onShaderCapture(Framebuffer before, Framebuffer after) {
      this.savedBefore = before;
      this.savedAfter = after;
      this.captureReady = true;
   }

   public void renderCapturedEffect() {
      if (this.shouldRender() && this.captureReady) {
         MinecraftClient mc = MinecraftClient.getInstance();
         Framebuffer mainFbo = mc.getFramebuffer();
         if (this.savedAfter != null && mainFbo != null) {
            if (this.savedAfter.getColorAttachment() > 0) {
               int depthAttachment = mainFbo.getDepthAttachment();
               if (depthAttachment > 0) {
                  if (this.outputFbo == null) {
                     this.outputFbo = new SimpleFramebuffer(this.savedAfter.textureWidth, this.savedAfter.textureHeight, false);
                  }

                  if (this.outputFbo.textureWidth != this.savedAfter.textureWidth || this.outputFbo.textureHeight != this.savedAfter.textureHeight) {
                     this.outputFbo.resize(this.savedAfter.textureWidth, this.savedAfter.textureHeight);
                  }

                  float[] tint = rgba(this.color.getColor(), this.opacity.getValue());
                  float[] tint2 = rgba(ThemeManager.secondaryColor.getColor(), this.opacity.getValue());
                  float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0F * this.shaderSpeed.getValue();
                  String var8 = this.mode.getSelected();

                  int modeIndex = switch (var8) {
                     case "Неполный" -> 1;
                     case "Веб-шейдер" -> 2;
                     case "Плазма" -> 3;
                     default -> 0;
                  };
                  new HandBlurRenderer(
                        this.savedAfter,
                        this.outputFbo,
                        depthAttachment,
                        this.blurRadius.getValue(),
                        this.threshold.getValue(),
                        tint,
                        tint2,
                        modeIndex,
                        time,
                        this.shaderScale.getValue()
                     )
                     .renderPostProcess();
               }
            }
         }
      }
   }

   private static float[] rgba(int argb, float alphaOverride) {
      Color c = new Color(argb, true);
      return new float[]{c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, alphaOverride};
   }
}
