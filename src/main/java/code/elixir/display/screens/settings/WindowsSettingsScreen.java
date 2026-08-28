package code.elixir.display.screens.settings;

import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

public class WindowsSettingsScreen extends Screen implements QuickImports {
   private static final Color C_DIM = new Color(6, 6, 8, 215);
   private static final Color C_WINDOW = new Color(22, 22, 26, 255);
   private static final Color C_HEADER = new Color(30, 30, 35, 255);
   private static final Color C_SIDEBAR = new Color(26, 26, 31, 255);
   private static final Color C_CONTENT = new Color(19, 19, 23, 255);
   private static final Color C_CARD = new Color(33, 33, 39, 255);
   private static final Color C_CARD_HOVER = new Color(44, 44, 52, 255);
   private static final Color C_NAV_SEL = new Color(46, 46, 54, 255);
   private static final Color C_ACCENT = new Color(141, 99, 246, 255);
   private static final Color C_ACCENT_DIM = new Color(141, 99, 246, 60);
   private static final Color C_TEXT = new Color(237, 237, 242, 255);
   private static final Color C_SUBTEXT = new Color(150, 150, 162, 255);
   private static final Color C_OUTLINE = new Color(64, 64, 74, 150);
   private static final Color C_TRACK = new Color(64, 64, 74, 255);
   private static final Color C_SEARCH = new Color(38, 38, 44, 255);
   private static final WindowsSettingsScreen.NavItem[] NAV = new WindowsSettingsScreen.NavItem[]{
      new WindowsSettingsScreen.NavItem("home", "Главная"),
      new WindowsSettingsScreen.NavItem("display", "Дисплей"),
      new WindowsSettingsScreen.NavItem("graphics", "Графика"),
      new WindowsSettingsScreen.NavItem("sound", "Звук"),
      new WindowsSettingsScreen.NavItem("controls", "Управление"),
      new WindowsSettingsScreen.NavItem("keys", "Клавиши"),
      new WindowsSettingsScreen.NavItem("chat", "Чат"),
      new WindowsSettingsScreen.NavItem("appearance", "Внешний вид"),
      new WindowsSettingsScreen.NavItem("language", "Язык"),
      new WindowsSettingsScreen.NavItem("accessibility", "Спец. возможности")
   };
   private final Screen parent;
   private int selected = 0;
   private final float[] navHover = new float[NAV.length];
   private float winX;
   private float winY;
   private float winW;
   private float winH;
   private float headerH;
   private float sidebarX;
   private float sidebarW;
   private float contentX;
   private float contentY;
   private float contentW;
   private float contentH;
   private final List<WindowsSettingsScreen.Slider> sliders = new ArrayList<>();
   private final List<WindowsSettingsScreen.Toggle> toggles = new ArrayList<>();
   private final List<WindowsSettingsScreen.Cycle> cycles = new ArrayList<>();
   private final List<WindowsSettingsScreen.ClickRow> rows = new ArrayList<>();
   private final HashMap<String, Float> toggleAnim = new HashMap<>();
   private WindowsSettingsScreen.Slider draggingSlider = null;
   private KeyBinding listeningBind = null;
   private float scroll = 0.0F;
   private float scrollTarget = 0.0F;
   private float contentVirtualHeight = 0.0F;
   private static float uiScale = 3.0F;
   private float lastScale = 3.0F;
   private float lastGuiScale = 1.0F;
   private float vw;
   private float vh;
   private String search = "";
   private boolean searchFocused = false;
   private float searchX;
   private float searchY;
   private float searchW;
   private float searchH;
   private float zPlusX;
   private float zMinusX;
   private float zBtnY;
   private float zBtnSize;

   public WindowsSettingsScreen(Screen parent) {
      super(Text.literal("Параметры"));
      this.parent = parent;
   }

   public void close() {
      mc.setScreen(this.parent);
   }

   public boolean shouldPause() {
      return true;
   }

   public void tick() {
      super.tick();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      MatrixStack matrices = context.getMatrices();
      float guiScale = (float)window.getScaleFactor();
      float realW = window.getWidth();
      float realH = window.getHeight();
      float maxS = Math.min(5.0F, Math.min(realW / 460.0F, realH / 300.0F));
      float S;
      this.lastScale = S = clamp(uiScale, 1.5F, Math.max(1.5F, maxS));
      this.lastGuiScale = guiScale;
      this.vw = realW / S;
      this.vh = realH / S;
      int vmx = (int)(mouseX * guiScale / S);
      int vmy = (int)(mouseY * guiScale / S);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      matrices.push();
      matrices.scale(S / guiScale, S / guiScale, 1.0F);
      rectangle.render(ShapeProperties.create(matrices, 0.0, 0.0, this.vw, this.vh).color(C_DIM.getRGB()).build());
      float margin;
      this.winX = margin = Math.max(10, Math.round(Math.min(this.vw, this.vh) * 0.05F));
      this.winY = margin;
      this.winW = this.vw - margin * 2.0F;
      this.winH = this.vh - margin * 2.0F;
      this.headerH = 30.0F;
      this.sidebarX = this.winX;
      this.sidebarW = Math.min(176.0F, Math.max(140.0F, this.winW * 0.26F));
      this.contentX = this.winX + this.sidebarW;
      this.contentY = this.winY + this.headerH;
      this.contentW = this.winW - this.sidebarW;
      this.contentH = this.winH - this.headerH;
      rectangle.render(ShapeProperties.create(matrices, this.winX, this.winY, this.winW, this.winH).round(10.0F).color(C_WINDOW.getRGB()).build());
      rectangle.render(
         ShapeProperties.create(matrices, this.contentX, this.contentY, this.contentW, this.contentH)
            .round(0.0F, 0.0F, 10.0F, 0.0F)
            .color(C_CONTENT.getRGB())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrices, this.sidebarX, this.contentY, this.sidebarW, this.contentH)
            .round(0.0F, 0.0F, 0.0F, 10.0F)
            .color(C_SIDEBAR.getRGB())
            .build()
      );
      this.sliders.clear();
      this.toggles.clear();
      this.cycles.clear();
      this.rows.clear();
      this.renderHeader(context, vmx, vmy);
      this.renderSidebar(context, vmx, vmy, delta);
      this.scroll = this.scroll + (this.scrollTarget - this.scroll) * 0.25F;
      this.renderContent(context, vmx, vmy, delta);
      this.renderZoomControls(context, vmx, vmy);
      matrices.pop();
   }

   private void renderZoomControls(DrawContext context, int mouseX, int mouseY) {
      MatrixStack m = context.getMatrices();
      this.zBtnSize = 18.0F;
      this.zBtnY = this.winY + this.winH - this.zBtnSize - 8.0F;
      this.zPlusX = this.winX + this.winW - this.zBtnSize - 10.0F;
      this.zMinusX = this.zPlusX - this.zBtnSize - 5.0F;
      float labelW = 40.0F;
      float bgX = this.zMinusX - labelW - 6.0F;
      rectangle.render(
         ShapeProperties.create(m, bgX - 6.0F, this.zBtnY - 5.0F, this.zPlusX + this.zBtnSize - (bgX - 6.0F) + 6.0F, this.zBtnSize + 10.0F)
            .round(8.0F)
            .thickness(1.0F)
            .outlineColor(C_OUTLINE.getRGB())
            .color(C_CARD.getRGB())
            .build()
      );
      int pct = Math.round(uiScale / 3.0F * 100.0F);
      Fonts.getSize(12, Fonts.Type.SEMI).drawCenteredString(m, pct + "%", bgX + labelW / 2.0F, this.zBtnY + this.zBtnSize / 2.0F - 2.5F, C_TEXT.getRGB());
      boolean hMinus = mouseX >= this.zMinusX && mouseX <= this.zMinusX + this.zBtnSize && mouseY >= this.zBtnY && mouseY <= this.zBtnY + this.zBtnSize;
      boolean hPlus = mouseX >= this.zPlusX && mouseX <= this.zPlusX + this.zBtnSize && mouseY >= this.zBtnY && mouseY <= this.zBtnY + this.zBtnSize;
      rectangle.render(
         ShapeProperties.create(m, this.zMinusX, this.zBtnY, this.zBtnSize, this.zBtnSize)
            .round(5.0F)
            .color((hMinus ? C_CARD_HOVER : C_TRACK).getRGB())
            .build()
      );
      rectangle.render(
         ShapeProperties.create(m, this.zPlusX, this.zBtnY, this.zBtnSize, this.zBtnSize).round(5.0F).color((hPlus ? C_ACCENT : C_TRACK).getRGB()).build()
      );
      Fonts.getSize(16, Fonts.Type.BOLD)
         .drawCenteredString(m, "-", this.zMinusX + this.zBtnSize / 2.0F, this.zBtnY + this.zBtnSize / 2.0F - 4.5F, C_TEXT.getRGB());
      Fonts.getSize(15, Fonts.Type.BOLD)
         .drawCenteredString(m, "+", this.zPlusX + this.zBtnSize / 2.0F, this.zBtnY + this.zBtnSize / 2.0F - 4.0F, C_TEXT.getRGB());
   }

   private void renderHeader(DrawContext context, int mouseX, int mouseY) {
      MatrixStack m = context.getMatrices();
      FontRenderer title = Fonts.getSize(18, Fonts.Type.BOLD);
      title.drawString(m, "Параметры", this.winX + 14.0F, this.winY + this.headerH / 2.0F - 1.0F, C_TEXT.getRGB());
      this.searchW = Math.min(360.0F, this.winW * 0.42F);
      this.searchH = 17.0F;
      this.searchX = this.winX + this.winW / 2.0F - this.searchW / 2.0F;
      this.searchY = this.winY + this.headerH / 2.0F - this.searchH / 2.0F;
      rectangle.render(
         ShapeProperties.create(m, this.searchX, this.searchY, this.searchW, this.searchH)
            .round(5.0F)
            .thickness(1.0F)
            .outlineColor((this.searchFocused ? C_ACCENT : C_OUTLINE).getRGB())
            .color(C_SEARCH.getRGB())
            .build()
      );
      FontRenderer sf = Fonts.getSize(13, Fonts.Type.DEFAULT);
      boolean caret = this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L;
      if (this.search.isEmpty() && !this.searchFocused) {
         sf.drawString(m, "Найти параметр", this.searchX + 8.0F, this.searchY + this.searchH / 2.0F, C_SUBTEXT.getRGB());
      } else {
         sf.drawString(m, this.search + (caret ? "|" : ""), this.searchX + 8.0F, this.searchY + this.searchH / 2.0F, C_TEXT.getRGB());
      }

      float dotX = this.winX + this.winW - 18.0F;
      float dotY = this.winY + this.headerH / 2.0F;
      boolean closeHover = this.dist(mouseX, mouseY, dotX, dotY) < 9.0F;
      rectangle.render(
         ShapeProperties.create(m, dotX - 7.0F, dotY - 7.0F, 14.0, 14.0).round(4.0F).color((closeHover ? C_CARD_HOVER : C_HEADER).getRGB()).build()
      );
      Fonts.getSize(15, Fonts.Type.BOLD).drawCenteredString(m, "x", dotX, dotY - 4.0F, C_SUBTEXT.getRGB());
   }

   private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack m = context.getMatrices();
      float pad = 10.0F;
      float x = this.sidebarX + pad;
      float w = this.sidebarW - pad * 2.0F;
      float cardY = this.contentY + pad;
      float cardH = 40.0F;
      rectangle.render(
         ShapeProperties.create(m, x, cardY, w, cardH).round(7.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build()
      );
      String user = this.safeUsername();
      float avSize = 26.0F;
      float avX = x + 7.0F;
      float avY = cardY + (cardH - avSize) / 2.0F;
      rectangle.render(ShapeProperties.create(m, avX, avY, avSize, avSize).round(13.0F).color(C_ACCENT.getRGB()).build());
      Fonts.getSize(18, Fonts.Type.BOLD)
         .drawCenteredString(
            m, user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), avX + avSize / 2.0F, avY + avSize / 2.0F - 5.0F, Color.WHITE.getRGB()
         );
      Fonts.getSize(14, Fonts.Type.SEMI)
         .drawString(m, this.trim(user, Fonts.getSize(14, Fonts.Type.SEMI), w - avSize - 18.0F), avX + avSize + 6.0F, cardY + 11.0F, C_TEXT.getRGB());
      Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "Локальный профиль", avX + avSize + 6.0F, cardY + 24.0F, C_SUBTEXT.getRGB());
      float navY = cardY + cardH + 8.0F;
      float itemH = 24.0F;
      float gap = 3.0F;

      for (int i = 0; i < NAV.length; i++) {
         float iy = navY + i * (itemH + gap);
         boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= iy && mouseY <= iy + itemH;
         this.navHover[i] = this.navHover[i] + ((!hover && i != this.selected ? 0.0F : 1.0F) - this.navHover[i]) * 0.25F;
         float h = this.navHover[i];
         if (i == this.selected) {
            rectangle.render(ShapeProperties.create(m, x, iy, w, itemH).round(6.0F).color(C_NAV_SEL.getRGB()).build());
            rectangle.render(ShapeProperties.create(m, x + 2.0F, iy + 5.0F, 3.0, itemH - 10.0F).round(1.5F).color(C_ACCENT.getRGB()).build());
         } else if (h > 0.01F) {
            int a = (int)(28.0F * h);
            rectangle.render(ShapeProperties.create(m, x, iy, w, itemH).round(6.0F).color(new Color(255, 255, 255, a).getRGB()).build());
         }

         Color icol = i == this.selected ? C_ACCENT : new Color(110, 110, 122, 255);
         rectangle.render(
            ShapeProperties.create(m, x + 11.0F, iy + itemH / 2.0F - 5.0F, 10.0, 10.0)
               .round(3.0F)
               .thickness(1.5F)
               .outlineColor(icol.getRGB())
               .color(new Color(0, 0, 0, 0).getRGB())
               .build()
         );
         int tcol = i == this.selected ? C_TEXT.getRGB() : C_SUBTEXT.getRGB();
         Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, NAV[i].title(), x + 28.0F, iy + itemH / 2.0F - 1.0F, tcol);
      }
   }

   private void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack m = context.getMatrices();
      float pad = 16.0F;
      float cx = this.contentX + pad;
      float cw = this.contentW - pad * 2.0F;
      int sx1 = (int)this.contentX;
      int sy1 = (int)this.contentY;
      int sx2 = (int)(this.contentX + this.contentW);
      int sy2 = (int)(this.contentY + this.contentH);
      float k = this.lastScale / this.lastGuiScale;
      context.enableScissor(Math.round(sx1 * k), Math.round(sy1 * k), Math.round(sx2 * k), Math.round(sy2 * k));
      float y = this.contentY + 14.0F - this.scroll;
      boolean searching = !this.search.isBlank();
      String id = NAV[this.selected].id();
      Fonts.getSize(26, Fonts.Type.BOLD).drawString(m, searching ? "Результаты поиска" : NAV[this.selected].title(), cx, y, C_TEXT.getRGB());
      float var21;
      float startY = var21 = y + 30.0F;
      if (searching) {
         y = this.renderDisplay(context, cx, cw, var21, mouseX, mouseY);
         y = this.renderGraphics(context, cx, cw, y, mouseX, mouseY);
         y = this.renderSound(context, cx, cw, y, mouseX, mouseY);
         y = this.renderControls(context, cx, cw, y, mouseX, mouseY);
         y = this.renderChat(context, cx, cw, y, mouseX, mouseY);
         y = this.renderAppearance(context, cx, cw, y, mouseX, mouseY);
         var21 = this.renderAccessibility(context, cx, cw, y, mouseX, mouseY);
         if (this.sliders.isEmpty() && this.toggles.isEmpty() && this.cycles.isEmpty()) {
            Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, "Ничего не найдено по запросу \"" + this.search + "\"", cx, var21, C_SUBTEXT.getRGB());
            var21 += 20.0F;
         }
      } else {
         switch (id) {
            case "home":
               var21 = this.renderHome(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "display":
               var21 = this.renderDisplay(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "graphics":
               var21 = this.renderGraphics(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "sound":
               var21 = this.renderSound(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "controls":
               var21 = this.renderControls(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "keys":
               var21 = this.renderKeys(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "chat":
               var21 = this.renderChat(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "appearance":
               var21 = this.renderAppearance(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "language":
               var21 = this.renderLanguage(context, cx, cw, var21, mouseX, mouseY);
               break;
            case "accessibility":
               var21 = this.renderAccessibility(context, cx, cw, var21, mouseX, mouseY);
         }
      }

      context.disableScissor();
      float used;
      this.contentVirtualHeight = used = var21 + this.scroll - startY + 40.0F;
      float maxScroll = Math.max(0.0F, used - (this.contentH - (startY - this.contentY)));
      this.scrollTarget = clamp(this.scrollTarget, 0.0F, maxScroll);
   }

   private float renderHome(DrawContext context, float cx, float cw, float y, int mouseX, int mouseY) {
      MatrixStack m = context.getMatrices();
      String user = this.safeUsername();
      float cardH = 78.0F;
      rectangle.render(ShapeProperties.create(m, cx, y, cw, cardH).round(9.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
      float avSize = 54.0F;
      float avX = cx + 12.0F;
      float avY = y + (cardH - avSize) / 2.0F;
      rectangle.render(ShapeProperties.create(m, avX, avY, avSize, avSize).round(10.0F).color(C_ACCENT.getRGB()).build());
      Fonts.getSize(34, Fonts.Type.BOLD)
         .drawCenteredString(
            m, user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), avX + avSize / 2.0F, avY + avSize / 2.0F - 9.0F, Color.WHITE.getRGB()
         );
      Fonts.getSize(20, Fonts.Type.BOLD).drawString(m, user, avX + avSize + 14.0F, y + 16.0F, C_TEXT.getRGB());
      Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(m, "Elixir Client dev", avX + avSize + 14.0F, y + 34.0F, C_SUBTEXT.getRGB());
      Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(m, "Minecraft " + mc.getGameVersion(), avX + avSize + 14.0F, y + 48.0F, C_SUBTEXT.getRGB());
      float statX = cx + cw - 190.0F;
      this.drawStatus(m, statX, y + 14.0F, "Baritone", "Активен");
      this.drawStatus(m, statX + 100.0F, y + 14.0F, "Fabric", "API активен");
      this.drawStatus(m, statX, y + 44.0F, "Сессия", this.trim(user, Fonts.getSize(11, Fonts.Type.DEFAULT), 80.0F));
      this.drawStatus(m, statX + 100.0F, y + 44.0F, "Java", System.getProperty("java.version", "?"));
      float colGap = 14.0F;
      float colW = (cw - colGap) / 2.0F;
      float var28;
      Fonts.getSize(16, Fonts.Type.SEMI).drawString(m, "Рекомендуемые параметры", cx, var28 = y + (cardH + 18.0F), C_TEXT.getRGB());
      Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "Часто используемые разделы", cx, var28 + 14.0F, C_SUBTEXT.getRGB());
      Fonts.getSize(16, Fonts.Type.SEMI).drawString(m, "Информация", cx + colW + colGap, var28, C_TEXT.getRGB());
      Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "Состояние клиента", cx + colW + colGap, var28 + 14.0F, C_SUBTEXT.getRGB());
      float rowY = var28 + 24.0F;
      String[][] recs = new String[][]{
         {"display", "Дисплей", "Разрешение, FOV, GUI"},
         {"sound", "Звук", "Громкость и каналы"},
         {"controls", "Управление", "Чувствительность мыши"},
         {"graphics", "Графика", "Дальность, FPS, графика"},
         {"language", "Язык", "Язык интерфейса"}
      };
      float rh = 32.0F;
      float ry = rowY;

      for (String[] r : recs) {
         boolean hover = mouseX >= cx && mouseX <= cx + colW && mouseY >= ry && mouseY <= ry + rh;
         rectangle.render(
            ShapeProperties.create(m, cx, ry, colW, rh)
               .round(7.0F)
               .thickness(1.0F)
               .outlineColor(C_OUTLINE.getRGB())
               .color((hover ? C_CARD_HOVER : C_CARD).getRGB())
               .build()
         );
         rectangle.render(ShapeProperties.create(m, cx + 9.0F, ry + rh / 2.0F - 8.0F, 16.0, 16.0).round(4.0F).color(C_ACCENT_DIM.getRGB()).build());
         rectangle.render(ShapeProperties.create(m, cx + 14.0F, ry + rh / 2.0F - 3.0F, 6.0, 6.0).round(2.0F).color(C_ACCENT.getRGB()).build());
         Fonts.getSize(13, Fonts.Type.SEMI).drawString(m, r[1], cx + 32.0F, ry + 9.0F, C_TEXT.getRGB());
         Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(m, r[2], cx + 32.0F, ry + 21.0F, C_SUBTEXT.getRGB());
         String target = r[0];
         this.rows.add(new WindowsSettingsScreen.ClickRow(cx, ry, colW, rh, () -> this.navigateTo(target)));
         ry += rh + 6.0F;
      }

      float infoX = cx + colW + colGap;
      float infoH = ry - rowY - 6.0F;
      rectangle.render(
         ShapeProperties.create(m, infoX, rowY, colW, infoH).round(7.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build()
      );
      Fonts.getSize(15, Fonts.Type.SEMI).drawString(m, "Elixir Client", infoX + 12.0F, rowY + 12.0F, C_TEXT.getRGB());
      String[] info = new String[]{
         "Версия: dev", "Minecraft: " + mc.getGameVersion(), "Java: " + System.getProperty("java.version", "?"), "ОС: " + System.getProperty("os.name", "?")
      };
      float iy = rowY + 28.0F;

      for (String s : info) {
         Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, s, infoX + 12.0F, iy, C_SUBTEXT.getRGB());
         iy += 14.0F;
      }

      return Math.max(ry, rowY + infoH) + 10.0F;
   }

   private void drawStatus(MatrixStack m, float x, float y, String title, String value) {
      Fonts.getSize(12, Fonts.Type.SEMI).drawString(m, title, x, y, C_TEXT.getRGB());
      Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(m, value, x, y + 12.0F, C_SUBTEXT.getRGB());
   }

   private float renderDisplay(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Поле зрения (FOV)",
         30.0,
         110.0,
         true,
         "°",
         () -> ((Integer)this.opt().getFov().getValue()).intValue(),
         v -> this.opt().getFov().setValue((int)v.doubleValue())
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Масштаб интерфейса",
         0.0,
         4.0,
         true,
         "",
         () -> ((Integer)this.opt().getGuiScale().getValue()).intValue(),
         v -> this.opt().getGuiScale().setValue((int)v.doubleValue())
      );
      y = this.rowSlider(
         ctx, cx, cw, y, mx, my, "Яркость", 0.0, 1.0, false, "%", () -> (Double)this.opt().getGamma().getValue(), v -> this.opt().getGamma().setValue(v)
      );
      y = this.rowToggle(
         ctx, cx, cw, y, mx, my, "disp_bob", "Покачивание камеры", () -> (Boolean)this.opt().getBobView().getValue(), v -> this.opt().getBobView().setValue(v)
      );
      return this.rowToggle(ctx, cx, cw, y, mx, my, "disp_full", "Полноэкранный режим", () -> window.isFullscreen(), v -> window.toggleFullscreen());
   }

   private float renderGraphics(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Дальность прорисовки",
         2.0,
         32.0,
         true,
         " ч.",
         () -> ((Integer)this.opt().getViewDistance().getValue()).intValue(),
         v -> this.opt().getViewDistance().setValue((int)v.doubleValue())
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Лимит кадров",
         10.0,
         260.0,
         true,
         " FPS",
         () -> ((Integer)this.opt().getMaxFps().getValue()).intValue(),
         v -> this.opt().getMaxFps().setValue((int)v.doubleValue())
      );
      y = this.rowCycle(ctx, cx, cw, y, mx, my, "Графика", () -> this.graphicsName((GraphicsMode)this.opt().getGraphicsMode().getValue()), () -> {
         GraphicsMode[] vals = GraphicsMode.values();
         GraphicsMode cur = (GraphicsMode)this.opt().getGraphicsMode().getValue();
         this.opt().getGraphicsMode().setValue(vals[(cur.ordinal() + 1) % vals.length]);
      });
      y = this.rowCycle(ctx, cx, cw, y, mx, my, "Облака", () -> this.cloudName((CloudRenderMode)this.opt().getCloudRenderMode().getValue()), () -> {
         CloudRenderMode[] vals = CloudRenderMode.values();
         CloudRenderMode cur = (CloudRenderMode)this.opt().getCloudRenderMode().getValue();
         this.opt().getCloudRenderMode().setValue(vals[(cur.ordinal() + 1) % vals.length]);
      });
      y = this.rowToggle(ctx, cx, cw, y, mx, my, "gfx_vsync", "Вертикальная синхронизация", () -> (Boolean)this.opt().getEnableVsync().getValue(), v -> {
         this.opt().getEnableVsync().setValue(v);
         window.setVsync(v);
      });
      return this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "gfx_shadows",
         "Тени сущностей",
         () -> (Boolean)this.opt().getEntityShadows().getValue(),
         v -> this.opt().getEntityShadows().setValue(v)
      );
   }

   private float renderSound(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      for (Object[] c : new Object[][]{
         {SoundCategory.MASTER, "Общая громкость"},
         {SoundCategory.MUSIC, "Музыка"},
         {SoundCategory.RECORDS, "Проигрыватель"},
         {SoundCategory.BLOCKS, "Блоки"},
         {SoundCategory.HOSTILE, "Враждебные существа"},
         {SoundCategory.NEUTRAL, "Дружелюбные существа"},
         {SoundCategory.PLAYERS, "Игроки"},
         {SoundCategory.AMBIENT, "Окружение"},
         {SoundCategory.VOICE, "Голос/речь"}
      }) {
         SoundCategory cat = (SoundCategory)c[0];
         String label = (String)c[1];
         y = this.rowSlider(
            ctx,
            cx,
            cw,
            y,
            mx,
            my,
            label,
            0.0,
            1.0,
            false,
            "%",
            () -> (Double)this.opt().getSoundVolumeOption(cat).getValue(),
            v -> this.opt().getSoundVolumeOption(cat).setValue(v)
         );
      }

      return y;
   }

   private float renderControls(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Чувствительность мыши",
         0.0,
         1.0,
         false,
         "%2x",
         () -> (Double)this.opt().getMouseSensitivity().getValue(),
         v -> this.opt().getMouseSensitivity().setValue(v)
      );
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "ctl_raw",
         "Необработанный ввод мыши",
         () -> (Boolean)this.opt().getRawMouseInput().getValue(),
         v -> this.opt().getRawMouseInput().setValue(v)
      );
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "ctl_inv",
         "Инверсия мыши",
         () -> (Boolean)this.opt().getInvertYMouse().getValue(),
         v -> this.opt().getInvertYMouse().setValue(v)
      );
      y = this.rowToggle(
         ctx, cx, cw, y, mx, my, "ctl_jump", "Автопрыжок", () -> (Boolean)this.opt().getAutoJump().getValue(), v -> this.opt().getAutoJump().setValue(v)
      );
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "ctl_sneak",
         "Переключаемое приседание",
         () -> (Boolean)this.opt().getSneakToggled().getValue(),
         v -> this.opt().getSneakToggled().setValue(v)
      );
      return this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "ctl_sprint",
         "Переключаемый бег",
         () -> (Boolean)this.opt().getSprintToggled().getValue(),
         v -> this.opt().getSprintToggled().setValue(v)
      );
   }

   private float renderChat(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Прозрачность чата",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getChatOpacity().getValue(),
         v -> this.opt().getChatOpacity().setValue(v)
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Масштаб чата",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getChatScale().getValue(),
         v -> this.opt().getChatScale().setValue(v)
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Прозрачность фона текста",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getTextBackgroundOpacity().getValue(),
         v -> this.opt().getTextBackgroundOpacity().setValue(v)
      );
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "chat_subs",
         "Показывать субтитры",
         () -> (Boolean)this.opt().getShowSubtitles().getValue(),
         v -> this.opt().getShowSubtitles().setValue(v)
      );
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "chat_colors",
         "Цвета в чате",
         () -> (Boolean)this.opt().getChatColors().getValue(),
         v -> this.opt().getChatColors().setValue(v)
      );
      return this.rowToggle(
         ctx, cx, cw, y, mx, my, "chat_links", "Ссылки в чате", () -> (Boolean)this.opt().getChatLinks().getValue(), v -> this.opt().getChatLinks().setValue(v)
      );
   }

   private float renderAppearance(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      for (PlayerModelPart part : PlayerModelPart.values()) {
         String label = part.getOptionName().getString();
         y = this.rowToggle(
            ctx, cx, cw, y, mx, my, "app_" + part.name(), label, () -> this.opt().isPlayerModelPartEnabled(part), v -> this.opt().setPlayerModelPart(part, v)
         );
      }

      return y;
   }

   private float renderAccessibility(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      y = this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "acc_contrast",
         "Высокая контрастность",
         () -> (Boolean)this.opt().getHighContrast().getValue(),
         v -> this.opt().getHighContrast().setValue(v)
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Сила тряски от урона",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getDamageTiltStrength().getValue(),
         v -> this.opt().getDamageTiltStrength().setValue(v)
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Искажение эффектов",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getDistortionEffectScale().getValue(),
         v -> this.opt().getDistortionEffectScale().setValue(v)
      );
      y = this.rowSlider(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "Эффект FOV",
         0.0,
         1.0,
         false,
         "%",
         () -> (Double)this.opt().getFovEffectScale().getValue(),
         v -> this.opt().getFovEffectScale().setValue(v)
      );
      return this.rowToggle(
         ctx,
         cx,
         cw,
         y,
         mx,
         my,
         "acc_mono",
         "Монохромный логотип",
         () -> (Boolean)this.opt().getMonochromeLogo().getValue(),
         v -> this.opt().getMonochromeLogo().setValue(v)
      );
   }

   private float renderLanguage(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      MatrixStack m = ctx.getMatrices();
      LanguageManager lm = mc.getLanguageManager();
      String current = lm.getLanguage();
      SortedMap<String, LanguageDefinition> all = lm.getAllLanguages();
      float rh = 26.0F;

      for (Entry<String, LanguageDefinition> e : all.entrySet()) {
         String code = e.getKey();
         LanguageDefinition def = e.getValue();
         boolean sel = code.equals(current);
         boolean hover = mx >= cx && mx <= cx + cw && my >= y && my <= y + rh;
         rectangle.render(
            ShapeProperties.create(m, cx, y, cw, rh)
               .round(6.0F)
               .thickness(1.0F)
               .outlineColor((sel ? C_ACCENT : C_OUTLINE).getRGB())
               .color((sel ? C_NAV_SEL : (hover ? C_CARD_HOVER : C_CARD)).getRGB())
               .build()
         );
         Fonts.getSize(13, Fonts.Type.SEMI).drawString(m, def.getDisplayText().getString(), cx + 12.0F, y + rh / 2.0F - 1.0F, C_TEXT.getRGB());
         Fonts.getSize(10, Fonts.Type.DEFAULT)
            .drawString(m, code, cx + cw - Fonts.getSize(10, Fonts.Type.DEFAULT).getStringWidth(code) - 12.0F, y + rh / 2.0F, C_SUBTEXT.getRGB());
         this.rows.add(new WindowsSettingsScreen.ClickRow(cx, y, cw, rh, () -> this.applyLanguage(code)));
         y += rh + 5.0F;
      }

      return y;
   }

   private float renderKeys(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
      MatrixStack m = ctx.getMatrices();
      Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "Нажмите на клавишу справа и задайте новую. ESC — сбросить.", cx, y, C_SUBTEXT.getRGB());
      y += 18.0F;
      float rh = 26.0F;

      for (KeyBinding kb : this.opt().allKeys) {
         boolean listening = this.listeningBind == kb;
         rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(6.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
         Fonts.getSize(12, Fonts.Type.DEFAULT)
            .drawString(m, Text.translatable(kb.getTranslationKey()).getString(), cx + 12.0F, y + rh / 2.0F - 1.0F, C_TEXT.getRGB());
         float bw = 86.0F;
         float bh = 17.0F;
         float bx = cx + cw - bw - 10.0F;
         float by = y + (rh - bh) / 2.0F;
         boolean hover = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
         int bc = listening ? C_ACCENT.getRGB() : (kb.isDefault() ? C_TRACK.getRGB() : new Color(80, 70, 110, 255).getRGB());
         rectangle.render(
            ShapeProperties.create(m, bx, by, bw, bh).round(5.0F).color((hover && !listening ? C_CARD_HOVER : new Color(bc, true)).getRGB()).build()
         );
         String txt = listening ? "> ... <" : kb.getBoundKeyLocalizedText().getString();
         Fonts.getSize(11, Fonts.Type.SEMI).drawCenteredString(m, txt, bx + bw / 2.0F, by + bh / 2.0F - 2.5F, C_TEXT.getRGB());
         this.rows.add(new WindowsSettingsScreen.ClickRow(bx, by, bw, bh, () -> this.listeningBind = this.listeningBind == kb ? null : kb));
         y += rh + 5.0F;
      }

      return y;
   }

   private float rowSlider(
      DrawContext ctx,
      float cx,
      float cw,
      float y,
      int mx,
      int my,
      String label,
      double min,
      double max,
      boolean integer,
      String unit,
      DoubleSupplier get,
      Consumer<Double> set
   ) {
      if (!this.matchesSearch(label)) {
         return y;
      } else {
         MatrixStack m = ctx.getMatrices();
         float rh = 34.0F;
         rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(7.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
         Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0F, y + rh / 2.0F - 1.0F, C_TEXT.getRGB());
         double value = clampD(get.getAsDouble(), min, max);
         float trackW = Math.min(150.0F, cw * 0.38F);
         float trackX = cx + cw - trackW - 16.0F;
         float trackY = y + rh / 2.0F;
         float t = (float)((value - min) / (max - min));
         rectangle.render(ShapeProperties.create(m, trackX, trackY - 1.5F, trackW, 3.0).round(1.5F).color(C_TRACK.getRGB()).build());
         rectangle.render(ShapeProperties.create(m, trackX, trackY - 1.5F, trackW * t, 3.0).round(1.5F).color(C_ACCENT.getRGB()).build());
         float knobX = trackX + trackW * t;
         rectangle.render(ShapeProperties.create(m, knobX - 4.5F, trackY - 4.5F, 9.0, 9.0).round(4.5F).color(Color.WHITE.getRGB()).build());
         Object valueText;
         if (integer) {
            int iv = (int)Math.round(value);
            valueText = " FPS".equals(unit) && iv >= 260 ? "Без лимита" : ("".equals(unit) && label.startsWith("Масштаб") && iv == 0 ? "Авто" : iv + unit);
         } else {
            double mul = "%2x".equals(unit) ? 200.0 : 100.0;
            valueText = Math.round(value * mul) + "%";
         }

         FontRenderer vf = Fonts.getSize(11, Fonts.Type.SEMI);
         vf.drawString(m, (String)valueText, trackX - vf.getStringWidth((String)valueText) - 8.0F, trackY - 1.0F, C_SUBTEXT.getRGB());
         this.sliders.add(new WindowsSettingsScreen.Slider(trackX, y, trackW, rh, min, max, integer, set));
         return y + rh + 6.0F;
      }
   }

   private float rowToggle(DrawContext ctx, float cx, float cw, float y, int mx, int my, String id, String label, BooleanSupplier get, Consumer<Boolean> set) {
      if (!this.matchesSearch(label)) {
         return y;
      } else {
         MatrixStack m = ctx.getMatrices();
         float rh = 30.0F;
         boolean hover = mx >= cx && mx <= cx + cw && my >= y && my <= y + rh;
         rectangle.render(
            ShapeProperties.create(m, cx, y, cw, rh)
               .round(7.0F)
               .thickness(1.0F)
               .outlineColor(C_OUTLINE.getRGB())
               .color((hover ? C_CARD_HOVER : C_CARD).getRGB())
               .build()
         );
         Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0F, y + rh / 2.0F - 1.0F, C_TEXT.getRGB());
         boolean on = get.getAsBoolean();
         float pillW = 30.0F;
         float pillH = 15.0F;
         float pillX = cx + cw - pillW - 14.0F;
         float pillY = y + (rh - pillH) / 2.0F;
         float a = this.toggleAnim.getOrDefault(id, on ? 1.0F : 0.0F);
         a += ((on ? 1.0F : 0.0F) - a) * 0.3F;
         this.toggleAnim.put(id, a);
         Color track = lerpColor(C_TRACK, C_ACCENT, a);
         rectangle.render(ShapeProperties.create(m, pillX, pillY, pillW, pillH).round(pillH / 2.0F).color(track.getRGB()).build());
         float knobD = pillH - 4.0F;
         float knobX = pillX + 2.0F + a * (pillW - knobD - 4.0F);
         rectangle.render(ShapeProperties.create(m, knobX, pillY + 2.0F, knobD, knobD).round(knobD / 2.0F).color(Color.WHITE.getRGB()).build());
         this.toggles.add(new WindowsSettingsScreen.Toggle(cx, y, cw, rh, get, set));
         return y + rh + 6.0F;
      }
   }

   private float rowCycle(DrawContext ctx, float cx, float cw, float y, int mx, int my, String label, Supplier<String> valueText, Runnable next) {
      if (!this.matchesSearch(label)) {
         return y;
      } else {
         MatrixStack m = ctx.getMatrices();
         float rh = 30.0F;
         rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(7.0F).thickness(1.0F).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
         Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0F, y + rh / 2.0F - 1.0F, C_TEXT.getRGB());
         String val = valueText.get();
         FontRenderer bf = Fonts.getSize(11, Fonts.Type.SEMI);
         float bw = Math.max(70.0F, bf.getStringWidth(val) + 22.0F);
         float bh = 17.0F;
         float bx = cx + cw - bw - 14.0F;
         float by = y + (rh - bh) / 2.0F;
         boolean hover = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
         rectangle.render(
            ShapeProperties.create(m, bx, by, bw, bh)
               .round(5.0F)
               .thickness(1.0F)
               .outlineColor(C_OUTLINE.getRGB())
               .color((hover ? C_CARD_HOVER : C_TRACK).getRGB())
               .build()
         );
         bf.drawCenteredString(m, val, bx + bw / 2.0F, by + bh / 2.0F - 2.5F, C_TEXT.getRGB());
         this.cycles.add(new WindowsSettingsScreen.Cycle(bx, by, bw, bh, next));
         return y + rh + 6.0F;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double vx = mouseX * this.lastGuiScale / this.lastScale;
      double vy = mouseY * this.lastGuiScale / this.lastScale;
      if (button == 0) {
         if (inside(vx, vy, this.zMinusX, this.zBtnY, this.zBtnSize, this.zBtnSize)) {
            this.changeScale(-0.3F);
            return true;
         }

         if (inside(vx, vy, this.zPlusX, this.zBtnY, this.zBtnSize, this.zBtnSize)) {
            this.changeScale(0.3F);
            return true;
         }

         if (inside(vx, vy, this.searchX, this.searchY, this.searchW, this.searchH)) {
            this.searchFocused = true;
            return true;
         }

         this.searchFocused = false;
         float dotX = this.winX + this.winW - 18.0F;
         float dotY = this.winY + this.headerH / 2.0F;
         if (this.dist((int)vx, (int)vy, dotX, dotY) < 9.0F) {
            this.close();
            return true;
         }

         float pad = 10.0F;
         float x = this.sidebarX + pad;
         float w = this.sidebarW - pad * 2.0F;
         float navY = this.contentY + pad + 40.0F + 8.0F;
         float itemH = 24.0F;
         float gap = 3.0F;

         for (int i = 0; i < NAV.length; i++) {
            float iy = navY + i * (itemH + gap);
            if (vx >= x && vx <= x + w && vy >= iy && vy <= iy + itemH) {
               this.selected = i;
               this.search = "";
               this.searchFocused = false;
               this.scroll = 0.0F;
               this.scrollTarget = 0.0F;
               return true;
            }
         }

         for (WindowsSettingsScreen.ClickRow r : this.rows) {
            if (r.contains(vx, vy)) {
               r.action.run();
               return true;
            }
         }

         for (WindowsSettingsScreen.Toggle tg : this.toggles) {
            if (tg.contains(vx, vy)) {
               tg.set.accept(!tg.get.getAsBoolean());
               this.saveOptions();
               return true;
            }
         }

         for (WindowsSettingsScreen.Cycle c : this.cycles) {
            if (c.contains(vx, vy)) {
               c.next.run();
               this.saveOptions();
               return true;
            }
         }

         for (WindowsSettingsScreen.Slider s : this.sliders) {
            if (s.contains(vx, vy)) {
               this.draggingSlider = s;
               this.applySlider(s, vx);
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.draggingSlider != null) {
         this.applySlider(this.draggingSlider, mouseX * this.lastGuiScale / this.lastScale);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.draggingSlider != null) {
         this.draggingSlider = null;
         this.saveOptions();
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      double vx = mouseX * this.lastGuiScale / this.lastScale;
      if (vx >= this.contentX && vx <= this.contentX + this.contentW) {
         this.scrollTarget -= (float)verticalAmount * 24.0F;
      }

      return true;
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.searchFocused && chr >= ' ' && this.search.length() < 48) {
         this.search = this.search + chr;
         this.scroll = 0.0F;
         this.scrollTarget = 0.0F;
         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listeningBind != null) {
         if (keyCode == 256) {
            this.listeningBind.setBoundKey(InputUtil.UNKNOWN_KEY);
         } else {
            this.listeningBind.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
         }

         KeyBinding.updateKeysByCode();
         this.listeningBind = null;
         this.saveOptions();
         return true;
      } else if (this.searchFocused) {
         if (keyCode == 256) {
            if (!this.search.isEmpty()) {
               this.search = "";
            } else {
               this.searchFocused = false;
            }

            return true;
         } else if (keyCode == 259) {
            if (!this.search.isEmpty()) {
               this.search = this.search.substring(0, this.search.length() - 1);
            }

            return true;
         } else if (keyCode != 257 && keyCode != 335) {
            return true;
         } else {
            this.searchFocused = false;
            return true;
         }
      } else if (keyCode == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void changeScale(float delta) {
      uiScale = clamp(uiScale + delta, 1.5F, 4.8F);
   }

   private static boolean inside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private boolean matchesSearch(String label) {
      return this.search != null && !this.search.isBlank() ? label.toLowerCase().contains(this.search.toLowerCase()) : true;
   }

   private void applySlider(WindowsSettingsScreen.Slider s, double mouseX) {
      double t = clampD((mouseX - s.x) / s.w, 0.0, 1.0);
      double val = s.min + t * (s.max - s.min);
      if (s.integer) {
         val = Math.round(val);
      }

      s.set.accept(val);
   }

   private GameOptions opt() {
      return mc.options;
   }

   private void saveOptions() {
      try {
         this.opt().write();
      } catch (Exception var2) {
      }
   }

   private void navigateTo(String id) {
      for (int i = 0; i < NAV.length; i++) {
         if (NAV[i].id().equals(id)) {
            this.selected = i;
            this.scroll = 0.0F;
            this.scrollTarget = 0.0F;
            return;
         }
      }
   }

   private void applyLanguage(String code) {
      LanguageManager lm = mc.getLanguageManager();
      if (!code.equals(lm.getLanguage())) {
         lm.setLanguage(code);
         this.opt().language = code;
         mc.reloadResources();
         this.saveOptions();
      }
   }

   private String graphicsName(GraphicsMode mode) {
      return switch (mode) {
         case FAST -> "Быстрая";
         case FANCY -> "Детальная";
         case FABULOUS -> "Превосходная";
         default -> throw new MatchException(null, null);
      };
   }

   private String cloudName(CloudRenderMode mode) {
      return switch (mode) {
         case OFF -> "Выкл";
         case FAST -> "Быстрые";
         case FANCY -> "Детальные";
         default -> throw new MatchException(null, null);
      };
   }

   private String safeUsername() {
      try {
         String u = mc.getSession().getUsername();
         return u == null ? "Player" : u;
      } catch (Exception var2) {
         return "Player";
      }
   }

   private String trim(String s, FontRenderer font, float maxWidth) {
      if (s == null) {
         return "";
      } else if (font.getStringWidth(s) <= maxWidth) {
         return s;
      } else {
         StringBuilder sb = new StringBuilder();

         for (char c : s.toCharArray()) {
            if (font.getStringWidth(sb.toString() + c + "..") > maxWidth) {
               break;
            }

            sb.append(c);
         }

         return sb + "..";
      }
   }

   private float dist(int mx, int my, float x, float y) {
      return (float)Math.hypot(mx - x, my - y);
   }

   private static float clamp(float v, float min, float max) {
      return v < min ? min : Math.min(v, max);
   }

   private static double clampD(double v, double min, double max) {
      return v < min ? min : Math.min(v, max);
   }

   private static Color lerpColor(Color a, Color b, float t) {
      t = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
      return new Color(
         (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
         (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
         (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
         (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
      );
   }

   private static final class ClickRow {
      final float x;
      final float y;
      final float w;
      final float h;
      final Runnable action;

      ClickRow(float x, float y, float w, float h, Runnable action) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.action = action;
      }

      boolean contains(double mx, double my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }

   private static final class Cycle {
      final float x;
      final float y;
      final float w;
      final float h;
      final Runnable next;

      Cycle(float x, float y, float w, float h, Runnable next) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.next = next;
      }

      boolean contains(double mx, double my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }

   private record NavItem(String id, String title) {
   }

   private static final class Slider {
      final float x;
      final float y;
      final float w;
      final float h;
      final double min;
      final double max;
      final boolean integer;
      final Consumer<Double> set;

      Slider(float x, float y, float w, float h, double min, double max, boolean integer, Consumer<Double> set) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.min = min;
         this.max = max;
         this.integer = integer;
         this.set = set;
      }

      boolean contains(double mx, double my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }

   private static final class Toggle {
      final float x;
      final float y;
      final float w;
      final float h;
      final BooleanSupplier get;
      final Consumer<Boolean> set;

      Toggle(float x, float y, float w, float h, BooleanSupplier get, Consumer<Boolean> set) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.get = get;
         this.set = set;
      }

      boolean contains(double mx, double my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }
}
