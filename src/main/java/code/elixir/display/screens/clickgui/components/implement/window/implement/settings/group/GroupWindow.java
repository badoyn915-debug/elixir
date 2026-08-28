package code.elixir.display.screens.clickgui.components.implement.window.implement.settings.group;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.elixir.features.module.setting.SettingComponentAdder;
import code.elixir.features.module.setting.implement.GroupSetting;
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

public class GroupWindow extends AbstractWindow {
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   private final GroupSetting setting;

   public GroupWindow(GroupSetting setting) {
      this.setting = setting;
      new SettingComponentAdder().addSettingComponent(setting.getSubSettings(), this.components, null);
   }

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
      this.height = MathHelper.clamp(this.getComponentHeight(), 0, 200);
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width + 30.0F, this.height)
            .round(4.0F)
            .softness(2.0F)
            .thickness(1.0F)
            .outlineColor(new Color(75, 75, 75, 255).getRGB())
            .color(
               new Color(14, 14, 16, 255).getRGB(),
               new Color(31, 27, 35, 255).getRGB(),
               new Color(31, 27, 35, 255).getRGB(),
               new Color(14, 14, 16, 255).getRGB()
            )
            .build()
      );
      Fonts.getSize(15, Fonts.Type.SEMI)
         .drawGradientString(
            context.getMatrices(),
            this.setting.getName() + " Settings",
            this.x + 10.0F,
            this.y + 10.0F,
            ColorAssist.getText(),
            new Color(165, 165, 165, 255).getRGB()
         );
      boolean isLimitedHeight = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F;
      if (isLimitedHeight) {
         scissorManager.push(matrix.peek().getPositionMatrix(), this.x, this.y + 23.0F, this.width, this.height - 28.0F);
      }

      float offset = 0.0F;
      int totalHeight = 0;

      for (int i = this.components.size() - 1; i >= 0; i--) {
         AbstractSettingComponent component = this.components.get(i);
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            component.x = this.x;
            component.y = (float)(this.y + 19.0F + offset + (this.getComponentHeight() - 25 - component.height) + this.smoothedScroll);
            component.width = this.width + 30.0F;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int)component.height;
         }
      }

      if (isLimitedHeight) {
         scissorManager.pop();
      }

      int maxScroll = (int)Math.max(0.0F, totalHeight - (this.height - 23.0F));
      this.scroll = MathHelper.clamp(this.scroll, -maxScroll, 0.0);
      this.smoothedScroll = MathHelper.lerp(0.1F, this.smoothedScroll, this.scroll);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.draggable(Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 19.0) && button == 0);
      boolean isAnyComponentHovered = this.components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
      if (isAnyComponentHovered) {
         this.components.forEach(abstractComponent -> {
            if (abstractComponent.isHover(mouseX, mouseY)) {
               abstractComponent.mouseClicked(mouseX, mouseY, button);
            }
         });
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         this.components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
         return super.mouseClicked(mouseX, mouseY, button);
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

      return super.isHover(mouseX, mouseY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
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

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public GroupSetting getSetting() {
      return this.setting;
   }
}
