package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.common.animation.inovated.EasingList;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.animation.util.Animation;
import code.elixir.utils.animation.util.Easings;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class SliderComponent extends AbstractSettingComponent {
   private final SliderSettings setting;
   private final Animation valueAnimation = new Animation();
   private final Animation movingAnimation = new Animation();
   private boolean drag;
   private float hoverAnimation;
   public static SliderComponent current;

   public SliderComponent(SliderSettings setting) {
      super(setting);
      this.setting = setting;
      this.height = setting.isBarOnly() ? 22.0F : 29.0F;
      this.valueAnimation.set(setting.getValue());
      this.movingAnimation.set(0.0);
   }

   public boolean isDragging() {
      return this.drag;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float sliderX = this.x + 10.0F;
      float sliderY = this.y + 1.0F;
      float sliderW = this.width - 20.0F;
      float leftPadding = 10.0F;

      if (GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) != 1) {
         this.drag = false;
      }

      if (this.drag) {
         this.valueAnimation.set(this.setting.getValue());
         current = this;
      } else {
         this.valueAnimation.run(this.setting.getValue(), 0.25, Easings.CUBIC_OUT, true);
         this.valueAnimation.update();
      }

      this.hoverAnimation = MathHelper.lerp(0.22F, this.hoverAnimation, this.isHover(mouseX, mouseY) ? 1.0F : 0.0F);
      float currentAnimated = (float)this.valueAnimation.getValue();
      float currentPercent = this.clampPercent(currentAnimated);
      float knobCenterX = sliderX + sliderW * currentPercent;

      rectangle.render(
         ShapeProperties.create(context.getMatrices(), sliderX, sliderY + this.height - 12.25F, sliderW, 2.5)
            .round(1.25F)
            .color(ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), ThemeManager.isLiquidGlassTheme() ? 58 : 88))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), sliderX, sliderY + this.height - 12.25F, sliderW * currentPercent, 2.5)
            .round(1.25F)
            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
            .build()
      );

      if (!this.setting.isBarOnly()) {
         this.movingAnimation.run(this.drag ? 1.0 : 0.0, 0.2, EasingList.CIRC_BOTH, true);
         this.movingAnimation.update();
         float moving = (float)this.movingAnimation.getValue();
         if (ThemeManager.isLiquidGlassTheme()) {
            rectangle.render(
               ShapeProperties.create(
                     context.getMatrices(),
                     knobCenterX - 4.5F - 3.0F * moving,
                     sliderY + this.height - 15.0F - 1.5F * moving,
                     9.0F + 6.0F * moving,
                     6.0F + 4.0F * moving
                  )
                  .round((6.0F + 4.0F * moving) / 2.0F)
                  .color(ColorAssist.applyOpacity(-16777216, (int)(75.0F + 50.0F * moving)))
                  .build()
            );
            rectangle.render(
               ShapeProperties.create(
                     context.getMatrices(),
                     knobCenterX - 4.5F - 3.0F * moving,
                     sliderY + this.height - 15.0F - 1.5F * moving,
                     9.0F + 6.0F * moving,
                     6.0F + 4.0F * moving
                  )
                  .round((6.0F + 4.0F * moving) / 2.0F)
                  .color(ColorAssist.applyOpacity(-1, (int)(255.0F * (1.0F - moving))))
                  .build()
            );
         } else {
            float knobW = 8.0F + moving * 2.0F;
            float knobH = 5.0F + moving * 1.5F;
            float knobCenterY = sliderY + this.height - 11.0F;
            rectangle.render(
               ShapeProperties.create(context.getMatrices(), knobCenterX - knobW / 2.0F, knobCenterY - knobH / 2.0F + 0.5F, knobW, knobH)
                  .round(knobH / 2.0F)
                  .color(ColorAssist.applyOpacity(-16777216, 64))
                  .build()
            );
            rectangle.render(
               ShapeProperties.create(context.getMatrices(), knobCenterX - knobW / 2.0F, knobCenterY - knobH / 2.0F, knobW, knobH)
                  .round(knobH / 2.0F)
                  .color(ThemeManager.textColor.getColor())
                  .build()
            );
         }
      }

      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(
            context.getMatrices(),
            this.setting.getName(),
            this.x + leftPadding,
            sliderY + 11.0F - 4.0F,
            ColorAssist.lerp(this.hoverAnimation, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 191), ThemeManager.textColor.getColor())
         );
      String value = this.formatValue(currentAnimated);
      float valueWidth = Fonts.getSize(11, Fonts.Type.ROCKSTAR_SEMIBOLD).getStringWidth(value);
      Fonts.getSize(11, Fonts.Type.ROCKSTAR_SEMIBOLD)
         .drawString(
            context.getMatrices(),
            value,
            sliderX + sliderW - valueWidth,
            sliderY + 11.0F - 3.5F,
            ColorAssist.lerp(this.hoverAnimation, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 191), ThemeManager.textColor.getColor())
         );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         this.drag = true;
         current = this;
         this.updateValue(mouseX);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.drag) {
         this.updateValue(mouseX);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.drag = false;
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 262 || keyCode == 263) && current == this) {
         float step = this.setting.getStep() <= 0.0F ? 0.01F : this.setting.getStep();
         float delta = step * 0.7F * (keyCode == 262 ? 1.0F : -1.0F);
         this.setting.setValue(this.clampAndSnap(this.setting.getValue() + delta));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   private void updateValue(double mouseX) {
      float sliderX = this.x + 10.0F;
      float sliderW = this.width - 20.0F;
      this.setting.setValue(this.getSliderValue(sliderX, sliderW, mouseX));
   }

   private float getSliderValue(float sliderX, float sliderW, double mouseX) {
      float percent = MathHelper.clamp((float)((mouseX - sliderX) / Math.max(1.0F, sliderW)), 0.0F, 1.0F);
      float raw = this.setting.getMin() + (this.setting.getMax() - this.setting.getMin()) * percent;
      return this.clampAndSnap(raw);
   }

   private float clampAndSnap(float value) {
      float step = this.setting.getStep() <= 0.0F ? 0.01F : this.setting.getStep();
      float snapped = Math.round(value / step) * step;
      return MathHelper.clamp(snapped, this.setting.getMin(), this.setting.getMax());
   }

   private float clampPercent(float value) {
      float range = Math.max(1.0E-4F, this.setting.getMax() - this.setting.getMin());
      return MathHelper.clamp((value - this.setting.getMin()) / range, 0.0F, 1.0F);
   }

   private String formatValue(float value) {
      if (this.setting.isInteger()) {
         return String.valueOf((int)value);
      } else {
         float step = this.setting.getStep() <= 0.0F ? 0.01F : this.setting.getStep();
         int precision = 0;
         String stepText = String.valueOf(step);
         int dot = stepText.indexOf(46);
         if (dot >= 0) {
            precision = stepText.length() - dot - 1;
         }

         String format = "%." + Math.max(1, precision) + "f";
         return String.format(Locale.US, format, value).replaceAll("0+$", "").replaceAll("\\.$", "");
      }
   }
}
