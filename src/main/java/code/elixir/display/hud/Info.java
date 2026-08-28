package code.elixir.display.hud;

import code.elixir.features.impl.misc.FreeCam;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.math.calc.Calculate;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public class Info extends AbstractDraggable {
   private static final Info INSTANCE = new Info();
   private static final float LINE_H = 14.0F;
   private static final float LINE_GAP = 1.0F;
   private static final float H_PAD = 7.0F;
   private static final float LABEL_VALUE_GAP = 2.0F;
   private static final int MAX_LINES = 4;
   private int fpsCount = 0;
   private boolean showTps = false;
   private boolean showFps = true;
   private boolean showBps = false;
   private boolean showCoords = true;
   private boolean rightSide = false;
   private final String[] lineLabels = new String[4];
   private final String[] lineValues = new String[4];
   private final float[] lineWidths = new float[4];
   private int lineCount = 0;

   private Info() {
      super("Info", 0, 0, 60, 0, false);
   }

   public static Info getInstance() {
      return INSTANCE;
   }

   public boolean isShowTps() {
      return this.showTps;
   }

   public void setShowTps(boolean showTps) {
      this.showTps = showTps;
   }

   public boolean isShowFps() {
      return this.showFps;
   }

   public void setShowFps(boolean showFps) {
      this.showFps = showFps;
   }

   public boolean isShowBps() {
      return this.showBps;
   }

   public void setShowBps(boolean showBps) {
      this.showBps = showBps;
   }

   public boolean isShowCoords() {
      return this.showCoords;
   }

   public void setShowCoords(boolean showCoords) {
      this.showCoords = showCoords;
   }

   public boolean isRightSide() {
      return this.rightSide;
   }

   public void setRightSide(boolean rightSide) {
      this.rightSide = rightSide;
   }

   private BlockPos resolveCoordinateBlockPos() {
      FreeCam freeCam = FreeCam.getInstance();
      return freeCam != null && freeCam.isState() && freeCam.pos != null ? BlockPos.ofFloored(freeCam.pos) : mc.player.getBlockPos();
   }

   @Override
   public boolean visible() {
      Hud hud = Hud.getInstance();
      if (hud == null) {
         return false;
      } else if (!hud.isState()) {
         return false;
      } else {
         return !Hud.interfaceSettings.isSelected("Info") ? false : this.hasAnyInfoLine();
      }
   }

   private boolean hasAnyInfoLine() {
      return this.showTps || this.showFps || this.showBps || this.showCoords;
   }

   @Override
   public void tick() {
      super.tick();
      this.fpsCount = mc.getCurrentFps();
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player != null && this.hasAnyInfoLine()) {
         MatrixStack matrix = context.getMatrices();
         BlockPos blockPos = this.resolveCoordinateBlockPos();
         FontRenderer font = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
         int valueColor = -1;
         this.lineCount = 0;
         if (this.showTps) {
            this.lineLabels[this.lineCount] = "TPS: ";
            this.lineValues[this.lineCount] = String.valueOf(Calculate.round(Network.TPS, 0.1F));
            this.lineCount++;
         }

         if (this.showFps) {
            this.lineLabels[this.lineCount] = "FPS: ";
            this.lineValues[this.lineCount] = String.valueOf(this.fpsCount);
            this.lineCount++;
         }

         if (this.showBps) {
            this.lineLabels[this.lineCount] = "BPS: ";
            this.lineValues[this.lineCount] = String.valueOf(Calculate.round(Simulations.getSpeedSqrt(mc.player) * 20.0, 0.25));
            this.lineCount++;
         }

         if (this.showCoords) {
            this.lineLabels[this.lineCount] = "Coords: ";
            this.lineValues[this.lineCount] = blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
            this.lineCount++;
         }

         if (this.lineCount != 0) {
            float maxPanelW = 0.0F;

            for (int i = 0; i < this.lineCount; i++) {
               this.lineWidths[i] = font.getStringWidth(this.lineLabels[i]) + 2.0F + font.getStringWidth(this.lineValues[i]) + 14.0F;
               maxPanelW = Math.max(maxPanelW, this.lineWidths[i]);
            }

            float totalH = this.lineCount * 14.0F + Math.max(0, this.lineCount - 1) * 1.0F;
            float screenW = window.getScaledWidth();
            float screenH = window.getScaledHeight();
            float scale = this.getScaleFactor();
            float sideMargin = 2.0F;
            float bottomMargin = PlayerInteractionHelper.isChat(mc.currentScreen) ? 16.0F : 4.0F;
            float baseBottom = screenH - bottomMargin;
            float posX = this.rightSide ? screenW - sideMargin - maxPanelW * (1.0F + scale) / 2.0F : sideMargin + maxPanelW * (scale - 1.0F) / 2.0F;
            float posY = baseBottom - totalH * (1.0F + scale) / 2.0F;
            this.setX(Math.round(posX));
            this.setY(Math.round(posY));
            this.setWidth(Math.round(maxPanelW));
            this.setHeight(Math.round(totalH));
            this.clampToScreen();
            posX = this.getX();
            float lineY = this.getY();

            for (int i = 0; i < this.lineCount; i++) {
               float lineX = this.rightSide ? posX + maxPanelW - this.lineWidths[i] : posX;
               this.drawInfoLine(matrix, font, lineX, lineY, this.lineWidths[i], this.lineLabels[i], this.lineValues[i], valueColor);
               lineY += 15.0F;
            }
         }
      }
   }

   private void drawInfoLine(MatrixStack matrix, FontRenderer font, float x, float y, float width, String label, String value, int valueColor) {
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), x, y, width, 14.0F, 7.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
      }

      rectangle.render(ShapeProperties.create(matrix, x, y, width, 14.0).round(7.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build());
      float textY = this.centeredTextY(font, y);
      float textX = x + 7.0F;
      font.drawRainbowString(matrix, label, textX, textY, ColorAssist.getHudColor(), ColorAssist.getHudColor2());
      font.drawString(matrix, value, textX + font.getStringWidth(label) + 2.0F, textY, valueColor);
   }

   private float centeredTextY(FontRenderer font, float boxY) {
      return font.getCenteredY(boxY, 14.0F);
   }
}
