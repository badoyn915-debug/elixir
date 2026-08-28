package code.elixir.display.screens.clickgui.components.implement.panel;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.customization.CustomizationComponent;
import code.elixir.display.screens.clickgui.components.implement.module.ModuleComponent;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class PanelComponent extends AbstractComponent implements QuickImports {
   private final List<ModuleComponent> moduleComponents = new ArrayList<>();
   private final ModuleCategory category;
   private final int index;
   private final CustomizationComponent customizationComponent;
   private static final Logger logger = Logger.getLogger(PanelComponent.class.getName());
   private float scroll = 0.0F;
   private float smoothedScroll = 0.0F;
   private boolean searching = false;
   private String searchText = "";

   public PanelComponent(ModuleCategory category, int index) {
      this.category = category;
      this.index = index;
      this.customizationComponent = new CustomizationComponent();
      this.initializeModules();
   }

   private boolean shouldRenderCustomization() {
      try {
         ClickGui clickGui = ClickGui.getInstance();
         return clickGui != null && clickGui.shouldRenderCustomization();
      } catch (Exception var2) {
         return true;
      }
   }

   private void initializeModules() {
      for (Module module : Elixir.getInstance().getModuleRepository().modules()) {
         if (module.getCategory() == this.category) {
            this.moduleComponents.add(new ModuleComponent(module));
         }
      }
   }

   public List<ModuleComponent> getModuleComponents() {
      return this.moduleComponents;
   }

   public boolean isModuleVisible(ModuleComponent component) {
      if (component == null) {
         return false;
      } else {
         float categoryTop = this.y + 35.0F - 8.0F;
         float categoryBottom = this.y + 35.0F + 22.0F;
         float moduleStartY;
         float clipTop = moduleStartY = categoryBottom + 4.0F;
         float clipBottom = this.y + this.height;
         float componentY = component.y;
         float componentHeight = component.getComponentHeight();
         float componentBottom = componentY + componentHeight;
         return componentBottom >= clipTop && componentY <= clipBottom;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.category != ModuleCategory.CUSTOMIZATION || this.shouldRenderCustomization()) {
         MatrixStack matrix = context.getMatrices();
         if (ThemeManager.isLiquidGlassTheme()) {
            this.blur(context, matrix, mouseX, mouseY);
         }

         this.drawPanelMainBackground(context, matrix, mouseX, mouseY);
         this.drawModulesVertical(context, matrix, mouseX, mouseY, delta);
         this.drawCategoryBackground(context, matrix, mouseX, mouseY);
         rectangle.render(ShapeProperties.create(matrix, this.x - 6.0F, this.y + 22.0F, this.width + 12.0F, 30.0).round(15.0F).color(255, 255, 255, 20).build());
         rectangle.render(ShapeProperties.create(matrix, this.x - 4.0F, this.y + 24.0F, this.width + 8.0F, 26.0).round(12.0F).color(255, 255, 255, 40).build());
         rectangle.render(ShapeProperties.create(matrix, this.x - 2.0F, this.y + 26.0F, this.width + 4.0F, 22.0).round(8.0F).color(255, 255, 255, 60).build());
         rectangle.render(ShapeProperties.create(matrix, this.x + 2.0F, this.y + 28.0F, this.width - 13.0F, 18.0).round(6.0F).color(255, 255, 255, 30).build());
      }
   }

   private void blur(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
      Render2D.rectangleWithMask(
         matrix.peek().getPositionMatrix(),
         this.x - 2.0F,
         this.y + 31.0F,
         this.width + 4.0F,
         this.height - 27.0F,
         6.0F,
         0,
         KawaseBlur.INSTANCE.getBlurTexture()
      );
   }

   private void drawPanelMainBackground(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
      rectangle.render(
         ShapeProperties.create(matrix, this.x - 2.0F, this.y + 31.0F, this.width + 4.0F, this.height - 27.0F)
            .round(6.0F)
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
   }

   private void drawCategoryBackground(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
      float moduleWidth = this.width - 4.0F;
      float categoryWidth = moduleWidth + 3.0F;
      float categoryX = this.x + 3.0F - 2.5F;
      float categoryY = this.y + 34.0F;
      rectangle.render(
         ShapeProperties.create(matrix, categoryX, categoryY, categoryWidth, 22.0)
            .round(4.0F)
            .softness(4.0F)
            .color(ColorAssist.getClientColor(1.0F), ColorAssist.getClientColor(1.0F), ColorAssist.getClientColor2(1.0F), ColorAssist.getClientColor2(1.0F))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, categoryX - 4.0F, categoryY - 4.0F, categoryWidth + 8.0F, 30.0)
            .round(8.0F)
            .softness(4.0F)
            .color(new Color(255, 255, 255, 5).getRGB())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, categoryX - 3.0F, categoryY - 3.0F, categoryWidth + 6.0F, 28.0)
            .round(7.0F)
            .softness(3.0F)
            .color(ColorAssist.getClientColor(0.1F), ColorAssist.getClientColor(0.1F), ColorAssist.getClientColor2(0.1F), ColorAssist.getClientColor2(0.1F))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, categoryX - 2.0F, categoryY - 2.0F, categoryWidth + 4.0F, 26.0)
            .round(6.0F)
            .softness(2.0F)
            .color(ColorAssist.getClientColor(0.1F), ColorAssist.getClientColor(0.1F), ColorAssist.getClientColor2(0.1F), ColorAssist.getClientColor2(0.1F))
            .build()
      );
      String icon = this.getCategoryIcon();
      boolean isRenderOrMisc = this.category == ModuleCategory.RENDER || this.category == ModuleCategory.MISC;
      int iconFontSize = isRenderOrMisc ? 80 : 18;
      float scale = isRenderOrMisc ? 0.23684211F : 1.0F;
      float iconSize = Fonts.getSize(iconFontSize, Fonts.Type.ELIXIR_ICONS).getStringWidth(icon) * scale;
      float nameWidth = Fonts.getSize(18, Fonts.Type.SuisseIntlSemiBold).getStringWidth(this.category.getReadableName());
      float categoryCenterX = categoryX + categoryWidth / 2.0F;
      float categoryCenterY = categoryY + 11.0F;
      float totalTextWidth = nameWidth + iconSize + 5.0F;
      float textStartX = categoryCenterX - totalTextWidth / 2.0F;
      float iconY = categoryCenterY - 1.0F;
      if (isRenderOrMisc) {
         iconY = categoryCenterY - 3.5F;
      }

      if (isRenderOrMisc) {
         matrix.push();
         matrix.translate(textStartX, iconY, 0.0F);
         matrix.scale(scale, scale, 1.0F);
         Fonts.getSize(iconFontSize, Fonts.Type.ELIXIR_ICONS).drawString(matrix, icon, 0.0, 0.0, ThemeManager.textColor.getColor());
         matrix.pop();
      } else {
         Fonts.getSize(iconFontSize, Fonts.Type.ELIXIR_ICONS).drawString(matrix, icon, textStartX, iconY, ThemeManager.textColor.getColor());
      }

      FontRenderer fontCategory = Fonts.getSize(18, Fonts.Type.SuisseIntlMedium);
      fontCategory.drawString(matrix, this.category.getReadableName(), textStartX + iconSize + 5.0F, categoryCenterY - 1.5F, ThemeManager.textColor.getColor());
   }

   private void drawModulesVertical(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
      float totalModuleHeight = 0.0F;
      if (this.category == ModuleCategory.CUSTOMIZATION && this.shouldRenderCustomization()) {
         totalModuleHeight += this.customizationComponent.height + 2.0F;
      }

      for (ModuleComponent component : this.moduleComponents) {
         if (this.shouldRenderModule(component)) {
            totalModuleHeight += component.getComponentHeight() + 2;
         }
      }

      float categoryTop = this.y + 35.0F - 8.0F;
      float categoryBottom = this.y + 35.0F + 22.0F;
      float moduleStartY = categoryBottom + 4.0F;
      float visibleHeight = this.y + this.height - moduleStartY;
      if (totalModuleHeight > visibleHeight) {
         float maxScroll = -(totalModuleHeight - visibleHeight);
         this.scroll = MathHelper.clamp(this.scroll, maxScroll, 0.0F);
      } else {
         this.scroll = 0.0F;
      }

      this.smoothedScroll = Calculate.interpolateSmooth(2.0, this.smoothedScroll, this.scroll);
      Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
      ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
      float clipTop = moduleStartY;
      float clipBottom = this.y + this.height;
      scissorManager.push(positionMatrix, this.x, moduleStartY, this.width, clipBottom - moduleStartY);
      float moduleX = this.x + 1.5F;
      float moduleY = moduleStartY + this.smoothedScroll;
      if (this.category == ModuleCategory.CUSTOMIZATION && this.shouldRenderCustomization()) {
         float customizationHeight = this.customizationComponent.height + 2.0F;
         float customizationBottom = moduleY + this.customizationComponent.height;
         if (customizationBottom >= moduleStartY && moduleY <= clipBottom) {
            this.customizationComponent.x = moduleX;
            this.customizationComponent.y = moduleY;
            this.customizationComponent.width = this.width - 3.0F;
            this.customizationComponent.render(context, mouseX, mouseY, delta);
         }

         moduleY += customizationHeight;
      }

      for (ModuleComponent componentx : this.moduleComponents) {
         if (this.shouldRenderModule(componentx)) {
            componentx.x = moduleX;
            componentx.y = moduleY;
            componentx.width = this.width - 3.0F;
            float componentBottom = moduleY + componentx.getComponentHeight();
            if (componentBottom >= clipTop && moduleY <= clipBottom) {
               int heightBefore = componentx.getComponentHeight();
               componentx.render(context, mouseX, mouseY, delta);
               int var21 = componentx.getComponentHeight();
            }

            float settingsBottomY = componentx.getSettingsBottomY();
            moduleY = settingsBottomY + 2.0F;
         }
      }

      scissorManager.pop();
      if (totalModuleHeight > visibleHeight) {
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         ArrayList<ModuleComponent> visibleModules = new ArrayList<>();
         float categoryTop = this.y + 35.0F - 8.0F;
         float categoryBottom = this.y + 35.0F + 22.0F;
         float moduleStartY = categoryBottom + 4.0F;
         float moduleY = moduleStartY + this.smoothedScroll;
         float clipTop = moduleStartY;
         float clipBottom = this.y + this.height;
         if (this.category == ModuleCategory.CUSTOMIZATION && this.shouldRenderCustomization()) {
            int customizationHeight = (int)(this.customizationComponent.height + 2.0F);
            float customizationBottom = moduleY + customizationHeight;
            if (customizationBottom >= moduleStartY && moduleY <= clipBottom) {
               this.customizationComponent.x = this.x + 1.5F;
               this.customizationComponent.y = moduleY;
               this.customizationComponent.width = this.width - 3.0F;
               if (this.customizationComponent.mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }
            }

            moduleY += customizationHeight;
         }

         for (ModuleComponent component : this.moduleComponents) {
            if (this.shouldRenderModule(component)) {
               component.x = this.x + 1.5F;
               component.y = moduleY;
               component.width = this.width - 3.0F;
               float moduleBottom = moduleY + component.getComponentHeight();
               if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                  visibleModules.add(component);
               }

               float settingsBottomY = component.getSettingsBottomY();
               moduleY = settingsBottomY + 2.0F;
            }
         }

         for (int i = visibleModules.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = visibleModules.get(i);
            if (moduleComponent.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         ArrayList<ModuleComponent> visibleModules = new ArrayList<>();
         float categoryTop = this.y + 35.0F - 8.0F;
         float categoryBottom = this.y + 35.0F + 22.0F;
         float moduleStartY = categoryBottom + 4.0F;
         float moduleY = moduleStartY + this.smoothedScroll;
         float clipTop = moduleStartY;
         float clipBottom = this.y + this.height;
         if (this.category == ModuleCategory.CUSTOMIZATION && this.shouldRenderCustomization()) {
            int customizationHeight = (int)(this.customizationComponent.height + 2.0F);
            float customizationBottom = moduleY + customizationHeight;
            if (customizationBottom >= moduleStartY && moduleY <= clipBottom) {
               this.customizationComponent.x = this.x + 1.5F;
               this.customizationComponent.y = moduleY;
               this.customizationComponent.width = this.width - 3.0F;
               if (this.customizationComponent.mouseReleased(mouseX, mouseY, button)) {
                  return true;
               }
            }

            moduleY += customizationHeight;
         }

         for (ModuleComponent component : this.moduleComponents) {
            if (this.shouldRenderModule(component)) {
               component.x = this.x + 1.5F;
               component.y = moduleY;
               component.width = this.width - 3.0F;
               float moduleBottom = moduleY + component.getComponentHeight();
               if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                  visibleModules.add(component);
               }

               float settingsBottomY = component.getSettingsBottomY();
               moduleY = settingsBottomY + 2.0F;
            }
         }

         for (int i = visibleModules.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = visibleModules.get(i);
            if (moduleComponent.mouseReleased(mouseX, mouseY, button)) {
               return true;
            }
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y + 56.0F, this.width, this.height - 52.0F)) {
         int scrollSpeed = (int)ClickGui.scrollSpeed.getValue();
         float baseModuleHeight = 22.0F;
         float scrollStep = baseModuleHeight * scrollSpeed;
         this.scroll += amount > 0.0 ? scrollStep : -scrollStep;
      }

      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         ArrayList<ModuleComponent> visibleModules = new ArrayList<>();
         float categoryTop = this.y + 35.0F - 8.0F;
         float categoryBottom = this.y + 35.0F + 22.0F;
         float moduleStartY = categoryBottom + 4.0F;
         float moduleY = moduleStartY + this.smoothedScroll;
         float clipTop = moduleStartY;
         float clipBottom = this.y + this.height;

         for (ModuleComponent component : this.moduleComponents) {
            if (this.shouldRenderModule(component)) {
               component.x = this.x + 1.5F;
               component.y = moduleY;
               component.width = this.width - 3.0F;
               float moduleBottom = moduleY + component.getComponentHeight();
               if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                  visibleModules.add(component);
               }

               float settingsBottomY = component.getSettingsBottomY();
               moduleY = settingsBottomY + 2.0F;
            }
         }

         for (int i = visibleModules.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = visibleModules.get(i);
            if (moduleComponent.mouseScrolled(mouseX, mouseY, amount)) {
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.isPanelVisible()) {
         return false;
      } else {
         for (int i = this.moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = this.moduleComponents.get(i);
            if (this.shouldRenderModule(moduleComponent) && moduleComponent.keyPressed(keyCode, scanCode, modifiers)) {
               return true;
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.isPanelVisible()) {
         return false;
      } else {
         for (int i = this.moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = this.moduleComponents.get(i);
            if (this.shouldRenderModule(moduleComponent) && moduleComponent.charTyped(chr, modifiers)) {
               return true;
            }
         }

         return super.charTyped(chr, modifiers);
      }
   }

   private boolean shouldRenderModule(ModuleComponent component) {
      if (!this.searchText.isEmpty()) {
         boolean matches = component.getModule().getVisibleName().toLowerCase().contains(this.searchText.toLowerCase());
         if (matches) {
         }

         return matches;
      } else {
         return true;
      }
   }

   private boolean isPanelVisible() {
      float screenWidth = window.getScaledWidth();
      float screenHeight = window.getScaledHeight();
      boolean visible = this.x < screenWidth && this.x + this.width > 0.0F && this.y < screenHeight && this.y + this.height > 0.0F;
      return !(screenWidth < 320.0F) && !(screenHeight < 240.0F) ? visible : false;
   }

   private String getCategoryIcon() {
      return switch (this.category) {
         case COMBAT -> "b";
         case MOVEMENT -> "d";
         case RENDER -> "e";
         case ESP -> "i";
         case PLAYER -> "c";
         case MISC -> "f";
         case CUSTOMIZATION -> "g";
         case CONFIGS -> "h";
      };
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public void setSearchText(String text) {
      this.searchText = text;
   }

   public int getIndex() {
      return this.index;
   }
}
