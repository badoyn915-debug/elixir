package code.elixir.display.screens.clickgui.components.implement.panel;

import code.elixir.Elixir;
import code.elixir.common.animation.Easy.Direction;
import code.elixir.common.animation.Easy.EaseBackIn;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.other.SearchComponent;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class PanelManager extends AbstractComponent {
   private final List<PanelComponent> panels = new ArrayList<>();
   private float basePanelWidth = 110.0F;
   private float basePanelHeight = 290.0F;
   private float baseGap = 10.0F;
   private float panelWidth;
   private float panelHeight;
   private float gap;
   private float guiOffsetX = 0.0F;
   private float guiOffsetY = -25.0F;
   private final SearchComponent searchComponent = new SearchComponent();
   private final List<PanelManager.PanelAnimation> panelAnimations = new ArrayList<>();
   private boolean lastCustomizationVisible = true;

   public PanelManager() {
      this.initializePanels();
      this.searchComponent.setOnTextChanged(this::updateSearch);
      this.panelWidth = this.basePanelWidth;
      this.panelHeight = this.basePanelHeight;
      this.gap = this.baseGap;
      ClickGui clickGui = ClickGui.getInstance();
      if (clickGui != null) {
         this.lastCustomizationVisible = clickGui.shouldRenderCustomization();
      }
   }

   private void updatePanelDimensions() {
      this.panelWidth = this.basePanelWidth;
      this.panelHeight = this.basePanelHeight;
      this.gap = this.baseGap;
   }

   private void initializePanels() {
      this.panels.clear();
      int index = 0;

      for (ModuleCategory category : ModuleCategory.values()) {
         if (category != ModuleCategory.CONFIGS) {
            this.panels.add(new PanelComponent(category, index));
            index++;
         }
      }
   }

   @Override
   public void tick() {
      for (PanelComponent panel : this.panels) {
         panel.tick();
      }

      this.searchComponent.tick();
      this.updatePanelPositions();

      for (PanelManager.PanelAnimation animation : this.panelAnimations) {
         animation.panel.x = animation.getAnimatedX();
         animation.panel.y = animation.getAnimatedY();
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      for (PanelManager.PanelAnimation animation : this.panelAnimations) {
         if (animation.isVisible()) {
            animation.panel.render(context, mouseX, mouseY, delta);
         }
      }

      float screenWidth = window.getScaledWidth();
      float screenHeight = window.getScaledHeight();
      this.searchComponent.x = (screenWidth - this.searchComponent.width) / 2.0F + this.guiOffsetX;
      this.searchComponent.y = (screenHeight - this.panelHeight) / 2.0F + this.panelHeight + 10.0F + this.guiOffsetY + 60.0F;
      float backgroundWidth = this.panelWidth + 4.0F;
      float backgroundHeight = this.searchComponent.height + 10.0F;
      float backgroundX = (screenWidth - backgroundWidth) / 2.0F + this.guiOffsetX;
      float backgroundY = this.searchComponent.y - 5.0F;
      if (ThemeManager.isLiquidGlassTheme()) {
         Render2D.rectangleWithMask(
            context.getMatrices().peek().getPositionMatrix(),
            backgroundX,
            backgroundY,
            backgroundWidth,
            backgroundHeight,
            10.0F,
            0,
            KawaseBlur.INSTANCE.getBlurTexture()
         );
      }

      rectangle.render(
         ShapeProperties.create(context.getMatrices(), backgroundX, backgroundY, backgroundWidth, backgroundHeight)
            .round(10.0F)
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
      String playerName = Elixir.getInstance().getNativeUsername();
      String playerUUID = "[" + Elixir.getInstance().getNativeUserIdentifier() + "]";
      String role = "[" + this.normalizeRole(Elixir.getInstance().getNativeUserRole()) + "]";
      String displayText = playerName + playerUUID + " " + role;
      float textWidth = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringWidth(displayText);
      float secondBackgroundWidth = Math.max(52.0F, textWidth + 16.0F);
      float secondBackgroundHeight = 19.0F;
      float secondBackgroundX = (screenWidth - secondBackgroundWidth) / 2.0F + this.guiOffsetX;
      float secondBackgroundY = backgroundY + (backgroundHeight - secondBackgroundHeight) / 2.0F;
      if (ThemeManager.isLiquidGlassTheme()) {
         Render2D.rectangleWithMask(
            context.getMatrices().peek().getPositionMatrix(),
            secondBackgroundX,
            secondBackgroundY - 30.0F,
            secondBackgroundWidth - 3.0F,
            secondBackgroundHeight,
            8.0F,
            0,
            KawaseBlur.INSTANCE.getBlurTexture()
         );
      }

      rectangle.render(
         ShapeProperties.create(context.getMatrices(), secondBackgroundX, secondBackgroundY - 30.0F, secondBackgroundWidth - 3.0F, secondBackgroundHeight)
            .round(8.0F)
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
      float textX = secondBackgroundX + (secondBackgroundWidth - textWidth) / 2.0F;
      float textY = secondBackgroundY - 30.0F + (secondBackgroundHeight - Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringHeight(displayText)) / 2.0F;
      float nameWidth = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringWidth(playerName);
      Fonts.getSize(14, Fonts.Type.SuisseIntlMedium)
         .drawString(context.getMatrices(), playerName, textX - 2.0F, textY + 6.5, ThemeManager.textColor.getColor());
      float uidX = textX + nameWidth;
      Fonts.getSize(14, Fonts.Type.SuisseIntlMedium)
         .drawRainbowString(context.getMatrices(), playerUUID, uidX, textY + 6.5, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
      float roleX = uidX + Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringWidth(playerUUID) + 4.0F;
      int roleColor = "FREE".equals(this.normalizeRole(Elixir.getInstance().getNativeUserRole()))
         ? ColorAssist.getClientColor2()
         : ThemeManager.textColor.getColor();
      Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(context.getMatrices(), role, roleX, textY + 6.5, roleColor);
      float squareSize = 20.0F;
      float squareX = backgroundX + backgroundWidth - squareSize - 5.0F;
      float squareY = backgroundY + (backgroundHeight - squareSize) / 2.0F;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), squareX, squareY, squareSize, squareSize)
            .round(4.0F)
            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
            .build()
      );
      float zX = squareX + (squareSize - Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).getStringWidth("z")) / 2.0F;
      float zY = squareY + (squareSize - Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).getStringHeight("z")) / 2.0F;
      Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS).drawString(context.getMatrices(), "z", zX, zY + 6.0F, ThemeManager.textColor.getColor());
      this.searchComponent.render(context, mouseX, mouseY, delta);
   }

   private void updatePanelPositions() {
      float screenWidth = window.getScaledWidth();
      float screenHeight = window.getScaledHeight();
      ClickGui clickGui = ClickGui.getInstance();
      boolean currentCustomizationVisible = clickGui != null && clickGui.shouldRenderCustomization();
      boolean visibilityChanged = currentCustomizationVisible != this.lastCustomizationVisible;
      if (visibilityChanged) {
         this.lastCustomizationVisible = currentCustomizationVisible;
      }

      ArrayList<PanelComponent> visiblePanels = new ArrayList<>();

      for (PanelComponent panel : this.panels) {
         if (this.shouldShowPanel(panel)) {
            visiblePanels.add(panel);
         }
      }

      int visiblePanelCount = visiblePanels.size();
      float totalWidth = visiblePanelCount * this.panelWidth + (visiblePanelCount > 0 ? (visiblePanelCount - 1) * this.gap : 0.0F);
      float startX = (screenWidth - totalWidth) / 2.0F + this.guiOffsetX;
      float startY = (screenHeight - this.panelHeight) / 2.0F + this.guiOffsetY;
      if (!visibilityChanged) {
         if (this.panelAnimations.size() != this.panels.size()) {
            visibilityChanged = true;
         } else {
            for (int i = 0; i < this.panels.size(); i++) {
               PanelComponent panelComponent = this.panels.get(i);
               PanelManager.PanelAnimation animation = this.panelAnimations.get(i);
               boolean shouldBeVisible = this.shouldShowPanel(panelComponent);
               if (animation.visible != shouldBeVisible) {
                  visibilityChanged = true;
                  break;
               }
            }
         }
      }

      if (!visibilityChanged && !this.panelAnimations.isEmpty()) {
         int visibleIndex = 0;

         for (PanelManager.PanelAnimation animation : this.panelAnimations) {
            boolean shouldBeVisible = this.shouldShowPanel(animation.panel);
            float targetX = shouldBeVisible ? startX + visibleIndex * (this.panelWidth + this.gap) : animation.targetX;
            animation.updatePosition(targetX, startY, shouldBeVisible);
            if (shouldBeVisible) {
               visibleIndex++;
            }
         }
      } else {
         this.panelAnimations.clear();
         int visibleIndex = 0;

         for (PanelComponent panelx : this.panels) {
            boolean shouldBeVisible = this.shouldShowPanel(panelx);
            float targetX = shouldBeVisible ? startX + visibleIndex * (this.panelWidth + this.gap) : panelx.x;
            if (this.panelAnimations.isEmpty()) {
               panelx.x = targetX;
               panelx.y = startY;
            }

            PanelManager.PanelAnimation animationx = new PanelManager.PanelAnimation(panelx, targetX, startY, shouldBeVisible);
            this.panelAnimations.add(animationx);
            if (shouldBeVisible) {
               visibleIndex++;
            }
         }
      }

      for (PanelComponent panelComponent : this.panels) {
         panelComponent.width = this.panelWidth;
         panelComponent.height = this.panelHeight;
      }
   }

   private boolean shouldShowPanel(PanelComponent panel) {
      try {
         ClickGui clickGui = ClickGui.getInstance();
         if (clickGui == null) {
            return true;
         } else {
            ModuleCategory category = panel.getCategory();
            return category == ModuleCategory.CUSTOMIZATION ? clickGui.shouldRenderCustomization() : category != ModuleCategory.CONFIGS;
         }
      } catch (Exception var4) {
         return true;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for (PanelManager.PanelAnimation animation : this.panelAnimations) {
         if (animation.isVisible() && animation.panel.mouseClicked(mouseX, mouseY, button)) {
            return true;
         }
      }

      return this.searchComponent.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      for (PanelManager.PanelAnimation animation : this.panelAnimations) {
         if (animation.isVisible()) {
            animation.panel.mouseReleased(mouseX, mouseY, button);
         }
      }

      this.searchComponent.mouseReleased(mouseX, mouseY, button);
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      for (PanelManager.PanelAnimation animation : this.panelAnimations) {
         if (animation.isVisible()) {
            animation.panel.mouseScrolled(mouseX, mouseY, amount);
         }
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 70 && (modifiers & 2) != 0) {
         MenuScreen.activateClickGuiInput(this.searchComponent);
         return true;
      } else {
         for (PanelComponent panel : this.panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
               return true;
            }
         }

         float moveSpeed = 10.0F;
         switch (keyCode) {
            case 262:
               this.guiOffsetX += moveSpeed;
               return true;
            case 263:
               this.guiOffsetX -= moveSpeed;
               return true;
            case 264:
               this.guiOffsetY += moveSpeed;
               return true;
            case 265:
               this.guiOffsetY -= moveSpeed;
               return true;
            default:
               return this.searchComponent.keyPressed(keyCode, scanCode, modifiers) ? true : super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (PanelComponent panel : this.panels) {
         panel.charTyped(chr, modifiers);
      }

      return this.searchComponent.charTyped(chr, modifiers) ? true : super.charTyped(chr, modifiers);
   }

   public void setSearchText(String text) {
      for (PanelComponent panel : this.panels) {
         panel.setSearchText(text);
      }

      this.searchComponent.setText(text);
   }

   public void updateSearch() {
      String searchText = this.searchComponent.getText();

      for (PanelComponent panel : this.panels) {
         panel.setSearchText(searchText);
      }
   }

   public List<PanelComponent> getPanels() {
      return this.panels;
   }

   public void setPanelDimensions(float width, float height, float gap) {
      this.basePanelWidth = width;
      this.basePanelHeight = height;
      this.baseGap = gap;
      this.updatePanelDimensions();
   }

   public void forceUpdatePositions() {
      this.panelAnimations.clear();
      this.updatePanelPositions();
   }

   private String normalizeRole(String rawRole) {
      if (rawRole == null) {
         return "FREE";
      } else {
         String role = rawRole.toUpperCase();
         if (role.contains("BETA")) {
            return "FREE";
         } else {
            return role.contains("FREE") ? "FREE" : "FREE";
         }
      }
   }

   private static class PanelAnimation {
      PanelComponent panel;
      float targetX;
      float targetY;
      float startX;
      float startY;
      EaseBackIn xAnimation;
      EaseBackIn yAnimation;
      boolean visible;
      boolean animating;

      PanelAnimation(PanelComponent panel, float targetX, float targetY, boolean visible) {
         this.panel = panel;
         this.targetX = targetX;
         this.targetY = targetY;
         this.startX = panel.x;
         this.startY = panel.y;
         this.visible = visible;
         this.animating = false;
         this.xAnimation = new EaseBackIn(200, 1.0, 1.5F);
         this.yAnimation = new EaseBackIn(200, 1.0, 1.5F);
      }

      void updatePosition(float newTargetX, float newTargetY, boolean newVisible) {
         boolean positionChanged = Math.abs(this.targetX - newTargetX) > 0.1F || Math.abs(this.targetY - newTargetY) > 0.1F;
         boolean visibilityChanged = this.visible != newVisible;
         if (positionChanged || visibilityChanged) {
            this.startX = this.panel.x;
            this.startY = this.panel.y;
            this.xAnimation.reset();
            this.yAnimation.reset();
            this.xAnimation.setDirection(Direction.FORWARDS);
            this.yAnimation.setDirection(Direction.FORWARDS);
            this.targetX = newTargetX;
            this.targetY = newTargetY;
            this.visible = newVisible;
            this.animating = true;
         }
      }

      float getAnimatedX() {
         if (this.animating && !this.xAnimation.isDone()) {
            double progress = this.xAnimation.getOutput();
            return (float)(this.startX + (this.targetX - this.startX) * progress);
         } else {
            this.animating = false;
            return this.targetX;
         }
      }

      float getAnimatedY() {
         if (this.animating && !this.yAnimation.isDone()) {
            double progress = this.yAnimation.getOutput();
            return (float)(this.startY + (this.targetY - this.startY) * progress);
         } else {
            return this.targetY;
         }
      }

      boolean isVisible() {
         return this.visible;
      }
   }
}
