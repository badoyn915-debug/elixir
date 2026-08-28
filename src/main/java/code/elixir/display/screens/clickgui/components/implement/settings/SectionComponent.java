package code.elixir.display.screens.clickgui.components.implement.settings;

import code.elixir.features.module.setting.SettingComponentAdder;
import code.elixir.features.module.setting.implement.SectionSetting;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class SectionComponent extends AbstractSettingComponent {
   private static final float HEADER_HEIGHT = 19.0F;
   private final SectionSetting setting;
   private final List<AbstractSettingComponent> children = new ArrayList<>();
   private boolean expanded;

   public SectionComponent(SectionSetting setting) {
      super(setting);
      this.setting = setting;
      this.height = 19.0F;
      new SettingComponentAdder().addSettingComponent(setting.getSubSettings(), this.children, setting.getBoundModule());
   }

   private float childrenHeight() {
      float total = 0.0F;

      for (AbstractSettingComponent child : this.children) {
         if (child.getSetting().isVisible()) {
            total += child.height + 1.0F;
         }
      }

      return total;
   }

   private void layoutChildren() {
      float cursorY = this.y + 19.0F;

      for (AbstractSettingComponent child : this.children) {
         if (child.getSetting().isVisible()) {
            child.x = this.x + 8.0F;
            child.y = cursorY;
            child.width = this.width - 10.0F;
            cursorY += child.height + 1.0F;
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float f = this.height = this.expanded ? 19.0F + this.childrenHeight() : 19.0F;
      if (this.expanded) {
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), this.x + 2.0F, this.y + 19.0F - 1.0F, 1.2F, this.childrenHeight() - 2.0F)
               .round(1.0F)
               .color(ColorAssist.applyOpacity(ColorAssist.getClientColor(), this.setting.isOn() ? 160 : 40))
               .build()
         );
      }

      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(context.getMatrices(), this.setting.getName(), this.x + 6.0F, this.y + 10.0F, ThemeManager.textColor.getColor());
      String arrow = this.expanded ? "j" : "i";
      Fonts.getSize(13, Fonts.Type.ELIXIR_ICONS)
         .drawString(context.getMatrices(), arrow, this.x + this.width - 26.0F, this.y + 4.5F, ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 160));
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + this.width - 14.0F, this.y + 6.0F, 8.0, 8.0)
            .round(2.0F)
            .color(this.setting.isOn() ? ColorAssist.getClientColor() : ColorAssist.applyOpacity(ThemeManager.textColor.getColor(), 35))
            .build()
      );
      if (this.expanded) {
         this.layoutChildren();

         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible()) {
               child.render(context, mouseX, mouseY, delta);
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.expanded) {
         this.layoutChildren();

         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible()
               && Calculate.isHovered(mouseX, mouseY, child.x, child.y, child.width, child.height)
               && child.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }
      }

      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 19.0)) {
         if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, this.x + this.width - 16.0F, this.y + 4.0F, 12.0, 12.0)) {
               this.setting.toggle();
            } else {
               this.expanded = !this.expanded;
            }

            return true;
         }

         if (button == 1) {
            this.expanded = !this.expanded;
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.layoutChildren();

      for (AbstractSettingComponent child : this.children) {
         child.mouseReleased(mouseX, mouseY, button);
      }

      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.expanded) {
         this.layoutChildren();

         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible() && child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.expanded) {
         this.layoutChildren();

         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible() && child.mouseScrolled(mouseX, mouseY, amount)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.expanded) {
         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible() && child.keyPressed(keyCode, scanCode, modifiers)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.expanded) {
         for (AbstractSettingComponent child : this.children) {
            if (child.getSetting().isVisible() && child.charTyped(chr, modifiers)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void tick() {
      for (AbstractSettingComponent child : this.children) {
         child.tick();
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }
}
