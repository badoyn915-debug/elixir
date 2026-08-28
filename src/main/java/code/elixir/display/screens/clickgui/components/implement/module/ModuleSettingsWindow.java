package code.elixir.display.screens.clickgui.components.implement.module;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.features.module.Module;
import code.elixir.features.module.setting.SettingComponentAdder;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ModuleSettingsWindow extends AbstractWindow {
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   public final Module module;

   public ModuleSettingsWindow(Module module) {
      this.module = module;
      new SettingComponentAdder().addSettingComponent(module.settings(), this.components, module);
      this.draggable(true);
   }

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
      this.height = MathHelper.clamp(this.getComponentHeight() + 5, 0, 200);
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(8.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(new Color(33, 33, 33, 255).getRGB())
            .color(new Color(18, 19, 20, 255).getRGB())
            .build()
      );
      rectangle.render(ShapeProperties.create(matrix, this.x, this.y + 22.0F, this.width, 0.5).color(new Color(48, 48, 48, 255).getRGB()).build());
      Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold)
         .drawGradientString(
            context.getMatrices(),
            this.module.getVisibleName() + " Settings",
            this.x + 10.0F,
            this.y + 10.0F,
            ColorAssist.getText(),
            new Color(165, 165, 165, 255).getRGB()
         );
      Fonts.getSize(17, Fonts.Type.ICONS).drawString(context.getMatrices(), "K", this.x + 145.0F, this.y + 10.0F, ColorAssist.getText(0.5F));
      boolean isLimitedHeight = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F;
      if (isLimitedHeight) {
         scissorManager.push(matrix.peek().getPositionMatrix(), this.x, this.y + 23.0F, this.width, this.height - 24.0F);
      }

      float offset = 0.0F;
      int totalHeight = 0;

      for (int i = this.components.size() - 1; i >= 0; i--) {
         AbstractSettingComponent component = this.components.get(i);
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            component.x = this.x;
            component.y = (float)(this.y + 22.0F + offset + (this.getComponentHeight() - 25 - component.height) + this.smoothedScroll);
            component.width = this.width;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int)component.height;
         }
      }

      if (isLimitedHeight) {
         scissorManager.pop();
      }

      int maxScroll = (int)Math.max(0.0F, totalHeight - (this.height - 28.0F));
      this.scroll = MathHelper.clamp(this.scroll, -maxScroll, 0.0);
      this.smoothedScroll = MathHelper.lerp(0.1F, this.smoothedScroll, this.scroll);
      if (isLimitedHeight) {
         float viewableHeight = this.height - 30.0F;
         float scrollbarHeight = Math.max(20.0F, viewableHeight / totalHeight * viewableHeight);
         float scrollPercent = (float)(-this.smoothedScroll / maxScroll);
         float scrollbarY = this.y + 30.0F + scrollPercent * (viewableHeight - scrollbarHeight);
         float scrollbarX = this.x + this.width - 6.0F;
         float scrollbarWidth = 3.0F;
         rectangle.render(
            ShapeProperties.create(matrix, scrollbarX, this.y + 30.0F, scrollbarWidth, viewableHeight - 6.0F)
               .round(1.0F)
               .color(new Color(30, 30, 30, 100).getRGB())
               .build()
         );
         rectangle.render(
            ShapeProperties.create(matrix, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight - 6.0F)
               .round(1.5F)
               .color(new Color(100, 100, 100, 180).getRGB())
               .build()
         );
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (Calculate.isHovered(mouseX, mouseY, this.x + 145.0F, this.y + 5.0F, 15.0, 15.0)) {
            this.startCloseAnimation();
            return true;
         }

         if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 19.0)) {
            this.dragging = true;
            this.dragX = (int)(this.x - mouseX);
            this.dragY = (int)(this.y - mouseY);
            return true;
         }
      }

      if (this.components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY))) {
         this.components.forEach(abstractComponent -> {
            if (abstractComponent.isHover(mouseX, mouseY)) {
               abstractComponent.mouseClicked(mouseX, mouseY, button);
            }
         });
         return true;
      } else {
         this.components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
         return true;
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      this.components.forEach(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));

      for (AbstractComponent abstractComponent2 : this.components) {
         if (abstractComponent2.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHovered(mouseX, mouseY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      this.components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      boolean scrolled = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F && Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      if (scrolled) {
         this.scroll += amount * 20.0;
      }

      this.components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
      return scrolled;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   public int getComponentHeight() {
      float offsetY = 0.0F;

      for (AbstractSettingComponent component : this.components) {
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            offsetY += component.height;
         }
      }

      return (int)(offsetY + 25.0F);
   }
}
