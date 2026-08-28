package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.other.ToggleSwitchRenderer;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.bind.BindCaptureHelper;
import code.elixir.utils.client.bind.BindCodes;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class BindComponent extends AbstractSettingComponent {
   private final BindSetting setting;

   public BindComponent(BindSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 21.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      FontRenderer labelFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      ToggleSwitchRenderer.drawLabel(labelFont, matrices, this.setting.getName(), this.x + 10.0F, this.y, this.height, ThemeManager.textColor.getColor());
      this.renderBindPill(context, mouseX, mouseY, labelFont);
   }

   private void renderBindPill(DrawContext context, int mouseX, int mouseY, FontRenderer labelFont) {
      MatrixStack matrices = context.getMatrices();
      boolean listening = BindCaptureHelper.isCapturing(this.setting);
      String keyText = listening ? "..." : BindCodes.getDisplayName(this.setting.getKey());
      FontRenderer pillFont = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD);
      float pillW = ToggleSwitchRenderer.bindPillWidth(keyText);
      float pillH = 14.0F;
      float pillX = this.x + this.width - 10.0F - pillW;
      float pillY = ToggleSwitchRenderer.bindPillTrackY(labelFont, this.setting.getName(), this.y, this.height);
      boolean hovered = Calculate.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH);
      long pulse = System.currentTimeMillis();
      int pulseAlpha = listening ? (int)(120.0 + 80.0 * Math.abs(Math.sin(pulse / 220.0))) : 0;
      int outlineColor = listening
         ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha + 80)
         : ColorAssist.applyOpacity(hovered ? ColorAssist.getClientColor() : ColorAssist.getOutline(), hovered ? 200 : 130);
      int fillColor = listening
         ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha)
         : ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), hovered ? 165 : 120);
      rectangle.render(
         ShapeProperties.create(matrices, pillX, pillY, pillW, pillH)
            .round(5.0F)
            .thickness(listening ? 1.2F : (hovered ? 1.0F : 0.7F))
            .outlineColor(outlineColor)
            .color(fillColor)
            .build()
      );
      ToggleSwitchRenderer.drawPillTextCentered(
         pillFont, matrices, keyText, pillX, pillY, pillW, pillH, listening ? ColorAssist.getClientColor() : ThemeManager.textColor.getColor()
      );
   }

   private float[] getPillBounds() {
      FontRenderer labelFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      boolean listening = BindCaptureHelper.isCapturing(this.setting);
      String keyText = listening ? "..." : BindCodes.getDisplayName(this.setting.getKey());
      float pillW = ToggleSwitchRenderer.bindPillWidth(keyText);
      float pillH = 14.0F;
      float pillX = this.x + this.width - 10.0F - pillW;
      float pillY = ToggleSwitchRenderer.bindPillTrackY(labelFont, this.setting.getName(), this.y, this.height);
      return new float[]{pillX, pillY, pillW, pillH};
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float[] pill = this.getPillBounds();
      float pillX = pill[0];
      float pillY = pill[1];
      float pillW = pill[2];
      float pillH = pill[3];
      if (BindCaptureHelper.isCapturing(this.setting)) {
         if (button == 0 && Calculate.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH)) {
            BindCaptureHelper.cancel();
            return true;
         } else {
            return BindCaptureHelper.handleMouse(button);
         }
      } else if (button != 0) {
         return false;
      } else if (!Calculate.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH)
         && !Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         return false;
      } else {
         BindCaptureHelper.toggle(this.setting);
         return true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return BindCaptureHelper.isCapturing(this.setting) ? BindCaptureHelper.handleKey(keyCode, scanCode) : false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      return BindCaptureHelper.isCapturing(this.setting) ? BindCaptureHelper.handleScroll(amount) : false;
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
