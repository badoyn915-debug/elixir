package code.elixir.features.impl.render;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.TriggerBot;
import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspManager;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetESP extends Module {
   private Animation esp_anim = new Decelerate().setMs(350).setValue(1.0);
   private LivingEntity currentRenderEntity = null;

   // 1. Mode & Colors
   private final SelectSetting targetEspType = new SelectSetting("Режим", "Выбирает тип анимации Target ESP")
      .value("Души", "Квадрат", "Круг", "Pulse", "Orbit", "Wave", "Radar", "Energy", "Echo", "Helix", "Glitch", "Квантум")
      .selected("Души");
   private final RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета", new String[]{"Клиентский", "Кастом"}, "Клиентский");
   private final ColorSetting maincolor = new ColorSetting("Цвет", "Основной цвет Target ESP")
      .value(new Color(255, 255, 255, 255).getRGB())
      .presets(
         new Color(0, 246, 255, 255).getRGB(),
         new Color(183, 1, 195, 255).getRGB(),
         new Color(255, 60, 0, 255).getRGB(),
         new Color(171, 253, 0, 255).getRGB()
      )
      .visible(() -> this.colorMode.get().equals("Кастом"));
   private final ColorSetting secondaryColor = new ColorSetting("Второй цвет", "Дополнительный цвет градиента")
      .value(new Color(0, 246, 255, 255).getRGB())
      .presets(
         new Color(183, 1, 195, 255).getRGB(),
         new Color(255, 60, 0, 255).getRGB(),
         new Color(171, 253, 0, 255).getRGB(),
         new Color(255, 255, 255, 255).getRGB()
      )
      .visible(() -> this.colorMode.get().equals("Кастом"));
   public final BooleanSetting redOnHit = new BooleanSetting("Краснеть при ударе", "ESP становится красным при получении урона целью").setValue(true);

   // 2. Animation & Transform Settings
   private final SliderSettings animSpeed = new SliderSettings("Скорость анимации", "Скорость воспроизведения эффекта").range(0.1F, 3.0F).setValue(1.0F).step(0.05F);
   private final SliderSettings animSize = new SliderSettings("Размер эффекта", "Базовый размер визуального эффекта").range(0.3F, 2.5F).setValue(1.0F).step(0.05F);
   private final SliderSettings animScale = new SliderSettings("Интенсивность масштаба", "Сила масштабирования эффекта").range(0.2F, 2.0F).setValue(1.0F).step(0.05F);
   private final SliderSettings rotationSpeed = new SliderSettings("Скорость вращения", "Скорость вращения элементов").range(0.1F, 5.0F).setValue(1.0F).step(0.1F);
   private final RadioSetting rotationDir = new RadioSetting("Направление вращения", "Направление вращения элементов", new String[]{"По часовой", "Против часовой"}, "По часовой");

   // 3. Pulse Settings
   private final BooleanSetting pulse = new BooleanSetting("Пульсация", "Включить плавную пульсацию эффекта").setValue(true);
   private final SliderSettings pulseSpeed = new SliderSettings("Скорость пульсации", "Частота дыхания/пульсации").range(0.2F, 4.0F).setValue(1.0F).step(0.05F).visible(this.pulse::isValue);
   private final SliderSettings pulseStrength = new SliderSettings("Сила пульсации", "Амплитуда изменения размера").range(0.05F, 1.0F).setValue(0.2F).step(0.05F).visible(this.pulse::isValue);

   // 4. Fade Settings
   private final BooleanSetting fade = new BooleanSetting("Fade", "Плавное появление и затухание при наведении").setValue(true);
   private final SliderSettings fadeSpeed = new SliderSettings("Скорость Fade", "Скорость появления и исчезновения в мс").range(100, 1000).setValue(350).step(25).visible(this.fade::isValue);

   // 5. Glow Settings
   private final BooleanSetting glow = new BooleanSetting("Свечение", "Мягкое неоновое свечение вокруг эффекта").setValue(true);
   private final SliderSettings glowSize = new SliderSettings("Радиус свечения", "Размер ореола свечения").range(0.5F, 3.0F).setValue(1.5F).step(0.1F).visible(this.glow::isValue);

   // 6. Wave Settings
   private final BooleanSetting wave = new BooleanSetting("Волна", "Волновой эффект распространения").setValue(true);
   private final SliderSettings waveSpeed = new SliderSettings("Скорость волны", "Скорость распространения волн").range(0.2F, 3.0F).setValue(1.0F).step(0.05F).visible(this.wave::isValue);
   private final SliderSettings waveStrength = new SliderSettings("Амплитуда волны", "Сила и высота волн").range(0.1F, 1.5F).setValue(0.5F).step(0.05F).visible(this.wave::isValue);

   // 7. Orbit Settings
   private final BooleanSetting orbit = new BooleanSetting("Орбита", "Вращение элементов по орбите").setValue(true);
   private final SliderSettings orbitRadius = new SliderSettings("Радиус орбиты", "Радиус вращения спутников").range(0.3F, 3.0F).setValue(1.0F).step(0.05F).visible(this.orbit::isValue);
   private final SliderSettings orbitSpeed = new SliderSettings("Скорость орбиты", "Скорость движения по орбите").range(0.2F, 4.0F).setValue(1.2F).step(0.05F).visible(this.orbit::isValue);

   // 8. Trail & Particle Settings
   private final SliderSettings particleAmount = new SliderSettings("Количество частиц", "Количество элементов / частиц").range(4, 60).setValue(20).range(4, 60).setInteger(true);
   private final SliderSettings trailLength = new SliderSettings("Длина следа", "Длина шлейфа частиц").range(5, 60).setValue(30).range(5, 60).setInteger(true);
   private final SliderSettings trailFade = new SliderSettings("Затухание следа", "Скорость затухания шлейфа").range(0.2F, 2.0F).setValue(1.0F).step(0.05F);

   public static TargetESP getInstance() {
      return Instance.get(TargetESP.class);
   }

   public TargetESP() {
      super("TargetEsp", "TargetEsp", ModuleCategory.RENDER);
      this.setup(new Setting[]{
         this.targetEspType,
         this.colorMode,
         this.maincolor,
         this.secondaryColor,
         this.redOnHit,
         this.animSpeed,
         this.animSize,
         this.animScale,
         this.rotationSpeed,
         this.rotationDir,
         this.pulse,
         this.pulseSpeed,
         this.pulseStrength,
         this.fade,
         this.fadeSpeed,
         this.glow,
         this.glowSize,
         this.wave,
         this.waveSpeed,
         this.waveStrength,
         this.orbit,
         this.orbitRadius,
         this.orbitSpeed,
         this.particleAmount,
         this.trailLength,
         this.trailFade
      });
   }

   public int getColor() {
      return this.colorMode.get().equals("Клиентский") ? ThemeManager.primaryColor.getColor() : this.maincolor.getColor();
   }

   public int getSecondaryColor() {
      return this.colorMode.get().equals("Клиентский") ? ThemeManager.secondaryColor.getColor() : this.secondaryColor.getColor();
   }

   @Override
   public void activate() {
      super.activate();
   }

   @Override
   public void deactivate() {
      this.currentRenderEntity = null;
      TargetEspManager.getInstance().resetAll();
      super.deactivate();
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 2) {
         Render3D.updateTargetEsp();
      }
   }

   private static LivingEntity getCombatTarget() {
      if (AttackAura.getInstance().isState() && AttackAura.getInstance().getTarget() != null) {
         return AttackAura.getInstance().getTarget();
      } else {
         TriggerBot tb = TriggerBot.getInstance();
         return tb != null && tb.isState() && tb.getTarget() != null ? tb.getTarget() : null;
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      float tickDelta = e.getPartialTicks();
      LivingEntity target = getCombatTarget();

      int targetMs = this.fade.isValue() ? (int)this.fadeSpeed.getValue() : 100;
      this.esp_anim.setMs(targetMs);

      if (target != null) {
         this.currentRenderEntity = target;
         this.esp_anim.setDirection(Direction.FORWARDS);
      } else {
         this.esp_anim.setDirection(Direction.BACKWARDS);
      }

      boolean combatActive = this.currentRenderEntity != null && !this.esp_anim.isFinished(Direction.BACKWARDS);
      if (combatActive) {
         float anim = this.esp_anim.getOutput().floatValue();
         if (anim > 0.005F) {
            float red = this.redOnHit.isValue()
               ? MathHelper.clamp((this.currentRenderEntity.hurtTime - tickCounter.getTickDelta(false)) / 20.0F, 0.0F, 1.0F)
               : 0.0F;

            Vec3d targetPos = this.currentRenderEntity.getLerpedPos(tickDelta);
            long timeMs = System.currentTimeMillis();

            TargetEspContext ctx = new TargetEspContext(
               e.getStack(),
               mc.gameRenderer.getCamera(),
               tickDelta,
               this.currentRenderEntity,
               targetPos,
               anim,
               red,
               this.getColor(),
               this.getSecondaryColor(),
               timeMs,
               this
            );

            TargetEspManager.getInstance().render(this.targetEspType.getSelected(), ctx);
         }
      } else {
         this.currentRenderEntity = null;
         TargetEspManager.getInstance().resetAll();
      }
   }

   // Getters for settings
   public SelectSetting getTargetEspType() { return this.targetEspType; }
   public RadioSetting getColorMode() { return this.colorMode; }
   public ColorSetting getMaincolor() { return this.maincolor; }
   public ColorSetting getSecondaryColorSetting() { return this.secondaryColor; }
   public BooleanSetting getRedOnHit() { return this.redOnHit; }
   public SliderSettings getAnimSpeed() { return this.animSpeed; }
   public SliderSettings getAnimSize() { return this.animSize; }
   public SliderSettings getAnimScale() { return this.animScale; }
   public SliderSettings getRotationSpeed() { return this.rotationSpeed; }
   public RadioSetting getRotationDir() { return this.rotationDir; }
   public BooleanSetting getPulse() { return this.pulse; }
   public SliderSettings getPulseSpeed() { return this.pulseSpeed; }
   public SliderSettings getPulseStrength() { return this.pulseStrength; }
   public BooleanSetting getFade() { return this.fade; }
   public SliderSettings getFadeSpeed() { return this.fadeSpeed; }
   public BooleanSetting getGlow() { return this.glow; }
   public SliderSettings getGlowSize() { return this.glowSize; }
   public BooleanSetting getWave() { return this.wave; }
   public SliderSettings getWaveSpeed() { return this.waveSpeed; }
   public SliderSettings getWaveStrength() { return this.waveStrength; }
   public BooleanSetting getOrbit() { return this.orbit; }
   public SliderSettings getOrbitRadius() { return this.orbitRadius; }
   public SliderSettings getOrbitSpeed() { return this.orbitSpeed; }
   public SliderSettings getParticleAmount() { return this.particleAmount; }
   public SliderSettings getTrailLength() { return this.trailLength; }
   public SliderSettings getTrailFade() { return this.trailFade; }
}
