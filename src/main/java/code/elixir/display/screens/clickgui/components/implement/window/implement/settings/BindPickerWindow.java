package code.elixir.display.screens.clickgui.components.implement.window.implement.settings;

import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.module.Module;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.bind.BindCodes;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class BindPickerWindow extends AbstractWindow {
   private final Object target;
   private boolean listening = true;

   public BindPickerWindow(BindSetting setting) {
      this.target = setting;
   }

   public BindPickerWindow(Module module) {
      this.target = module;
   }

   public String getTitle() {
      if (this.target instanceof BindSetting setting) {
         return setting.getName();
      } else {
         return this.target instanceof Module module ? module.getVisibleName() : "Бинд";
      }
   }

   private int getBindKey() {
      if (this.target instanceof BindSetting setting) {
         return setting.getKey();
      } else {
         return this.target instanceof Module module ? module.getKey() : -1;
      }
   }

   private void applyBind(int key) {
      if (this.target instanceof BindSetting setting) {
         setting.setKey(key);
         setting.clearSecondKey();
      } else if (this.target instanceof Module module) {
         module.setKey(key);
         module.setSecondKey(-1);
      }

      this.listening = false;
   }

   private void clearBind() {
      this.applyBind(-1);
      this.startCloseAnimation();
   }

   @Override
   protected void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      ClickGui clickGui = ClickGui.getInstance();
      if (ThemeManager.isLiquidGlassTheme() || clickGui != null && clickGui.shouldRenderBackgroundBlur()) {
         Render2D.rectangleWithMask(matrices.peek().getPositionMatrix(), this.x, this.y, this.width, this.height, 8.0F, 0, KawaseBlur.INSTANCE.getBlurTexture());
      }

      rectangle.render(
         ShapeProperties.create(matrices, this.x, this.y, this.width, this.height)
            .round(8.0F)
            .thickness(1.5F)
            .outlineColor(ColorAssist.applyOpacity(ColorAssist.getOutline(), 180))
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrices, this.x, this.y, this.width, 2.0)
            .round(2.0F)
            .color(ColorAssist.applyOpacity(ColorAssist.getClientColor(), 200))
            .build()
      );
      Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).drawString(matrices, this.getTitle(), this.x + 10.0F, this.y + 11.0F, ThemeManager.textColor.getColor());
      float closeSize = 10.0F;
      float closeX = this.x + this.width - closeSize - 8.0F;
      float closeY = this.y + 8.0F;
      boolean closeHovered = Calculate.isHovered(mouseX, mouseY, closeX - 2.0F, closeY - 2.0F, closeSize + 4.0F, closeSize + 4.0F);
      Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawCenteredString(
            matrices, "×", closeX + closeSize / 2.0F, closeY + 1.0F, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), closeHovered ? 255 : 150)
         );
      String pillText = this.listening ? "Нажмите клавишу..." : BindCodes.getDisplayName(this.getBindKey());
      int pillFontSize = this.listening ? 11 : 12;
      float pillTextWidth = Fonts.getSize(pillFontSize, Fonts.Type.ROCKSTAR_SEMIBOLD).getStringWidth(pillText);
      float pillW = Math.max(118.0F, pillTextWidth + 22.0F);
      float pillH = 22.0F;
      float pillX = this.x + (this.width - pillW) / 2.0F;
      float pillY = this.y + 34.0F;
      long pulse = System.currentTimeMillis();
      int pulseAlpha = this.listening ? (int)(120.0 + 80.0 * Math.abs(Math.sin(pulse / 220.0))) : 90;
      int pillOutline = this.listening
         ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha + 60)
         : ColorAssist.applyOpacity(ColorAssist.getOutline(), 160);
      int pillFill = this.listening
         ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha)
         : ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), 130);
      rectangle.render(
         ShapeProperties.create(matrices, pillX, pillY, pillW, pillH)
            .round(6.0F)
            .thickness(this.listening ? 1.2F : 0.8F)
            .outlineColor(pillOutline)
            .color(pillFill)
            .build()
      );
      Fonts.getSize(pillFontSize, Fonts.Type.ROCKSTAR_SEMIBOLD)
         .drawCenteredString(
            matrices, pillText, pillX + pillW / 2.0F, pillY + 8.5F, this.listening ? ColorAssist.getClientColor() : ThemeManager.textColor.getColor()
         );
      String hint = this.listening ? "Клавиатура, мышь, колёсико • Esc — сбросить" : "Esc — сбросить • × — закрыть";
      float hintW = Fonts.getSize(10, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(hint);
      Fonts.getSize(10, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(
            matrices,
            hint,
            this.x + (this.width - hintW) / 2.0F,
            this.y + this.height - 14.0F,
            ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 145)
         );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float closeSize = 10.0F;
      float closeX = this.x + this.width - closeSize - 8.0F;
      float closeY = this.y + 8.0F;
      if (button == 0 && Calculate.isHovered(mouseX, mouseY, closeX - 2.0F, closeY - 2.0F, closeSize + 4.0F, closeSize + 4.0F)) {
         this.startCloseAnimation();
         return true;
      } else {
         float pillW = 118.0F;
         float pillH = 22.0F;
         float pillX = this.x + (this.width - pillW) / 2.0F;
         float pillY = this.y + 34.0F;
         if (button == 0 && Calculate.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH)) {
            this.listening = true;
            return true;
         } else if (this.listening && BindCodes.canBind(button)) {
            this.applyBind(button);
            return true;
         } else {
            this.draggable(Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 18.0));
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.listening && amount != 0.0) {
         this.applyBind(amount > 0.0 ? 998 : 999);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         if (this.listening) {
            this.clearBind();
         } else {
            this.startCloseAnimation();
         }

         return true;
      } else if (this.listening && BindCodes.canBind(keyCode)) {
         this.applyBind(keyCode);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public Object getTarget() {
      return this.target;
   }
}
