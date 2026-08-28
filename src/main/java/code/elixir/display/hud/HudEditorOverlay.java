package code.elixir.display.hud;

import code.elixir.Elixir;
import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.managers.file.impl.ElementsFile;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.projection.Projection;
import code.elixir.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4d;

public class HudEditorOverlay implements QuickImports {
   private static final HudEditorOverlay INSTANCE = new HudEditorOverlay();
   private static final float MENU_HEADER_H = 18.0F;
   private static final float MENU_ROW_H = 16.0F;
   private static final float CAPSULE_H = 13.0F;
   private static final float SCALE_BUTTON_SIZE = 22.0F;
   private static final float SCALE_BUTTON_MARGIN = 6.0F;
   private static final float SCALE_BUTTONS_AREA_H = 28.0F;
   private static final long MENU_ANIM_MS = 240L;
   private boolean menuOpen;
   private boolean menuClosing;
   private long menuAnimStart;
   private float menuX;
   private float menuY;
   private float menuWidth;
   private float menuHeight;
   private HudEditorOverlay.MenuMode menuMode;
   private String menuTitle = "";
   private AbstractDraggable targetDraggable;
   private final List<HudEditorOverlay.MenuEntry> menuEntries = new ArrayList<>();

   public static HudEditorOverlay getInstance() {
      return INSTANCE;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState()) {
         for (AbstractDraggable draggable : this.getVisibleDraggables(hud)) {
            draggable.render(context, mouseX, mouseY, delta);
         }

         if (this.menuOpen) {
            float progress = this.getMenuAnimProgress();
            if (this.menuClosing && progress <= 0.01F) {
               this.closeMenu();
               return;
            }

            this.renderMenu(context.getMatrices(), mouseX, mouseY, progress);
         }
      } else {
         this.closeMenu();
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState()) {
         if (this.menuOpen && !this.menuClosing) {
            if (button == 0) {
               if (this.menuMode == HudEditorOverlay.MenuMode.ACTIONS && this.targetDraggable != null && this.handleScaleButtonClick(mouseX, mouseY)) {
                  return true;
               }

               if (this.isInsideMenu(mouseX, mouseY)) {
                  int index = this.getMenuIndex(mouseY);
                  if (index >= 0 && index < this.menuEntries.size()) {
                     this.menuEntries.get(index).action().run();
                     this.beginClose();
                     this.saveHudState();
                     return true;
                  }

                  return true;
               }
            }

            if (this.isInsideMenu(mouseX, mouseY)) {
               return true;
            }

            this.beginClose();
         }

         AbstractDraggable hovered = this.getHoveredDraggable(hud, mouseX, mouseY);
         if (button == 1) {
            if (hovered != null) {
               this.openActionsMenu((float)mouseX, (float)mouseY, hovered);
               return true;
            } else {
               PlayerEntity hoveredPlayer = this.getHoveredPlayer(mouseX, mouseY);
               if (hoveredPlayer != null) {
                  this.openFriendMenu((float)mouseX, (float)mouseY, hoveredPlayer);
                  return true;
               } else {
                  List<AbstractDraggable> disabled = this.getDisabledDraggables(hud);
                  if (!disabled.isEmpty()) {
                     this.openAddMenu((float)mouseX, (float)mouseY, disabled, hud);
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         } else {
            return button == 0 && hovered != null ? hovered.beginDrag(mouseX, mouseY) : false;
         }
      } else {
         this.closeMenu();
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      Hud hud = Hud.getInstance();
      if (hud == null) {
         return false;
      } else {
         boolean handled = false;

         for (AbstractDraggable draggable : Elixir.getInstance().getDraggableRepository().draggable()) {
            handled |= draggable.mouseReleased(mouseX, mouseY, button);
         }

         return handled;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      return false;
   }

   private void renderMenu(MatrixStack matrix, int mouseX, int mouseY, float progress) {
      float t = easeOutCubic(MathHelper.clamp(progress, 0.0F, 1.0F));
      float scale = 0.72F + 0.28F * t;
      float slideY = (1.0F - t) * 12.0F;
      float animY = this.menuY - slideY;
      float pivotX = this.menuX + this.menuWidth / 2.0F;
      float pivotY = animY + this.menuHeight / 2.0F;
      matrix.push();
      matrix.translate(pivotX, pivotY, 0.0F);
      matrix.scale(scale, scale, 1.0F);
      matrix.translate(-pivotX, -pivotY, 0.0F);
      this.drawMenu(matrix, mouseX, mouseY, t, animY, t);
      matrix.pop();
   }

   private void drawMenu(MatrixStack matrix, int mouseX, int mouseY, float alpha, float animY, float progress) {
      FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
      FontRenderer itemFont = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
      int baseBg = ThemeManager.BackgroundGui.getColor() | 0xFF000000;
      int fill = ColorAssist.multAlpha(baseBg, 0.97F * alpha);
      int border = ColorAssist.multAlpha(ThemeManager.textColor.getColor(), 0.16F * alpha);
      int accent = ColorAssist.getHudColor();
      int textCol = ThemeManager.textColor.getColor();
      rectangle.render(ShapeProperties.create(matrix, this.menuX, animY, this.menuWidth, this.menuHeight).round(10.0F).color(fill).build());
      rectangle.render(
         ShapeProperties.create(matrix, this.menuX, animY, this.menuWidth, this.menuHeight).round(10.0F).thickness(1.0F).outlineColor(border).color(0).build()
      );
      float currentY = animY;
      if (this.menuMode == HudEditorOverlay.MenuMode.ACTIONS && this.targetDraggable != null) {
         this.drawScaleButtons(matrix, mouseX, mouseY, alpha, animY, progress, accent, textCol, baseBg);
         currentY = animY + 28.0F;
      }

      float centerX = this.menuX + this.menuWidth / 2.0F;
      float titleAreaH = 12.0F;
      titleFont.drawCenteredString(
         matrix, this.menuTitle, centerX, centeredTextY(titleFont, this.menuTitle, currentY, titleAreaH), ColorAssist.multAlpha(textCol, alpha)
      );
      float lineW = Math.min(this.menuWidth - 18.0F, titleFont.getStringWidth(this.menuTitle) + 16.0F);
      rectangle.render(
         ShapeProperties.create(matrix, centerX - lineW / 2.0F, currentY + 18.0F - 5.0F, lineW, 1.4F)
            .round(1.0F)
            .color(ColorAssist.multAlpha(accent, 0.85F * alpha))
            .build()
      );
      currentY += 18.0F;

      for (HudEditorOverlay.MenuEntry entry : this.menuEntries) {
         float capX = this.menuX + 6.0F;
         float capW = this.menuWidth - 12.0F;
         float capY = currentY + 1.5F;
         boolean hovered = !this.menuClosing && progress > 0.85F && mouseX >= capX && mouseX <= capX + capW && mouseY >= capY && mouseY <= capY + 13.0F;
         int capFill = hovered ? ColorAssist.multAlpha(accent, 0.22F * alpha) : ColorAssist.multAlpha(blend(baseBg, -1, 0.07F), 0.9F * alpha);
         rectangle.render(ShapeProperties.create(matrix, capX, capY, capW, 13.0).round(6.5F).color(capFill).build());
         rectangle.render(
            ShapeProperties.create(matrix, capX, capY, capW, 13.0)
               .round(6.5F)
               .thickness(0.8F)
               .outlineColor(ColorAssist.multAlpha(hovered ? accent : textCol, (hovered ? 0.45F : 0.1F) * alpha))
               .color(0)
               .build()
         );
         int labelColor = ColorAssist.multAlpha(textCol, (hovered ? 1.0F : 0.86F) * alpha);
         itemFont.drawCenteredString(matrix, entry.label(), centerX, centeredTextY(itemFont, entry.label(), capY, 13.0F), labelColor);
         currentY += 16.0F;
      }
   }

   private void drawScaleButtons(MatrixStack matrix, int mouseX, int mouseY, float alpha, float currentY, float progress, int accent, int textCol, int baseBg) {
      FontRenderer buttonFont = Fonts.getSize(20, Fonts.Type.SuisseIntlSemiBold);
      float centerX = this.menuX + this.menuWidth / 2.0F;
      float buttonY = currentY + 3.0F;
      float leftButtonX = centerX - 22.0F - 3.0F;
      boolean leftHovered = !this.menuClosing
         && progress > 0.85F
         && mouseX >= leftButtonX
         && mouseX <= leftButtonX + 22.0F
         && mouseY >= buttonY
         && mouseY <= buttonY + 22.0F;
      this.drawScaleButton(matrix, leftButtonX, buttonY, "-", leftHovered, alpha, accent, textCol, baseBg, buttonFont);
      float rightButtonX = centerX + 3.0F;
      boolean rightHovered = !this.menuClosing
         && progress > 0.85F
         && mouseX >= rightButtonX
         && mouseX <= rightButtonX + 22.0F
         && mouseY >= buttonY
         && mouseY <= buttonY + 22.0F;
      this.drawScaleButton(matrix, rightButtonX, buttonY, "+", rightHovered, alpha, accent, textCol, baseBg, buttonFont);
   }

   private void drawScaleButton(
      MatrixStack matrix, float x, float y, String symbol, boolean hovered, float alpha, int accent, int textCol, int baseBg, FontRenderer font
   ) {
      int buttonFill = hovered ? ColorAssist.multAlpha(accent, 0.25F * alpha) : ColorAssist.multAlpha(blend(baseBg, -1, 0.1F), 0.9F * alpha);
      rectangle.render(ShapeProperties.create(matrix, x, y, 22.0, 22.0).round(11.0F).color(buttonFill).build());
      rectangle.render(
         ShapeProperties.create(matrix, x, y, 22.0, 22.0)
            .round(11.0F)
            .thickness(1.0F)
            .outlineColor(ColorAssist.multAlpha(hovered ? accent : textCol, (hovered ? 0.55F : 0.18F) * alpha))
            .color(0)
            .build()
      );
      int symbolColor = ColorAssist.multAlpha(textCol, (hovered ? 1.0F : 0.8F) * alpha);
      font.drawCenteredBoth(matrix, symbol, x + 11.0F, y, 22.0F, symbolColor);
   }

   private boolean handleScaleButtonClick(double mouseX, double mouseY) {
      float centerX = this.menuX + this.menuWidth / 2.0F;
      float buttonY = this.menuY + 3.0F;
      float leftButtonX = centerX - 22.0F - 3.0F;
      if (mouseX >= leftButtonX && mouseX <= leftButtonX + 22.0F && mouseY >= buttonY && mouseY <= buttonY + 22.0F) {
         this.targetDraggable.decreaseScale();
         this.saveHudState();
         return true;
      } else {
         float rightButtonX = centerX + 3.0F;
         if (mouseX >= rightButtonX && mouseX <= rightButtonX + 22.0F && mouseY >= buttonY && mouseY <= buttonY + 22.0F) {
            this.targetDraggable.increaseScale();
            this.saveHudState();
            return true;
         } else {
            return false;
         }
      }
   }

   private float getMenuAnimProgress() {
      float raw = (float)(System.currentTimeMillis() - this.menuAnimStart) / 240.0F;
      return this.menuClosing ? 1.0F - MathHelper.clamp(raw, 0.0F, 1.0F) : MathHelper.clamp(raw, 0.0F, 1.0F);
   }

   private static float centeredTextY(FontRenderer font, String text, float boxY, float boxHeight) {
      return font.getCenteredY(boxY, boxHeight, text);
   }

   private void openAddMenu(float x, float y, List<AbstractDraggable> disabled, Hud hud) {
      this.menuEntries.clear();
      this.targetDraggable = null;
      this.menuMode = HudEditorOverlay.MenuMode.ADD;
      this.menuTitle = "Добавить элемент";

      for (AbstractDraggable draggable : disabled) {
         String name = draggable.getName();
         this.menuEntries.add(new HudEditorOverlay.MenuEntry(this.russianName(name), () -> hud.setHudElementEnabled(name, true)));
      }

      this.openMenu(x, y);
   }

   private PlayerEntity getHoveredPlayer(double mouseX, double mouseY) {
      if (mc.world != null && mc.player != null) {
         PlayerEntity best = null;
         double bestDistance = Double.MAX_VALUE;

         for (PlayerEntity player : mc.world.getPlayers()) {
            double distance;
            Vector4d vec;
            if (player != mc.player
               && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
               && Projection.isWithinRenderDistance(player)
               && Projection.canSee(player.getBoundingBox())
               && !Projection.cantSee(vec = Projection.getVector4D(player))
               && !(mouseX < vec.x)
               && !(mouseX > vec.z)
               && !(mouseY < vec.y)
               && !(mouseY > vec.w)
               && (distance = mc.player.distanceTo(player)) < bestDistance) {
               bestDistance = distance;
               best = player;
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void openFriendMenu(float x, float y, PlayerEntity player) {
      this.menuEntries.clear();
      this.targetDraggable = null;
      this.menuMode = HudEditorOverlay.MenuMode.ADD;
      String name;
      this.menuTitle = name = player.getGameProfile().getName();
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(FriendUtils.isFriend(name) ? "Убрать из друзей" : "Добавить в друзья", () -> {
         if (FriendUtils.isFriend(name)) {
            FriendUtils.removeFriend(name);
         } else {
            FriendUtils.addFriend(name);
         }
      }));
      this.openMenu(x, y);
   }

   private void openActionsMenu(float x, float y, AbstractDraggable draggable) {
      this.menuEntries.clear();
      this.targetDraggable = draggable;
      this.menuMode = HudEditorOverlay.MenuMode.ACTIONS;
      this.menuTitle = this.russianName(draggable.getName());
      Hud hud = Hud.getInstance();
      if (hud != null) {
         this.menuEntries.add(new HudEditorOverlay.MenuEntry("Удалить", () -> hud.setHudElementEnabled(draggable.getName(), false)));
      }

      this.menuEntries.add(new HudEditorOverlay.MenuEntry("Сбросить масштаб", () -> draggable.setScaleFactor(1.0F)));
      this.menuEntries.add(new HudEditorOverlay.MenuEntry("Сбросить позицию", () -> {
         AbstractDraggable fresh = this.createFresh(draggable);
         if (fresh != null) {
            draggable.setX(fresh.getX());
            draggable.setY(fresh.getY());
            draggable.clampToScreen();
         }
      }));
      if (draggable instanceof Info info) {
         this.addInfoMenuEntries(info);
      }

      if (draggable instanceof TargetHud targetHud) {
         this.addTargetHudMenuEntries(targetHud);
      }

      this.openMenu(x, y);
   }

   private void addTargetHudMenuEntries(TargetHud targetHud) {
      this.menuEntries.add(new HudEditorOverlay.MenuEntry((targetHud.isShowOnHover() ? "✓ " : "") + "Отображать при наведении", () -> {
         targetHud.setShowOnHover(!targetHud.isShowOnHover());

         try {
            Elixir.getInstance().getFileController().saveFile(ElementsFile.class);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }));
   }

   private void addInfoMenuEntries(Info info) {
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(this.toggleLabel("TPS", info.isShowTps()), () -> info.setShowTps(!info.isShowTps())));
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(this.toggleLabel("FPS", info.isShowFps()), () -> info.setShowFps(!info.isShowFps())));
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(this.toggleLabel("BPS", info.isShowBps()), () -> info.setShowBps(!info.isShowBps())));
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(this.toggleLabel("Корды", info.isShowCoords()), () -> info.setShowCoords(!info.isShowCoords())));
      this.menuEntries.add(new HudEditorOverlay.MenuEntry(info.isRightSide() ? "Слева" : "Справа", () -> info.setRightSide(!info.isRightSide())));
   }

   private String toggleLabel(String name, boolean enabled) {
      return enabled ? "Убрать " + name : "Добавить " + name;
   }

   private void openMenu(float x, float y) {
      FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
      FontRenderer itemFont = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
      float contentW = titleFont.getStringWidth(this.menuTitle);

      for (HudEditorOverlay.MenuEntry entry : this.menuEntries) {
         contentW = Math.max(contentW, itemFont.getStringWidth(entry.label()));
      }

      this.menuWidth = MathHelper.clamp(contentW + 20.0F, 84.0F, 220.0F);
      float totalHeight = 6.0F;
      if (this.menuMode == HudEditorOverlay.MenuMode.ACTIONS) {
         totalHeight += 28.0F;
      }

      totalHeight += 18.0F;
      this.menuHeight = totalHeight + 16.0F * this.menuEntries.size();
      this.menuX = MathHelper.clamp(x, 4.0F, window.getScaledWidth() - this.menuWidth - 4.0F);
      this.menuY = MathHelper.clamp(y, 4.0F, window.getScaledHeight() - this.menuHeight - 4.0F);
      this.menuOpen = true;
      this.menuClosing = false;
      this.menuAnimStart = System.currentTimeMillis();
   }

   private String russianName(String name) {
      if (name == null) {
         return "";
      } else {
         String var2 = name.toLowerCase(Locale.ROOT);

         return switch (var2) {
            case "watermark" -> "Ватермарка";
            case "target hud" -> "Информация о цели";
            case "cooldowns" -> "Перезарядки";
            case "keybinds" -> "Клавиши";
            case "potions" -> "Зелья";
            case "info" -> "Информация";
            case "armorbar" -> "Панель брони";
            case "totembar" -> "Панель тотемов";
            case "customhotbar" -> "Кастомный хотбар";
            default -> name;
         };
      }
   }

   private boolean isInsideMenu(double mouseX, double mouseY) {
      return mouseX >= this.menuX && mouseX <= this.menuX + this.menuWidth && mouseY >= this.menuY && mouseY <= this.menuY + this.menuHeight;
   }

   private int getMenuIndex(double mouseY) {
      float startY = this.menuY;
      if (this.menuMode == HudEditorOverlay.MenuMode.ACTIONS) {
         startY += 28.0F;
      }

      float localY;
      float var5;
      return (localY = (float)mouseY - (var5 = startY + 18.0F)) < 0.0F ? -1 : (int)(localY / 16.0F);
   }

   private void beginClose() {
      if (this.menuOpen && !this.menuClosing) {
         this.menuClosing = true;
         this.menuAnimStart = System.currentTimeMillis();
      }
   }

   private void closeMenu() {
      this.menuOpen = false;
      this.menuClosing = false;
      this.menuAnimStart = 0L;
      this.targetDraggable = null;
      this.menuEntries.clear();
   }

   private List<AbstractDraggable> getVisibleDraggables(Hud hud) {
      ArrayList<AbstractDraggable> visible = new ArrayList<>();

      for (AbstractDraggable draggable : Elixir.getInstance().getDraggableRepository().draggable()) {
         if (this.isSupported(draggable) && draggable.canDraw(hud, draggable)) {
            visible.add(draggable);
         }
      }

      return visible;
   }

   private List<AbstractDraggable> getDisabledDraggables(Hud hud) {
      ArrayList<AbstractDraggable> disabled = new ArrayList<>();

      for (AbstractDraggable draggable : Elixir.getInstance().getDraggableRepository().draggable()) {
         if (this.isSupported(draggable) && !hud.isHudElementEnabled(draggable.getName())) {
            disabled.add(draggable);
         }
      }

      return disabled;
   }

   private AbstractDraggable getHoveredDraggable(Hud hud, double mouseX, double mouseY) {
      List<AbstractDraggable> reversed = this.getVisibleDraggables(hud);
      Collections.reverse(reversed);

      for (AbstractDraggable draggable : reversed) {
         if (draggable.isHovered(mouseX, mouseY)) {
            return draggable;
         }
      }

      return null;
   }

   private boolean isSupported(AbstractDraggable draggable) {
      return draggable != null && !"Notifications".equalsIgnoreCase(draggable.getName());
   }

   private AbstractDraggable createFresh(AbstractDraggable draggable) {
      if (draggable instanceof Info) {
         return Info.getInstance();
      } else {
         try {
            return (AbstractDraggable)draggable.getClass().getDeclaredConstructor().newInstance();
         } catch (Exception var3) {
            return null;
         }
      }
   }

   private void saveHudState() {
      try {
         Elixir.getInstance().getFileController().saveFiles();
      } catch (Exception var2) {
      }
   }

   private static float easeOutCubic(float value) {
      float t = MathHelper.clamp(value, 0.0F, 1.0F);
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static int blend(int from, int to, float t) {
      float p = MathHelper.clamp(t, 0.0F, 1.0F);
      int fr = from >> 16 & 0xFF;
      int fg = from >> 8 & 0xFF;
      int fb = from & 0xFF;
      int fa = from >>> 24 & 0xFF;
      int tr = to >> 16 & 0xFF;
      int tg = to >> 8 & 0xFF;
      int tb = to & 0xFF;
      int ta = to >>> 24 & 0xFF;
      int r = (int)(fr + (tr - fr) * p);
      int g = (int)(fg + (tg - fg) * p);
      int b = (int)(fb + (tb - fb) * p);
      int a = (int)(fa + (ta - fa) * p);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private record MenuEntry(String label, Runnable action) {
   }

   private static enum MenuMode {
      ADD,
      ACTIONS;
   }
}
