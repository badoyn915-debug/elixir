package code.elixir.display.hud;

import code.elixir.Elixir;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.ModuleRepository;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class Keybinds extends AbstractDraggable {
   private final List<Module> keysList = new ArrayList<>();
   private long lastKeyChange = 0L;
   private String currentRandomKey = "NONE";
   private static final String[] EXAMPLE_KEYS = new String[]{"A", "B", "C", "D", "E"};
   private static final Random RANDOM = new Random();
   private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
   private final FontRenderer fontBind = Fonts.getSize(13, Fonts.Type.DEFAULT);
   private final FontRenderer fontModule = Fonts.getSize(14, Fonts.Type.DEFAULT);
   private final FontRenderer categoryIcon = Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS);
   private static final float KEYBIND_ROW_ICON_SIZE = 9.0F;

   public Keybinds() {
      super("Keybinds", 300, 40, 80, 23, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("Keybinds") && Hud.getInstance().state
         ? !this.keysList.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen)
         : false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         FontRenderer iconFont = Fonts.getSize(15, Fonts.Type.ELIXIR_ICONS);
         float pX = this.getX() + this.getWidth() - iconFont.getStringWidth("p") - 8.0F;
         float pY = this.getY() + 9.5F;
         float pWidth = iconFont.getStringWidth("p");
         float pHeight = iconFont.getStringHeight("p");
         if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
            Hud.getInstance();
            ArrayList<String> selected = new ArrayList<>(Hud.interfaceSettings.getSelected());
            selected.remove("Keybinds");
            Hud.getInstance();
            Hud.interfaceSettings.setSelected(selected);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void tick() {
      Elixir alphaDlc = Elixir.getInstance();
      if (alphaDlc == null) {
         this.keysList.clear();
      } else {
         ModuleRepository moduleRepository = alphaDlc.getModuleRepository();
         this.keysList.clear();
         if (moduleRepository != null) {
            for (Module module : moduleRepository.modules()) {
               if (module.getAnimation().getOutput().floatValue() != 0.0F && module.getKey() != -1 && !"ClickGui".equals(module.getName())) {
                  this.keysList.add(module);
               }
            }
         }

         long currentTime;
         if (this.keysList.isEmpty()
            && PlayerInteractionHelper.isChat(mc.currentScreen)
            && (currentTime = System.currentTimeMillis()) - this.lastKeyChange >= 1000L) {
            this.currentRandomKey = EXAMPLE_KEYS[RANDOM.nextInt(EXAMPLE_KEYS.length)];
            this.lastKeyChange = currentTime;
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      this.drawMode1(context);
   }

   private void drawMode1(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer fontMedium = this.fontMedium;
      FontRenderer font = this.fontHeader;
      FontRenderer fontBind = this.fontBind;
      FontRenderer fontModule = this.fontModule;
      FontRenderer categoryIcon = this.categoryIcon;
      float posX = this.getX();
      float posY = this.getY();
      float maxElementsWidth = HudListPanel.headerMinWidth(font, "Keybinds");
      float headerHeight = 21.0F;
      float moduleHeight = 11.0F;
      boolean isFound = false;

      for (Module module : this.keysList) {
         if (module.getAnimation().getOutput().floatValue() > 0.0F) {
            isFound = true;
            String bind = StringHelper.getBindName(module.getKey());
            float elementsWidth = HudListPanel.rowWidth(fontModule, module.getName(), 0.0F, fontBind, bind);
            if (elementsWidth > maxElementsWidth) {
               maxElementsWidth = elementsWidth;
            }
         }
      }

      if (this.keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         isFound = true;
         float elementsWidth = HudListPanel.rowWidth(fontModule, "Module", 0.0F, fontBind, this.currentRandomKey);
         if (elementsWidth > maxElementsWidth) {
            maxElementsWidth = elementsWidth;
         }
      }

      float totalContentHeight = 0.0F;

      for (Module modulex : this.keysList) {
         float animValue = modulex.getAnimation().getOutput().floatValue();
         if (animValue > 0.0F) {
            totalContentHeight += moduleHeight * animValue;
         }
      }

      if (this.keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         totalContentHeight += moduleHeight;
      }

      float f = HudListPanel.panelHeight(totalContentHeight);
      float width = maxElementsWidth;
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrix.peek().getPositionMatrix(), posX, posY, maxElementsWidth, f, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrix, posX, posY, maxElementsWidth, f).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build()
      );
      float headerTextY = HudListPanel.headerTextY(font, posY, "Keybinds");
      font.drawString(matrix, "Keybinds", posX + 7.0F, headerTextY, ThemeManager.textColor.getColor());
      float repBoxW = fontBind.getStringWidth("R") + 6.0F;
      float bindCenterX = posX + maxElementsWidth - 6.5F - repBoxW / 2.0F;
      BackgroundIcons.drawHudHeaderIcon(context, BackgroundIcons.KEYBINDS, font, "Keybinds", headerTextY, bindCenterX, ColorAssist.getHudColor());
      posY = HudListPanel.contentStartY(posY);
      if (this.keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         String bind = this.currentRandomKey;
         String moduleName = "Module";
         String icon = "b";
         matrix.push();
         float centerX = posX + maxElementsWidth / 2.0F;
         float centerY = posY + moduleHeight / 2.0F;
         matrix.translate(centerX, centerY, 0.0F);
         matrix.scale(1.0F, 1.0F, 1.0F);
         matrix.translate(-centerX, -centerY, 0.0F);
         categoryIcon.drawRainbowString(
            matrix, icon, posX + 7.0F, HudListPanel.alignedRowTextY(fontModule, categoryIcon, posY), ColorAssist.getHudColor(), ColorAssist.getHudColor2()
         );
         rectangle.render(
            ShapeProperties.create(matrix, posX + 17.8F, HudListPanel.rowDotY(posY), 2.0, 2.0).round(1.0F).color(new Color(87, 87, 90, 255).getRGB()).build()
         );
         float rowTextY = HudListPanel.rowIconAlignedTextY(fontModule, posY, 9.0F, moduleName);
         fontModule.drawString(matrix, moduleName, Math.round(posX + 22.0F), Math.round(rowTextY), ThemeManager.textColor.getColor());
         float bindWidth = fontBind.getStringWidth(bind);
         float bindBoxWidth = bindWidth + 6.0F;
         float bindBoxX = posX + maxElementsWidth - 6.5F - bindBoxWidth;
         float bindBoxHeight = 8.0F;
         float bindBoxY = HudListPanel.rowBadgeY(posY, bindBoxHeight);
         rectangle.render(
            ShapeProperties.create(matrix, bindBoxX, bindBoxY, bindBoxWidth, bindBoxHeight)
               .round(2.0F)
               .thickness(1.0F)
               .outlineColor(new Color(33, 33, 33, 255).getRGB())
               .color(new Color(0, 0, 0, 100).getRGB())
               .build()
         );
         float bindTextX = bindBoxX + bindBoxWidth / 2.0F - bindWidth / 2.0F;
         fontBind.drawString(
            matrix, bind, Math.round(bindTextX), Math.round(HudListPanel.rowBadgeTextY(fontBind, posY, bindBoxHeight, bind)), ThemeManager.textColor.getColor()
         );
         matrix.pop();
         posY += moduleHeight;
      } else {
         for (Module modulexx : this.keysList) {
            float animValue = modulexx.getAnimation().getOutput().floatValue();
            if (animValue > 0.0F) {
               String bind = StringHelper.getBindName(modulexx.getKey());
               String moduleName = modulexx.getName();
               int alpha = (int)(255.0F * animValue);
               boolean isLightTheme = ThemeManager.BackgroundGui.getColor() == Color.WHITE.getRGB()
                  || (ThemeManager.BackgroundGui.getColor() >> 16 & 0xFF) > 200;
               Color textColor = isLightTheme ? new Color(0, 0, 0, alpha) : new Color(255, 255, 255, alpha);
               matrix.push();
               float centerX = posX + width / 2.0F;
               float centerY = posY + moduleHeight / 2.0F;
               matrix.translate(centerX, centerY, 0.0F);
               matrix.scale(animValue, animValue, 1.0F);
               matrix.translate(-centerX, -centerY, 0.0F);
               Color gradColor1 = new Color(
                  ColorAssist.getHudColor() >> 16 & 0xFF, ColorAssist.getHudColor() >> 8 & 0xFF, ColorAssist.getHudColor() & 0xFF, alpha
               );
               Color gradColor2 = new Color(
                  ColorAssist.getHudColor2() >> 16 & 0xFF, ColorAssist.getHudColor2() >> 8 & 0xFF, ColorAssist.getHudColor2() & 0xFF, alpha
               );
               this.drawKeybindCategoryIcon(context, matrix, modulexx.getCategory(), posX + 7.0F, posY, gradColor1.getRGB(), gradColor2.getRGB(), alpha);
               rectangle.render(
                  ShapeProperties.create(matrix, posX + 17.9F, HudListPanel.rowDotY(posY), 2.0, 2.0)
                     .round(4.0F)
                     .color(new Color(87, 87, 90, alpha).getRGB())
                     .build()
               );
               float shaderAlpha = alpha / 255.0F;
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(shaderAlpha, 0.0F, 1.0F));
               float rowTextY = HudListPanel.rowIconAlignedTextY(fontModule, posY, 9.0F, moduleName);
               fontModule.drawString(matrix, moduleName, Math.round(posX + 20.0F), Math.round(rowTextY), textColor.getRGB());
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               float bindWidth = fontBind.getStringWidth(bind);
               float bindBoxWidth = bindWidth + 6.0F;
               float bindBoxX = posX + width - 6.5F - bindBoxWidth;
               float bindBoxHeight = 8.0F;
               float bindBoxY = HudListPanel.rowBadgeY(posY, bindBoxHeight);
               rectangle.render(
                  ShapeProperties.create(matrix, bindBoxX, bindBoxY, bindBoxWidth, bindBoxHeight)
                     .round(2.0F)
                     .thickness(1.0F)
                     .outlineColor(new Color(33, 33, 33, alpha).getRGB())
                     .color(new Color(0, 0, 0, (int)(100.0F * animValue)).getRGB())
                     .build()
               );
               float bindTextX = bindBoxX + bindBoxWidth / 2.0F - bindWidth / 2.0F;
               fontBind.drawString(matrix, bind, bindTextX, HudListPanel.rowBadgeTextY(fontBind, posY, bindBoxHeight, bind), textColor.getRGB());
               matrix.pop();
               posY += moduleHeight * animValue;
            }
         }
      }

      this.setWidth((int)width);
      this.setHeight((int)f);
   }

   private void drawKeybindCategoryIcon(
      DrawContext context, MatrixStack matrix, ModuleCategory category, float x, float rowY, int color1, int color2, int alpha
   ) {
      if (category == ModuleCategory.ESP) {
         float iconY = HudListPanel.rowIconY(rowY, 9.0F);
         long time = System.currentTimeMillis();
         float t = (float)(Math.sin(time / 500.0) + 1.0) / 2.0F;
         int iconColor = ColorAssist.interpolateColor(color1, color2, t);
         BackgroundIcons.draw(context, BackgroundIcons.ESP, x, iconY, 9.0F, iconColor);
      } else {
         this.categoryIcon
            .drawRainbowString(matrix, getCategoryIcon(category), x, HudListPanel.alignedRowTextY(this.fontModule, this.categoryIcon, rowY), color1, color2);
      }
   }

   private static String getCategoryIcon(ModuleCategory category) {
      if (category == ModuleCategory.COMBAT) {
         return "b";
      } else if (category == ModuleCategory.MOVEMENT) {
         return "d";
      } else if (category == ModuleCategory.RENDER) {
         return "e";
      } else if (category == ModuleCategory.ESP) {
         return "i";
      } else if (category == ModuleCategory.PLAYER) {
         return "c";
      } else {
         return category == ModuleCategory.MISC ? "f" : category.getReadableName().substring(0, 1);
      }
   }

   private void drawMode2(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
      FontRenderer moduleFont = Fonts.getSize(13, Fonts.Type.BOLD);
      FontRenderer keyFont = Fonts.getSize(13, Fonts.Type.BOLD);
      float x = this.getX();
      float y = this.getY();
      float panelWidth = 95.0F;
      float titleHeight = 24.0F;
      float playerHeight = 16.0F;
      float spacing = -3.5F;
      float titleSpacing = -1.5F;
      float statusPanelWidth = 16.0F;
      float mainPanelWidth = panelWidth - statusPanelWidth - 11.0F;
      boolean showExamples = this.keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen);
      int moduleCount = showExamples ? 3 : this.keysList.size();
      float contentHeight = moduleCount * (playerHeight + spacing) - (moduleCount > 0 ? spacing : 0.0F);
      float totalHeight = titleHeight + titleSpacing + 7.0F + contentHeight;
      this.setWidth((int)panelWidth);
      this.setHeight((int)totalHeight);
      rectangle.render(ShapeProperties.create(matrix, x, y, panelWidth, totalHeight).round(7.0F).color(ThemeManager.BackgroundGui.getColor()).build());
      rectangle.render(
         ShapeProperties.create(matrix, x + 4.5F, y + 4.5F, panelWidth - 9.0F, titleHeight - 9.0F)
            .round(3.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      titleFont.drawString(matrix, "Binds", x + 10.0F, y + 10.0F, ThemeManager.textColor.getColor());
      Fonts.getSize(19, Fonts.Type.ICONS).drawString(matrix, "D", x + panelWidth - 19.0F, y + 9.5F, ThemeManager.textColor.getColor());
      float currentY = y + titleHeight + titleSpacing;
      rectangle.render(
         ShapeProperties.create(matrix, x + 4.5F, currentY, mainPanelWidth, contentHeight)
            .round(3.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      float statusPanelX = x + 4.5F + mainPanelWidth + 2.5F;
      rectangle.render(
         ShapeProperties.create(matrix, statusPanelX, currentY, statusPanelWidth, contentHeight)
            .round(3.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      if (showExamples) {
         String[] exampleModules = new String[]{"Aura", "Velocity", "Flight"};
         String[] exampleBinds = new String[]{"R", "V", "F"};

         for (int i = 0; i < 3; i++) {
            this.drawModuleBind(matrix, currentY, x, statusPanelX, statusPanelWidth, playerHeight, moduleFont, keyFont, exampleModules[i], exampleBinds[i]);
            currentY += playerHeight + spacing;
         }
      } else {
         for (Module module : this.keysList) {
            String moduleName = module.getVisibleName();
            String keyName = StringHelper.getBindName(module.getKey());
            this.drawModuleBind(matrix, currentY, x, statusPanelX, statusPanelWidth, playerHeight, moduleFont, keyFont, moduleName, keyName);
            currentY += playerHeight + spacing;
         }
      }
   }

   private void drawModuleBind(
      MatrixStack matrix,
      float currentY,
      float x,
      float statusPanelX,
      float statusPanelWidth,
      float playerHeight,
      FontRenderer moduleFont,
      FontRenderer keyFont,
      String moduleName,
      String keyName
   ) {
      float barWidth = 1.3F;
      float barHeight = 7.0F;
      float barX = x + 4.5F + 2.8F;
      float barY = currentY + 5.5F;
      rectangle.render(
         ShapeProperties.create(matrix, barX, barY, barWidth, barHeight)
            .round(0.9F)
            .color(ColorAssist.getHudColor(), ColorAssist.getHudColor2(), ColorAssist.getHudColor(), ColorAssist.getHudColor2())
            .build()
      );
      float nameX = barX + barWidth + 3.5F;
      float nameY = currentY + playerHeight - 9.0F;
      moduleFont.drawString(matrix, moduleName, nameX, nameY, ThemeManager.textColor.getColor());
      float keyTextWidth = keyFont.getStringWidth(keyName);
      float maxWidth = statusPanelWidth - 2.0F;
      if (keyTextWidth > maxWidth) {
         float scale = maxWidth / keyTextWidth;
         float keyX = statusPanelX + 1.0F;
         float keyY = currentY + playerHeight - 9.0F;
         matrix.push();
         matrix.translate(keyX, keyY, 0.0F);
         matrix.scale(scale, scale, 1.0F);
         keyFont.drawString(matrix, keyName, 0.0, 0.0, ColorAssist.getHudColor());
         matrix.pop();
      } else {
         float keyX = statusPanelX + (statusPanelWidth - keyTextWidth) / 2.0F;
         float keyY = currentY + playerHeight - 9.0F;
         keyFont.drawString(matrix, keyName, keyX, keyY, ColorAssist.getHudColor());
      }
   }
}
