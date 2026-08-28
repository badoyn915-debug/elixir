package code.elixir.display.screens.clickgui.components.implement.category;

import code.elixir.Elixir;
import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.InOutBack;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.module.ModuleComponent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class CategoryComponent extends AbstractComponent {
   private final List<ModuleComponent> moduleComponents = new ArrayList<>();
   private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
   private final ModuleCategory category;
   private final Animation alphaAnimation = new InOutBack().setMs(300).setValue(1.0);
   private final Animation scaleAnimation = new InOutBack().setMs(300).setValue(1.0);
   private boolean initializedAnimations = false;
   private float scroll = 0.0F;
   private float smoothedScroll = 0.0F;

   private void initializeModules() {
      for (Module module : Elixir.getInstance().getModuleRepository().modules()) {
         ModuleComponent newComponent;
         if (module.getCategory() == this.category && !module.isHidden() && globalModuleComponents.add(newComponent = new ModuleComponent(module))) {
            this.moduleComponents.add(newComponent);
         }
      }
   }

   public void postInitialize() {
      if (!this.initializedAnimations) {
         if (MenuScreen.getInstance().getCategory().equals(this.category)) {
            this.alphaAnimation.setDirection(Direction.FORWARDS);
            this.scaleAnimation.setDirection(Direction.FORWARDS);
            this.alphaAnimation.reset();
            this.scaleAnimation.reset();
            this.alphaAnimation.setMs(0);
            this.scaleAnimation.setMs(0);
         } else {
            this.alphaAnimation.setDirection(Direction.BACKWARDS);
            this.scaleAnimation.setDirection(Direction.BACKWARDS);
            this.alphaAnimation.reset();
            this.scaleAnimation.reset();
            this.alphaAnimation.setMs(0);
            this.scaleAnimation.setMs(0);
         }

         this.initializedAnimations = true;
      }
   }

   public CategoryComponent(ModuleCategory category) {
      this.category = category;
      this.initializeModules();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.postInitialize();
      MenuScreen menuScreen = MenuScreen.getInstance();
      globalModuleComponents.clear();
      Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
      ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
      this.drawCategoryTab(context, context.getMatrices(), mouseX, mouseY);
      this.drawModulesVertical(context, mouseX, mouseY, delta, positionMatrix, scissorManager, menuScreen);
   }

   private void drawModulesVertical(
      DrawContext context, int mouseX, int mouseY, float delta, Matrix4f positionMatrix, ScissorAssist scissorManager, MenuScreen menuScreen
   ) {
      float totalModuleHeight = 0.0F;

      for (ModuleComponent component : this.moduleComponents) {
         if (this.shouldRenderComponent(component)) {
            totalModuleHeight += component.getComponentHeight() + 5;
         }
      }

      float offsetX = 35.0F;
      float offsetY = 14.0F;
      float visibleHeight = menuScreen.height - offsetY;
      float categoryTop = menuScreen.y + 14 - 8;
      if (totalModuleHeight > visibleHeight) {
         float maxScroll = -(totalModuleHeight - visibleHeight);
         this.scroll = MathHelper.clamp(this.scroll, maxScroll, 0.0F);
      } else {
         this.scroll = 0.0F;
      }

      this.smoothedScroll = Calculate.interpolateSmooth(8.0, this.smoothedScroll, this.scroll);
      scissorManager.push(
         positionMatrix, menuScreen.x + offsetX - 75.0F, categoryTop, menuScreen.width - offsetX + 150.0F, menuScreen.height - categoryTop + 15.0F
      );
      float moduleX = menuScreen.x + 36;
      float moduleY = menuScreen.y + 25 + this.smoothedScroll;
      float clipTop = categoryTop;
      float clipBottom = categoryTop + (menuScreen.height - categoryTop + 15.0F);

      for (ModuleComponent componentx : this.moduleComponents) {
         if (this.shouldRenderComponent(componentx)) {
            int componentHeight = componentx.getComponentHeight() + 5;
            float moduleBottom = moduleY + componentHeight;
            if (!(moduleBottom < clipTop) && !(moduleY > clipBottom)) {
               componentx.x = moduleX;
               componentx.y = moduleY;
               componentx.width = 179.0F;
               if (componentx.y > menuScreen.y - componentHeight && menuScreen.y + menuScreen.height + 15 > componentx.y) {
                  componentx.render(context, mouseX, mouseY, delta);
               }

               moduleY += componentHeight;
            } else {
               moduleY += componentHeight;
            }
         }
      }

      scissorManager.pop();
      if (totalModuleHeight > visibleHeight) {
      }
   }

   private void renderScrollbar(DrawContext context, MenuScreen menuScreen, float totalHeight, float visibleHeight, float offsetX, float offsetY) {
      float scrollbarWidth = 4.0F;
      float scrollbarX = menuScreen.x + menuScreen.width - offsetX - scrollbarWidth + 50.0F;
      float scrollbarY = menuScreen.y + offsetY + 12.0F;
      float scrollbarHeight = menuScreen.height - offsetY * 2.0F;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
            .round(2.0F)
            .color(new Color(30, 30, 30, 100).getRGB())
            .build()
      );
      float handleHeight = Math.max(20.0F, visibleHeight * (visibleHeight / totalHeight));
      float maxScroll = -(totalHeight - visibleHeight);
      float scrollRatio = maxScroll != 0.0F ? -this.smoothedScroll / -maxScroll : 0.0F;
      float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
            .round(2.0F)
            .color(new Color(100, 100, 100, 150).getRGB())
            .build()
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      MenuScreen menuScreen = MenuScreen.getInstance();
      float scale = 0.5F + this.scaleAnimation.getOutput().floatValue() * 0.5F;
      float baseWidth = 20.0F;
      float baseHeight = 20.0F;
      float scaledWidth = baseWidth * scale;
      float scaledHeight = baseHeight * scale;
      float baseX = ModuleCategory.RENDER.equals(this.category)
         ? this.x + 4.65F
         : (ModuleCategory.MOVEMENT.equals(this.category) ? this.x + 4.75F : this.x + 5.25F);
      float baseY = this.y;
      float centerX = baseX + baseWidth / 2.0F;
      float centerY = baseY + baseHeight / 2.0F;
      float scaledX = centerX - scaledWidth / 2.0F;
      float scaledY = centerY - scaledHeight / 2.0F;
      float hoverX = ModuleCategory.RENDER.equals(this.category)
         ? this.x + 4.65F
         : (ModuleCategory.MOVEMENT.equals(this.category) ? this.x + 4.75F : this.x + 5.25F);
      double var10002 = hoverX;
      float hoverY = this.y;
      if (Calculate.isHovered(mouseX, mouseY, var10002, this.y, baseWidth, baseHeight) && button == 0) {
         MenuScreen.getInstance().setCategory(this.category);
         this.alphaAnimation.setMs(300);
         this.scaleAnimation.setMs(300);
         this.alphaAnimation.setDirection(Direction.FORWARDS);
         this.scaleAnimation.setDirection(Direction.FORWARDS);
         return true;
      } else {
         float offsetX = 84.0F;
         float offsetY = 29.0F;
         if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX, menuScreen.height - offsetY)) {
            for (ModuleComponent moduleComponent : this.moduleComponents) {
               if (this.shouldRenderComponent(moduleComponent) && moduleComponent.isHover(mouseX, mouseY)) {
                  moduleComponent.mouseClicked(mouseX, mouseY, button);
               }
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      float scale = 0.5F + this.scaleAnimation.getOutput().floatValue() * 0.5F;
      float baseWidth = 20.0F;
      float baseHeight = 20.0F;
      float scaledWidth = baseWidth * scale;
      float scaledHeight = baseHeight * scale;
      float baseX = ModuleCategory.RENDER.equals(this.category)
         ? this.x + 4.65F
         : (ModuleCategory.MOVEMENT.equals(this.category) ? this.x + 4.75F : this.x + 5.25F);
      float centerX = baseX + baseWidth / 2.0F;
      float scaledX = centerX - scaledWidth / 2.0F;
      double var10002 = scaledX;
      float baseY = this.y;
      float centerY;
      boolean isHovered = Calculate.isHovered(mouseX, mouseY, var10002, (centerY = this.y + baseHeight / 2.0F) - scaledHeight / 2.0F, scaledWidth, scaledHeight);
      if (isHovered) {
         return true;
      } else {
         this.moduleComponents.forEach(moduleComponent -> moduleComponent.isHover(mouseX, mouseY));

         for (ModuleComponent moduleComponent2 : this.moduleComponents) {
            if (moduleComponent2.isHover(mouseX, mouseY)) {
               return true;
            }
         }

         return super.isHover(mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      MenuScreen menuScreen = MenuScreen.getInstance();
      float offsetX = 35.0F;
      float offsetY = 13.0F;
      if (Calculate.isHovered(
         mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX + 7.0F, menuScreen.height - offsetY + 15.0F
      )) {
         this.scroll = (float)(this.scroll + amount * 20.0);
      }

      this.moduleComponents.forEach(moduleComponent -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.mouseScrolled(mouseX, mouseY, amount);
         }
      });
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.moduleComponents.forEach(moduleComponent -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.keyPressed(keyCode, scanCode, modifiers);
         }
      });
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.moduleComponents.forEach(moduleComponent -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.charTyped(chr, modifiers);
         }
      });
      return super.charTyped(chr, modifiers);
   }

   private void drawCategoryTab(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
      this.alphaAnimation.setDirection(MenuScreen.getInstance().getCategory().equals(this.category) ? Direction.FORWARDS : Direction.BACKWARDS);
      this.scaleAnimation.setDirection(MenuScreen.getInstance().getCategory().equals(this.category) ? Direction.FORWARDS : Direction.BACKWARDS);
      float anim = this.alphaAnimation.getOutput().floatValue();
      float scale = 0.5F + this.scaleAnimation.getOutput().floatValue() * 0.5F;
      int alpha = MathHelper.clamp((int)(anim * 135.0F), 0, 135);
      float baseWidth = 180.0F;
      float baseHeight = 20.0F;
      float scaledWidth = baseWidth * scale;
      float scaledHeight = baseHeight * scale;
      MenuScreen menuScreen = MenuScreen.getInstance();
      float moduleCenterX = menuScreen.x + 36 + 89.5F;
      float moduleCenterY = menuScreen.y + 14;
      float baseX = moduleCenterX - baseWidth / 2.0F;
      float baseY = moduleCenterY - baseHeight / 2.0F;
      float centerX = baseX + baseWidth / 2.0F;
      float centerY = baseY + baseHeight / 2.0F;
      float scaledX = centerX - scaledWidth / 2.0F;
      float scaledY = centerY - scaledHeight / 2.0F;
      if (!MenuScreen.getInstance().getCategory().equals(this.category)) {
         new Color(ColorAssist.getClientColor());
         rectangle.render(
            ShapeProperties.create(matrix, baseX, baseY, baseWidth, baseHeight)
               .round(4.0F)
               .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor2(), ColorAssist.getClientColor())
               .build()
         );
      }

      switch (this.category) {
         case COMBAT: {
            String icon = "b";
            break;
         }
         case MOVEMENT: {
            String icon = "c";
            break;
         }
         case RENDER: {
            String icon = "d";
            break;
         }
         case ESP: {
            String icon = "g";
            break;
         }
         case PLAYER: {
            String icon = "e";
            break;
         }
         case MISC: {
            String icon = "f";
            break;
         }
         case CONFIGS: {
            String icon = "h";
            break;
         }
         case CUSTOMIZATION: {
            String icon = "i";
            break;
         }
         default:
            String iconX = this.category.getReadableName().substring(0, 1);
      }

      if (ModuleCategory.COMBAT.equals(this.category)) {
         Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "A", centerX, centerY, ColorAssist.getText(0.7F));
      }

      if (ModuleCategory.MOVEMENT.equals(this.category)) {
         Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "B", centerX, centerY, ColorAssist.getText(0.7F));
      }

      if (ModuleCategory.RENDER.equals(this.category)) {
         Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "C", centerX, centerY, ColorAssist.getText(0.7F));
      }

      if (ModuleCategory.PLAYER.equals(this.category)) {
         Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "D", centerX, centerY, ColorAssist.getText(0.7F));
      }

      if (ModuleCategory.MISC.equals(this.category)) {
         Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "E", centerX, centerY, ColorAssist.getText(0.7F));
      }

      if (ModuleCategory.ESP.equals(this.category)) {
         int iconColor = ColorAssist.getText(0.7F);
         float iconDrawX = centerX - 4.5F;
         float iconDrawY = centerY - 4.5F + 2.0F;
         BackgroundIcons.drawEspCategory(context, BackgroundIcons.ESP, iconDrawX, iconDrawY, iconColor);
      }

      if (ModuleCategory.CUSTOMIZATION.equals(this.category)) {
         Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "H", centerX, centerY, ColorAssist.getText(0.7F));
      }
   }

   private boolean shouldRenderComponent(ModuleComponent component) {
      return component.getModule().getCategory().equals(this.category);
   }
}
