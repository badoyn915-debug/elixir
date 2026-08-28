package code.elixir.display.screens.clickgui.components.implement.module;

import code.elixir.Elixir;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.features.module.Module;
import code.elixir.features.module.setting.SettingComponentAdder;
import code.elixir.utils.animation.AnimationHelper;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class ModuleComponent extends AbstractComponent {
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   private final Module module;
   private final Rectangle rectangle = new Rectangle();
   private boolean binding = false;
   private boolean isExpanded = false;
   private boolean wasExpanded = false;

   public ModuleComponent(Module module) {
      this.module = module;
      new SettingComponentAdder().addSettingComponent(module.settings(), this.components, module);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      boolean hasSettings = !this.module.settings().isEmpty();
      boolean isExpanded = this.isExpanded();
      this.height = 22.0F;
      int backgroundColor = this.module.isState() ? ColorAssist.getClientColor() : ThemeManager.offModuleColor.getColor();
      int secondaryColor = this.module.isState() ? ColorAssist.getClientColor2() : ThemeManager.offModuleColor.getColor();
      this.rectangle
         .render(
            ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, this.height)
               .round(7.0F)
               .color(backgroundColor, backgroundColor, secondaryColor, secondaryColor)
               .build()
         );
      this.rectangle
         .render(
            ShapeProperties.create(context.getMatrices(), this.x + 4.0F, this.y, this.width - 8.0F, 1.0F)
               .color(this.module.isState() ? 0x40FFFFFF : 0x18FFFFFF)
               .build()
         );
      String moduleName = this.binding ? "binding.." : this.module.getVisibleName();
      float textWidth = Fonts.getSize(14, Fonts.Type.ROCKSTAR_SEMIBOLD).getStringWidth(moduleName);
      float textX = this.x + (this.width - textWidth) / 2.0F;
      float textY = this.y + 17.0F - Fonts.getSize(14, Fonts.Type.ROCKSTAR_SEMIBOLD).getStringHeight(moduleName);
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_SEMIBOLD).drawString(context.getMatrices(), moduleName, textX, textY, ThemeManager.textColor.getColor());
      if (hasSettings) {
         String arrow = isExpanded ? "j" : "i";
         float arrowX = this.x + this.width - 15.0F;
         float arrowY = this.y + 16.0F - Fonts.getSize(12, Fonts.Type.ELIXIR_ICONS).getStringHeight(arrow);
         Fonts.getSize(12, Fonts.Type.ELIXIR_ICONS).drawString(context.getMatrices(), arrow, arrowX, arrowY, ThemeManager.textColor.getColor());
      }

      String animationKey = "module_settings_" + this.module.getName();
      boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
      if (this.isExpanded() && hasSettings || isAnimating) {
         this.renderSettings(context, mouseX, mouseY, delta);
      }
   }

   public void renderModuleDescriptionAt(DrawContext context, float moduleX, float moduleY, float moduleWidth, boolean isHovered) {
      if (context != null) {
         try {
            String description = ModuleDescriptions.tryGetDescription(this.module);
            if (ModuleDescriptions.isMissingDescription(description)) {
               return;
            }

            MatrixStack matrix = context.getMatrices();
            if (matrix == null) {
               return;
            }

            FontRenderer font = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD);
            if (font == null) {
               return;
            }

            String animationKey = "module_description_alpha_" + this.module.getName();
            boolean isAnimationActive = AnimationHelper.isAnimationActive(animationKey);
            float currentAlpha = AnimationHelper.getAnimationValue(animationKey, isHovered ? 1.0F : 0.0F);
            if (isHovered && !isAnimationActive && currentAlpha < 0.99F) {
               AnimationHelper.fadeIn(animationKey, 400L);
            }

            float alpha;
            if ((alpha = AnimationHelper.getAnimationValue(animationKey, isHovered ? 1.0F : 0.0F)) <= 0.0F) {
               return;
            }

            float maxWidth = 200.0F;
            String wrappedText = StringHelper.wrap(description, (int)maxWidth, 12);
            if (wrappedText == null) {
               return;
            }

            String[] lines = wrappedText.split("\n");
            if (lines == null || lines.length == 0) {
               return;
            }

            float lineHeight = font.getStringHeight("A");
            float textPadding = 6.0F;
            float descriptionWidth = 0.0F;

            for (String line : lines) {
               float lineWidth;
               if (line != null && (lineWidth = font.getStringWidth(line)) > descriptionWidth) {
                  descriptionWidth = lineWidth;
               }
            }

            descriptionWidth = Math.min(descriptionWidth + textPadding, maxWidth);
            float descriptionHeight = lines.length * lineHeight;
            float descriptionX = moduleX + moduleWidth + 5.0F;
            float descriptionY = moduleY + 3.5F;
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            if (descriptionX + descriptionWidth > screenWidth) {
               descriptionX = moduleX - descriptionWidth - 2.5F;
            }

            if (descriptionY + descriptionHeight > screenHeight) {
               descriptionY = screenHeight - descriptionHeight;
            }

            if (descriptionY < 0.0F) {
               descriptionY = 0.0F;
            }

            int alphaValue = (int)(alpha * 255.0F);
            int backgroundColor = new Color(25, 25, 25, alphaValue).getRGB();
            int textColor = new Color(255, 255, 255, alphaValue).getRGB();
            RenderSystem.disableDepthTest();
            RenderSystem.disableScissor();
            matrix.push();
            matrix.translate(0.0F, 0.0F, 3000.0F);
            this.rectangle
               .render(
                  ShapeProperties.create(matrix, descriptionX, descriptionY, descriptionWidth, descriptionHeight).round(2.0F).color(backgroundColor).build()
               );
            float textX = descriptionX + textPadding / 2.0F;
            float textY = descriptionY + textPadding;

            for (String linex : lines) {
               if (linex != null && font != null) {
                  font.drawString(matrix, linex, textX, textY, textColor);
                  textY += lineHeight;
               }
            }

            matrix.pop();
            RenderSystem.enableDepthTest();
         } catch (Throwable var33) {
            RenderSystem.enableDepthTest();
         }
      }
   }

   public void renderModuleDescription(DrawContext context, int mouseX, int mouseY) {
      if (context != null) {
         try {
            String description = ModuleDescriptions.tryGetDescription(this.module);
            if (ModuleDescriptions.isMissingDescription(description)) {
               return;
            }

            MatrixStack matrix = context.getMatrices();
            if (matrix == null) {
               return;
            }

            FontRenderer font = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD);
            if (font == null) {
               return;
            }

            float maxWidth = 200.0F;
            String wrappedText = StringHelper.wrap(description, (int)maxWidth, 12);
            if (wrappedText == null) {
               return;
            }

            String[] lines = wrappedText.split("\n");
            if (lines == null || lines.length == 0) {
               return;
            }

            float lineHeight = font.getStringHeight("A");
            float textPadding = 6.0F;
            float descriptionWidth = 0.0F;

            for (String line : lines) {
               float lineWidth;
               if (line != null && (lineWidth = font.getStringWidth(line)) > descriptionWidth) {
                  descriptionWidth = lineWidth;
               }
            }

            descriptionWidth = Math.min(descriptionWidth + textPadding, maxWidth);
            float descriptionHeight = lines.length * lineHeight;
            float descriptionX = this.x + this.width + 5.0F;
            float descriptionY = this.y;
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            if (descriptionX + descriptionWidth > screenWidth) {
               descriptionX = this.x - descriptionWidth - 2.5F;
            }

            if (descriptionY + descriptionHeight > screenHeight) {
               descriptionY = screenHeight - descriptionHeight;
            }

            if (descriptionY < 0.0F) {
               descriptionY = 0.0F;
            }

            RenderSystem.disableDepthTest();
            RenderSystem.disableScissor();
            matrix.push();
            matrix.translate(0.0F, 0.0F, 3000.0F);
            this.rectangle
               .render(
                  ShapeProperties.create(matrix, descriptionX, descriptionY, descriptionWidth, descriptionHeight)
                     .round(3.0F)
                     .color(new Color(25, 25, 25, 255).getRGB())
                     .build()
               );
            float textX = descriptionX + textPadding / 2.0F;
            float textY = descriptionY + textPadding;
            int textColor = ThemeManager.textColor != null ? ThemeManager.textColor.getColor() : 16777215;

            for (String linex : lines) {
               if (linex != null && font != null) {
                  font.drawString(matrix, linex, textX, textY, textColor);
                  textY += lineHeight;
               }
            }

            matrix.pop();
            RenderSystem.enableDepthTest();
         } catch (Throwable var25) {
            RenderSystem.enableDepthTest();
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         this.updateSettingsPositions();
         String animationKey = "module_settings_" + this.module.getName();
         float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1.0F);

         for (AbstractSettingComponent setting : this.components) {
            float actualSettingY;
            if (setting.getSetting().isVisible()
               && Calculate.isHovered(
                  mouseX, mouseY, this.x, actualSettingY = setting.y - (setting.y - this.y - 20.0F - 1.0F) * (1.0F - slideProgress), this.width, setting.height
               )
               && setting.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }
      }

      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 20.0)) {
         if (button == 0) {
            String moduleToggleKey = "module_toggle_" + this.module.getName();
            AnimationHelper.startAnimation(moduleToggleKey, 1.0F, 1.1F, 150L, AnimationHelper.EasingType.EASE_OUT);
            this.module.switchState();
            return true;
         }

         if (button == 1 && !this.module.settings().isEmpty()) {
            this.wasExpanded = this.isExpanded;
            this.isExpanded = !this.isExpanded;
            String animationKey = "module_settings_" + this.module.getName();
            if (this.isExpanded) {
               AnimationHelper.startAnimation(animationKey, 0.0F, 1.0F, 200L, AnimationHelper.EasingType.EASE_OUT);
            } else {
               AnimationHelper.startAnimation(animationKey, 1.0F, 0.0F, 150L, AnimationHelper.EasingType.EASE_IN);
            }

            SoundManager.playSound(SoundManager.CATEGORY_CLICK);
            return true;
         }

         if (button == 2) {
            this.binding = !this.binding;
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (Calculate.isHovered(mouseX, mouseY, this.x, this.y, this.width, 20.0)) {
         return true;
      } else {
         if (this.isExpanded && !this.module.settings().isEmpty()) {
            this.updateSettingsPositions();
            String animationKey = "module_settings_" + this.module.getName();
            float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1.0F);

            for (AbstractSettingComponent setting : this.components) {
               float actualSettingY;
               if (setting.getSetting().isVisible()
                  && Calculate.isHovered(
                     mouseX,
                     mouseY,
                     this.x,
                     actualSettingY = setting.y - (setting.y - this.y - 20.0F - 1.0F) * (1.0F - slideProgress),
                     this.width,
                     setting.height
                  )) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @Override
   public void tick() {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         this.components.stream().filter(setting -> setting.getSetting().isVisible()).forEach(AbstractComponent::tick);
      }

      super.tick();
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         this.updateSettingsPositions();

         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible() && setting.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
               return true;
            }
         }
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         this.updateSettingsPositions();

         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible()) {
               setting.mouseReleased(mouseX, mouseY, button);
            }
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         this.updateSettingsPositions();

         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible() && setting.mouseScrolled(mouseX, mouseY, amount)) {
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.binding) {
         if (keyCode == 256) {
            this.binding = false;
            return true;
         } else if (keyCode == 261) {
            this.module.setKey(-1);
            this.binding = false;
            return true;
         } else {
            this.module.setKey(keyCode);
            this.binding = false;
            return true;
         }
      } else {
         if (this.isExpanded && !this.module.settings().isEmpty()) {
            for (AbstractSettingComponent setting : this.components) {
               if (setting.getSetting().isVisible() && setting.keyPressed(keyCode, scanCode, modifiers)) {
                  return true;
               }
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.isExpanded && !this.module.settings().isEmpty()) {
         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible() && setting.charTyped(chr, modifiers)) {
               return true;
            }
         }
      }

      return super.charTyped(chr, modifiers);
   }

   public int getComponentHeight() {
      int baseHeight = 20;
      String animationKey = "module_settings_" + this.module.getName();
      boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
      if ((!this.isExpanded() || this.module.settings().isEmpty()) && !isAnimating) {
         return baseHeight;
      } else {
         int settingsHeight = 0;

         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible()) {
               settingsHeight = (int)(settingsHeight + (setting.height + 1.0F));
            }
         }

         float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1.0F);
         settingsHeight += 2;
         return baseHeight + (int)(settingsHeight * slideProgress);
      }
   }

   public float getSettingsBottomY() {
      String animationKey = "module_settings_" + this.module.getName();
      boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
      if ((!this.isExpanded() || this.module.settings().isEmpty()) && !isAnimating) {
         return this.y + 20.0F;
      } else {
         int settingsHeight = 0;

         for (AbstractSettingComponent setting : this.components) {
            if (setting.getSetting().isVisible()) {
               settingsHeight = (int)(settingsHeight + (setting.height + 1.0F));
            }
         }

         float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1.0F);
         settingsHeight += 2;
         float currentHeight = settingsHeight * slideProgress;
         return this.y + 20.0F + 1.0F + currentHeight;
      }
   }

   private boolean isExpanded() {
      return this.isExpanded;
   }

   private void renderSettings(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateSettingsPositions();
      String animationKey = "module_settings_" + this.module.getName();
      float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1.0F);
      int settingsHeight = 0;

      for (AbstractSettingComponent setting : this.components) {
         if (setting.getSetting().isVisible()) {
            settingsHeight = (int)(settingsHeight + (setting.height + 1.0F));
         }
      }

      settingsHeight += 2;
      float backgroundHeight = settingsHeight * slideProgress;
      int backgroundColor = ThemeManager.BackgroundSettings.getColor();
      if (backgroundHeight > 0.0F) {
         this.rectangle
            .render(
               ShapeProperties.create(context.getMatrices(), this.x, this.y + 20.0F + 1.0F, this.width, backgroundHeight)
                  .round(3.0F)
                  .color(backgroundColor)
                  .build()
            );
      }

      boolean clip = slideProgress < 0.999F;
      ScissorAssist scissor = Elixir.getInstance().getScissorManager();
      if (clip) {
         Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
         scissor.push(positionMatrix, this.x, this.y + 20.0F + 1.0F, this.width, Math.max(0.0F, backgroundHeight));
      }

      float prevAlpha = RenderSystem.getShaderColor()[3];
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, slideProgress * prevAlpha);

      for (AbstractSettingComponent settingx : this.components) {
         if (settingx.getSetting().isVisible()) {
            float originalY = settingx.y;
            float originalX = settingx.x;
            float settingOffset = (settingx.y - this.y - 20.0F - 1.0F) * (1.0F - slideProgress);
            settingx.x = this.x;
            settingx.y = originalY - settingOffset;
            settingx.render(context, mouseX, mouseY, delta);
            settingx.y = originalY;
            settingx.x = originalX;
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prevAlpha);
      if (clip) {
         scissor.pop();
      }
   }

   private void updateSettingsPositions() {
      float settingsY = this.y + 20.0F + 2.0F + 4.0F;

      for (AbstractSettingComponent setting : this.components) {
         if (setting.getSetting().isVisible()) {
            setting.x = this.x;
            setting.y = settingsY;
            setting.width = this.width;
            settingsY += setting.height + 1.0F;
         }
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ModuleComponent that = (ModuleComponent)o;
         return this.module.equals(that.module);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.module);
   }

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public Module getModule() {
      return this.module;
   }

   public Rectangle getRectangle() {
      return this.rectangle;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public boolean isWasExpanded() {
      return this.wasExpanded;
   }
}
