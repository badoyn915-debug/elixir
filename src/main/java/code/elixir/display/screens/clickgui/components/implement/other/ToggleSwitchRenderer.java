package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class ToggleSwitchRenderer {
   public static final float WIDTH = 22.0F;
   public static final float HEIGHT = 10.0F;
   public static final float PILL_WIDTH = 22.0F;
   public static final float PILL_HEIGHT = 10.0F;
   public static final float HEADER_WIDTH = 22.0F;
   public static final float HEADER_HEIGHT = 10.0F;
   public static final float HEADER_RIGHT_INSET = 8.0F;
   public static final float RIGHT_MARGIN = 6.0F;
   public static final float ROW_HEIGHT = 21.0F;
   public static final int BIND_PILL_FONT_SIZE = 12;
   public static final float BIND_PILL_HEIGHT = 14.0F;
   public static final float BIND_PILL_MIN_WIDTH = 48.0F;
   public static final float BIND_PILL_TEXT_PADDING = 14.0F;
   private static final float KNOB_PADDING = 1.25F;
   private static final float OUTLINE_THICKNESS = 1.0F;
   private static final int OUTLINE_COLOR = -16777216;
   private static final float KNOB_LERP_SPEED = 11.0F;
   private static final Map<Object, ToggleSwitchRenderer.KnobState> knobStates = new HashMap<>();

   private ToggleSwitchRenderer() {
   }

   public static float trackY(float rowY, float rowHeight) {
      return rowY + (rowHeight - 10.0F) / 2.0F;
   }

   public static float headerTrackY(float rowY, float rowHeight) {
      return trackY(rowY, rowHeight);
   }

   public static float alignedControlY(FontRenderer labelFont, String labelText, float rowY, float rowHeight, float controlHeight) {
      return rowY + (rowHeight - controlHeight) / 2.0F;
   }

   public static float toggleAlignedY(FontRenderer labelFont, String labelText, float rowY, float rowHeight) {
      return alignedControlY(labelFont, labelText, rowY, rowHeight, 10.0F);
   }

   public static float bindPillTrackY(FontRenderer labelFont, String labelText, float rowY, float rowHeight) {
      return alignedControlY(labelFont, labelText, rowY, rowHeight, 14.0F);
   }

   public static float bindPillWidth(String keyText) {
      float textW = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD).getStringWidth(keyText);
      return Math.max(48.0F, textW + 14.0F);
   }

   public static void drawPillTextCentered(FontRenderer font, MatrixStack matrices, String text, float pillX, float pillY, float pillW, float pillH, int color) {
      font.drawCenteredBoth(matrices, text, pillX + pillW / 2.0F, pillY, pillH, color);
   }

   public static float resolveHeaderX(float headerX, float headerWidth) {
      float x = headerX + headerWidth - 22.0F - 8.0F;
      float minX = headerX + 58.0F;
      float maxX = headerX + headerWidth - 22.0F - 4.0F;
      return Math.max(minX, Math.min(x, maxX));
   }

   public static float labelDrawY(FontRenderer font, String text, float rowY, float rowHeight) {
      return font.getCenteredY(rowY, rowHeight, text);
   }

   public static void drawLabel(FontRenderer font, MatrixStack matrices, String text, float labelX, float rowY, float rowHeight, int color) {
      font.drawString(matrices, text, labelX, labelDrawY(font, text, rowY, rowHeight), color);
   }

   public static void renderDefault(DrawContext context, float x, float y, boolean enabled, Object animId) {
      renderOnOffPill(context, x, y, enabled, animId);
   }

   public static void renderHeader(DrawContext context, float x, float y, boolean enabled, Object animId) {
      renderOnOffPill(context, x, y, enabled, animId);
   }

   public static void renderOnOffPill(DrawContext context, float x, float y, boolean enabled, Object animId) {
      Rectangle.flushPending();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      MatrixStack matrices = context.getMatrices();
      float progress = knobProgress(animId, enabled);
      float round = 5.0F;
      int offBg = ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), 220);
      int onBg = ColorAssist.getClientColor() | 0xFF000000;
      int bg = ColorAssist.interpolateColor(offBg, onBg, progress);
      QuickImports.rectangle.render(ShapeProperties.create(matrices, x, y, 22.0, 10.0).round(round).thickness(1.0F).outlineColor(-16777216).color(bg).build());
      Rectangle.flushPending();
      float knobSize = 7.5F;
      float knobTravel = 19.5F - knobSize;
      float knobX = x + 1.25F + knobTravel * progress;
      float knobY = y + 1.25F;
      QuickImports.rectangle
         .render(
            ShapeProperties.create(matrices, knobX, knobY, knobSize, knobSize).round(knobSize / 2.0F).thickness(1.0F).outlineColor(-16777216).color(-1).build()
         );
      Rectangle.flushPending();
   }

   private static float knobProgress(Object id, boolean enabled) {
      float target = enabled ? 1.0F : 0.0F;
      ToggleSwitchRenderer.KnobState state = knobStates.computeIfAbsent(id, key -> {
         ToggleSwitchRenderer.KnobState created = new ToggleSwitchRenderer.KnobState();
         created.progress = target;
         created.lastNanos = System.nanoTime();
         return created;
      });
      long now = System.nanoTime();
      float dt = (float)(now - state.lastNanos) / 1.0E9F;
      state.lastNanos = now;
      dt = Math.min(dt, 0.05F);
      float blend = 1.0F - (float)Math.exp(-11.0F * dt);
      state.progress = state.progress + (target - state.progress) * blend;
      if (Math.abs(state.progress - target) < 0.002F) {
         state.progress = target;
      }

      return state.progress;
   }

   public static boolean isHeaderHovered(double mouseX, double mouseY, float x, float y) {
      return isPillHovered(mouseX, mouseY, x, y);
   }

   public static boolean isDefaultHovered(double mouseX, double mouseY, float x, float y) {
      return isPillHovered(mouseX, mouseY, x, y);
   }

   public static boolean isPillHovered(double mouseX, double mouseY, float x, float y) {
      return isHovered(mouseX, mouseY, x, y, 22.0F, 10.0F);
   }

   public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
   }

   private static final class KnobState {
      float progress;
      long lastNanos;
   }
}
