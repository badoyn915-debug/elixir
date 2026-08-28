package code.elixir.features.impl.render;

import code.elixir.events.item.HandAnimationEvent;
import code.elixir.events.item.SwingDurationEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimations extends Module {
   private boolean swimmingAnimation = true;
   private boolean climbAndCrawl = true;
   private boolean mb3DCompat = false;
   private final BooleanSetting hmiEnable = new BooleanSetting("Мод на красивые руки", "Кастомный рендер рук").setValue(false);
   private final SelectSetting hmiAnimationType = new SelectSetting("Вид анимации", "Вид анимации").value("Классик", "Шарп").visible(this.hmiEnable::isValue);
   private final MultiSelectSetting hmiClassicSwing = new MultiSelectSetting("Стиль удара", "Какие удары использовать в классическом режиме")
      .value("Через лево", "Через право")
      .selected("Через лево", "Через право")
      .visible(() -> this.hmiEnable.isValue() && this.hmiAnimationType.isSelected("Классик"));
   private final SliderSettings hmiSmoothness = new SliderSettings("Плавность", "Плавность движения удара. 1 — как сейчас, 100 — максимально плавно")
      .setValue(1.0F)
      .range(1.0F, 100.0F)
      .step(1.0F)
      .visible(this.hmiEnable::isValue);
   private final BooleanSetting swingEnabled = new BooleanSetting("Анимация свинга", "Включить анимацию свинга")
      .setValue(true)
      .visible(() -> !this.hmiEnable.isValue());
   private final SelectSetting swingType = new SelectSetting("Тип свинга", "Тип анимации свинга")
      .value("Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack", "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant")
      .visible(() -> !this.hmiEnable.isValue() && this.swingEnabled.isValue());
   private final SliderSettings swingStrength = new SliderSettings("Сила анимации", "Сила анимации свинга")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .step(0.01F)
      .visible(() -> !this.hmiEnable.isValue() && this.swingEnabled.isValue() && !this.swingType.isSelected("Pander"));
   private final SliderSettings corner = new SliderSettings("Угол DropDown", "Угол для DropDown")
      .setValue(12.0F)
      .range(1.0F, 360.0F)
      .step(1.0F)
      .visible(() -> !this.hmiEnable.isValue() && this.swingEnabled.isValue() && this.swingType.isSelected("DropDown"));
   private final SliderSettings slant = new SliderSettings("Наклон DropDown", "Наклон для DropDown")
      .setValue(12.0F)
      .range(1.0F, 360.0F)
      .step(1.0F)
      .visible(() -> !this.hmiEnable.isValue() && this.swingEnabled.isValue() && this.swingType.isSelected("DropDown"));
   private final BooleanSetting smoothEnabled = new BooleanSetting("Плавная анимация", "Замедление анимации")
      .setValue(false)
      .visible(() -> !this.hmiEnable.isValue());
   private final SliderSettings slowAnimationSpeed = new SliderSettings("Скорость анимации", "Скорость плавной анимации")
      .setValue(12.0F)
      .range(1.0F, 50.0F)
      .step(1.0F)
      .visible(() -> !this.hmiEnable.isValue() && this.smoothEnabled.isValue());
   private final BooleanSetting auraTargetOnly = new BooleanSetting("Только при Aura", "Анимация только при цели Aura")
      .setValue(false)
      .visible(() -> !this.hmiEnable.isValue());

   public SwingAnimations() {
      super("SwingAnimations", "SwingAnimations", ModuleCategory.RENDER);
      this.setup(
         new Setting[]{
            this.hmiEnable,
            this.hmiAnimationType,
            this.hmiClassicSwing,
            this.hmiSmoothness,
            this.swingEnabled,
            this.swingType,
            this.swingStrength,
            this.corner,
            this.slant,
            this.smoothEnabled,
            this.slowAnimationSpeed,
            this.auraTargetOnly
         }
      );
   }

   public static SwingAnimations getInstance() {
      return Instance.get(SwingAnimations.class);
   }

   public boolean isHmiMode() {
      return this.hmiEnable.isValue();
   }

   public boolean shouldRestartSwingOnRetrigger() {
      return this.isState() && (this.hmiEnable.isValue() || this.smoothEnabled.isValue());
   }

   public boolean isOnlyAuraActive() {
      return this.auraTargetOnly.isValue();
   }

   public boolean isSharpAnimation() {
      return this.hmiAnimationType.isSelected("Шарп");
   }

   public boolean shouldAlternateClassicSwordSwing() {
      return !this.isSharpAnimation() && this.hmiClassicSwing.isSelected("Через лево") && this.hmiClassicSwing.isSelected("Через право");
   }

   public boolean resolveClassicSwordLeft(boolean alternateLeft) {
      boolean allowLeft = this.hmiClassicSwing.isSelected("Через лево");
      boolean allowRight = this.hmiClassicSwing.isSelected("Через право");
      if (allowLeft && allowRight) {
         return alternateLeft;
      } else if (allowLeft) {
         return true;
      } else {
         return allowRight ? false : alternateLeft;
      }
   }

   public float getHmiSmoothness() {
      return this.hmiSmoothness.getValue();
   }

   public int getHmiSwingDuration() {
      float value = this.hmiSmoothness.getValue();
      return value <= 1.0F ? 6 : Math.round(MathHelper.lerp((value - 1.0F) / 99.0F, 6.0F, 80.0F));
   }

   public float remapHmiSwingProgress(float progress) {
      float t = MathHelper.clamp(progress, 0.0F, 1.0F);
      float value = this.hmiSmoothness.getValue();
      if (value <= 1.0F) {
         return t;
      } else {
         float amount = (value - 1.0F) / 99.0F;
         float eased = t * t * (3.0F - 2.0F * t);
         return MathHelper.lerp(amount * 0.35F, t, eased);
      }
   }

   public boolean isSwimmingAnimation() {
      return this.swimmingAnimation;
   }

   public boolean isClimbAndCrawl() {
      return this.climbAndCrawl;
   }

   public boolean isMb3DCompat() {
      return this.mb3DCompat;
   }

   private boolean hasAuraTarget() {
      AttackAura aura = AttackAura.getInstance();
      return aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive();
   }

   @EventHandler
   public void onSwingDuration(SwingDurationEvent e) {
      if (this.isState()) {
         if (this.hmiEnable.isValue()) {
            if (this.hmiSmoothness.getValue() > 1.0F) {
               e.setAnimation(this.getHmiSwingDuration());
               e.cancel();
            }
         } else {
            if (this.smoothEnabled.isValue()) {
               e.setAnimation(this.slowAnimationSpeed.getValue());
               e.cancel();
            }
         }
      }
   }

   @EventHandler
   public void onHandAnimation(HandAnimationEvent e) {
      if (this.isState() && !this.hmiEnable.isValue() && this.swingEnabled.isValue()) {
         if (!this.auraTargetOnly.isValue() || this.hasAuraTarget()) {
            if (mc.player != null) {
               Arm expectedSwingArm = mc.player.getMainArm();
               if (e.getArm() != expectedSwingArm) {
                  return;
               }
            }

            MatrixStack matrices = e.getMatrices();
            float swingProgress = e.getSwingProgress();
            float equipProgress = e.getEquipProgress();
            int i = e.getArm() == Arm.RIGHT ? 1 : -1;
            float strength = this.swingStrength.getValue();
            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            boolean handled = true;
            String var10 = this.swingType.getSelected();
            switch (var10) {
               case "Down":
                  matrices.translate(i * 0.56F, -0.32F, -0.72F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
                  break;
               case "Poke": {
                  float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
                  float tilt = strength / 3.0F;
                  matrices.translate(i * 0.56F, -0.52F, -0.72F);
                  matrices.translate(0.0F, 0.0F, tilt * -anim);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0F * i));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75.0F * (strength / 4.0F) * anim - 60.0F) * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0F));
                  break;
               }
               case "Static":
                  matrices.translate(i * 0.56F, -0.42F, -0.72F);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60.0F * strength));
                  matrices.translate(0.0, -0.1, 0.0);
                  break;
               case "Feast":
                  matrices.translate(i * 0.56F, -0.32F, -0.72F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i * strength));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65.0F * strength));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                  break;
               case "Akrien":
                  matrices.translate(i * 0.65F, -0.32F, -0.72F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25.0F * strength));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25.0F * strength));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15.0F * strength));
                  matrices.translate(sin2 * 0.18F * strength, sin2 * 0.59F * strength, 0.0F);
                  break;
               case "Smooth":
                  this.applySwingOffset(matrices, i, swingProgress, strength);
                  break;
               case "Block":
                  if (swingProgress > 0.0F) {
                     float gx = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                     matrices.translate(0.56F * i, equipProgress * -0.2F - 0.5F, -0.7F);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(gx * -85.0F * strength));
                     matrices.translate(-0.1F * i, 0.28F, 0.2F);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                  } else {
                     float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                     float m = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                     float f1 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                     matrices.translate(n * i * strength, m * strength, f1 * strength);
                     this.applyEquipOffset(matrices, i, equipProgress);
                     this.applySwingOffset(matrices, i, swingProgress, strength);
                  }
                  break;
               case "ToBack":
                  float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                  matrices.translate(0.65F * i, -0.45F, -0.9F);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30.0F * (1.0F - g * strength) - 30.0F) * i));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F * i));
                  break;
               case "SelfBack": {
                  float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
                  matrices.translate(0.65F * i, -0.3F, -0.8F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * i));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70 * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F - 60.0F * strength * anim));
                  break;
               }
               case "Break":
               case "Брик":
                  matrices.translate(0.66F * i, -0.3F, -0.38F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270 * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10.0F * strength));
                  matrices.scale(0.5F, 0.5F, 0.5F);
                  matrices.translate(-0.1F * i, 0.2F, 0.0F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105.0F * i));
                  break;
               case "DropDown": {
                  float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
                  this.applyEquipOffset(matrices, i, 0.0F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0F));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.corner.getValue()));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.slant.getValue() * anim * strength));
                  break;
               }
               case "Pander":
                  float panderAnim = MathHelper.sin(swingProgress * (float) Math.PI);
                  float panderF = 1.0F - equipProgress;
                  matrices.translate(i * 0.56F, -0.52F, -0.72F);
                  matrices.translate((0.3F - panderAnim * 0.15F) * i, 0.2F - panderF * 0.12F, -0.15F - panderAnim * 0.13F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76.0F - 10.0F * panderAnim) * i));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16.0F - 8.0F * panderAnim) * i));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * panderAnim));
                  break;
               case "Slant": {
                  float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
                  float rotate = 35.0F * strength;
                  matrices.translate(i * 0.56F, -0.52F, -0.72F);
                  matrices.translate(0.0F, 0.0F, -0.3F * anim * strength);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
                  break;
               }
               default:
                  handled = false;
            }

            if (handled) {
               e.cancel();
            }
         }
      }
   }

   private void applyEquipOffset(MatrixStack matrices, int i, float equipProgress) {
      matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(MatrixStack matrices, int i, float swingProgress, float strength) {
      float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      matrices.translate(0.56F * i, -0.52F, -0.72F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F * strength)));
      float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F * strength));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F * strength));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
   }
}
