package code.elixir.display.screens.mainmenu;

import code.elixir.utils.client.text.TextAnimation;
import code.elixir.utils.display.atlasfont.msdf.MsdfFont;
import code.elixir.utils.display.gif.GifRender;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.BloodyBackgroundRenderer;
import code.elixir.utils.display.render.MainMenuSkyRenderer;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.systemrender.builders.Builder;
import code.elixir.utils.display.render.systemrender.builders.states.QuadColorState;
import code.elixir.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.elixir.utils.display.render.systemrender.builders.states.SizeState;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class MainMenu extends Screen implements QuickImports {
   public static MainMenu INSTANCE = new MainMenu();
   public int x;
   public int y;
   public int width;
   public int height;
   private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());
   private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   public boolean isDarkMode = true;
   public String themeIcon = "I";
   private final TextAnimation textAnimation = new TextAnimation();
   private final Color darkButtonColor = new Color(0, 0, 0, 80);
   private final Color darkButtonHoverColor = new Color(25, 25, 25, 140);
   private final Color darkTextColor = new Color(235, 235, 235, 255);
   private long authTransitionStartedAt = -1L;
   private static final Identifier SOLO_ICON = BackgroundIcons.SOLO;
   private static final Identifier MULTI_ICON = BackgroundIcons.MULTIPLAYER;
   private static final Identifier SETTINGS_ICON = BackgroundIcons.SETTINGS;
   private static final Identifier ACCOUNT_ICON = BackgroundIcons.ACCOUNT;
   private static final Identifier EXIT_ICON_ID = BackgroundIcons.EXIT;

   private float hoverSingle = 0.0F;
   private float hoverMulti = 0.0F;
   private float hoverSettings = 0.0F;
   private float hoverAccounts = 0.0F;
   private float hoverExit = 0.0F;
   private boolean exitSliding = false;
   private float exitSlideProgress = 0.0F;
   private double exitDragStartX = 0.0;
   private float exitStartProgress = 0.0F;

   public MainMenu() {
      super(Text.of("MainMenu"));
   }

   @Override
   protected void init() {
      super.init();
      this.resetHovers();
   }

   public void resetHovers() {
      this.hoverSingle = 0.0F;
      this.hoverMulti = 0.0F;
      this.hoverSettings = 0.0F;
      this.hoverAccounts = 0.0F;
      this.hoverExit = 0.0F;
      this.exitSliding = false;
      this.exitSlideProgress = 0.0F;
   }

   public void startAuthTransition() {
      this.authTransitionStartedAt = System.currentTimeMillis();
   }

   public void tick() {
      super.tick();
      this.textAnimation.updateText();
   }

   private Color interpolateColor(Color start, Color end, float t) {
      int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * t);
      int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * t);
      int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * t);
      int a = (int)(start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
      return new Color(r, g, b, a);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.x = window.getScaledWidth();
      this.y = window.getScaledHeight();
      this.width = window.getScaledWidth();
      this.height = window.getScaledHeight();
      float centerX = this.width / 2.0F;
      float centerY = this.height / 2.0F;

      this.renderBackgroundLayer(context, mouseX, mouseY, 1.0F);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      this.renderMainMenuContent(context, mouseX, mouseY, centerX, centerY);
      this.renderAuthTransitionOverlay(context);
   }

   private void renderMainMenuContent(DrawContext context, int mouseX, int mouseY, float centerX, float centerY) {
      MatrixStack matrices = context.getMatrices();
      float buttonWidth = 104.0F;
      float buttonHeight = 22.0F;
      float colGap = 8.0F;
      float rowGap = 10.0F;
      float round = 6.0F;
      float gridHeight = buttonHeight + rowGap + buttonHeight;
      float startY = centerY - gridHeight / 2.0F + 14.0F;
      float leftX = centerX - colGap / 2.0F - buttonWidth;
      float rightX = centerX + colGap / 2.0F;
      float y2 = startY + buttonHeight + rowGap;

      FontRenderer font = Fonts.getSize(36, Fonts.Type.BOLD);
      float textWidth = font.getStringWidth("Elixir");
      float shimmerTime = (float)(System.currentTimeMillis() % 8000L) / 8000.0F;
      float sin1 = (float)Math.sin(shimmerTime * Math.PI * 2.0) * 0.5F + 0.5F;
      float sin2 = (float)Math.cos(shimmerTime * Math.PI * 2.0) * 0.5F + 0.5F;
      int r1 = (int)MathHelper.lerp(sin1, 140.0F, 235.0F);
      int g1 = (int)MathHelper.lerp(sin1, 145.0F, 240.0F);
      int b1 = (int)MathHelper.lerp(sin1, 160.0F, 255.0F);
      int r2 = (int)MathHelper.lerp(sin2, 230.0F, 100.0F);
      int g2 = (int)MathHelper.lerp(sin2, 235.0F, 105.0F);
      int b2 = (int)MathHelper.lerp(sin2, 250.0F, 120.0F);
      font.drawRainbowString(matrices, "Elixir", centerX - textWidth / 2.0F, startY - 70.0F, new Color(r1, g1, b1).getRGB(), new Color(r2, g2, b2).getRGB());

      boolean hSingle = this.isHover(mouseX, mouseY, leftX, startY, buttonWidth, buttonHeight);
      this.hoverSingle += ((hSingle ? 1.0F : 0.0F) - this.hoverSingle) * 0.3F;
      if (!hSingle && this.hoverSingle < 0.01F) this.hoverSingle = 0.0F;

      boolean hMulti = this.isHover(mouseX, mouseY, rightX, startY, buttonWidth, buttonHeight);
      this.hoverMulti += ((hMulti ? 1.0F : 0.0F) - this.hoverMulti) * 0.3F;
      if (!hMulti && this.hoverMulti < 0.01F) this.hoverMulti = 0.0F;

      boolean hSettings = this.isHover(mouseX, mouseY, leftX, y2, buttonWidth, buttonHeight);
      this.hoverSettings += ((hSettings ? 1.0F : 0.0F) - this.hoverSettings) * 0.3F;
      if (!hSettings && this.hoverSettings < 0.01F) this.hoverSettings = 0.0F;

      boolean hAccounts = this.isHover(mouseX, mouseY, rightX, y2, buttonWidth, buttonHeight);
      this.hoverAccounts += ((hAccounts ? 1.0F : 0.0F) - this.hoverAccounts) * 0.3F;
      if (!hAccounts && this.hoverAccounts < 0.01F) this.hoverAccounts = 0.0F;

      this.drawSimpleButton(context, leftX, startY, buttonWidth, buttonHeight, round, "SinglePlayer", SOLO_ICON, this.hoverSingle);
      this.drawSimpleButton(context, rightX, startY, buttonWidth, buttonHeight, round, "MultiPlayer", MULTI_ICON, this.hoverMulti);
      this.drawSimpleButton(context, leftX, y2, buttonWidth, buttonHeight, round, "Settings", SETTINGS_ICON, this.hoverSettings);
      this.drawSimpleButton(context, rightX, y2, buttonWidth, buttonHeight, round, "Accounts", ACCOUNT_ICON, this.hoverAccounts);

      this.renderSlideExit(context, mouseX, mouseY, round);
      Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(matrices, "© 2026 Elixir. All rights reserved.", centerX, this.height - 14, this.darkTextColor.getRGB());
   }

   private void renderBackgroundLayer(DrawContext context, int mouseX, int mouseY, float menuReveal) {
      MainMenuSkyRenderer.render(context, this.width, this.height);
   }

   private void renderAuthTransitionOverlay(DrawContext context) {
      if (this.authTransitionStartedAt >= 0L) {
         float progress = (float)(System.currentTimeMillis() - this.authTransitionStartedAt) / 700.0F;
         if (progress >= 1.0F) {
            this.authTransitionStartedAt = -1L;
         } else {
            float eased = 1.0F - (float)Math.pow(1.0F - MathHelper.clamp(progress, 0.0F, 1.0F), 3.0);
            int alpha = (int)(255.0F * (1.0F - eased));
            rectangle.render(ShapeProperties.create(context.getMatrices(), 0.0, 0.0, this.width, this.height).color(new Color(0, 0, 0, alpha).getRGB()).build());
         }
      }
   }

   private boolean isHover(double mouseX, double mouseY, float x, float y, float w, float h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean shouldPause() {
      return false;
   }

   private void drawRoundedTexture(MatrixStack matrices, Identifier id, float x, float y, float w, float h, QuadRadiusState radius) {
      int glId = mc.getTextureManager().getTexture(id).getGlId();
      Builder.texture()
         .size(new SizeState(w, h))
         .radius(radius)
         .color(QuadColorState.WHITE)
         .smoothness(1.0F)
         .texture(0.0F, 0.0F, 1.0F, 1.0F, glId)
         .build()
         .render(matrices.peek().getPositionMatrix(), x, y, 0.0F);
   }

   private void drawButtonLabel(MatrixStack matrices, float bx, float by, float bw, float bh, String label, Identifier icon, float iconSize, int textColor) {
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      float textW = font.getStringWidth(label);
      float gap = 4.0F;
      float groupW = textW + gap + iconSize;
      float groupX = bx + (bw - groupW) / 2.0F;
      float textY = font.getCenteredY(by, bh, label);
      font.drawString(matrices, label, groupX, textY, textColor);
      float iconX = groupX + textW + gap;
      float iconY = by + (bh - iconSize) / 2.0F;
      this.drawRoundedTexture(matrices, icon, iconX, iconY, iconSize, iconSize, QuadRadiusState.NO_ROUND);
   }

   private void drawSimpleButton(
      DrawContext context,
      float bx,
      float by,
      float buttonWidth,
      float buttonHeight,
      float round,
      String label,
      Identifier icon,
      float hover
   ) {
      MatrixStack matrices = context.getMatrices();
      float h = MathHelper.clamp(hover, 0.0F, 1.0F);
      Color btnBg = this.interpolateColor(this.darkButtonColor, this.darkButtonHoverColor, h);

      rectangle.render(
         ShapeProperties.create(matrices, bx, by, buttonWidth, buttonHeight)
            .round(round)
            .color(btnBg.getRGB())
            .build()
      );
      this.drawButtonLabel(matrices, bx, by, buttonWidth, buttonHeight, label, icon, 11.0F, this.darkTextColor.getRGB());
   }

   private void renderSlideExit(DrawContext context, int mouseX, int mouseY, float round) {
      MatrixStack matrices = context.getMatrices();
      float exW = 68.0F;
      float exH = 22.0F;
      float exX = 12.0F;
      float exY = this.height - exH - 12.0F;
      float pad = 3.0F;
      float circleD = exH - pad * 2.0F;
      float maxTravel = exW - circleD - pad * 2.0F;

      if (!this.exitSliding && this.exitSlideProgress > 0.0F && this.exitSlideProgress < 0.985F) {
         this.exitSlideProgress += (0.0F - this.exitSlideProgress) * 0.25F;
         if (this.exitSlideProgress < 0.01F) {
            this.exitSlideProgress = 0.0F;
         }
      }

      boolean hExit = this.isHover(mouseX, mouseY, exX, exY, exW, exH) || this.exitSliding;
      this.hoverExit += ((hExit ? 1.0F : 0.0F) - this.hoverExit) * 0.3F;
      if (!hExit && this.hoverExit < 0.01F) this.hoverExit = 0.0F;

      float circleX = exX + pad + this.exitSlideProgress * maxTravel;
      float circleY = exY + pad;

      Color btnBg = this.interpolateColor(this.darkButtonColor, this.darkButtonHoverColor, this.hoverExit);

      rectangle.render(
         ShapeProperties.create(matrices, exX, exY, exW, exH)
            .round(round)
            .color(btnBg.getRGB())
            .build()
      );

      if (this.exitSlideProgress > 0.001F) {
         int a = (int)(120.0F + this.exitSlideProgress * 120.0F);
         rectangle.render(
            ShapeProperties.create(matrices, exX + pad, circleY, circleX - exX - pad + circleD, circleD)
               .round(circleD / 2.0F)
               .color(new Color(210, 60, 60, a).getRGB())
               .build()
         );
      }

      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      String label = "Exit";
      float iconSize = 9.0F;
      float gap = 4.0F;
      float textW = font.getStringWidth(label);
      float groupW = textW + gap + iconSize;
      float groupX = exX + (exW - groupW) / 2.0F;
      float textY = font.getCenteredY(exY, exH, label);
      font.drawString(matrices, label, groupX, textY, this.darkTextColor.getRGB());
      float iconY = exY + (exH - iconSize) / 2.0F;
      this.drawRoundedTexture(matrices, EXIT_ICON_ID, groupX + textW + gap, iconY, iconSize, iconSize, QuadRadiusState.NO_ROUND);

      float circleVis = Math.max(this.hoverExit, this.exitSliding ? 1.0F : 0.0F);
      if (circleVis > 0.01F) {
         Color base = this.exitSlideProgress > 0.001F ? new Color(220, 70, 70) : new Color(205, 205, 205);
         int a = (int)(235.0F * MathHelper.clamp(circleVis, 0.0F, 1.0F));
         rectangle.render(
            ShapeProperties.create(matrices, circleX, circleY, circleD, circleD)
               .round(circleD / 2.0F)
               .color(new Color(base.getRed(), base.getGreen(), base.getBlue(), a).getRGB())
               .build()
         );
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float centerX = this.width / 2.0F;
      float centerY = this.height / 2.0F;
      float buttonWidth = 104.0F;
      float buttonHeight = 22.0F;
      float colGap = 8.0F;
      float rowGap = 10.0F;
      float gridHeight = buttonHeight + rowGap + buttonHeight;
      float startY = centerY - gridHeight / 2.0F + 14.0F;
      float leftX = centerX - colGap / 2.0F - buttonWidth;
      float rightX = centerX + colGap / 2.0F;
      float y2 = startY + buttonHeight + rowGap;
      float exW = 68.0F;
      float exH = 22.0F;
      float pad = 3.0F;
      float exX = 12.0F;
      float exY = this.height - exH - 12.0F;
      float circleD = exH - pad * 2.0F;
      float maxTravel = exW - circleD - pad * 2.0F;
      float circleX = exX + pad + this.exitSlideProgress * maxTravel;
      float circleY = exY + pad;

      if (button == 0 && this.isHover(mouseX, mouseY, circleX, circleY, circleD, circleD)) {
         this.exitSliding = true;
         this.exitDragStartX = mouseX;
         this.exitStartProgress = this.exitSlideProgress;
         return true;
      } else {
         if (button == 0) {
            if (this.isHover(mouseX, mouseY, leftX, startY, buttonWidth, buttonHeight)) {
               this.resetHovers();
               mc.setScreen(new SelectWorldScreen(this));
               return true;
            }

            if (this.isHover(mouseX, mouseY, rightX, startY, buttonWidth, buttonHeight)) {
               this.resetHovers();
               mc.setScreen(new MultiplayerScreen(this));
               return true;
            }

            if (this.isHover(mouseX, mouseY, leftX, y2, buttonWidth, buttonHeight)) {
               this.resetHovers();
               mc.setScreen(new OptionsScreen(this, mc.options));
               return true;
            }

            if (this.isHover(mouseX, mouseY, rightX, y2, buttonWidth, buttonHeight)) {
               this.resetHovers();
               mc.setScreen(new AccountsScreen(this));
               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.exitSliding && button == 0) {
         float exW = 68.0F;
         float exH = 22.0F;
         float pad = 3.0F;
         float circleD = exH - pad * 2.0F;
         float maxTravel = exW - circleD - pad * 2.0F;
         float delta = (float)(mouseX - this.exitDragStartX);
         this.exitSlideProgress = MathHelper.clamp(this.exitStartProgress + delta / maxTravel, 0.0F, 1.0F);
         if (this.exitSlideProgress >= 0.985F) {
            this.exitSliding = false;
            mc.stop();
         }

         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.exitSliding && button == 0) {
         this.exitSliding = false;
         if (this.exitSlideProgress >= 0.985F) {
            mc.stop();
         }

         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }
}
