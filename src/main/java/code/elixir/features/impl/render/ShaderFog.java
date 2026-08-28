package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shader.SkyShaderUtil;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class ShaderFog extends Module {
   private final SelectSetting mode = new SelectSetting("Режим", "Режим шейдера неба")
      .value("Nebula", "Вода", "Каустика", "Северное сияние", "Черная дыра", "Матрица", "Гибли", "Киберпанк", "Солнечное затмение", "Магма", "Небула")
      .selected("Nebula");

   // Nebula Sub-mode Settings & Presets
   private final SelectSetting nebulaQuality = new SelectSetting("Quality Preset", "Пресет качества Raymarching").value("HIGH", "LOW", "MEDIUM", "ULTRA");
   private final SliderSettings nebulaSpeed = new SliderSettings("Animation Speed", "Скорость анимации космической туманности").setValue(1.0F).range(0.1F, 4.0F);
   private final SliderSettings nebulaMouseSens = new SliderSettings("Mouse Sensitivity", "Чувствительность поворота камеры").setValue(1.0F).range(0.1F, 3.0F);
   private final SliderSettings nebulaCameraSmoothing = new SliderSettings("Camera Smoothing", "Плавность следования 3D камеры").setValue(8.0F).range(1.0F, 20.0F);
   private final SliderSettings nebulaFov = new SliderSettings("Internal FOV", "Внутренний FOV для лучей камеры").setValue(75.0F).range(40.0F, 120.0F);
   private final SliderSettings nebulaDensity = new SliderSettings("Nebula Density", "Плотность газовых облаков туманности").setValue(1.2F).range(0.2F, 3.0F);
   private final SliderSettings nebulaCoreGlow = new SliderSettings("Core Glow", "Интенсивность свечения ядра").setValue(1.5F).range(0.1F, 4.0F);
   private final SliderSettings nebulaParticles = new SliderSettings("Particles Density", "Плотность звездных частиц и пыли").setValue(1.0F).range(0.0F, 3.0F);
   private final SliderSettings nebulaParallax = new SliderSettings("Parallax Strength", "Сила 3D параллакса при движении камеры").setValue(1.0F).range(0.1F, 3.0F);

   // General Shader Settings
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет шейдера тумана")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB());
   private final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность эффекта").setValue(1.0F).range(0.1F, 1.0F);
   private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации").setValue(1.0F).range(0.1F, 5.0F);
   private final SliderSettings scale = new SliderSettings("Масштаб", "Масштаб узора").setValue(5.0F).range(1.0F, 20.0F);
   private final SliderSettings intensity = new SliderSettings("Интенсивность", "Интенсивность эффекта").setValue(0.01F).range(0.001F, 0.05F);

   private static final ShaderProgramKey WATER_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/water"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey CAUSTIC_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/caustic"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey AURORA_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/aurora"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey BLACKHOLE_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/blackhole"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey MATRIX_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/matrix"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey GHIBLI_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/ghibli"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey NEBULA_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/nebula"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey CYBER_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/cyber"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey ECLIPSE_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/eclipse"), VertexFormats.POSITION, Defines.EMPTY);
   private static final ShaderProgramKey MAGMA_SHADER = new ShaderProgramKey(SkyShaderUtil.getShader("post/sky/magma"), VertexFormats.POSITION, Defines.EMPTY);

   private long startMillis = -1L;
   private long lastFrameMillis = -1L;

   // Virtual 3D camera smooth angles
   private float smoothCameraYaw = 0.0F;
   private float smoothCameraPitch = 0.0F;
   private boolean cameraInitialized = false;

   public static ShaderFog getInstance() {
      return Instance.get(ShaderFog.class);
   }

   public ShaderFog() {
      super("Shader Fog", ModuleCategory.RENDER);

      this.setup(new Setting[]{
         this.mode,
         this.nebulaQuality,
         this.nebulaSpeed,
         this.nebulaMouseSens,
         this.nebulaCameraSmoothing,
         this.nebulaFov,
         this.nebulaDensity,
         this.nebulaCoreGlow,
         this.nebulaParticles,
         this.nebulaParallax,
         this.color,
         this.alpha,
         this.speed,
         this.scale,
         this.intensity
      });
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.startMillis = -1L;
      this.lastFrameMillis = -1L;
      this.cameraInitialized = false;
   }

   public static boolean shouldReplaceVanillaSky() {
      ShaderFog module = getInstance();
      return module != null && module.isState() ? !isBlockingScreen(MinecraftClient.getInstance().currentScreen) : false;
   }

   private static boolean isBlockingScreen(Screen screen) {
      return screen instanceof GameMenuScreen;
   }

   public void renderSkyShader() {
      if (this.isState()) {
         long now = System.currentTimeMillis();
         if (this.startMillis < 0L) {
            this.startMillis = now;
            this.lastFrameMillis = now;
         }

         float dt = MathHelper.clamp((float)(now - this.lastFrameMillis) / 1000.0F, 0.001F, 0.05F);
         this.lastFrameMillis = now;

         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            if (!isBlockingScreen(mc.currentScreen)) {
               Camera guardCam = mc.gameRenderer.getCamera();
               if (guardCam.getSubmersionType() == CameraSubmersionType.NONE) {
                  float time = (float)(now - this.startMillis) / 1000.0F;
                  float fw = mc.getWindow().getFramebufferWidth();
                  float fh = mc.getWindow().getFramebufferHeight();
                  Color tint = new Color(this.color.getColor());
                  float cr = tint.getRed() / 255.0F;
                  float cg = tint.getGreen() / 255.0F;
                  float cb = tint.getBlue() / 255.0F;

                  boolean isNebula = this.mode.isSelected("Nebula") || this.mode.isSelected("Небула");
                  ShaderProgramKey key;
                  if (isNebula) {
                     key = NEBULA_SHADER;
                  } else if (this.mode.isSelected("Каустика")) {
                     key = CAUSTIC_SHADER;
                  } else if (this.mode.isSelected("Северное сияние")) {
                     key = AURORA_SHADER;
                  } else if (this.mode.isSelected("Черная дыра")) {
                     key = BLACKHOLE_SHADER;
                  } else if (this.mode.isSelected("Матрица")) {
                     key = MATRIX_SHADER;
                  } else if (this.mode.isSelected("Гибли")) {
                     key = GHIBLI_SHADER;
                  } else if (this.mode.isSelected("Киберпанк")) {
                     key = CYBER_SHADER;
                  } else if (this.mode.isSelected("Солнечное затмение")) {
                     key = ECLIPSE_SHADER;
                  } else if (this.mode.isSelected("Магма")) {
                     key = MAGMA_SHADER;
                  } else {
                     key = WATER_SHADER;
                  }

                  // Virtual 3D camera smooth angle calculation
                  Camera cam = mc.gameRenderer.getCamera();
                  if (cam != null) {
                     float targetYawRad = (float)Math.toRadians(-cam.getYaw());
                     float targetPitchRad = (float)Math.toRadians(cam.getPitch());

                     if (isNebula) {
                        float sens = this.nebulaMouseSens.getValue();
                        targetYawRad *= sens;
                        targetPitchRad *= sens;

                        if (!this.cameraInitialized) {
                           this.smoothCameraYaw = targetYawRad;
                           this.smoothCameraPitch = targetPitchRad;
                           this.cameraInitialized = true;
                        }

                        float smoothRate = 1.0F - (float)Math.exp(-this.nebulaCameraSmoothing.getValue() * dt);
                        this.smoothCameraYaw = MathHelper.lerp(smoothRate, this.smoothCameraYaw, targetYawRad);
                        this.smoothCameraPitch = MathHelper.lerp(smoothRate, this.smoothCameraPitch, targetPitchRad);
                     } else {
                        this.smoothCameraYaw = targetYawRad;
                        this.smoothCameraPitch = targetPitchRad;
                     }
                  }

                  Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

                  try {
                     try {
                        ShaderProgram shader = RenderSystem.setShader(key);
                        if (shader != null) {
                           if (shader.getUniform("uTime") != null) {
                              shader.getUniform("uTime").set(time);
                           }

                           if (shader.getUniform("uResolution") != null) {
                              shader.getUniform("uResolution").set(fw, fh);
                           }

                           if (shader.getUniform("uColor") != null) {
                              shader.getUniform("uColor").set(cr, cg, cb);
                           }

                           if (shader.getUniform("uAlpha") != null) {
                              shader.getUniform("uAlpha").set(this.alpha.getValue());
                           }

                           if (isNebula) {
                              if (shader.getUniform("uSpeed") != null) {
                                 shader.getUniform("uSpeed").set(this.nebulaSpeed.getValue());
                              }
                              if (shader.getUniform("uCameraAngles") != null) {
                                 shader.getUniform("uCameraAngles").set(this.smoothCameraYaw, this.smoothCameraPitch);
                              }
                              if (shader.getUniform("uCameraDir") != null) {
                                 shader.getUniform("uCameraDir").set(this.smoothCameraYaw, this.smoothCameraPitch);
                              }
                              if (shader.getUniform("uFov") != null) {
                                 shader.getUniform("uFov").set(this.nebulaFov.getValue());
                              }
                              if (shader.getUniform("uDensity") != null) {
                                 shader.getUniform("uDensity").set(this.nebulaDensity.getValue());
                              }
                              if (shader.getUniform("uGlow") != null) {
                                 shader.getUniform("uGlow").set(this.nebulaCoreGlow.getValue());
                              }
                              if (shader.getUniform("uParallax") != null) {
                                 shader.getUniform("uParallax").set(this.nebulaParallax.getValue());
                              }
                              if (shader.getUniform("uParticles") != null) {
                                 shader.getUniform("uParticles").set(this.nebulaParticles.getValue());
                              }
                               if (shader.getUniform("uQualitySteps") != null) {
                                  int steps = 18;
                                  if (this.nebulaQuality.isSelected("LOW")) steps = 10;
                                  else if (this.nebulaQuality.isSelected("MEDIUM")) steps = 14;
                                  else if (this.nebulaQuality.isSelected("ULTRA")) steps = 24;
                                  shader.getUniform("uQualitySteps").set(steps);
                               }
                           } else {
                              if (shader.getUniform("uSpeed") != null) {
                                 shader.getUniform("uSpeed").set(this.speed.getValue());
                              }
                              if (shader.getUniform("uScale") != null) {
                                 shader.getUniform("uScale").set(this.scale.getValue());
                              }
                              if (shader.getUniform("uIntensity") != null) {
                                 shader.getUniform("uIntensity").set(this.intensity.getValue());
                              }
                              if (shader.getUniform("uCameraDir") != null) {
                                 shader.getUniform("uCameraDir").set(this.smoothCameraYaw, this.smoothCameraPitch);
                              }
                              if (shader.getUniform("uFov") != null && mc.options != null && mc.options.getFov() != null) {
                                 shader.getUniform("uFov").set(((Integer)mc.options.getFov().getValue()).floatValue());
                              }
                           }

                           RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);
                           RenderSystem.enableBlend();
                           RenderSystem.defaultBlendFunc();
                           RenderSystem.enableDepthTest();
                           RenderSystem.depthFunc(514);
                           RenderSystem.depthMask(false);
                           RenderSystem.disableCull();
                           Matrix4f identity = new Matrix4f();
                           BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
                           buf.vertex(identity, -1.0F, -1.0F, 1.0F);
                           buf.vertex(identity, 1.0F, -1.0F, 1.0F);
                           buf.vertex(identity, 1.0F, 1.0F, 1.0F);
                           buf.vertex(identity, -1.0F, 1.0F, 1.0F);
                           BufferDraw.draw(buf);
                        }
                     } finally {
                        RenderSystem.setProjectionMatrix(savedProj, ProjectionType.PERSPECTIVE);
                        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.depthFunc(515);
                        RenderSystem.depthMask(true);
                        RenderSystem.enableCull();
                        RenderSystem.disableBlend();
                     }
                  } catch (Exception ignored) {
                  }
               }
            }
         }
      }
   }

   public void renderSky(MatrixStack matrices) {
      this.renderSkyShader();
   }

   public boolean shouldCancelClouds() {
      return this.isState();
   }
}
