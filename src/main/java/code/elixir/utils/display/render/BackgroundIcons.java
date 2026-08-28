package code.elixir.utils.display.render;

import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.systemrender.builders.Builder;
import code.elixir.utils.display.render.systemrender.builders.states.QuadColorState;
import code.elixir.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.elixir.utils.display.render.systemrender.builders.states.SizeState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class BackgroundIcons {
   public static final float HUD_ICON_SIZE = 9.5F;
   public static final float SIDEBAR_ICON_SIZE = 14.0F;
   public static final float ESP_CATEGORY_ICON_SIZE = 9.0F;
   public static final Identifier ESP = icon("esp.png");
   public static final Identifier POTIONS = icon("potions.png");
   public static final Identifier KEYBINDS = icon("keybinds.png");
   public static final Identifier STAFFLIST = icon("stafflist.png");
   public static final Identifier COOLDOWNS = icon("cooldowns.png");
   public static final Identifier BACKGROUND = icon("background.png");
   public static final Identifier SINGLE = icon("1.png");
   public static final Identifier MULTI = icon("2.png");
   public static final Identifier SOLO = icon("solo.png");
   public static final Identifier MULTIPLAYER = icon("multiplayer.png");
   public static final Identifier SETTINGS = icon("settings.png");
   public static final Identifier ACCOUNT = icon("account.png");
   public static final Identifier EXIT = icon("exit.png");
   public static final Identifier TELEGRAM = icon("telegram.png");
   public static final Identifier YOUTUBE = icon("youtube.png");
   public static final Identifier DISCORD = icon("discord.png");

   private BackgroundIcons() {
   }

   public static Identifier icon(String fileName) {
      return Identifier.of("elixir", "background/" + fileName);
   }

   public static void draw(DrawContext context, Identifier texture, float x, float y, float size, int color) {
      MatrixStack matrices = context.getMatrices();
      int glId = MinecraftClient.getInstance().getTextureManager().getTexture(texture).getGlId();
      Builder.texture()
         .size(new SizeState(size, size))
         .radius(QuadRadiusState.NO_ROUND)
         .color(new QuadColorState(color))
         .smoothness(1.0F)
         .texture(0.0F, 0.0F, 1.0F, 1.0F, glId)
         .build()
         .render(matrices.peek().getPositionMatrix(), x, y, 0.0F);
   }

   public static void drawSmall(DrawContext context, Identifier texture, float x, float y, int color) {
      draw(context, texture, x, y, 9.5F, color);
   }

   public static void drawSidebar(DrawContext context, Identifier texture, float x, float y, int color) {
      draw(context, texture, x, y, 14.0F, color);
   }

   public static void drawEspCategory(DrawContext context, Identifier texture, float x, float y, int color) {
      draw(context, texture, x, y, 9.0F, color);
   }

   public static void drawSmallCentered(DrawContext context, Identifier texture, float centerX, float y, int color) {
      drawSmall(context, texture, centerX - 4.75F, y, color);
   }

   public static void drawHudHeaderIcon(DrawContext context, Identifier texture, FontRenderer font, String label, float textY, float centerX, int color) {
      float iconY = textY + (font.getStringHeight(label) - 9.5F) / 2.0F;
      drawSmallCentered(context, texture, centerX, iconY, color);
   }
}
