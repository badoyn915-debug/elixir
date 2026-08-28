package code.elixir.display.hud;

import code.elixir.utils.display.render.font.FontRenderer;

public final class HudListPanel {
   public static final float HEADER_HEIGHT = 21.0F;
   public static final float ROW_HEIGHT = 11.0F;
   public static final float ROW_LEFT_PAD = 22.0F;
   public static final float ROW_RIGHT_PAD = 12.5F;
   public static final float ROW_CONTENT_GAP = 8.0F;
   public static final float HEADER_RIGHT_PAD = 14.0F;
   public static final float CONTENT_TOP_GAP = 0.0F;
   public static final float CONTENT_BOTTOM_PAD = 3.0F;
   public static final float LEVEL_NAME_GAP = 3.0F;

   private HudListPanel() {
   }

   public static float contentStartY(float panelY) {
      return panelY + 21.0F + 0.0F;
   }

   public static float panelHeight(float contentHeight) {
      return 21.0F + contentHeight + 3.0F;
   }

   public static float headerTextY(FontRenderer font, float panelY, String title) {
      return font.getCenteredY(panelY, 21.0F, title);
   }

   public static float rowTextY(FontRenderer font, float rowY) {
      return font.getCenteredY(rowY, 11.0F);
   }

   public static float rowTextY(FontRenderer font, float rowY, String text) {
      return font.getCenteredY(rowY, 11.0F, text);
   }

   public static float rowIconAlignedTextY(FontRenderer font, float rowY, float iconSize, String text) {
      return font.getCenteredY(rowIconY(rowY, iconSize), iconSize, text);
   }

   public static float rowBadgeTextY(FontRenderer font, float rowY, float badgeHeight, String text) {
      return font.getCenteredY(rowBadgeY(rowY, badgeHeight), badgeHeight, text);
   }

   public static float alignedRowTextY(FontRenderer referenceFont, FontRenderer font, float rowY) {
      return alignedRowTextY(referenceFont, font, rowY, "A");
   }

   public static float alignedRowTextY(FontRenderer referenceFont, FontRenderer font, float rowY, String referenceText) {
      float referenceY = rowIconAlignedTextY(referenceFont, rowY, 9.0F, referenceText);
      return referenceY + (referenceFont.getStringHeight(referenceText) - font.getStringHeight(referenceText)) / 2.0F;
   }

   public static float rowDotY(float rowY) {
      return rowY + 4.5F;
   }

   public static float rowBadgeY(float rowY, float badgeHeight) {
      return rowY + (11.0F - badgeHeight) / 2.0F;
   }

   public static float rowIconY(float rowY, float iconSize) {
      return rowY + (11.0F - iconSize) / 2.0F;
   }

   public static float rowWidth(FontRenderer nameFont, String name, float middleWidth, FontRenderer rightFont, String rightText) {
      return 22.0F + nameFont.getStringWidth(name) + middleWidth + 8.0F + rightFont.getStringWidth(rightText) + 12.5F;
   }

   public static float headerMinWidth(FontRenderer headerFont, String title) {
      return headerFont.getStringWidth(title) + 14.0F + 9.5F + 8.0F;
   }
}
