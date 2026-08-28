package code.elixir.display.screens.clickgui;

import code.elixir.Elixir;
import code.elixir.common.animation.Easy.Direction;
import code.elixir.common.animation.Easy.EaseBackIn;
import code.elixir.display.screens.clickgui.animation.CloseAnimationController;
import code.elixir.display.screens.clickgui.animation.CloseAnimationOverlay;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.customization.CustomizationComponent;
import code.elixir.display.screens.clickgui.components.implement.module.ModuleComponent;
import code.elixir.display.screens.clickgui.components.implement.module.ModuleDescriptions;
import code.elixir.display.screens.clickgui.components.implement.other.ToggleSwitchRenderer;
import code.elixir.display.screens.clickgui.components.implement.panel.PanelManager;
import code.elixir.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.BindComponent;
import code.elixir.features.impl.misc.Panic;
import code.elixir.features.impl.render.ClickGui;
import code.elixir.features.impl.render.Esp;
import code.elixir.features.impl.render.EspSkeleton;
import code.elixir.features.impl.render.NameTags;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.SettingComponentAdder;
import code.elixir.utils.client.bind.BindCaptureHelper;
import code.elixir.utils.client.bind.BindCodes;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.gif.GifTextureRenderer;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;

public class MenuScreen extends Screen implements QuickImports {
   private static final Identifier WAIT_GIF_TEXTURE = Identifier.of("minecraft", "gifs/wait.gif");
   private static MenuScreen INSTANCE;
   private static final List<MenuScreen.ClickGuiInput> activeClickGuiInputs = new ArrayList<>();
   private final List<AbstractComponent> components = new ArrayList<>();
   private final PanelManager panelManager = new PanelManager();
   private final GifTextureRenderer waitGifRenderer = new GifTextureRenderer(WAIT_GIF_TEXTURE);
   public final EaseBackIn animation = new EaseBackIn(220, 1.0, 0.0F);
   public ModuleCategory category = ModuleCategory.COMBAT;
   public int x;
   public int y;
   public int width;
   public int height;
   private final Random random = new Random();
   private final List<MenuScreen.GuiSnowflake> guiSnowflakes = new ArrayList<>();
   private float snowflakesAnim = 0.0F;
   private long lastSnowUpdateTime = -1L;
   private float targetAppliedGuiScale = 1.0F;
   private float smoothGuiScale = 1.0F;
   private boolean initializedGuiScale = false;
   private boolean resizeModeActive = false;
   private long lastClickTime = 0L;
   private static final long DOUBLE_CLICK_DELAY_MS = 300L;
   private float scaleFactor = 1.0F;
   private static final float GUI_WIDTH = 560.0F;
   private static final float GUI_HEIGHT = 320.0F;
   private static final float SIDEBAR_WIDTH = 150.0F;
   private static final float HEADER_HEIGHT = 28.0F;
   private static final float MODULE_HEIGHT = 20.0F;
   private static final float ESP_SECTION_HEADER_HEIGHT = 20.0F;
   private static final float ESP_SECTION_GAP = 8.0F;
   private static final float ESP_SETTINGS_TOP_PAD = 6.0F;
   private static final float PADDING = 10.0F;
   private static final float CONTENT_GAP = 12.0F;
   private static final float COLUMN_WIDTH = 130.0F;
   private static final float COLUMN_SPACING = 2.0F;
   private static final int MAX_VISIBLE_MODULES = 9;
   private Map<ModuleCategory, MenuScreen.CategoryColumn> categoryColumns = new LinkedHashMap<>();
   private boolean initialized = false;
   private String hoveredModuleName = null;
   private Module bindingModule = null;
   private int firstBindKey = -1;
   private Module selectedModule = null;
   private Module hoveredModule = null;
   private boolean tooltipVisible = false;
   private float tooltipX = 0.0F;
   private float tooltipY = 0.0F;
   private float tooltipW = 0.0F;
   private float tooltipH = 0.0F;
   private String searchText = "";
   private boolean searchFieldFocused = false;
   private float searchTextScroll = 0.0F;
   private int searchCursor = 0;
   private int searchSelectionStart = -1;
   private int searchSelectionEnd = -1;
   private static final int SEARCH_MAX_LENGTH = 40;
   private static final float SEARCH_TEXT_PADDING = 8.0F;
   private float searchSuggestionsAnim = 0.0F;
   private int searchSuggestionIndex = -1;
   private final List<String> searchSuggestions = new ArrayList<>();
   private float searchSuggestionsX = 0.0F;
   private float searchSuggestionsY = 0.0F;
   private float searchSuggestionsW = 0.0F;
   private float searchSuggestionsH = 0.0F;
   private float espPreviewX = 0.0F;
   private float espPreviewY = 0.0F;
   private float espPreviewW = 0.0F;
   private float espPreviewH = 0.0F;
   private boolean espPreviewDragging = false;
   private float espPreviewYaw = 180.0F;
   private float espPreviewPitch = 15.0F;
   private float espDragStartX = 0.0F;
   private float espDragStartY = 0.0F;
   private float espDragStartYaw = 180.0F;
   private float espDragStartPitch = 15.0F;
   private LivingEntity espPreviewEntity = null;
   private final UUID espPreviewUuid = UUID.fromString("7e6b0ad2-6d6b-4b8d-9b6c-1b1c0b1c0b1c");
   private final String espPreviewName = "Enemy";
   private final Map<Module, List<AbstractSettingComponent>> espSettingsCache = new HashMap<>();
   private final Map<Module, Integer> espSettingsCacheVersion = new HashMap<>();
   private final MenuScreen.EspSectionLayout espSectionLayout = new MenuScreen.EspSectionLayout();
   private final MenuScreen.EspSectionLayout skeletonSectionLayout = new MenuScreen.EspSectionLayout();
   private final MenuScreen.EspSectionLayout nameTagsSectionLayout = new MenuScreen.EspSectionLayout();
   private boolean espSettingsLayoutReady;
   private Map<Module, List<AbstractSettingComponent>> expandedModules = new HashMap<>();
   private Map<Module, Float> expandAnimations = new HashMap<>();
   private float settingsScrollOffset = 0.0F;
   private float settingsTargetScroll = 0.0F;
   private float settingsMaxScroll = 0.0F;
   private float settingsPanelAnim = 0.0F;
   private ModuleCategory lastCategory = ModuleCategory.COMBAT;
   private float categoryFade = 1.0F;
   private float categorySlideOffset = 0.0F;
   private float categorySlideDirection = 1.0F;
   private final Map<ModuleCategory, Float> categorySelectAnimations = new HashMap<>();
   private Map<Module, Float> moduleEnabledAnimations = new HashMap<>();
   private final Map<Module, Float> moduleHoverAnimations = new HashMap<>();
   private final Map<Module, Float> moduleSelectAnimations = new HashMap<>();
   private final Map<Module, Long> moduleBindAnimations = new HashMap<>();
   private final Map<Module, Long> moduleNoSettingsShakeUntil = new HashMap<>();
   private float categoryIndicatorY = 0.0F;
   private float sidebarSearchFocusAnim = 0.0F;
   private Map<ModuleCategory, Float> columnSlideAnimations = new HashMap<>();
   private Map<ModuleCategory, Long> columnSlideDelays = new HashMap<>();
   private long openTime = 0L;
   public boolean sidebarCollapsed = false;
   public float sidebarCollapseAnim = 0.0F;
   private CustomizationComponent customizationComponent = new CustomizationComponent();
   private SettingComponentAdder settingComponentAdder = new SettingComponentAdder();
   private int lastMouseX = 0;
   private int lastMouseY = 0;
   private String currentHoveredModule = null;
   private ModuleComponent currentHoveredComponent = null;
   private float lastModuleX = 0.0F;
   private float lastModuleY = 0.0F;
   private float lastModuleWidth = 0.0F;
   private Module displayedSettingsModule = null;
   private Module settingsPanelClosingModule = null;
   private static final float SETTINGS_PANEL_SLIDE = 72.0F;
   private Module settingsMorphModule = null;
   private float settingsMorphOriginX = 0.0F;
   private float settingsMorphOriginY = 0.0F;
   private float settingsMorphOriginW = 0.0F;
   private float settingsMorphOriginH = 20.0F;
   private boolean blurMaskFailed = false;
   private String lastSearchText = "";
   private MenuScreen.ModuleFilterKey cachedModuleFilterKey = null;
   private List<Module> cachedFilteredModules = List.of();
   private static final float MODULE_LIST_CLIP_PAD = 2.0F;

   public static MenuScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new MenuScreen();
      }

      return INSTANCE;
   }

   public static void activateClickGuiInput(MenuScreen.ClickGuiInput input) {
      for (MenuScreen.ClickGuiInput activeInput : activeClickGuiInputs) {
         if (activeInput != input && activeInput.isTyping()) {
            activeInput.setTyping(false);
         }
      }

      input.setTyping(true);
      if (!activeClickGuiInputs.contains(input)) {
         activeClickGuiInputs.add(input);
      }
   }

   public static void clearAllClickGuiInputs() {
      for (MenuScreen.ClickGuiInput input : activeClickGuiInputs) {
         input.setTyping(false);
         if (input instanceof MenuScreen.ClearableClickGuiInput clearable) {
            clearable.clearText();
         }
      }

      activeClickGuiInputs.clear();
   }

   public static boolean hasActiveClickGuiInputs() {
      return activeClickGuiInputs.stream().anyMatch(MenuScreen.ClickGuiInput::isTyping);
   }

   public void initialize() {
      if (!this.initialized) {
         if (this.category == ModuleCategory.CONFIGS) {
            this.category = ModuleCategory.COMBAT;
         }

         List<ModuleCategory> visibleCategories = this.getVisibleCategories();
         this.categoryColumns.clear();

         for (ModuleCategory category : visibleCategories) {
            this.categoryColumns.put(category, new MenuScreen.CategoryColumn(category, 0.0F, 0.0F));
         }

         this.initialized = true;
      }
   }

   public MenuScreen() {
      super(Text.of("MenuScreen"));
   }

   public void tick() {
      super.tick();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.initialized) {
         this.initialize();
      }

      if (ThemeManager.isLiquidGlassTheme()) {
         KawaseBlur.INSTANCE.render(context.getMatrices(), 1, 0.0F);
      }

      if (!this.initializedGuiScale) {
         this.targetAppliedGuiScale = ClickGui.guiScale != null ? ClickGui.guiScale.getValue() : 1.0F;
         this.smoothGuiScale = this.targetAppliedGuiScale;
         this.initializedGuiScale = true;
      }

      boolean isDraggingSlider = code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent.current != null
         && code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent.current.isDragging();

      if (!isDraggingSlider) {
         this.targetAppliedGuiScale = ClickGui.guiScale != null ? ClickGui.guiScale.getValue() : 1.0F;
      }
      this.smoothGuiScale = Calculate.interpolateSmooth(8.0, this.smoothGuiScale, this.targetAppliedGuiScale);

      this.updateColumnPositions();
      this.sidebarCollapseAnim = Calculate.interpolateSmooth(6.0, this.sidebarCollapseAnim, this.sidebarCollapsed ? 1.0F : 0.0F);
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      CloseAnimationController animController = CloseAnimationOverlay.getInstance().getController();
      float currentScale = animController.isClosing() ? animController.getScale() : this.getGuiScale();
      float currentAlpha = animController.isClosing() ? animController.getAlpha() : this.getScaleAnimation();
      float offsetX = animController.isClosing() ? animController.getOffsetX() : 0.0F;
      float offsetY = animController.isClosing() ? animController.getOffsetY() : 0.0F;
      code.elixir.utils.display.render.MainMenuSkyRenderer.render(context, window.getScaledWidth(), window.getScaledHeight(), currentAlpha);
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), 0.0, 0.0, window.getScaledWidth(), window.getScaledHeight())
            .color(Calculate.applyOpacity(-16777216, 125.0F * currentAlpha))
            .build()
      );

      ClickGui clickGui = ClickGui.getInstance();
      boolean renderSnow = clickGui != null && clickGui.shouldRenderSnowflakes();
      this.snowflakesAnim = Calculate.interpolateSmooth(5.0, this.snowflakesAnim, renderSnow ? 1.0F : 0.0F);
      if (this.snowflakesAnim > 0.005F) {
         this.renderSnowflakes(context, currentAlpha * this.snowflakesAnim);
      }
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      float scale = currentScale * this.scaleFactor;
      double transformedMouseX = (mouseX - centerX) / scale + centerX;
      double transformedMouseY = (mouseY - centerY) / scale + centerY;
      context.getMatrices().push();
      context.getMatrices().translate(centerX + offsetX, centerY + offsetY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      context.getMatrices().translate(-centerX, -centerY, 0.0F);
      Calculate.setAlpha(currentAlpha, () -> this.renderSidebarLayout(context, (float)transformedMouseX, (float)transformedMouseY, delta));
      this.drawModuleTooltip(context);
      context.getMatrices().pop();
   }

   private void updateColumnPositions() {
      List<ModuleCategory> visibleCategories = this.getVisibleCategories();
      float totalWidth = visibleCategories.size() * 130.0F + (visibleCategories.size() - 1) * 2.0F;
      float startX = (window.getScaledWidth() - totalWidth) / 2.0F;
      float startY = (window.getScaledHeight() - 300) / 2.0F;
      if (this.categoryColumns.isEmpty()) {
         for (int i = 0; i < visibleCategories.size(); i++) {
            float x = startX + i * 132.0F;
            this.categoryColumns.put(visibleCategories.get(i), new MenuScreen.CategoryColumn(visibleCategories.get(i), x, startY));
         }
      } else {
         this.categoryColumns.keySet().removeIf(cat -> !visibleCategories.contains(cat));

         for (ModuleCategory category : visibleCategories) {
            if (!this.categoryColumns.containsKey(category)) {
               this.categoryColumns.put(category, new MenuScreen.CategoryColumn(category, 0.0F, startY));
            }
         }

         int i = 0;

         for (ModuleCategory categoryx : visibleCategories) {
            MenuScreen.CategoryColumn column = this.categoryColumns.get(categoryx);
            if (column != null) {
               column.x = startX + i * 132.0F;
               column.y = startY;
               i++;
            }
         }
      }
   }

   private void renderColumns(DrawContext context, float mouseX, float mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      float progress = this.getScaleAnimation();
      this.hoveredModuleName = null;

      for (MenuScreen.CategoryColumn column : this.categoryColumns.values()) {
         if (this.getVisibleCategories().contains(column.category)) {
            this.renderColumn(context, column, mouseX, mouseY, progress);
         }
      }

      this.renderSearchField(context, mouseX, mouseY, progress);
      windowManager.render(context, (int)mouseX, (int)mouseY, delta);
   }

   private void renderSidebarLayout(DrawContext context, float mouseX, float mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      MenuScreen.GuiLayout layout = this.getLayout();
      int bodyColor = ThemeManager.BackgroundGui.getColor();
      int sidebarColor = Calculate.applyOpacity(-16777216, 75.0F);
      ClickGui clickGui = ClickGui.getInstance();
      if (ThemeManager.isLiquidGlassTheme() || clickGui != null && clickGui.shouldRenderBackgroundBlur()) {
         this.drawBlurMaskSafe(matrices, layout.x, layout.y, layout.width, layout.height, 16.0F);
      }

      rectangle.render(ShapeProperties.create(matrices, layout.x, layout.y, layout.width, layout.height).round(12.0F).color(bodyColor).build());
      rectangle.render(ShapeProperties.create(matrices, layout.x, layout.y, layout.sidebarWidth, layout.height).round(12.0F, 0.0F, 12.0F, 0.0F).color(sidebarColor).build());
      rectangle.render(ShapeProperties.create(matrices, layout.x + layout.sidebarWidth, layout.y + 10.0F, 1.0F, layout.height - 20.0F).color(0x20FFFFFF).build());
      rectangle.render(ShapeProperties.create(matrices, layout.x + 10.0F, layout.y, layout.width - 20.0F, 1.0F).color(0x30FFFFFF).build());
      this.renderSidebar(context, layout, mouseX, mouseY);
      this.renderContent(context, layout, mouseX, mouseY, delta);
      windowManager.render(context, (int)mouseX, (int)mouseY, delta);
      if (clickGui == null || clickGui.shouldRenderShine()) {
         if (clickGui != null) {
            clickGui.getShineSize();
         } else {
            float var10000 = 1.0F;
         }

         if (clickGui != null) {
            clickGui.getShineSpeed();
         } else {
            float var14 = 1.0F;
         }

         if (clickGui != null) {
            clickGui.getShineBrightness();
         } else {
            float var15 = 0.42F;
         }
      }
   }

   private void renderSidebar(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      MatrixStack matrices = context.getMatrices();
      List<ModuleCategory> categories = this.getVisibleCategories();
      float textFadeAlpha = 1.0F - this.sidebarCollapseAnim;
      float elixirAlpha = Math.max(0.0F, 1.0F - this.sidebarCollapseAnim * 2.0F);
      float eAlpha = Math.max(0.0F, (this.sidebarCollapseAnim - 0.5F) * 2.0F);
      float brandCenterX = layout.x + layout.sidebarWidth / 2.0F;
      if (elixirAlpha > 0.05F) {
         Fonts.getSize(24, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawCenteredBoth(
               matrices,
               "Elixir",
               brandCenterX,
               layout.y + 12.0F,
               26.0F,
               Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(elixirAlpha * 255.0F))
            );
      }

      if (eAlpha > 0.05F) {
         Fonts.getSize(24, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawCenteredBoth(
               matrices, "E", brandCenterX, layout.y + 12.0F, 26.0F, Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(eAlpha * 255.0F))
            );
      }

      Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), layout.x, layout.y, layout.sidebarWidth, layout.height);
      if (textFadeAlpha > 0.05F) {
         this.renderSearchField(context, layout, mouseX, mouseY, textFadeAlpha);
      }

      float itemOpenY = layout.sidebarSearchY + layout.sidebarSearchHeight + 12.0F;
      float itemClosedY = layout.y + 55.0F;
      float itemY = itemOpenY + (itemClosedY - itemOpenY) * this.sidebarCollapseAnim;
      float itemHeight = 22.0F;
      float iconX = layout.x + 16.0F;
      float textX = layout.x + 34.0F;
      if (this.categoryIndicatorY == 0.0F) {
         this.categoryIndicatorY = itemOpenY;
      }

      float targetIndicatorY = this.categoryIndicatorY;

      for (ModuleCategory category : categories) {
         boolean selected = category == this.category;
         boolean hovered = Calculate.isHovered(mouseX, mouseY, layout.x + 8.0F, itemY, layout.sidebarWidth - 16.0F, itemHeight);
         float anim = this.categorySelectAnimations.getOrDefault(category, 0.0F);
         float target = selected ? 1.0F : 0.0F;
         anim = Calculate.interpolateSmooth(6.0, anim, target);
         this.categorySelectAnimations.put(category, anim);
         float mix = Math.min(1.0F, anim + (hovered ? 0.35F : 0.0F));
         if (selected || hovered) {
            int bg = selected
               ? Calculate.applyOpacity(ThemeManager.primaryColor.getColor(), (int)(110.0F + mix * 90.0F))
               : Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(20.0F + mix * 35.0F));
            rectangle.render(ShapeProperties.create(matrices, layout.x + 8.0F, itemY, layout.sidebarWidth - 16.0F, itemHeight).round(7.0F).color(bg).build());
            if (selected) {
               rectangle.render(ShapeProperties.create(matrices, layout.x + 8.0F, itemY + 4.0F, 2.5F, itemHeight - 8.0F).round(1.5F).color(ThemeManager.textColor.getColor()).build());
            }
         }

         if (selected) {
            targetIndicatorY = itemY;
         }

         int iconColor = selected ? ThemeManager.textColor.getColor() : Calculate.applyOpacity(ThemeManager.textColor.getColor(), 140.0F);
         int textColor = selected
            ? Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(textFadeAlpha * 255.0F))
            : Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(textFadeAlpha * 130.0F));
         String label = this.getCategoryLabel(category);
         this.drawCategorySidebarIcon(context, category, iconX, itemY, itemHeight, iconColor);
         if (textFadeAlpha > 0.05F) {
            Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).drawVerticallyCentered(matrices, label, textX, itemY, itemHeight, textColor);
         }

         itemY += itemHeight + 4.0F;
      }

      if (Math.abs(this.categoryIndicatorY - targetIndicatorY) > 500.0F) {
         this.categoryIndicatorY = targetIndicatorY;
      }

      this.categoryIndicatorY = Calculate.interpolateSmooth(5.0, this.categoryIndicatorY, targetIndicatorY);
      rectangle.render(
         ShapeProperties.create(matrices, layout.x + 4.0F, this.categoryIndicatorY + 3.0F, 2.0, itemHeight - 6.0F)
            .round(1.0F)
            .color(ColorAssist.getClientColor())
            .build()
      );
      float cardWidth = layout.sidebarWidth - 16.0F;
      float cardHeight = 28.0F;
      float cardX = layout.x + 8.0F;
      float cardY = layout.y + layout.height - cardHeight - 8.0F;
      rectangle.render(
         ShapeProperties.create(matrices, cardX, cardY, cardWidth, cardHeight)
            .round(6.0F)
            .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 120.0F))
            .build()
      );
      float avatarSize = 16.0F;
      float avatarX = cardX + 6.0F;
      float avatarY = cardY + (cardHeight - avatarSize) / 2.0F;
      Identifier skinTexture = null;
      if (mc.player != null) {
         skinTexture = mc.player.getSkinTextures().texture();
      }

      if (skinTexture == null) {
         skinTexture = DefaultSkinHelper.getSkinTextures(UUID.randomUUID()).texture();
      }

      Render2D.drawTexture(
         context, skinTexture, avatarX, avatarY, avatarSize, 8.0F, 8, 8, 64, Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 120.0F), -1
      );
      if (textFadeAlpha > 0.05F) {
         String username = mc.player != null ? mc.player.getName().getString() : "Player";
         float nameX = avatarX + avatarSize + 6.0F;
         float nameY = cardY + 3.0F;
         Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawString(matrices, username, nameX, nameY + 6.0F, Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(textFadeAlpha * 255.0F)));
         Fonts.getSize(10, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawString(matrices, "FREE", nameX, nameY + 14.0F, Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(textFadeAlpha * 120.0F)));
      }

      Elixir.getInstance().getScissorManager().pop();
      float arrowOpenX = layout.x + 150.0F - 20.0F;
      float arrowClosedX = layout.x - 14.0F;
      float arrowX = arrowOpenX + (arrowClosedX - arrowOpenX) * this.sidebarCollapseAnim;
      float arrowOpenY = cardY + (cardHeight - 12.0F) / 2.0F;
      float arrowClosedY = layout.y + layout.height - 35.0F;
      float arrowY = arrowOpenY + (arrowClosedY - arrowOpenY) * this.sidebarCollapseAnim;
      boolean arrowHovered = Calculate.isHovered(mouseX, mouseY, arrowX - 4.0F, arrowY - 4.0F, 20.0, 20.0);
      int arrowColor = arrowHovered ? ThemeManager.textColor.getColor() : Calculate.applyOpacity(ThemeManager.textColor.getColor(), 160.0F);
      matrices.push();
      matrices.translate(arrowX + 6.0F, arrowY + 6.0F, 0.0);
      matrices.multiply(new Quaternionf().rotationZ((float)Math.toRadians(180.0F * this.sidebarCollapseAnim)));
      matrices.translate(-(arrowX + 6.0F), -(arrowY + 6.0F), 0.0);
      Fonts.getSize(18, Fonts.Type.ROCKSTAR_MEDIUM).drawCenteredBoth(matrices, "<", arrowX + 6.0F, arrowY, 12.0F, arrowColor);
      matrices.pop();
   }

   private void renderContent(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY, float delta) {
      this.renderHeaderBar(context, layout, mouseX, mouseY);
      if (this.category != this.lastCategory) {
         int previousIndex = this.getCategoryOrderIndex(this.lastCategory);
         int currentIndex = this.getCategoryOrderIndex(this.category);
         this.categorySlideDirection = currentIndex >= previousIndex ? 1.0F : -1.0F;
         this.categorySlideOffset = 16.0F;
         this.categoryFade = 0.0F;
         this.lastCategory = this.category;
         this.tooltipVisible = false;
         this.hoveredModule = null;
         this.espSettingsLayoutReady = false;
      }

      this.categoryFade = Calculate.interpolateSmooth(6.0, this.categoryFade, 1.0F);
      this.categorySlideOffset = Calculate.interpolateSmooth(7.0, this.categorySlideOffset, 0.0F);
      Runnable contentRenderer;
      if (this.category == ModuleCategory.CUSTOMIZATION) {
         this.tooltipVisible = false;
         this.hoveredModule = null;
         contentRenderer = () -> this.renderCustomizationContent(context, layout, mouseX, mouseY);
      } else {
         contentRenderer = this.category == ModuleCategory.ESP ? () -> this.renderEspContent(context, layout, mouseX, mouseY, delta) : () -> {
            this.renderModulesContent(context, layout, mouseX, mouseY);
            this.renderSettingsPanel(context, layout, mouseX, mouseY);
         };
      }

      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(this.categorySlideDirection * this.categorySlideOffset, 0.0F, 0.0F);
      float clipX = layout.contentX + 10.0F;
      float clipY = layout.moduleListY;
      float clipW = Math.max(0.0F, layout.contentWidth - 20.0F);
      float clipH = layout.moduleListHeight;
      Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), clipX, clipY - 2.0F, clipW, clipH + 4.0F);
      Calculate.setAlpha(this.categoryFade, contentRenderer);
      Elixir.getInstance().getScissorManager().pop();
      matrices.pop();
   }

   private void renderHeaderBar(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      MatrixStack matrices = context.getMatrices();
      String title = this.getCategoryLabel(this.category);
      String icon = this.getCategoryIcon(this.category);
      FontRenderer iconFont = Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS);
      FontRenderer titleFont = Fonts.getSize(15, Fonts.Type.ROCKSTAR_MEDIUM);
      float iconSize = this.getCategoryIconWidth(this.category, iconFont, icon);
      float textWidth = titleFont.getStringWidth(title);
      float badgeWidth = iconSize + textWidth + 18.0F;
      float badgeX = layout.headerX + 10.0F;
      float badgeH = layout.headerHeight - 6.0F;
      float badgeY = layout.headerY + (layout.headerHeight - badgeH) / 2.0F;
      rectangle.render(
         ShapeProperties.create(matrices, badgeX, badgeY, badgeWidth, badgeH)
            .round(6.0F)
            .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 140.0F))
            .build()
      );
      float iconTextH = this.category == ModuleCategory.ESP ? 9.0F : Math.min(iconFont.getStringHeight(icon), badgeH);
      float iconDrawY;
      if (this.category == ModuleCategory.ESP) {
         iconDrawY = badgeY + (badgeH - 9.0F) / 2.0F;
         BackgroundIcons.drawEspCategory(context, BackgroundIcons.ESP, badgeX + 6.0F, iconDrawY, ThemeManager.textColor.getColor());
      } else {
         iconDrawY = badgeY + (badgeH - iconTextH) / 2.0F;
         iconFont.drawString(matrices, icon, badgeX + 6.0F, iconDrawY, ThemeManager.textColor.getColor());
      }

      float titleDrawY = this.category == ModuleCategory.ESP
         ? iconDrawY + (9.0F - titleFont.getStringHeight(title)) / 2.0F
         : titleFont.getCenteredY(badgeY, badgeH, title);
      titleFont.drawString(matrices, title, badgeX + 6.0F + iconSize + 6.0F, titleDrawY, ThemeManager.textColor.getColor());
      if (this.category != ModuleCategory.CUSTOMIZATION) {
         List<Module> categoryModules = Elixir.getInstance().getModuleRepository().modules().stream().filter(m -> m.getCategory() == this.category).toList();
         long enabled = categoryModules.stream().filter(Module::isState).count();
         String countText = enabled + "/" + categoryModules.size();
         FontRenderer countFont = Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM);
         float countWidth = countFont.getStringWidth(countText) + 10.0F;
         float countX = layout.headerX + layout.headerWidth - countWidth - 10.0F;
         rectangle.render(
            ShapeProperties.create(matrices, countX, badgeY, countWidth, badgeH)
               .round(6.0F)
               .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 180.0F))
               .build()
         );
         float countDrawY = this.category == ModuleCategory.ESP
            ? iconDrawY + (9.0F - countFont.getStringHeight(countText)) / 2.0F
            : countFont.getCenteredY(badgeY, badgeH, countText);
         countFont.drawString(matrices, countText, countX + 5.0F, countDrawY, Calculate.applyOpacity(ThemeManager.textColor.getColor(), 150.0F));
      }
   }

   private void renderModulesContent(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      MatrixStack matrices = context.getMatrices();
      MenuScreen.CategoryColumn column = this.categoryColumns.getOrDefault(this.category, new MenuScreen.CategoryColumn(this.category, 0.0F, 0.0F));
      if (!this.searchText.equals(this.lastSearchText)) {
         this.lastSearchText = this.searchText;
         column.scrollOffset = 0.0F;
         column.targetScrollOffset = 0.0F;
      }

      column.updateScroll();
      this.updateExpandAnimations();
      this.hoveredModule = null;
      this.tooltipVisible = false;
      List<Module> categoryModules = this.getFilteredModuleList(this.category, true);
      float visibleModulesHeight = layout.moduleListHeight;
      float totalModulesHeight = categoryModules.size() * 22.0F;
      Elixir.getInstance()
         .getScissorManager()
         .push(matrices.peek().getPositionMatrix(), layout.moduleListX, layout.moduleListY - 2.0F, layout.moduleListWidth, visibleModulesHeight + 4.0F);
      float moduleY = layout.moduleListY - column.scrollOffset;

      for (Module module : categoryModules) {
         boolean moduleVisible = moduleY + 20.0F > layout.moduleListY && moduleY < layout.moduleListY + visibleModulesHeight;
         if (moduleVisible) {
            this.renderModule(
               context,
               module,
               layout.moduleListX,
               moduleY,
               layout.moduleListWidth,
               mouseX,
               mouseY,
               1.0F,
               layout.moduleListY,
               layout.moduleListY + visibleModulesHeight
            );
            if (Calculate.isHovered(mouseX, mouseY, layout.moduleListX, moduleY, layout.moduleListWidth, 20.0)
               && mouseY >= layout.moduleListY
               && mouseY <= layout.moduleListY + visibleModulesHeight) {
               this.hoveredModuleName = module.getName();
               this.hoveredModule = module;
            }
         }

         moduleY += 22.0F;
      }

      Elixir.getInstance().getScissorManager().pop();
      if (totalModulesHeight > visibleModulesHeight) {
         float scrollbarWidth = 2.0F;
         float scrollbarHeight = visibleModulesHeight / totalModulesHeight * visibleModulesHeight;
         float clampedOffset = Math.max(0.0F, Math.min(column.maxScroll, column.scrollOffset));
         float scrollbarY = layout.moduleListY + clampedOffset / totalModulesHeight * visibleModulesHeight;
         rectangle.render(
            ShapeProperties.create(
                  matrices, layout.moduleListX + layout.moduleListWidth - scrollbarWidth, layout.moduleListY, scrollbarWidth, visibleModulesHeight
               )
               .round(1.0F)
               .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 60.0F))
               .build()
         );
         rectangle.render(
            ShapeProperties.create(matrices, layout.moduleListX + layout.moduleListWidth - scrollbarWidth, scrollbarY, scrollbarWidth, scrollbarHeight)
               .round(1.0F)
               .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
               .build()
         );
      }

      column.maxScroll = Math.max(0.0F, totalModulesHeight - visibleModulesHeight);
      if (column.targetScrollOffset < 0.0F) {
         column.targetScrollOffset = Calculate.interpolateSmooth(5.0, column.targetScrollOffset, 0.0F);
         if (Math.abs(column.targetScrollOffset) < 0.1F) {
            column.targetScrollOffset = 0.0F;
         }
      } else if (column.targetScrollOffset > column.maxScroll) {
         column.targetScrollOffset = Calculate.interpolateSmooth(5.0, column.targetScrollOffset, column.maxScroll);
         if (Math.abs(column.targetScrollOffset - column.maxScroll) < 0.1F) {
            column.targetScrollOffset = column.maxScroll;
         }
      }

      this.categoryColumns.put(this.category, column);
      this.updateModuleTooltip(layout, mouseX, mouseY);
   }

   private void updateExpandAnimations() {
      for (Module module2 : this.expandedModules.keySet()) {
         float currentProgress = this.expandAnimations.getOrDefault(module2, 0.0F);
         if (currentProgress < 1.0F) {
            float speed = 0.08F;
            currentProgress += speed;
            currentProgress = Math.min(1.0F, currentProgress);
            this.expandAnimations.put(module2, currentProgress);
         }
      }

      this.expandAnimations.keySet().removeIf(module -> {
         if (!this.expandedModules.containsKey(module)) {
            float currentProgressx = this.expandAnimations.getOrDefault(module, 0.0F);
            if (currentProgressx > 0.0F) {
               float speedx = 0.12F;
               currentProgressx -= speedx;
               currentProgressx = Math.max(0.0F, currentProgressx);
               this.expandAnimations.put(module, currentProgressx);
               return currentProgressx <= 0.0F;
            } else {
               return true;
            }
         } else {
            return false;
         }
      });
   }

   private void renderCustomizationContent(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      MatrixStack matrices = context.getMatrices();
      this.customizationComponent.x = layout.moduleListX + 10.0F;
      this.customizationComponent.y = layout.moduleListY + 10.0F;
      this.customizationComponent.width = layout.moduleListWidth - 20.0F;
      this.customizationComponent.height = layout.moduleListHeight - 20.0F;
      this.customizationComponent.render(context, (int)mouseX, (int)mouseY, 0.0F);
   }

   private void renderSettingsPanel(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      MatrixStack matrices = context.getMatrices();
      boolean wantsOpen = this.selectedModule != null && this.expandedModules.containsKey(this.selectedModule);
      if (!wantsOpen) {
         this.displayedSettingsModule = null;
         this.settingsPanelAnim = 0.0F;
         this.renderWaitGifPlaceholder(context, layout);
      } else {
         this.displayedSettingsModule = this.selectedModule;
         this.settingsPanelAnim = Calculate.interpolateSmooth(8.0, this.settingsPanelAnim, 1.0F);
         if (this.displayedSettingsModule == null) {
            this.renderWaitGifPlaceholder(context, layout);
         } else {
            float eased = 1.0F - (float)Math.pow(1.0F - this.settingsPanelAnim, 3.0);
            float slideOffset = (1.0F - eased) * 72.0F;
            float panelX = layout.settingsX + slideOffset;
            float panelY = layout.settingsY;
            float panelW = layout.settingsWidth;
            float panelH = layout.settingsHeight;
            float panelAlpha = 0.55F + eased * 0.45F;
            int settingsBg = Calculate.applyOpacity(ThemeManager.BackgroundSettings.getColor(), (int)(215.0F * panelAlpha));
            rectangle.render(ShapeProperties.create(matrices, panelX, panelY, panelW, panelH).round(10.0F).color(settingsBg).build());
            rectangle.render(ShapeProperties.create(matrices, panelX + 6.0F, panelY, panelW - 12.0F, 1.0F).color(0x25FFFFFF).build());
            if (!(this.settingsPanelAnim < 0.01F)) {
               List<AbstractSettingComponent> settingComponents = this.expandedModules.get(this.displayedSettingsModule);
               if (settingComponents != null && !settingComponents.isEmpty()) {
                  float contentAlpha = MathHelper.clamp(eased * this.settingsPanelAnim, 0.0F, 1.0F);
                  this.settingsScrollOffset = Calculate.interpolateSmooth(5.0, this.settingsScrollOffset, this.settingsTargetScroll);
                  float titleBarH = 20.0F;
                  float contentX = panelX + 10.0F;
                  float headerY = panelY + 10.0F;
                  float scrollAreaY = headerY + titleBarH;
                  float contentWidth = panelW - 20.0F;
                  float scrollAreaHeight = panelH - 20.0F - titleBarH;
                  if (!(contentWidth <= 10.0F) && !(scrollAreaHeight <= 10.0F)) {
                     String title = this.displayedSettingsModule.getVisibleName();
                     Calculate.setAlpha(
                        contentAlpha,
                        () -> Fonts.getSize(16, Fonts.Type.ROCKSTAR_MEDIUM)
                           .drawVerticallyCentered(matrices, title, contentX, headerY, titleBarH, ThemeManager.textColor.getColor())
                     );
                     float clipInset = 2.0F;
                     Elixir.getInstance()
                        .getScissorManager()
                        .push(matrices.peek().getPositionMatrix(), panelX + clipInset, scrollAreaY, panelW - clipInset * 2.0F, scrollAreaHeight);
                     float settingY = scrollAreaY - this.settingsScrollOffset;
                     float totalHeight = 0.0F;

                     for (AbstractSettingComponent component : settingComponents) {
                        if (component.getSetting().isVisible()) {
                           component.prepareLayout(contentWidth);
                           component.x = contentX;
                           component.y = settingY;
                           Calculate.setAlpha(contentAlpha, () -> component.render(context, (int)mouseX, (int)mouseY, 0.0F));
                           settingY += component.height + 4.0F;
                           totalHeight += component.height + 4.0F;
                        }
                     }

                     Elixir.getInstance().getScissorManager().pop();
                     this.settingsMaxScroll = Math.max(0.0F, totalHeight - scrollAreaHeight);
                     if (this.settingsTargetScroll < 0.0F) {
                        this.settingsTargetScroll = Calculate.interpolateSmooth(5.0, this.settingsTargetScroll, 0.0F);
                        if (Math.abs(this.settingsTargetScroll) < 0.1F) {
                           this.settingsTargetScroll = 0.0F;
                        }
                     } else if (this.settingsTargetScroll > this.settingsMaxScroll) {
                        this.settingsTargetScroll = Calculate.interpolateSmooth(5.0, this.settingsTargetScroll, this.settingsMaxScroll);
                        if (Math.abs(this.settingsTargetScroll - this.settingsMaxScroll) < 0.1F) {
                           this.settingsTargetScroll = this.settingsMaxScroll;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void renderWaitGifPlaceholder(DrawContext context, MenuScreen.GuiLayout layout) {
      MatrixStack matrices = context.getMatrices();
      String hint = "Function not selected.";
      float hintX = layout.settingsX + layout.settingsWidth / 2.0F - 6.0F;
      Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawCenteredBoth(matrices, hint, hintX, layout.settingsY, layout.settingsHeight, Calculate.applyOpacity(ThemeManager.textColor.getColor(), 140.0F));
   }

   private void renderEspContent(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY, float delta) {
      this.hoveredModule = null;
      this.tooltipVisible = false;
      this.renderEspPreviewPanel(context, layout, mouseX, mouseY, delta);
      this.renderEspSettingsPanel(context, layout, mouseX, mouseY);
   }

   private void renderEspPreviewPanel(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(
            matrices,
            "Preview",
            layout.moduleListX + 10.0F,
            layout.moduleListY + 10.0F - 2.0F,
            Calculate.applyOpacity(ThemeManager.textColor.getColor(), 170.0F)
         );
      float innerX = layout.moduleListX + 10.0F;
      float innerY = layout.moduleListY + 10.0F + 10.0F;
      float innerW = layout.moduleListWidth - 20.0F;
      float innerH = layout.moduleListHeight - 20.0F - 10.0F;
      this.espPreviewX = innerX;
      this.espPreviewY = innerY;
      this.espPreviewW = innerW;
      this.espPreviewH = innerH;
      boolean hovered = Calculate.isHovered(mouseX, mouseY, innerX, innerY, innerW, innerH);
      int hintColor = Calculate.applyOpacity(ThemeManager.textColor.getColor(), hovered ? 170.0F : 110.0F);
      Fonts.getSize(10, Fonts.Type.ROCKSTAR_MEDIUM).drawString(matrices, "Drag to rotate", innerX + 6.0F, innerY + 6.0F, hintColor);
      if (mc.player != null) {
         LivingEntity previewEntity = this.getEspPreviewEntity();
         if (previewEntity != null) {
            float entityHeight = Math.max(1.0F, previewEntity.getHeight());
            float entityWidth = Math.max(0.6F, previewEntity.getWidth());
            float sizeByHeight = (innerH - 16.0F) / entityHeight;
            float sizeByWidth = (innerW - 20.0F) / (entityWidth * 2.0F);
            float size = Math.max(8.0F, Math.min(sizeByHeight, sizeByWidth) * 0.62F);
            float entityX = innerX + innerW / 2.0F;
            float entityY = innerY + innerH / 2.0F + entityHeight * size * 0.42F + 6.0F;
            Render2D.onRender(context);
            Rectangle.flushPending();
            Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), innerX, innerY, innerW, innerH);
            this.drawEntityPreview(context, entityX, entityY, size, this.espPreviewYaw, this.espPreviewPitch, previewEntity);
            this.renderEspPreviewDecorations(context, innerX, innerY, innerW, innerH, entityX, entityY, size);
            Elixir.getInstance().getScissorManager().pop();
         }
      } else {
         Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawString(
               matrices,
               "No player",
               innerX + (innerW - Fonts.getSize(12, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth("No player")) / 2.0F,
               innerY + innerH / 2.0F,
               Calculate.applyOpacity(ThemeManager.textColor.getColor(), 120.0F)
            );
      }
   }

   private void renderEspSettingsPanel(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      this.espSettingsLayoutReady = false;
      MatrixStack matrices = context.getMatrices();
      this.settingsPanelAnim = Calculate.interpolateSmooth(6.0, this.settingsPanelAnim, 1.0F);
      float panelY = layout.settingsY + (1.0F - this.settingsPanelAnim) * 6.0F;
      Calculate.setAlpha(0.6F + this.settingsPanelAnim * 0.4F, () -> {});
      this.settingsScrollOffset = Calculate.interpolateSmooth(5.0, this.settingsScrollOffset, this.settingsTargetScroll);
      float clipInset = 2.0F;
      float clipX = layout.settingsX + clipInset;
      float clipY = panelY + clipInset;
      float clipW = Math.max(0.0F, layout.settingsWidth - clipInset * 2.0F);
      float clipH = Math.max(0.0F, layout.settingsHeight - clipInset * 2.0F);
      Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), clipX, clipY, clipW, clipH);
      float contentX = layout.settingsX + 10.0F;
      float contentY = panelY + 10.0F;
      float contentWidth = layout.settingsWidth - 20.0F;
      float contentHeight = layout.settingsHeight - 20.0F;
      float sectionY = contentY - this.settingsScrollOffset;
      Module espModule = this.getEspModule();
      Module skeletonModule = this.getEspSkeletonModule();
      Module nameTagsModule = this.getNameTagsModule();
      sectionY = this.renderEspSection(context, contentX, sectionY, contentWidth, "ESP", espModule, this.espSectionLayout, mouseX, mouseY);
      sectionY += 8.0F;
      sectionY = this.renderEspSection(context, contentX, sectionY, contentWidth, "Skeleton", skeletonModule, this.skeletonSectionLayout, mouseX, mouseY);
      sectionY += 8.0F;
      sectionY = this.renderEspSection(context, contentX, sectionY, contentWidth, "NameTags", nameTagsModule, this.nameTagsSectionLayout, mouseX, mouseY);
      Elixir.getInstance().getScissorManager().pop();
      float totalHeight = sectionY - (contentY - this.settingsScrollOffset);
      this.settingsMaxScroll = Math.max(0.0F, totalHeight - contentHeight);
      if (this.settingsTargetScroll < 0.0F) {
         this.settingsTargetScroll = Calculate.interpolateSmooth(5.0, this.settingsTargetScroll, 0.0F);
         if (Math.abs(this.settingsTargetScroll) < 0.1F) {
            this.settingsTargetScroll = 0.0F;
         }
      } else if (this.settingsTargetScroll > this.settingsMaxScroll) {
         this.settingsTargetScroll = Calculate.interpolateSmooth(5.0, this.settingsTargetScroll, this.settingsMaxScroll);
         if (Math.abs(this.settingsTargetScroll - this.settingsMaxScroll) < 0.1F) {
            this.settingsTargetScroll = this.settingsMaxScroll;
         }
      }

      this.espSettingsLayoutReady = true;
   }

   private float renderEspSection(
      DrawContext context, float x, float y, float width, String title, Module module, MenuScreen.EspSectionLayout layout, float mouseX, float mouseY
   ) {
      MatrixStack matrices = context.getMatrices();
      float headerHeight = 20.0F;
      layout.sectionStartY = y;
      layout.headerX = x;
      layout.headerY = y;
      layout.headerW = width;
      layout.headerH = headerHeight;
      layout.sectionTitle = title;
      rectangle.render(
         ShapeProperties.create(matrices, x, y, width, headerHeight)
            .round(6.0F)
            .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 160.0F))
            .build()
      );
      rectangle.render(ShapeProperties.create(matrices, x, y, width, headerHeight).round(6.0F).color(Calculate.applyOpacity(-16777216, 18.0F)).build());
      FontRenderer headerFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      ToggleSwitchRenderer.drawLabel(headerFont, matrices, title, x + 6.0F, y, headerHeight, ThemeManager.textColor.getColor());
      float toggleX = ToggleSwitchRenderer.resolveHeaderX(x, width);
      float toggleY = ToggleSwitchRenderer.alignedControlY(headerFont, title, y, headerHeight, 10.0F);
      layout.toggleX = toggleX;
      layout.toggleY = toggleY;
      layout.toggleW = 22.0F;
      layout.toggleH = 10.0F;
      if (module != null) {
         this.layoutEspSectionBindPill(layout, module);
         this.renderEspSectionBindPill(context, layout, module, mouseX, mouseY);
         ToggleSwitchRenderer.renderHeader(context, toggleX, toggleY, module.isState(), module);
      }

      float settingY = y + headerHeight + 6.0F;
      if (module == null) {
         Fonts.getSize(11, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawString(matrices, "Module not found", x + 6.0F, settingY + 4.0F, Calculate.applyOpacity(ThemeManager.textColor.getColor(), 120.0F));
         settingY += 12.0F;
      } else {
         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(width);
               component.x = x;
               component.y = settingY;
               component.render(context, (int)mouseX, (int)mouseY, 0.0F);
               settingY += component.height + 4.0F;
            }
         }
      }

      layout.sectionEndY = settingY + 8.0F;
      return layout.sectionEndY;
   }

   private void layoutEspSectionBindPill(MenuScreen.EspSectionLayout layout, Module module) {
      if (module == null) {
         layout.bindPillW = 0.0F;
      } else {
         FontRenderer headerFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
         boolean listening = BindCaptureHelper.isCapturing(module);
         String keyText = listening ? "..." : BindCodes.getDisplayName(module.getKey());
         layout.bindPillW = ToggleSwitchRenderer.bindPillWidth(keyText);
         layout.bindPillH = 14.0F;
         layout.bindPillX = layout.toggleX - layout.bindPillW - 4.0F;
         layout.bindPillY = ToggleSwitchRenderer.bindPillTrackY(headerFont, layout.sectionTitle, layout.headerY, layout.headerH);
      }
   }

   private void renderEspSectionBindPill(DrawContext context, MenuScreen.EspSectionLayout layout, Module module, float mouseX, float mouseY) {
      if (module != null && !(layout.bindPillW <= 0.0F)) {
         MatrixStack matrices = context.getMatrices();
         boolean listening = BindCaptureHelper.isCapturing(module);
         String keyText = listening ? "..." : BindCodes.getDisplayName(module.getKey());
         FontRenderer pillFont = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD);
         boolean hovered = Calculate.isHovered(mouseX, mouseY, layout.bindPillX, layout.bindPillY, layout.bindPillW, layout.bindPillH);
         long pulse = System.currentTimeMillis();
         int pulseAlpha = listening ? (int)(120.0 + 80.0 * Math.abs(Math.sin(pulse / 220.0))) : 0;
         int outlineColor = listening
            ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha + 80)
            : ColorAssist.applyOpacity(hovered ? ColorAssist.getClientColor() : ColorAssist.getOutline(), hovered ? 200 : 130);
         int fillColor = listening
            ? ColorAssist.applyOpacity(ColorAssist.getClientColor(), pulseAlpha)
            : ColorAssist.applyOpacity(ThemeManager.offModuleColor.getColor(), hovered ? 165 : 120);
         rectangle.render(
            ShapeProperties.create(matrices, layout.bindPillX, layout.bindPillY, layout.bindPillW, layout.bindPillH)
               .round(5.0F)
               .thickness(listening ? 1.2F : (hovered ? 1.0F : 0.7F))
               .outlineColor(outlineColor)
               .color(fillColor)
               .build()
         );
         ToggleSwitchRenderer.drawPillTextCentered(
            pillFont,
            matrices,
            keyText,
            layout.bindPillX,
            layout.bindPillY,
            layout.bindPillW,
            layout.bindPillH,
            listening ? ColorAssist.getClientColor() : ThemeManager.textColor.getColor()
         );
      }
   }

   private boolean tryClickEspSectionBind(double mouseX, double mouseY, int button, MenuScreen.EspSectionLayout layout, Module module) {
      if (module != null && !(layout.sectionStartY < 0.0F)) {
         this.layoutEspSectionBindPill(layout, module);
         if (BindCaptureHelper.isCapturing(module)) {
            if (button == 0 && Calculate.isHovered(mouseX, mouseY, layout.bindPillX, layout.bindPillY, layout.bindPillW, layout.bindPillH)) {
               BindCaptureHelper.cancel();
               return true;
            } else {
               return BindCaptureHelper.handleMouse(button);
            }
         } else if (button == 0 && Calculate.isHovered(mouseX, mouseY, layout.bindPillX, layout.bindPillY, layout.bindPillW, layout.bindPillH)) {
            BindCaptureHelper.toggle(module);
            return true;
         } else if (button == 2
            && Calculate.isHovered(mouseX, mouseY, layout.headerX, layout.headerY, layout.headerW, layout.headerH)
            && !ToggleSwitchRenderer.isHeaderHovered(mouseX, mouseY, layout.toggleX, layout.toggleY)) {
            BindCaptureHelper.toggle(module);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isEspModuleBindTarget(Module module) {
      if (module != null && BindCaptureHelper.isCapturing()) {
         Object target = BindCaptureHelper.getTarget();
         return target == module;
      } else {
         return false;
      }
   }

   private List<AbstractSettingComponent> getEspSettingsComponents(Module module) {
      if (module == null) {
         return Collections.emptyList();
      } else {
         int version = module.settings().size();
         Integer cachedVersion = this.espSettingsCacheVersion.get(module);
         if (cachedVersion == null || cachedVersion != version) {
            ArrayList<AbstractSettingComponent> components = new ArrayList<>();
            this.settingComponentAdder.addSettingComponent(module.settings(), components, module);
            this.espSettingsCache.put(module, components);
            this.espSettingsCacheVersion.put(module, version);
         }

         return this.espSettingsCache.get(module);
      }
   }

   private void drawEntityPreview(DrawContext context, float x, float y, float size, float yaw, float pitch, LivingEntity entity) {
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(x, y, 1050.0);
      matrices.scale(1.0F, 1.0F, -1.0F);
      matrices.scale(size, size, size);
      float pitchRad = (float)Math.toRadians(pitch);
      Quaternionf cameraRotation = new Quaternionf().rotateX(-pitchRad);
      Quaternionf entityRotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraRotation);
      matrices.multiply(entityRotation);
      float prevBodyYaw = entity.bodyYaw;
      float prevYaw = entity.getYaw();
      float prevPitch = entity.getPitch();
      float prevHeadYaw = entity.headYaw;
      float prevPrevHeadYaw = entity.prevHeadYaw;
      float prevPrevBodyYaw = entity.prevBodyYaw;
      float prevPrevYaw = entity.prevYaw;
      float prevPrevPitch = entity.prevPitch;
      float bodyYaw = MathHelper.wrapDegrees(yaw);
      float modelPitch = -pitch * 0.9F;
      entity.bodyYaw = bodyYaw;
      entity.prevBodyYaw = bodyYaw;
      entity.setYaw(bodyYaw);
      entity.prevYaw = bodyYaw;
      entity.setPitch(modelPitch);
      entity.prevPitch = modelPitch;
      entity.headYaw = bodyYaw;
      entity.prevHeadYaw = bodyYaw;
      EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      dispatcher.setRotation(new Quaternionf(cameraRotation).conjugate());
      dispatcher.setRenderShadows(false);
      RenderSystem.enableDepthTest();
      Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
      dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, matrices, vertexConsumers, 15728880);
      if (vertexConsumers instanceof Immediate) {
         vertexConsumers.draw();
      } else {
         context.draw();
      }

      dispatcher.setRotation(new Quaternionf());
      dispatcher.setRenderShadows(true);
      RenderSystem.disableDepthTest();
      entity.bodyYaw = prevBodyYaw;
      entity.prevBodyYaw = prevPrevBodyYaw;
      entity.setYaw(prevYaw);
      entity.prevYaw = prevPrevYaw;
      entity.setPitch(prevPitch);
      entity.prevPitch = prevPrevPitch;
      entity.headYaw = prevHeadYaw;
      entity.prevHeadYaw = prevPrevHeadYaw;
      matrices.pop();
   }

   private void renderEspPreviewDecorations(
      DrawContext context, float innerX, float innerY, float innerW, float innerH, float entityX, float entityY, float size
   ) {
      MatrixStack matrices = context.getMatrices();
      LivingEntity previewEntity = this.getEspPreviewEntity();
      float boxW = previewEntity != null ? previewEntity.getWidth() * size : size * 0.8F;
      float boxH = previewEntity != null ? previewEntity.getHeight() * size : size * 1.8F;
      boxW = Math.min(boxW, innerW - 12.0F);
      boxH = Math.min(boxH, innerH - 12.0F);
      float bottom = Math.min(entityY, innerY + innerH - 6.0F);
      float top = Math.max(innerY + 6.0F, bottom - boxH);
      Vector4d vec = new Vector4d(entityX - boxW / 2.0F, top, entityX + boxW / 2.0F, bottom);
      Module espModule = this.getEspModule();
      Esp espInstance;
      if (espModule instanceof Esp && (espInstance = (Esp)espModule).isState()) {
         RenderSystem.disableDepthTest();
         Calculate.setAlpha(1.0F, () -> {
            espInstance.renderPreviewBox(matrices, vec, false, this.espPreviewYaw, this.espPreviewPitch);
            if (previewEntity != null) {
               espInstance.renderPreviewPotions(matrices, previewEntity, vec);
            }

            Render2D.onRender(context);
         });
      }

      NameTags nameTagsInstance;
      Module nameTagsModule;
      if ((nameTagsModule = this.getNameTagsModule()) instanceof NameTags && (nameTagsInstance = (NameTags)nameTagsModule).isState()) {
         RenderSystem.disableDepthTest();
         if (previewEntity instanceof PlayerEntity playerEntity) {
            Vector4d nameTagVec = new Vector4d(vec.x, vec.y - 5.0, vec.z, vec.w);
            Calculate.setAlpha(1.0F, () -> nameTagsInstance.renderPreview(context, matrices, playerEntity, nameTagVec));
         }
      }
   }

   private LivingEntity getEspPreviewEntity() {
      if (mc.world == null) {
         return null;
      } else {
         if (!(this.espPreviewEntity instanceof OtherClientPlayerEntity) || this.espPreviewEntity.getWorld() != mc.world) {
            GameProfile profile = new GameProfile(this.espPreviewUuid, "Enemy");
            this.espPreviewEntity = new OtherClientPlayerEntity(mc.world, profile);
         }

         this.espPreviewEntity.setPose(EntityPose.STANDING);
         this.espPreviewEntity.setHealth(20.0F);
         this.espPreviewEntity.setAbsorptionAmount(0.0F);
         this.espPreviewEntity.clearStatusEffects();
         this.espPreviewEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3560, 1));
         this.espPreviewEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 3560, 1));
         this.espPreviewEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 3560, 1));

         for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.espPreviewEntity.equipStack(slot, ItemStack.EMPTY);
         }

         this.espPreviewEntity.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
         this.espPreviewEntity.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
         this.espPreviewEntity.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
         this.espPreviewEntity.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
         this.espPreviewEntity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
         this.espPreviewEntity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         return this.espPreviewEntity;
      }
   }

   private Module getEspModule() {
      Esp module = Esp.getInstance();
      return (Module)(module != null ? module : this.getModuleByName("Esp"));
   }

   private Module getEspSkeletonModule() {
      EspSkeleton module = EspSkeleton.getInstance();
      return (Module)(module != null ? module : this.getModuleByName("EspSkeleton"));
   }

   private Module getNameTagsModule() {
      NameTags module = NameTags.getInstance();
      return (Module)(module != null ? module : this.getModuleByName("NameTags"));
   }

   private float getEspSettingsPanelY(MenuScreen.GuiLayout layout) {
      return layout.settingsY + (1.0F - this.settingsPanelAnim) * 6.0F;
   }

   private float getEspSettingsContentY(MenuScreen.GuiLayout layout) {
      return this.getEspSettingsPanelY(layout) + 10.0F - this.settingsScrollOffset;
   }

   private boolean isEspSettingsPanelHovered(double mouseX, double mouseY, MenuScreen.GuiLayout layout) {
      float panelY = this.getEspSettingsPanelY(layout);
      return Calculate.isHovered(mouseX, mouseY, layout.settingsX, panelY, layout.settingsWidth, layout.settingsHeight);
   }

   private void syncEspSettingsClickLayout(MenuScreen.GuiLayout layout) {
      if (!this.espSettingsLayoutReady) {
         float contentX = layout.settingsX + 10.0F;
         float contentWidth = layout.settingsWidth - 20.0F;
         float sectionY = this.getEspSettingsContentY(layout);
         this.espSectionLayout.sectionTitle = "ESP";
         this.espSectionLayout.sectionStartY = sectionY;
         this.espSectionLayout.sectionEndY = this.measureEspSectionEnd(sectionY, contentWidth, this.getEspModule());
         this.espSectionLayout.headerX = contentX;
         this.espSectionLayout.headerY = sectionY;
         this.espSectionLayout.headerW = contentWidth;
         this.espSectionLayout.headerH = 20.0F;
         FontRenderer espHeaderFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
         this.espSectionLayout.toggleX = ToggleSwitchRenderer.resolveHeaderX(contentX, contentWidth);
         this.espSectionLayout.toggleY = ToggleSwitchRenderer.alignedControlY(espHeaderFont, "ESP", sectionY, 20.0F, 10.0F);
         this.espSectionLayout.toggleW = 22.0F;
         this.espSectionLayout.toggleH = 10.0F;
         this.layoutEspSectionBindPill(this.espSectionLayout, this.getEspModule());
         float skeletonStart = this.espSectionLayout.sectionEndY + 8.0F;
         this.skeletonSectionLayout.sectionTitle = "Skeleton";
         this.skeletonSectionLayout.sectionStartY = skeletonStart;
         this.skeletonSectionLayout.sectionEndY = this.measureEspSectionEnd(skeletonStart, contentWidth, this.getEspSkeletonModule());
         this.skeletonSectionLayout.headerX = contentX;
         this.skeletonSectionLayout.headerY = skeletonStart;
         this.skeletonSectionLayout.headerW = contentWidth;
         this.skeletonSectionLayout.headerH = 20.0F;
         this.skeletonSectionLayout.toggleX = ToggleSwitchRenderer.resolveHeaderX(contentX, contentWidth);
         this.skeletonSectionLayout.toggleY = ToggleSwitchRenderer.alignedControlY(espHeaderFont, "Skeleton", skeletonStart, 20.0F, 10.0F);
         this.skeletonSectionLayout.toggleW = 22.0F;
         this.skeletonSectionLayout.toggleH = 10.0F;
         this.layoutEspSectionBindPill(this.skeletonSectionLayout, this.getEspSkeletonModule());
         float nameTagsStart = this.skeletonSectionLayout.sectionEndY + 8.0F;
         this.nameTagsSectionLayout.sectionTitle = "NameTags";
         this.nameTagsSectionLayout.sectionStartY = nameTagsStart;
         this.nameTagsSectionLayout.sectionEndY = this.measureEspSectionEnd(nameTagsStart, contentWidth, this.getNameTagsModule());
         this.nameTagsSectionLayout.headerX = contentX;
         this.nameTagsSectionLayout.headerY = nameTagsStart;
         this.nameTagsSectionLayout.headerW = contentWidth;
         this.nameTagsSectionLayout.headerH = 20.0F;
         this.nameTagsSectionLayout.toggleX = ToggleSwitchRenderer.resolveHeaderX(contentX, contentWidth);
         this.nameTagsSectionLayout.toggleY = ToggleSwitchRenderer.alignedControlY(espHeaderFont, "NameTags", nameTagsStart, 20.0F, 10.0F);
         this.nameTagsSectionLayout.toggleW = 22.0F;
         this.nameTagsSectionLayout.toggleH = 10.0F;
         this.layoutEspSectionBindPill(this.nameTagsSectionLayout, this.getNameTagsModule());
         this.layoutEspSectionComponents(this.espSectionLayout.sectionStartY, contentX, contentWidth, this.getEspModule());
         this.layoutEspSectionComponents(this.skeletonSectionLayout.sectionStartY, contentX, contentWidth, this.getEspSkeletonModule());
         this.layoutEspSectionComponents(this.nameTagsSectionLayout.sectionStartY, contentX, contentWidth, this.getNameTagsModule());
      }
   }

   private float measureEspSectionEnd(float sectionStartY, float contentWidth, Module module) {
      float settingY = sectionStartY + 20.0F + 6.0F;
      if (module == null) {
         return settingY + 12.0F;
      } else {
         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(contentWidth);
               settingY += component.height + 4.0F;
            }
         }

         return settingY + 8.0F;
      }
   }

   private void layoutEspSectionComponents(float sectionStartY, float contentX, float contentWidth, Module module) {
      if (module != null) {
         float settingY = sectionStartY + 20.0F + 6.0F;

         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(contentWidth);
               component.x = contentX;
               component.y = settingY;
               settingY += component.height + 4.0F;
            }
         }
      }
   }

   private boolean tryClickEspSectionToggle(double mouseX, double mouseY, int button, MenuScreen.EspSectionLayout layout, Module module) {
      if (module == null || button != 0 || layout.sectionStartY < 0.0F) {
         return false;
      } else if (ToggleSwitchRenderer.isHeaderHovered(mouseX, mouseY, layout.toggleX, layout.toggleY)) {
         module.switchState();
         SoundManager.playSound(SoundManager.SETTING_INTERACT);
         return true;
      } else {
         return false;
      }
   }

   private boolean handleEspSettingsClick(double mouseX, double mouseY, int button, MenuScreen.GuiLayout layout) {
      this.syncEspSettingsClickLayout(layout);
      Module espModule = this.getEspModule();
      Module skeletonModule = this.getEspSkeletonModule();
      Module nameTagsModule = this.getNameTagsModule();
      if (!BindCaptureHelper.isCapturing()
         || !this.isEspModuleBindTarget(espModule) && !this.isEspModuleBindTarget(skeletonModule) && !this.isEspModuleBindTarget(nameTagsModule)) {
         if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.nameTagsSectionLayout, nameTagsModule)) {
            return true;
         } else if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.skeletonSectionLayout, skeletonModule)) {
            return true;
         } else if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.espSectionLayout, espModule)) {
            return true;
         } else if (this.tryClickEspSectionToggle(mouseX, mouseY, button, this.nameTagsSectionLayout, nameTagsModule)) {
            return true;
         } else if (this.tryClickEspSectionToggle(mouseX, mouseY, button, this.skeletonSectionLayout, skeletonModule)) {
            return true;
         } else if (this.tryClickEspSectionToggle(mouseX, mouseY, button, this.espSectionLayout, espModule)) {
            return true;
         } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, this.getNameTagsModule())) {
            SoundManager.playSound(SoundManager.SETTING_INTERACT);
            return true;
         } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, this.getEspSkeletonModule())) {
            SoundManager.playSound(SoundManager.SETTING_INTERACT);
            return true;
         } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, this.getEspModule())) {
            SoundManager.playSound(SoundManager.SETTING_INTERACT);
            return true;
         } else {
            return false;
         }
      } else if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.nameTagsSectionLayout, nameTagsModule)) {
         return true;
      } else if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.skeletonSectionLayout, skeletonModule)) {
         return true;
      } else if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.espSectionLayout, espModule)) {
         return true;
      } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, nameTagsModule)) {
         return true;
      } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, skeletonModule)) {
         return true;
      } else if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, espModule)) {
         return true;
      } else {
         return BindCaptureHelper.handleMouse(button) ? true : this.isEspSettingsPanelHovered(mouseX, mouseY, layout);
      }
   }

   private boolean handleEspSectionComponentClickFromLayout(double mouseX, double mouseY, int button, Module module) {
      if (module == null) {
         return false;
      } else {
         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible() && component.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean handleEspSectionComponentClick(
      double mouseX, double mouseY, int button, float sectionStartY, float contentX, float contentWidth, Module module
   ) {
      this.layoutEspSectionComponents(sectionStartY, contentX, contentWidth, module);
      return this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, module);
   }

   private void handleEspSectionComponentReleaseFromLayout(double mouseX, double mouseY, int button, Module module) {
      if (module != null) {
         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.mouseReleased(mouseX, mouseY, button);
            }
         }
      }
   }

   private boolean handleEspSectionComponentDragFromLayout(double mouseX, double mouseY, int button, double deltaX, double deltaY, Module module) {
      if (module == null) {
         return false;
      } else {
         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible() && component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
               return true;
            }
         }

         return false;
      }
   }

   private void handleEspSectionComponentRelease(
      double mouseX, double mouseY, int button, float sectionStartY, float contentX, float contentWidth, Module module
   ) {
      if (module != null) {
         float settingY = sectionStartY + 20.0F + 6.0F;

         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(contentWidth);
               component.x = contentX;
               component.y = settingY;
               component.mouseReleased(mouseX, mouseY, button);
               settingY += component.height + 4.0F;
            }
         }
      }
   }

   private boolean handleEspSectionComponentDrag(
      double mouseX, double mouseY, int button, double deltaX, double deltaY, float sectionStartY, float contentX, float contentWidth, Module module
   ) {
      if (module == null) {
         return false;
      } else {
         float settingY = sectionStartY + 20.0F + 6.0F;

         for (AbstractSettingComponent component : this.getEspSettingsComponents(module)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(contentWidth);
               component.x = contentX;
               component.y = settingY;
               if (component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                  return true;
               }

               settingY += component.height + 4.0F;
            }
         }

         return false;
      }
   }

   private Module getModuleByName(String name) {
      return Elixir.getInstance().getModuleRepository().modules().stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   private static String safeModuleDescription(Module module) {
      if (module == null) {
         return null;
      } else {
         try {
            return ModuleDescriptions.tryGetDescription(module);
         } catch (Throwable var2) {
            return null;
         }
      }
   }

   private static boolean isMissingModuleDescription(String description) {
      try {
         return ModuleDescriptions.isMissingDescription(description);
      } catch (Throwable var2) {
         return true;
      }
   }

   private void updateModuleTooltip(MenuScreen.GuiLayout layout, float mouseX, float mouseY) {
      ClickGui clickGui = ClickGui.getInstance();
      if (clickGui != null && !clickGui.shouldShowModuleDescriptions()) {
         this.tooltipVisible = false;
      } else if (this.hoveredModule == null) {
         this.tooltipVisible = false;
      } else {
         String description = safeModuleDescription(this.hoveredModule);
         if (isMissingModuleDescription(description)) {
            this.tooltipVisible = false;
         } else {
            String wrapped = StringHelper.wrap(description, 160, 11);
            String[] lines = wrapped.split("\n");
            float maxWidth = 0.0F;

            for (String line : lines) {
               maxWidth = Math.max(maxWidth, Fonts.getSize(11, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(line));
            }

            float padding = 6.0F;
            float width = maxWidth + padding * 2.0F;
            float height = lines.length * (Fonts.getSize(11, Fonts.Type.ROCKSTAR_MEDIUM).getStringHeight("A") + 2.0F) + padding * 2.0F;
            float x = mouseX + 10.0F;
            float y = mouseY + 10.0F;
            float screenW = window.getScaledWidth();
            float screenH = window.getScaledHeight();
            float listRight = layout.moduleListX + layout.moduleListWidth - 4.0F;
            if (x + width > listRight) {
               x = mouseX - width - 10.0F;
            }

            if (x + width > listRight) {
               x = listRight - width;
            }

            if (x < layout.moduleListX + 4.0F) {
               x = layout.moduleListX + 4.0F;
            }

            if (x + width > screenW) {
               x = screenW - width - 4.0F;
            }

            if (y + height > screenH) {
               y = mouseY - height - 10.0F;
            }

            this.tooltipX = x;
            this.tooltipY = y;
            this.tooltipW = width;
            this.tooltipH = height;
            this.tooltipVisible = true;
         }
      }
   }

   private void drawModuleTooltip(DrawContext context) {
      if (this.tooltipVisible && this.hoveredModule != null) {
         String description = safeModuleDescription(this.hoveredModule);
         if (!isMissingModuleDescription(description)) {
            String wrapped = StringHelper.wrap(description, 160, 11);
            String[] lines = wrapped.split("\n");
            float padding = 6.0F;
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0.0F, 0.0F, 500.0F);
            rectangle.render(
               ShapeProperties.create(matrices, this.tooltipX, this.tooltipY, this.tooltipW, this.tooltipH)
                  .round(6.0F)
                  .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 220.0F))
                  .build()
            );
            rectangle.render(
               ShapeProperties.create(matrices, this.tooltipX, this.tooltipY, this.tooltipW, this.tooltipH)
                  .round(6.0F)
                  .color(Calculate.applyOpacity(-16777216, 40.0F))
                  .build()
            );
            float textY = this.tooltipY + padding;

            for (String line : lines) {
               Fonts.getSize(11, Fonts.Type.ROCKSTAR_MEDIUM)
                  .drawString(matrices, line, this.tooltipX + padding, textY + 6.0F, ThemeManager.textColor.getColor());
               textY += Fonts.getSize(11, Fonts.Type.ROCKSTAR_MEDIUM).getStringHeight("A") + 2.0F;
            }

            matrices.pop();
         }
      }
   }

   private void renderSearchField(DrawContext context, MenuScreen.GuiLayout layout, float mouseX, float mouseY, float alphaMultiplier) {
      MatrixStack matrices = context.getMatrices();
      this.sidebarSearchFocusAnim = Calculate.interpolateSmooth(6.0, this.sidebarSearchFocusAnim, this.searchFieldFocused ? 1.0F : 0.0F);
      boolean hovered = Calculate.isHovered(mouseX, mouseY, layout.sidebarSearchX, layout.sidebarSearchY, layout.sidebarSearchWidth, layout.sidebarSearchHeight);
      int bgAlpha = ThemeManager.isLiquidGlassTheme()
         ? 90 + (int)(this.sidebarSearchFocusAnim * 30.0F) + (hovered ? 10 : 0)
         : 180 + (int)(this.sidebarSearchFocusAnim * 25.0F) + (hovered ? 8 : 0);
      bgAlpha = (int)(bgAlpha * alphaMultiplier);
      int bgColor = ThemeManager.BackgroundGui.getColor();
      if (ThemeManager.isLiquidGlassTheme()) {
         bgColor &= 16777215;
      }

      float boxX = layout.sidebarSearchX;
      float boxY = layout.sidebarSearchY;
      float boxW = layout.sidebarSearchWidth;
      float boxH = layout.sidebarSearchHeight;
      rectangle.render(ShapeProperties.create(matrices, boxX, boxY, boxW, boxH).round(6.0F).color(ColorAssist.applyOpacity(bgColor, bgAlpha)).build());
      rectangle.render(
         ShapeProperties.create(matrices, boxX + 1.0F, boxY + 1.0F, boxW - 2.0F, boxH - 2.0F)
            .round(5.0F)
            .color(Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(this.sidebarSearchFocusAnim * 24.0F * alphaMultiplier)))
            .build()
      );
      FontRenderer searchFont = Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM);
      float textX = boxX + 8.0F;
      float textAreaW = boxW - 16.0F;
      boolean hasQuery = !this.searchText.isEmpty();
      String displayText = hasQuery ? this.searchText : "Поиск";
      int searchTextColor = hasQuery
         ? Calculate.applyOpacity(ThemeManager.textColor.getColor(), 255.0F * alphaMultiplier)
         : Calculate.applyOpacity(ThemeManager.textColor.getColor(), 80.0F * alphaMultiplier);
      float textY = searchFont.getCenteredY(boxY, boxH, displayText);
      float textH = searchFont.getStringHeight(displayText);
      if (hasQuery) {
         float textW = searchFont.getStringWidth(this.searchText);
         if (textW > textAreaW) {
            float cursorOffset = searchFont.getStringWidth(this.searchText.substring(0, this.searchCursor));
            float targetScroll = Math.max(0.0F, cursorOffset - textAreaW + 6.0F);
            this.searchTextScroll = Math.min(targetScroll, textW - textAreaW);
         } else {
            this.searchTextScroll = 0.0F;
         }
      } else {
         this.searchTextScroll = 0.0F;
      }

      float drawX = textX - this.searchTextScroll;
      Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), boxX + 1.0F, boxY + 1.0F, boxW - 2.0F, boxH - 2.0F);
      if (this.searchFieldFocused && this.hasSearchSelection()) {
         int selStart = this.getSearchSelectionStart();
         int selEnd = this.getSearchSelectionEnd();
         float selX1 = drawX + searchFont.getStringWidth(this.searchText.substring(0, selStart));
         float selX2 = drawX + searchFont.getStringWidth(this.searchText.substring(0, selEnd));
         float selW = Math.max(0.0F, selX2 - selX1);
         if (selW > 0.0F) {
            rectangle.render(
               ShapeProperties.create(matrices, selX1, textY, selW, textH)
                  .round(2.0F)
                  .color(Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(120.0F * alphaMultiplier)))
                  .build()
            );
         }
      }

      searchFont.drawString(matrices, displayText, drawX, textY, searchTextColor);
      if (this.searchFieldFocused && !this.hasSearchSelection() && System.currentTimeMillis() % 1000L < 500L) {
         this.syncSearchCursor();
         String beforeCursor = this.searchText.substring(0, this.searchCursor);
         float cursorX = drawX + searchFont.getStringWidth(beforeCursor);
         float cursorH = Math.min(boxH - 6.0F, textH + 1.0F);
         float cursorY = boxY + (boxH - cursorH) / 2.0F;
         rectangle.render(
            ShapeProperties.create(matrices, cursorX + 1.0F, cursorY, 1.0, cursorH)
               .color(Calculate.applyOpacity(ThemeManager.textColor.getColor(), 255.0F * alphaMultiplier))
               .build()
         );
      }

      Elixir.getInstance().getScissorManager().pop();
      this.searchSuggestionsAnim = 0.0F;
      this.searchSuggestionsH = 0.0F;
      this.searchSuggestionsW = 0.0F;
      this.searchSuggestionsY = 0.0F;
      this.searchSuggestionsX = 0.0F;
   }

   private MenuScreen.GuiLayout getLayout() {
      float screenWidth = window.getScaledWidth();
      float screenHeight = window.getScaledHeight();
      float width = Math.min(560.0F, screenWidth - 40.0F);
      float height = Math.min(320.0F, screenHeight - 40.0F);
      float x = (screenWidth - width) / 2.0F;
      float y = (screenHeight - height) / 2.0F;
      float currentSidebarWidth = 150.0F - 105.0F * this.sidebarCollapseAnim;
      float contentX = x + currentSidebarWidth + 12.0F;
      float contentY = y + 12.0F;
      float contentWidth = width - currentSidebarWidth - 24.0F;
      float contentHeight = height - 24.0F;
      float headerHeight = 28.0F;
      float sidebarSearchWidth = 130.0F - 105.0F * this.sidebarCollapseAnim;
      float sidebarSearchHeight = 26.0F;
      float sidebarSearchX = x + 10.0F;
      float sidebarSearchY = y + 40.0F;
      float innerContentWidth = contentWidth - 30.0F;
      float moduleListWidth = Math.max(0.0F, innerContentWidth * 0.52F);
      float moduleListX = contentX + 10.0F;
      float moduleListY = contentY + headerHeight + 10.0F;
      float moduleListHeight = contentHeight - headerHeight - 20.0F;
      float settingsX = moduleListX + moduleListWidth + 10.0F;
      float settingsWidth = contentWidth - moduleListWidth - 10.0F;
      return new MenuScreen.GuiLayout(
         x,
         y,
         width,
         height,
         currentSidebarWidth,
         contentX,
         contentY,
         contentWidth,
         contentHeight,
         contentX,
         contentY,
         contentWidth,
         headerHeight,
         moduleListX,
         moduleListY,
         moduleListWidth,
         moduleListHeight,
         settingsX,
         moduleListY,
         settingsWidth,
         moduleListHeight,
         sidebarSearchX,
         sidebarSearchY,
         sidebarSearchWidth,
         sidebarSearchHeight
      );
   }

   private List<Module> getFilteredModuleList(ModuleCategory cat, boolean crossCategorySearch) {
      String query = this.searchText == null ? "" : this.searchText;
      MenuScreen.ModuleFilterKey key = new MenuScreen.ModuleFilterKey(query, cat, crossCategorySearch);
      if (key.equals(this.cachedModuleFilterKey)) {
         return this.cachedFilteredModules;
      } else {
         this.cachedModuleFilterKey = key;
         String lower = query.toLowerCase(Locale.ROOT);
         this.cachedFilteredModules = Elixir.getInstance().getModuleRepository().modules().stream().filter(m -> {
            if (query.isEmpty()) {
               return m.getCategory() == cat;
            } else {
               return !crossCategorySearch && m.getCategory() != cat ? false : m.getVisibleName().toLowerCase(Locale.ROOT).contains(lower);
            }
         }).sorted(Comparator.comparing(Module::getVisibleName, String.CASE_INSENSITIVE_ORDER)).toList();
         return this.cachedFilteredModules;
      }
   }

   private void updateSearchSuggestions() {
      this.searchSuggestions.clear();
      String q = this.searchText == null ? "" : this.searchText.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty()) {
         this.searchSuggestionIndex = -1;
      } else {
         Elixir.getInstance()
            .getModuleRepository()
            .modules()
            .stream()
            .map(Module::getVisibleName)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .filter(name -> {
               String low = name.toLowerCase(Locale.ROOT);
               return low.contains(q) || low.replace(" ", "").contains(q.replace(" ", ""));
            })
            .limit(6L)
            .forEach(this.searchSuggestions::add);
         if (this.searchSuggestions.isEmpty()) {
            this.searchSuggestionIndex = -1;
         } else if (this.searchSuggestionIndex < 0 || this.searchSuggestionIndex >= this.searchSuggestions.size()) {
            this.searchSuggestionIndex = 0;
         }
      }
   }

   private void applySearchSuggestion(int index) {
      if (index >= 0 && index < this.searchSuggestions.size()) {
         this.searchText = this.searchSuggestions.get(index);
         this.searchCursor = this.searchText.length();
         this.clearSearchSelection();
         this.searchSuggestionIndex = index;
      }
   }

   private void syncSearchCursor() {
      this.searchCursor = MathHelper.clamp(this.searchCursor, 0, this.searchText.length());
   }

   private void clearSearchSelection() {
      this.searchSelectionStart = -1;
      this.searchSelectionEnd = -1;
   }

   private boolean hasSearchSelection() {
      return this.searchSelectionStart != -1 && this.searchSelectionEnd != -1 && this.searchSelectionStart != this.searchSelectionEnd;
   }

   private int getSearchSelectionStart() {
      return Math.min(this.searchSelectionStart, this.searchSelectionEnd);
   }

   private int getSearchSelectionEnd() {
      return Math.max(this.searchSelectionStart, this.searchSelectionEnd);
   }

   private String getSearchSelectedText() {
      return this.searchText.substring(this.getSearchSelectionStart(), this.getSearchSelectionEnd());
   }

   private void deleteSearchSelection() {
      if (this.hasSearchSelection()) {
         int start = this.getSearchSelectionStart();
         int end = this.getSearchSelectionEnd();
         this.searchText = this.searchText.substring(0, start) + this.searchText.substring(end);
         this.searchCursor = start;
         this.clearSearchSelection();
      }
   }

   private void replaceSearchText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.searchText.length()) {
         end = this.searchText.length();
      }

      if (start > end) {
         start = end;
      }

      String next;
      if ((next = this.searchText.substring(0, start) + replacement + this.searchText.substring(end)).length() > 40) {
         next = next.substring(0, 40);
      }

      this.searchText = next;
      this.searchCursor = Math.min(start + replacement.length(), this.searchText.length());
      this.clearSearchSelection();
      this.searchSuggestionIndex = 0;
   }

   private void selectAllSearchText() {
      this.searchSelectionStart = 0;
      this.searchSelectionEnd = this.searchText.length();
      this.searchCursor = this.searchText.length();
   }

   private int getSearchCursorIndexAt(float mouseX, float textStartX) {
      FontRenderer font = Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM);
      float relativeX = mouseX - textStartX;

      for (int i = 0; i <= this.searchText.length(); i++) {
         if (font.getStringWidth(this.searchText.substring(0, i)) > relativeX) {
            return i;
         }
      }

      return this.searchText.length();
   }

   private void clearSearchField() {
      this.searchText = "";
      this.searchTextScroll = 0.0F;
      this.searchCursor = 0;
      this.clearSearchSelection();
      this.searchSuggestions.clear();
      this.searchSuggestionIndex = -1;
   }

   private boolean handleSearchKeyPressed(int keyCode) {
      this.updateSearchSuggestions();
      if (Screen.hasControlDown()) {
         switch (keyCode) {
            case 65:
               this.selectAllSearchText();
               return true;
            case 67:
               if (this.hasSearchSelection()) {
                  GLFW.glfwSetClipboardString(window.getHandle(), this.getSearchSelectedText());
               }

               return true;
            case 86:
               String clipboard = GLFW.glfwGetClipboardString(window.getHandle());
               if (clipboard != null) {
                  this.deleteSearchSelection();
                  this.replaceSearchText(this.searchCursor, this.searchCursor, clipboard);
               }

               return true;
            case 88:
               if (this.hasSearchSelection()) {
                  GLFW.glfwSetClipboardString(window.getHandle(), this.getSearchSelectedText());
                  this.deleteSearchSelection();
               }

               return true;
         }
      }

      switch (keyCode) {
         case 256:
            this.searchFieldFocused = false;
            this.clearSearchField();
            return true;
         case 257:
            if (!this.searchSuggestions.isEmpty()) {
               this.applySearchSuggestion(this.searchSuggestionIndex < 0 ? 0 : this.searchSuggestionIndex);
            }

            return true;
         case 258:
            if (!this.searchSuggestions.isEmpty()) {
               this.applySearchSuggestion(this.searchSuggestionIndex < 0 ? 0 : this.searchSuggestionIndex);
            }

            return true;
         case 259:
            if (this.hasSearchSelection()) {
               this.deleteSearchSelection();
            } else if (this.searchCursor > 0) {
               this.searchText = this.searchText.substring(0, this.searchCursor - 1) + this.searchText.substring(this.searchCursor);
               this.searchCursor--;
            }

            return true;
         case 260:
         case 266:
         case 267:
         default:
            return true;
         case 261:
            if (this.hasSearchSelection()) {
               this.deleteSearchSelection();
            } else if (this.searchCursor < this.searchText.length()) {
               this.searchText = this.searchText.substring(0, this.searchCursor) + this.searchText.substring(this.searchCursor + 1);
            }

            return true;
         case 262:
            if (this.searchCursor < this.searchText.length()) {
               this.searchCursor++;
            }

            if (Screen.hasShiftDown()) {
               if (this.searchSelectionStart == -1) {
                  this.searchSelectionStart = this.searchCursor - 1;
               }

               this.searchSelectionEnd = this.searchCursor;
            } else {
               this.clearSearchSelection();
            }

            return true;
         case 263:
            if (this.searchCursor > 0) {
               this.searchCursor--;
            }

            if (Screen.hasShiftDown()) {
               if (this.searchSelectionStart == -1) {
                  this.searchSelectionStart = this.searchCursor + 1;
               }

               this.searchSelectionEnd = this.searchCursor;
            } else {
               this.clearSearchSelection();
            }

            return true;
         case 264:
            if (!this.searchSuggestions.isEmpty()) {
               this.searchSuggestionIndex = (this.searchSuggestionIndex + 1) % this.searchSuggestions.size();
            }

            return true;
         case 265:
            if (!this.searchSuggestions.isEmpty()) {
               this.searchSuggestionIndex = (this.searchSuggestionIndex - 1 + this.searchSuggestions.size()) % this.searchSuggestions.size();
            }

            return true;
         case 268:
            this.searchCursor = 0;
            if (Screen.hasShiftDown()) {
               this.searchSelectionEnd = this.searchCursor;
               if (this.searchSelectionStart == -1) {
                  this.searchSelectionStart = this.searchText.length();
               }
            } else {
               this.clearSearchSelection();
            }

            return true;
         case 269:
            this.searchCursor = this.searchText.length();
            if (Screen.hasShiftDown()) {
               this.searchSelectionEnd = this.searchCursor;
               if (this.searchSelectionStart == -1) {
                  this.searchSelectionStart = 0;
               }
            } else {
               this.clearSearchSelection();
            }

            return true;
      }
   }

   private void renderColumn(DrawContext context, MenuScreen.CategoryColumn column, float mouseX, float mouseY, float progress) {
      MatrixStack matrices = context.getMatrices();
      float x = column.x;
      float y = column.y;
      column.updateScroll();
      float slideProgress = this.columnSlideAnimations.getOrDefault(column.category, 0.0F);
      long delay = this.columnSlideDelays.getOrDefault(column.category, 0L);
      long timeSinceOpen = System.currentTimeMillis() - this.openTime;
      if (timeSinceOpen >= delay && slideProgress < 1.0F) {
         slideProgress = Math.min(1.0F, slideProgress + 0.06F);
         this.columnSlideAnimations.put(column.category, slideProgress);
      }

      float easedProgress;
      if (slideProgress < 1.0F) {
         float c1 = 1.70158F;
         float c3 = c1 + 1.0F;
         easedProgress = (float)(1.0 + c3 * Math.pow(slideProgress - 1.0F, 3.0) + c1 * Math.pow(slideProgress - 1.0F, 2.0));
      } else {
         easedProgress = 1.0F;
      }

      float slideOffset = (1.0F - easedProgress) * 150.0F;
      float finalY = y + slideOffset;
      float alpha = (float)Math.pow(easedProgress, 0.5);
      matrices.push();
      Calculate.setAlpha(alpha, () -> this.renderColumnContent(context, column, x, finalY, mouseX, mouseY, progress));
      matrices.pop();

      for (Module module2 : this.expandedModules.keySet()) {
         float currentProgress = this.expandAnimations.getOrDefault(module2, 0.0F);
         if (currentProgress < 1.0F) {
            float speed = 0.08F;
            currentProgress += speed;
            currentProgress = Math.min(1.0F, currentProgress);
            this.expandAnimations.put(module2, currentProgress);
         }
      }

      this.expandAnimations.keySet().removeIf(module -> {
         if (!this.expandedModules.containsKey(module)) {
            float currentProgressx = this.expandAnimations.get(module);
            if (currentProgressx > 0.0F) {
               float speedx = 0.12F;
               currentProgressx -= speedx;
               currentProgressx = Math.max(0.0F, currentProgressx);
               this.expandAnimations.put(module, currentProgressx);
               return currentProgressx <= 0.0F;
            } else {
               return true;
            }
         } else {
            return false;
         }
      });
   }

   private void renderColumnContent(DrawContext context, MenuScreen.CategoryColumn column, float x, float y, float mouseX, float mouseY, float progress) {
      MatrixStack matrices = context.getMatrices();
      if (column.category == ModuleCategory.CUSTOMIZATION) {
         this.renderCustomizationColumn(context, column, x, y, mouseX, mouseY, progress);
      } else {
         List<Module> categoryModules = this.getFilteredModuleList(column.category, false);
         float maxVisibleHeight = 198.0F;
         float totalModulesHeight = 0.0F;

         for (Module module : categoryModules) {
            totalModulesHeight += 22.0F;
            if (this.expandedModules.containsKey(module)) {
               List<AbstractSettingComponent> settingComponents = this.expandedModules.get(module);
               float expandProgress = this.expandAnimations.getOrDefault(module, 0.0F);
               float easedProgress = (float)(1.0 - Math.pow(1.0F - expandProgress, 3.0));

               for (AbstractSettingComponent component : settingComponents) {
                  totalModulesHeight += (component.height + 2.0F) * easedProgress;
               }
            }
         }

         float visibleModulesHeight = maxVisibleHeight;
         String categoryIcon = this.getCategoryIcon(column.category);
         String categoryName = column.category.getReadableName();
         float iconX = x + 10.0F;
         float iconY = y + (28.0F - Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).getStringHeight(categoryIcon)) / 2.0F;
         float iconWidth = this.getCategoryColumnIconWidth(column.category, categoryIcon);
         float textX = iconX + iconWidth + 4.0F;
         float textY = y + (28.0F - Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).getStringHeight(categoryName)) / 2.0F;
         this.drawCategoryColumnIcon(context, column.category, categoryIcon, iconX, iconY, ColorAssist.getClientColor());
         Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).drawString(matrices, categoryName, textX, textY, ThemeManager.textColor.getColor());
         float panelY = y + 28.0F + 4.0F;
         float panelHeight = maxVisibleHeight + 20.0F;
         float contentY = panelY + 10.0F;
         float contentX = x + 10.0F;
         float contentWidth = 110.0F;
         Elixir.getInstance().getScissorManager().push(matrices.peek().getPositionMatrix(), contentX, contentY, contentWidth, maxVisibleHeight);
         float moduleY = contentY - column.scrollOffset;

         for (Module modulex : categoryModules) {
            boolean moduleVisible = moduleY + 20.0F > contentY && moduleY < contentY + visibleModulesHeight;
            if (moduleVisible) {
               this.renderModule(context, modulex, contentX, moduleY, contentWidth, mouseX, mouseY, progress, contentY, contentY + visibleModulesHeight);
               if (Calculate.isHovered(mouseX, mouseY, contentX, moduleY, contentWidth, 20.0)) {
                  this.hoveredModuleName = modulex.getName();
               }
            }

            moduleY += 22.0F;
            if (this.expandedModules.containsKey(modulex)) {
               List<AbstractSettingComponent> settingComponents = this.expandedModules.get(modulex);
               float expandProgress = this.expandAnimations.getOrDefault(modulex, 0.0F);
               float easedProgress = (float)(1.0 - Math.pow(1.0F - expandProgress, 3.0));

               for (AbstractSettingComponent component : settingComponents) {
                  float settingHeight = (component.height + 2.0F) * easedProgress;
                  boolean settingVisible = moduleY < contentY + visibleModulesHeight && moduleY + settingHeight > contentY;
                  if (settingVisible && easedProgress > 0.01F) {
                     component.x = contentX;
                     component.y = moduleY;
                     component.width = contentWidth;
                     matrices.push();
                     Calculate.setAlpha(easedProgress, () -> component.render(context, (int)mouseX, (int)mouseY, 0.0F));
                     matrices.pop();
                  }

                  moduleY += settingHeight;
               }
            }
         }

         Elixir.getInstance().getScissorManager().pop();
         if (totalModulesHeight > visibleModulesHeight) {
            float scrollbarWidth = 2.0F;
            float scrollbarHeight = visibleModulesHeight / totalModulesHeight * visibleModulesHeight;
            float clampedOffset = Math.max(0.0F, Math.min(column.maxScroll, column.scrollOffset));
            float scrollbarY = contentY + clampedOffset / totalModulesHeight * visibleModulesHeight;
            rectangle.render(
               ShapeProperties.create(matrices, x + 130.0F - 10.0F - scrollbarWidth, contentY, scrollbarWidth, visibleModulesHeight)
                  .round(1.0F)
                  .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), 30.0F))
                  .build()
            );
            rectangle.render(
               ShapeProperties.create(matrices, x + 130.0F - 10.0F - scrollbarWidth, scrollbarY, scrollbarWidth, scrollbarHeight)
                  .round(1.0F)
                  .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                  .build()
            );
         }

         column.maxScroll = Math.max(0.0F, totalModulesHeight - visibleModulesHeight);
         if (column.targetScrollOffset < 0.0F) {
            column.targetScrollOffset = Calculate.interpolateSmooth(5.0, column.targetScrollOffset, 0.0F);
            if (Math.abs(column.targetScrollOffset) < 0.1F) {
               column.targetScrollOffset = 0.0F;
            }
         } else if (column.targetScrollOffset > column.maxScroll) {
            column.targetScrollOffset = Calculate.interpolateSmooth(5.0, column.targetScrollOffset, column.maxScroll);
            if (Math.abs(column.targetScrollOffset - column.maxScroll) < 0.1F) {
               column.targetScrollOffset = column.maxScroll;
            }
         }
      }
   }

   private void renderCustomizationColumn(DrawContext context, MenuScreen.CategoryColumn column, float x, float y, float mouseX, float mouseY, float progress) {
      MatrixStack matrices = context.getMatrices();
      String categoryIcon = this.getCategoryIcon(column.category);
      String categoryName = column.category.getReadableName();
      float iconX = x + 10.0F;
      float iconY = y + (28.0F - Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).getStringHeight(categoryIcon)) / 2.0F;
      float iconWidth = this.getCategoryColumnIconWidth(column.category, categoryIcon);
      float textX = iconX + iconWidth + 4.0F;
      float textY = y + (28.0F - Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).getStringHeight(categoryName)) / 2.0F;
      this.drawCategoryColumnIcon(context, column.category, categoryIcon, iconX, iconY, ColorAssist.getClientColor());
      Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).drawString(matrices, categoryName, textX, textY, ThemeManager.textColor.getColor());
      float panelY = y + 28.0F + 4.0F;
      float maxVisibleHeight = 198.0F;
      float panelHeight = maxVisibleHeight + 20.0F;
      float contentY = panelY + 10.0F;
      float contentX = x + 10.0F;
      float contentWidth = 110.0F;
      this.customizationComponent.x = contentX;
      this.customizationComponent.y = contentY;
      this.customizationComponent.width = contentWidth;
      this.customizationComponent.render(context, (int)mouseX, (int)mouseY, 0.0F);
   }

   private void renderModuleSettings(DrawContext context, Module module, float x, float y, float width, float mouseX, float mouseY, float progress) {
   }

   private String getSettingValue(Setting setting) {
      return null;
   }

   private void renderModuleLegacy(DrawContext context, Module module, float x, float y, float width, float mouseX, float mouseY, float progress) {
      MatrixStack matrices = context.getMatrices();
      boolean hovered = Calculate.isHovered(mouseX, mouseY, x, y, width, 20.0);
      boolean enabled = module.isState();
      boolean expanded = this.expandedModules.containsKey(module);
      float enabledProgress = this.getModuleEnabledAnimation(module, enabled);
      if (!enabled && !(enabledProgress > 0.01F)) {
         int bgColor = ThemeManager.BackgroundGui.getColor();
         if (hovered) {
            bgColor = Calculate.applyOpacity(bgColor, 150.0F);
         }

         if (expanded) {
            bgColor = Calculate.applyOpacity(bgColor, 80.0F);
         }

         rectangle.render(ShapeProperties.create(matrices, x, y, width, 20.0).round(4.0F).color(Calculate.applyOpacity(bgColor, 50.0F)).build());
      } else {
         float alpha = 0.3F + enabledProgress * 0.5F;
         if (expanded) {
            alpha = Math.min(1.0F, alpha + 0.1F);
         }

         rectangle.render(
            ShapeProperties.create(matrices, x, y, width, 20.0)
               .round(4.0F)
               .color(
                  ColorAssist.getClientColor(alpha), ColorAssist.getClientColor(alpha), ColorAssist.getClientColor2(alpha), ColorAssist.getClientColor2(alpha)
               )
               .build()
         );
      }

      String moduleName = module.getVisibleName();
      float textX = x + 10.0F;
      FontRenderer moduleFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      float textY = moduleFont.getCenteredY(y, 20.0F, moduleName);
      int moduleTextColor = enabled ? ThemeManager.textColor.getColor() : Calculate.applyOpacity(ThemeManager.textColor.getColor(), 70.0F);
      moduleFont.drawString(matrices, moduleName, textX, textY, moduleTextColor);
      float rightX = x + width - 10.0F;
   }

   private void renderModule(
      DrawContext context, Module module, float x, float y, float width, float mouseX, float mouseY, float progress, float clipTop, float clipBottom
   ) {
      MatrixStack matrices = context.getMatrices();
      long now = System.currentTimeMillis();
      long shakeUntil = this.moduleNoSettingsShakeUntil.getOrDefault(module, 0L);
      float shakeX = 0.0F;
      float shakeColorMix = 0.0F;
      if (shakeUntil > now) {
         float remain = (float)(shakeUntil - now) / 220.0F;
         shakeX = (float)Math.sin((float)now * 0.09F) * (2.0F * remain);
         shakeColorMix = remain;
      }

      boolean hovered = Calculate.isHovered(mouseX, mouseY, x, y, width, 20.0) && mouseY >= clipTop && mouseY <= clipBottom;
      boolean enabled = module.isState();
      boolean selected = module == this.selectedModule;
      float enabledAnim = this.getModuleEnabledAnimation(module, enabled);
      float hoverAnim = this.moduleHoverAnimations.getOrDefault(module, 0.0F);
      hoverAnim = Calculate.interpolateSmooth(7.0, hoverAnim, hovered ? 1.0F : 0.0F);
      this.moduleHoverAnimations.put(module, hoverAnim);
      float selectedAnim = this.moduleSelectAnimations.getOrDefault(module, selected ? 1.0F : 0.0F);
      selectedAnim = Calculate.interpolateSmooth(7.0, selectedAnim, selected ? 1.0F : 0.0F);
      this.moduleSelectAnimations.put(module, selectedAnim);
      float cardRound = 7.0F;
      int offBg = Calculate.applyOpacity(ThemeManager.offModuleColor.getColor(), (int)(150.0F + hoverAnim * 40.0F + selectedAnim * 30.0F));
      rectangle.render(ShapeProperties.create(matrices, x, y, width, 20.0).round(cardRound).color(offBg).build());
      if (enabledAnim > 0.002F) {
         int activeColor1 = Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(enabledAnim * 200.0F));
         int activeColor2 = Calculate.applyOpacity(ColorAssist.getClientColor2(), (int)(enabledAnim * 180.0F));
         rectangle.render(
            ShapeProperties.create(matrices, x, y, width, 20.0)
               .round(cardRound)
               .color(activeColor1, activeColor1, activeColor2, activeColor2)
               .build()
         );
      }

      int rimColor = enabled
         ? Calculate.applyOpacity(-1, (int)(45.0F + enabledAnim * 45.0F))
         : Calculate.applyOpacity(-1, (int)(15.0F + hoverAnim * 25.0F));
      rectangle.render(ShapeProperties.create(matrices, x + 4.0F, y, width - 8.0F, 1.0F).color(rimColor).build());

      if (selectedAnim > 0.01F) {
         int selBorderColor = Calculate.applyOpacity(ThemeManager.primaryColor.getColor(), (int)(selectedAnim * 160.0F));
         rectangle.render(ShapeProperties.create(matrices, x, y, 2.5F, 20.0).round(1.5F).color(selBorderColor).build());
      }

      String moduleName = module.getVisibleName();
      float textX = x + 10.0F;
      FontRenderer moduleFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      float textY = moduleFont.getCenteredY(y, 20.0F, moduleName);
      int moduleTextColor = enabled
         ? ThemeManager.textColor.getColor()
         : Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(110.0F + hoverAnim * 35.0F));
      if (shakeColorMix > 0.001F) {
         moduleTextColor = ColorAssist.lerp(shakeColorMix, moduleTextColor, -49088);
      }

      moduleFont.drawString(matrices, moduleName, textX + shakeX, textY, moduleTextColor);

      float rightX = x + width - 6.0F;
      float switchW = 18.0F;
      float switchH = 10.0F;
      float switchX = rightX - switchW;
      float switchY = y + 10.0F - switchH / 2.0F;
      int trackColor = enabled
         ? Calculate.applyOpacity(ColorAssist.getClientColor2(), (int)(180.0F + enabledAnim * 75.0F))
         : Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(40.0F + hoverAnim * 30.0F));
      rectangle.render(ShapeProperties.create(matrices, switchX, switchY, switchW, switchH).round(5.0F).color(trackColor).build());
      float thumbSize = 7.0F;
      float thumbX = switchX + 1.5F + enabledAnim * (switchW - thumbSize - 3.0F);
      float thumbY = switchY + (switchH - thumbSize) / 2.0F;
      rectangle.render(ShapeProperties.create(matrices, thumbX, thumbY, thumbSize, thumbSize).round(3.5F).color(-1).build());
      boolean isBinding = this.bindingModule == module;
      String bindText = isBinding ? "" : this.getBindText(module);
      if (isBinding) {
         int dotCount = (int)(now / 300L % 4L);
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < dotCount; i++) {
            sb.append(".");
         }

         bindText = sb.toString();
      }

      if (bindText != null || isBinding) {
         if (bindText == null) {
            bindText = "";
         }

         long bindTime = this.moduleBindAnimations.getOrDefault(module, 0L);
         float scalePop = 1.0F;
         if (now - bindTime < 300L) {
            float animProgress = (float)(now - bindTime) / 300.0F;
            scalePop = 1.0F + (float)Math.sin(animProgress * Math.PI) * 0.3F;
         }

         FontRenderer bindFont = Fonts.getSize(12, Fonts.Type.ROCKSTAR_SEMIBOLD);
         float bindTextWidth = bindFont.getStringWidth(bindText);
         float badgeWidth = Math.max(16.0F, bindTextWidth + 8.0F);
         float badgeHeight = 14.0F;
         float badgeX = rightX - badgeWidth - 8.0F;
         float badgeY = y + 10.0F - badgeHeight / 2.0F;
         int badgeColor = Calculate.applyOpacity(-16777216, (int)(60.0F - hoverAnim * 20.0F));
         if (isBinding) {
            badgeColor = Calculate.applyOpacity(ThemeManager.textColor.getColor(), 40.0F);
         }

         matrices.push();
         float centerX = badgeX + badgeWidth / 2.0F;
         float centerY = badgeY + badgeHeight / 2.0F;
         matrices.translate(centerX, centerY, 0.0);
         matrices.scale(scalePop, scalePop, 1.0F);
         matrices.translate(-centerX, -centerY, 0.0);
         rectangle.render(ShapeProperties.create(matrices, badgeX, badgeY, badgeWidth, badgeHeight).round(4.0F).color(badgeColor).build());
         bindFont.drawCenteredBoth(matrices, bindText, centerX, badgeY, badgeHeight, moduleTextColor);
         matrices.pop();
         rightX = badgeX - 6.0F;
      }

      if (module == this.selectedModule) {
         this.lastModuleX = x;
         this.lastModuleY = y;
         this.lastModuleWidth = width;
      }
   }

   private void triggerNoSettingsFeedback(Module module) {
      this.moduleNoSettingsShakeUntil.put(module, System.currentTimeMillis() + 220L);
      SoundManager.playSound(SoundManager.CRITICAL);
   }

   private void renderSearchField(DrawContext context, float mouseX, float mouseY, float progress) {
      MatrixStack matrices = context.getMatrices();
      List<ModuleCategory> visibleCategories = this.getVisibleCategories();
      float totalWidth = visibleCategories.size() * 130.0F + (visibleCategories.size() - 1) * 2.0F;
      float startX = (window.getScaledWidth() - totalWidth) / 2.0F;
      float startY = (window.getScaledHeight() - 300) / 2.0F;
      float searchWidth = 150.0F;
      float searchHeight = 20.0F;
      float searchX = (window.getScaledWidth() - searchWidth) / 2.0F;
      float searchY = startY + 28.0F + 4.0F + 198.0F + 20.0F + 20.0F;
      if (ThemeManager.isLiquidGlassTheme()) {
         Render2D.rectangleWithMask(
            matrices.peek().getPositionMatrix(),
            searchX - 4.0F,
            searchY - 2.0F,
            searchWidth + 8.0F,
            searchHeight + 4.0F,
            16.0F,
            0,
            KawaseBlur.INSTANCE.getBlurTexture()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrices, searchX - 4.0F, searchY - 2.0F, searchWidth + 8.0F, searchHeight + 4.0F)
            .round(4.0F)
            .color(ThemeManager.BackgroundGui.getColor())
            .build()
      );
      String displayText = this.searchText.isEmpty() ? "Поиск модулей..." : this.searchText;
      int searchTextColor = this.searchText.isEmpty() ? Calculate.applyOpacity(ThemeManager.textColor.getColor(), 50.0F) : ThemeManager.textColor.getColor();
      FontRenderer searchFont = Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM);
      float textY = searchFont.getCenteredY(searchY, searchHeight, displayText);
      searchFont.drawString(matrices, displayText, searchX, textY, searchTextColor);
      if (this.searchFieldFocused && !this.searchText.isEmpty()) {
         float cursorX = searchX + Fonts.getSize(14, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(this.searchText);
         float cursorH = 11.0F;
         float cursorY = searchY + (searchHeight - cursorH) / 2.0F;
         if (System.currentTimeMillis() % 1000L < 500L) {
            rectangle.render(ShapeProperties.create(matrices, cursorX + 2.0F, cursorY, 1.0, cursorH).color(ThemeManager.textColor.getColor()).build());
         }
      }
   }

   private void renderBindingPopup(DrawContext context, float progress) {
      MatrixStack matrices = context.getMatrices();
      String text = this.firstBindKey == -1 ? "Нажмите клавишу, кнопку мыши или колёсико" : "Нажмите вторую клавишу (Enter — одна клавиша)";
      String moduleName = this.bindingModule.getVisibleName();
      Object currentBind = "";
      if (this.firstBindKey != -1) {
         currentBind = BindCodes.getDisplayName(this.firstBindKey) + " + ...";
      }

      float padding = 14.0F;
      float textWidth = Math.max(
         Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(text), Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(moduleName)
      );
      if (!((String)currentBind).isEmpty()) {
         textWidth = Math.max(textWidth, Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth((String)currentBind));
      }

      float popupWidth = textWidth + padding * 2.0F + 14.0F;
      float popupHeight = ((String)currentBind).isEmpty() ? 66.0F : 80.0F;
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      float scale = 0.92F + progress * 0.08F;
      float popupX = centerX - popupWidth / 2.0F;
      float popupY = centerY - popupHeight / 2.0F;
      ClickGui clickGui = ClickGui.getInstance();
      if (ThemeManager.isLiquidGlassTheme() || clickGui != null && clickGui.shouldRenderBackgroundBlur()) {
         this.drawBlurMaskSafe(matrices, popupX, popupY, popupWidth, popupHeight, 16.0F);
      }

      matrices.push();
      matrices.translate(centerX, centerY, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-centerX, -centerY, 0.0F);
      rectangle.render(
         ShapeProperties.create(matrices, popupX, popupY, popupWidth, popupHeight)
            .round(8.0F)
            .color(Calculate.applyOpacity(ThemeManager.BackgroundGui.getColor(), (int)(180.0F + progress * 55.0F)))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrices, popupX, popupY, popupWidth, popupHeight)
            .round(8.0F)
            .color(Calculate.applyOpacity(-16777216, (int)(26.0F + progress * 25.0F)))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrices, popupX, popupY, popupWidth, 2.0)
            .round(2.0F)
            .color(Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(140.0F + progress * 110.0F)))
            .build()
      );
      float textY = popupY + padding - 1.0F;
      Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawString(
            matrices,
            text,
            popupX + (popupWidth - Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(text)) / 2.0F,
            textY + 6.0F,
            Calculate.applyOpacity(ThemeManager.textColor.getColor(), (int)(180.0F + progress * 75.0F))
         );
      if (!((String)currentBind).isEmpty()) {
         Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM)
            .drawString(
               matrices,
               (String)currentBind,
               popupX + (popupWidth - Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth((String)currentBind)) / 2.0F,
               textY + 22.0F,
               Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(200.0F + progress * 55.0F))
            );
      }

      float pillW = Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM).getStringWidth(moduleName) + 16.0F;
      float pillH = 16.0F;
      float pillX = popupX + (popupWidth - pillW) / 2.0F;
      float pillY = popupY + popupHeight - padding - pillH + 1.0F;
      rectangle.render(
         ShapeProperties.create(matrices, pillX, pillY, pillW, pillH)
            .round(6.0F)
            .color(Calculate.applyOpacity(ColorAssist.getClientColor(), (int)(70.0F + progress * 85.0F)))
            .build()
      );
      Fonts.getSize(13, Fonts.Type.ROCKSTAR_MEDIUM)
         .drawCenteredBoth(matrices, moduleName, pillX + pillW / 2.0F, pillY, pillH, ThemeManager.textColor.getColor());
      matrices.pop();
   }

   private List<ModuleCategory> getVisibleCategories() {
      ArrayList<ModuleCategory> visibleCategories = new ArrayList<>();
      ClickGui clickGui = ClickGui.getInstance();

      for (ModuleCategory cat : ModuleCategory.values()) {
         if (cat != ModuleCategory.CONFIGS && (cat != ModuleCategory.CUSTOMIZATION || clickGui == null || clickGui.shouldRenderCustomization())) {
            visibleCategories.add(cat);
         }
      }

      return visibleCategories;
   }

   private int getCategoryOrderIndex(ModuleCategory targetCategory) {
      List<ModuleCategory> categories = this.getVisibleCategories();
      int index = categories.indexOf(targetCategory);
      return index >= 0 ? index : 0;
   }

   private void drawBlurMaskSafe(MatrixStack matrices, float x, float y, float w, float h, float round) {
      if (!this.blurMaskFailed) {
         try {
            Render2D.rectangleWithMask(matrices.peek().getPositionMatrix(), x, y, w, h, round, 0, KawaseBlur.INSTANCE.getBlurTexture());
         } catch (Throwable var8) {
            this.blurMaskFailed = true;
         }
      }
   }

   private float getCategoryIconWidth(ModuleCategory category, FontRenderer iconFont, String icon) {
      return category == ModuleCategory.ESP ? 9.0F : iconFont.getStringWidth(icon);
   }

   private float getCategoryColumnIconWidth(ModuleCategory category, String icon) {
      return category == ModuleCategory.ESP ? 9.0F : Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).getStringWidth(icon);
   }

   private void drawCategorySidebarIcon(DrawContext context, ModuleCategory category, float iconX, float itemY, float itemHeight, int iconColor) {
      if (category == ModuleCategory.ESP) {
         float iconY = itemY + (itemHeight - 9.0F) / 2.0F;
         BackgroundIcons.drawEspCategory(context, BackgroundIcons.ESP, iconX, iconY, iconColor);
      } else {
         Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS)
            .drawVerticallyCentered(context.getMatrices(), this.getCategoryIcon(category), iconX, itemY, itemHeight, iconColor);
      }
   }

   private void drawCategoryColumnIcon(DrawContext context, ModuleCategory category, String icon, float iconX, float iconY, int color) {
      if (category == ModuleCategory.ESP) {
         BackgroundIcons.drawEspCategory(context, BackgroundIcons.ESP, iconX, iconY + 6.0F, color);
      } else {
         Fonts.getSize(18, Fonts.Type.ELIXIR_ICONS).drawString(context.getMatrices(), icon, iconX, iconY + 6.0F, color);
      }
   }

   private String getCategoryIcon(ModuleCategory category) {
      return switch (category) {
         case COMBAT -> "b";
         case MOVEMENT -> "d";
         case RENDER -> "e";
         case ESP -> "i";
         case PLAYER -> "c";
         case MISC -> "f";
         case CUSTOMIZATION -> "g";
         case CONFIGS -> "h";
      };
   }

   private String getCategoryLabel(ModuleCategory category) {
      return category == ModuleCategory.CUSTOMIZATION ? "Themes" : category.getReadableName();
   }

   private String getKeyName(int keyCode) {
      return BindCodes.getDisplayName(keyCode);
   }

   private boolean handleModuleBindInput(int keyCode) {
      if (this.bindingModule == null) {
         return false;
      } else if (keyCode == 256 || keyCode == 261 || keyCode == 259) {
         this.bindingModule.setKey(-1);
         this.bindingModule.setSecondKey(-1);
         this.firstBindKey = -1;
         this.moduleBindAnimations.put(this.bindingModule, System.currentTimeMillis());
         this.bindingModule = null;
         return true;
      } else if (!BindCodes.canBind(keyCode)) {
         return true;
      } else {
         this.bindingModule.setKey(keyCode);
         this.bindingModule.setSecondKey(-1);
         this.firstBindKey = -1;
         this.moduleBindAnimations.put(this.bindingModule, System.currentTimeMillis());
         this.bindingModule = null;
         return true;
      }
   }

   private String getBindText(Module module) {
      int keyCode = module.getKey();
      if (keyCode != -1 && keyCode != 0 && keyCode != -1) {
         String firstKey = this.getKeyName(keyCode);
         int secondKey = module.getSecondKey();
         if (secondKey != -1 && secondKey != -1) {
            String secondKeyName = this.getKeyName(secondKey);
            return firstKey + " + " + secondKeyName;
         } else {
            return firstKey;
         }
      } else {
         return null;
      }
   }

   public void openGui() {
      if (!Panic.isPanicMode()) {
         if (!this.initialized) {
            this.initialize();
         }

         CloseAnimationOverlay.getInstance().stop();
         this.animation.reset();
         this.animation.setDirection(Direction.FORWARDS);
         this.openTime = System.currentTimeMillis();
         SoundManager.playSound(SoundManager.OPEN_GUI);
         this.columnSlideAnimations.clear();
         this.columnSlideDelays.clear();
         int index = 0;

         for (ModuleCategory category : this.categoryColumns.keySet()) {
            this.columnSlideAnimations.put(category, 0.0F);
            this.columnSlideDelays.put(category, index * 80L);
            index++;
         }

         mc.setScreen(this);
      }
   }

   public float getScaleAnimation() {
      float progress = (float)this.animation.getOutput();
      return MathHelper.clamp(progress, 0.0F, 1.0F);
   }

   public float getGuiScale() {
      return (0.95F + 0.05F * this.getScaleAnimation()) * this.smoothGuiScale;
   }

   private float getModuleEnabledAnimation(Module module, boolean enabled) {
      float currentProgress = this.moduleEnabledAnimations.getOrDefault(module, enabled ? 1.0F : 0.0F);
      float targetProgress = enabled ? 1.0F : 0.0F;
      if (Math.abs((currentProgress = Calculate.interpolateSmooth(7.0, currentProgress, targetProgress)) - targetProgress) < 0.005F) {
         currentProgress = targetProgress;
      }

      this.moduleEnabledAnimations.put(module, currentProgress);
      return currentProgress;
   }

   private void startSettingsMorph(Module module, float rowX, float rowY, float rowWidth) {
      if (module != null) {
         this.settingsMorphModule = module;
         this.settingsMorphOriginX = rowX;
         this.settingsMorphOriginY = rowY;
         this.settingsMorphOriginW = rowWidth;
         this.settingsMorphOriginH = 20.0F;
      }
   }

   private void openModuleSettings(Module module, float rowX, float rowY, float rowWidth) {
      if (module != null && !module.settings().isEmpty()) {
         if (this.selectedModule != null && this.selectedModule != module) {
            this.expandedModules.remove(this.selectedModule);
         }

         if (this.settingsPanelClosingModule != null) {
            this.expandedModules.remove(this.settingsPanelClosingModule);
            this.settingsPanelClosingModule = null;
         }

         this.selectedModule = module;
         this.displayedSettingsModule = module;
         this.startSettingsMorph(module, rowX, rowY, rowWidth);
         this.settingsPanelAnim = 0.0F;
         this.settingsScrollOffset = 0.0F;
         this.settingsTargetScroll = 0.0F;
         if (!this.expandedModules.containsKey(module)) {
            ArrayList<AbstractSettingComponent> settingComponents = new ArrayList<>();
            this.settingComponentAdder.addSettingComponent(module.settings(), settingComponents, module);
            this.expandedModules.put(module, settingComponents);
         }

         this.expandAnimations.put(module, 1.0F);
      }
   }

   private void closeModuleSettings(Module module) {
      if (module != null) {
         this.expandAnimations.remove(module);
         this.expandedModules.remove(module);
         if (this.selectedModule == module) {
            this.selectedModule = null;
         }

         if (this.displayedSettingsModule == module) {
            this.displayedSettingsModule = null;
         }

         this.settingsPanelClosingModule = null;
         this.settingsPanelAnim = 0.0F;
         this.settingsMorphModule = null;
      }
   }

   private void clearSettingsPanelState() {
      this.selectedModule = null;
      this.displayedSettingsModule = null;
      this.settingsPanelClosingModule = null;
      this.settingsPanelAnim = 0.0F;
      this.settingsMorphModule = null;
      this.settingsScrollOffset = 0.0F;
      this.settingsTargetScroll = 0.0F;
      this.expandedModules.clear();
      this.expandAnimations.clear();
   }

   private boolean isSettingsPanelInteractive() {
      return this.selectedModule != null && this.expandedModules.containsKey(this.selectedModule) && this.settingsPanelAnim > 0.88F;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      float scale = this.getGuiScale() * this.scaleFactor;
      double transformedX = (mouseX - centerX) / scale + centerX;
      double transformedY = (mouseY - centerY) / scale + centerY;
      if (this.bindingModule != null) {
         this.handleModuleBindInput(button);
         return true;
      } else if (BindCaptureHelper.isCapturing()) {
         return this.dispatchBindCaptureMouse(transformedX, transformedY, button) ? true : true;
      } else if (windowManager.mouseClicked(transformedX, transformedY, button)) {
         return true;
      } else {
         MenuScreen.GuiLayout layout = this.getLayout();
         float cardHeight = 28.0F;
         float cardY = layout.y + layout.height - cardHeight - 8.0F;
         float arrowOpenX = layout.x + 150.0F - 20.0F;
         float arrowClosedX = layout.x - 14.0F;
         float arrowX = arrowOpenX + (arrowClosedX - arrowOpenX) * this.sidebarCollapseAnim;
         float arrowOpenY = cardY + (cardHeight - 12.0F) / 2.0F;
         float arrowClosedY = layout.y + layout.height - 35.0F;
         float arrowY = arrowOpenY + (arrowClosedY - arrowOpenY) * this.sidebarCollapseAnim;
         if (Calculate.isHovered(transformedX, transformedY, arrowX - 4.0F, arrowY - 4.0F, 20.0, 20.0) && button == 0) {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            SoundManager.playSound(SoundManager.CATEGORY_CLICK);
            return true;
         } else if (this.tooltipVisible
            && this.hoveredModule != null
            && Calculate.isHovered(transformedX, transformedY, this.tooltipX, this.tooltipY, this.tooltipW, this.tooltipH)) {
            if (!this.hoveredModule.settings().isEmpty()) {
               this.openModuleSettings(this.hoveredModule, layout.moduleListX, layout.moduleListY, layout.moduleListWidth);
               SoundManager.playSound(SoundManager.CATEGORY_CLICK);
            }

            return true;
         } else {
            boolean insideGui = Calculate.isHovered(transformedX, transformedY, layout.x, layout.y, layout.width, layout.height);
            if (!insideGui) {
               return false;
            } else {
               if (Calculate.isHovered(transformedX, transformedY, layout.x, layout.y, layout.sidebarWidth, layout.height)) {
                  float itemOpenY = layout.sidebarSearchY + layout.sidebarSearchHeight + 12.0F;
                  float itemClosedY = layout.y + 55.0F;
                  float itemY = itemOpenY + (itemClosedY - itemOpenY) * this.sidebarCollapseAnim;
                  float itemHeight = 22.0F;

                  for (ModuleCategory moduleCategory : this.getVisibleCategories()) {
                     if (Calculate.isHovered(transformedX, transformedY, layout.x + 8.0F, itemY, layout.sidebarWidth - 16.0F, itemHeight)) {
                        this.category = moduleCategory;
                        this.searchFieldFocused = false;
                        this.clearSettingsPanelState();
                        this.espPreviewDragging = false;
                        SoundManager.playSound(SoundManager.CATEGORY_CLICK);
                        return true;
                     }

                     itemY += itemHeight + 4.0F;
                  }
               }

               if (this.searchSuggestionsAnim > 0.05F
                  && Calculate.isHovered(
                     transformedX, transformedY, this.searchSuggestionsX, this.searchSuggestionsY, this.searchSuggestionsW, this.searchSuggestionsH
                  )) {
                  double var10000 = transformedY - this.searchSuggestionsY;
                  float pad = 4.0F;
                  var10000 -= 4.0F;
                  float rowH = 16.0F;
                  int clickedIndex;
                  if ((clickedIndex = (int)(var10000 / 16.0F)) >= 0 && clickedIndex < this.searchSuggestions.size()) {
                     this.applySearchSuggestion(clickedIndex);
                     this.searchFieldFocused = true;
                     return true;
                  }
               }

               if (Calculate.isHovered(
                  transformedX, transformedY, layout.sidebarSearchX, layout.sidebarSearchY, layout.sidebarSearchWidth, layout.sidebarSearchHeight
               )) {
                  this.searchFieldFocused = true;
                  this.searchCursor = this.getSearchCursorIndexAt((float)transformedX, layout.sidebarSearchX + 8.0F - this.searchTextScroll);
                  this.clearSearchSelection();
                  return true;
               } else {
                  this.searchFieldFocused = false;
                  if (this.category == ModuleCategory.CUSTOMIZATION) {
                     this.customizationComponent.x = layout.moduleListX + 10.0F;
                     this.customizationComponent.y = layout.moduleListY + 10.0F;
                     this.customizationComponent.width = layout.moduleListWidth - 20.0F;
                     if (this.customizationComponent.mouseClicked(transformedX, transformedY, button)) {
                        SoundManager.playSound(SoundManager.SETTING_INTERACT);
                        return true;
                     } else {
                        return true;
                     }
                  } else if (this.category == ModuleCategory.ESP) {
                     if (button == 0 && Calculate.isHovered(transformedX, transformedY, this.espPreviewX, this.espPreviewY, this.espPreviewW, this.espPreviewH)
                        )
                      {
                        this.espPreviewDragging = true;
                        this.espDragStartX = (float)transformedX;
                        this.espDragStartY = (float)transformedY;
                        this.espDragStartYaw = this.espPreviewYaw;
                        this.espDragStartPitch = this.espPreviewPitch;
                        return true;
                     } else if (this.isEspSettingsPanelHovered(transformedX, transformedY, layout)) {
                        return this.handleEspSettingsClick(transformedX, transformedY, button, layout) ? true : true;
                     } else {
                        return true;
                     }
                  } else {
                     MenuScreen.CategoryColumn column = this.categoryColumns
                        .getOrDefault(this.category, new MenuScreen.CategoryColumn(this.category, 0.0F, 0.0F));
                     List<Module> categoryModules = this.getFilteredModuleList(this.category, true);
                     if (Calculate.isHovered(
                        transformedX, transformedY, layout.moduleListX, layout.moduleListY, layout.moduleListWidth, layout.moduleListHeight
                     )) {
                        float moduleY = layout.moduleListY - column.scrollOffset;

                        for (Module module : categoryModules) {
                           if (moduleY >= layout.moduleListY - 20.0F
                              && moduleY < layout.moduleListY + layout.moduleListHeight
                              && Calculate.isHovered(transformedX, transformedY, layout.moduleListX, moduleY, layout.moduleListWidth, 20.0)) {
                              if (button == 0) {
                                 module.switchState();
                                 if (this.expandedModules.containsKey(module)) {
                                    this.selectedModule = module;
                                 }

                                 return true;
                              }

                              if (button == 1) {
                                 if (!module.settings().isEmpty()) {
                                    if (this.expandedModules.containsKey(module)) {
                                       this.closeModuleSettings(module);
                                       SoundManager.playSound(SoundManager.CLOSE_GUI);
                                    } else {
                                       this.openModuleSettings(module, layout.moduleListX, moduleY, layout.moduleListWidth);
                                       SoundManager.playSound(SoundManager.OPEN_GUI);
                                    }
                                 } else {
                                    this.triggerNoSettingsFeedback(module);
                                 }

                                 return true;
                              }

                              if (button == 2) {
                                 this.bindingModule = module;
                                 this.selectedModule = module;
                                 return true;
                              }
                           }

                           moduleY += 22.0F;
                        }
                     }

                     if (!Calculate.isHovered(transformedX, transformedY, layout.settingsX, layout.settingsY, layout.settingsWidth, layout.settingsHeight)) {
                        return true;
                     } else {
                        if (this.isSettingsPanelInteractive()) {
                           List<AbstractSettingComponent> settingComponents = this.expandedModules.get(this.selectedModule);
                           float f = layout.settingsX + 10.0F;
                           float contentY = layout.settingsY + 10.0F + 18.0F - this.settingsScrollOffset;
                           float contentWidth = layout.settingsWidth - 20.0F;

                           for (AbstractSettingComponent component : settingComponents) {
                              if (component.getSetting().isVisible()) {
                                 component.prepareLayout(contentWidth);
                                 component.x = f;
                                 component.y = contentY;
                                 if (component.mouseClicked(transformedX, transformedY, button)) {
                                    SoundManager.playSound(SoundManager.SETTING_INTERACT);
                                    return true;
                                 }

                                 contentY += component.height + 4.0F;
                              }
                           }
                        }

                        return true;
                     }
                  }
               }
            }
         }
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      float scale = this.getGuiScale() * this.scaleFactor;
      double transformedX = (mouseX - centerX) / scale + centerX;
      double transformedY;
      if (windowManager.mouseReleased(transformedX, transformedY = (mouseY - centerY) / scale + centerY, button)) {
         return true;
      } else {
         if (this.category == ModuleCategory.ESP && button == 0) {
            this.espPreviewDragging = false;
         }

         MenuScreen.GuiLayout layout = this.getLayout();
         if (!Calculate.isHovered(transformedX, transformedY, layout.x, layout.y, layout.width, layout.height)) {
            return false;
         } else if (this.category == ModuleCategory.CUSTOMIZATION) {
            this.customizationComponent.mouseReleased(transformedX, transformedY, button);
            return true;
         } else if (this.category == ModuleCategory.ESP) {
            this.espPreviewDragging = false;
            this.syncEspSettingsClickLayout(layout);
            this.handleEspSectionComponentReleaseFromLayout(transformedX, transformedY, button, this.getNameTagsModule());
            this.handleEspSectionComponentReleaseFromLayout(transformedX, transformedY, button, this.getEspSkeletonModule());
            this.handleEspSectionComponentReleaseFromLayout(transformedX, transformedY, button, this.getEspModule());
            return true;
         } else {
            if (this.selectedModule != null && this.expandedModules.containsKey(this.selectedModule)) {
               List<AbstractSettingComponent> settingComponents = this.expandedModules.get(this.selectedModule);
               float contentX = layout.settingsX + 10.0F;
               float contentY = layout.settingsY + 10.0F + 18.0F - this.settingsScrollOffset;
               float contentWidth = layout.settingsWidth - 20.0F;

               for (AbstractSettingComponent component : settingComponents) {
                  if (component.getSetting().isVisible()) {
                     component.prepareLayout(contentWidth);
                     component.x = contentX;
                     component.y = contentY;
                     component.mouseReleased(transformedX, transformedY, button);
                     contentY += component.height + 4.0F;
                  }
               }
            }

            return true;
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      float scale = this.getGuiScale() * this.scaleFactor;
      double transformedX = (mouseX - centerX) / scale + centerX;
      double transformedY = (mouseY - centerY) / scale + centerY;
      double transformedDeltaX = deltaX / scale;
      double transformedDeltaY = deltaY / scale;

      if (code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent.current != null
         && code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent.current.isDragging()) {
         return code.elixir.display.screens.clickgui.components.implement.settings.SliderComponent.current.mouseDragged(
            transformedX, transformedY, button, transformedDeltaX, transformedDeltaY
         );
      }

      if (windowManager.mouseDragged(
         transformedX, transformedY, button, transformedDeltaX, transformedDeltaY
      )) {
         return true;
      } else {
         MenuScreen.GuiLayout layout = this.getLayout();
         if (!Calculate.isHovered(transformedX, transformedY, layout.x, layout.y, layout.width, layout.height)) {
            return false;
         } else if (this.category == ModuleCategory.CUSTOMIZATION) {
            return this.customizationComponent.mouseDragged(transformedX, transformedY, button, transformedDeltaX, transformedDeltaY);
         } else if (this.category == ModuleCategory.ESP) {
            if (this.espPreviewDragging) {
               float yaw = this.espDragStartYaw - (float)(transformedX - this.espDragStartX) * 1.2F;
               this.espPreviewYaw = (yaw % 360.0F + 360.0F) % 360.0F;
               this.espPreviewPitch = MathHelper.clamp(this.espDragStartPitch + (float)(transformedY - this.espDragStartY) * 0.8F, -60.0F, 60.0F);
               return true;
            } else {
               this.syncEspSettingsClickLayout(layout);
               if (this.handleEspSectionComponentDragFromLayout(
                  transformedX, transformedY, button, transformedDeltaX, transformedDeltaY, this.getNameTagsModule()
               )) {
                  return true;
               } else if (this.handleEspSectionComponentDragFromLayout(
                  transformedX, transformedY, button, transformedDeltaX, transformedDeltaY, this.getEspSkeletonModule()
               )) {
                  return true;
               } else {
                  return this.handleEspSectionComponentDragFromLayout(
                        transformedX, transformedY, button, transformedDeltaX, transformedDeltaY, this.getEspModule()
                     )
                     ? true
                     : true;
               }
            }
         } else {
            if (this.selectedModule != null && this.expandedModules.containsKey(this.selectedModule)) {
               List<AbstractSettingComponent> settingComponents = this.expandedModules.get(this.selectedModule);
               float contentX = layout.settingsX + 10.0F;
               float contentY = layout.settingsY + 10.0F + 18.0F - this.settingsScrollOffset;
               float contentWidth = layout.settingsWidth - 20.0F;

               for (AbstractSettingComponent component : settingComponents) {
                  if (component.getSetting().isVisible()) {
                     component.prepareLayout(contentWidth);
                     component.x = contentX;
                     component.y = contentY;
                     if (component.mouseDragged(transformedX, transformedY, button, transformedDeltaX, transformedDeltaY)) {
                        return true;
                     }

                     contentY += component.height + 4.0F;
                  }
               }
            }

            return true;
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.bindingModule != null && verticalAmount != 0.0) {
         this.handleModuleBindInput(verticalAmount > 0.0 ? 998 : 999);
         return true;
      } else if (BindCaptureHelper.isCapturing() && verticalAmount != 0.0) {
         return BindCaptureHelper.handleScroll(verticalAmount);
      } else {
         if (this.resizeModeActive) {
            float[] levels = new float[]{0.5F, 0.7F, 0.85F, 1.0F, 1.3333F, 1.7F};
            int currentLevel = 3;

            for (int i = 0; i < levels.length; i++) {
               if (Math.abs(this.scaleFactor - levels[i]) < 0.05F) {
                  currentLevel = i;
                  break;
               }
            }

            int newLevel = currentLevel + (verticalAmount > 0.0 ? 1 : -1);
            if ((newLevel = Math.max(0, Math.min(5, newLevel))) != currentLevel) {
               this.scaleFactor = levels[newLevel];
               return true;
            }
         }

         float centerX = window.getScaledWidth() / 2.0F;
         float centerY = window.getScaledHeight() / 2.0F;
         float scale = this.getGuiScale() * this.scaleFactor;
         double transformedX = (mouseX - centerX) / scale + centerX;
         double transformedY;
         if (windowManager.mouseScrolled(transformedX, transformedY = (mouseY - centerY) / scale + centerY, verticalAmount)) {
            return true;
         } else {
            MenuScreen.GuiLayout layout = this.getLayout();
            if (Calculate.isHovered(transformedX, transformedY, layout.x, layout.y, layout.width, layout.height)) {
               if (this.category == ModuleCategory.CUSTOMIZATION) {
                  return this.customizationComponent.mouseScrolled(transformedX, transformedY, verticalAmount);
               } else if (this.category == ModuleCategory.ESP) {
                  if (this.isEspSettingsPanelHovered(transformedX, transformedY, layout)) {
                     this.settingsTargetScroll -= (float)verticalAmount * 60.0F;
                     this.settingsTargetScroll = MathHelper.clamp(this.settingsTargetScroll, -100.0F, this.settingsMaxScroll + 100.0F);
                  }

                  return true;
               } else if (Calculate.isHovered(
                  transformedX, transformedY, layout.moduleListX, layout.moduleListY, layout.moduleListWidth, layout.moduleListHeight
               )) {
                  MenuScreen.CategoryColumn column = this.categoryColumns.getOrDefault(this.category, new MenuScreen.CategoryColumn(this.category, 0.0F, 0.0F));
                  column.targetScrollOffset -= (float)verticalAmount * 60.0F;
                  column.targetScrollOffset = MathHelper.clamp(column.targetScrollOffset, -100.0F, column.maxScroll + 100.0F);
                  this.categoryColumns.put(this.category, column);
                  return true;
               } else if (Calculate.isHovered(transformedX, transformedY, layout.settingsX, layout.settingsY, layout.settingsWidth, layout.settingsHeight)) {
                  this.settingsTargetScroll -= (float)verticalAmount * 60.0F;
                  this.settingsTargetScroll = MathHelper.clamp(this.settingsTargetScroll, -100.0F, this.settingsMaxScroll + 100.0F);
                  return true;
               } else {
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.bindingModule != null) {
         return this.handleModuleBindInput(keyCode);
      } else if (BindCaptureHelper.isCapturing()) {
         return BindCaptureHelper.handleKey(keyCode, scanCode);
      } else if (this.customizationComponent.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         for (List<AbstractSettingComponent> settingComponents : this.expandedModules.values()) {
            for (AbstractSettingComponent component : settingComponents) {
               if (component.keyPressed(keyCode, scanCode, modifiers)) {
                  return true;
               }
            }
         }

         if (this.category == ModuleCategory.ESP) {
            Module espModule = this.getEspModule();
            Module skeletonModule = this.getEspSkeletonModule();
            Module nameTagsModule = this.getNameTagsModule();
            if (espModule != null) {
               for (AbstractSettingComponent componentx : this.getEspSettingsComponents(espModule)) {
                  if (componentx.keyPressed(keyCode, scanCode, modifiers)) {
                     return true;
                  }
               }
            }

            if (skeletonModule != null) {
               for (AbstractSettingComponent componentxx : this.getEspSettingsComponents(skeletonModule)) {
                  if (componentxx.keyPressed(keyCode, scanCode, modifiers)) {
                     return true;
                  }
               }
            }

            if (nameTagsModule != null) {
               for (AbstractSettingComponent componentxxx : this.getEspSettingsComponents(nameTagsModule)) {
                  if (componentxxx.keyPressed(keyCode, scanCode, modifiers)) {
                     return true;
                  }
               }
            }
         }

         return this.searchFieldFocused ? this.handleSearchKeyPressed(keyCode) : super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private boolean dispatchBindCaptureMouse(double mouseX, double mouseY, int button) {
      if (this.category == ModuleCategory.ESP) {
         MenuScreen.GuiLayout layout = this.getLayout();
         this.syncEspSettingsClickLayout(layout);
         Module espModule = this.getEspModule();
         Module skeletonModule = this.getEspSkeletonModule();
         Module nameTagsModule = this.getNameTagsModule();
         if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.nameTagsSectionLayout, nameTagsModule)) {
            return true;
         }

         if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.skeletonSectionLayout, skeletonModule)) {
            return true;
         }

         if (this.tryClickEspSectionBind(mouseX, mouseY, button, this.espSectionLayout, espModule)) {
            return true;
         }

         if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, nameTagsModule)) {
            return true;
         }

         if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, skeletonModule)) {
            return true;
         }

         if (this.handleEspSectionComponentClickFromLayout(mouseX, mouseY, button, espModule)) {
            return true;
         }
      }

      if (this.selectedModule != null && this.expandedModules.containsKey(this.selectedModule)) {
         MenuScreen.GuiLayout layoutx = this.getLayout();
         float contentX = layoutx.settingsX + 10.0F;
         float contentY = layoutx.settingsY + 10.0F + 18.0F - this.settingsScrollOffset;
         float contentWidth = layoutx.settingsWidth - 20.0F;

         for (AbstractSettingComponent component : this.expandedModules.get(this.selectedModule)) {
            if (component.getSetting().isVisible()) {
               component.prepareLayout(contentWidth);
               component.x = contentX;
               component.y = contentY;
               BindComponent bindComponent;
               if (component instanceof BindComponent && (bindComponent = (BindComponent)component).mouseClicked(mouseX, mouseY, button)) {
                  return true;
               }

               contentY += component.height + 4.0F;
            }
         }
      }

      return BindCaptureHelper.handleMouse(button);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.customizationComponent.charTyped(chr, modifiers)) {
         return true;
      } else {
         for (List<AbstractSettingComponent> settingComponents : this.expandedModules.values()) {
            for (AbstractSettingComponent component : settingComponents) {
               if (component.charTyped(chr, modifiers)) {
                  return true;
               }
            }
         }

         if (this.category == ModuleCategory.ESP) {
            Module espModule = this.getEspModule();
            Module skeletonModule = this.getEspSkeletonModule();
            Module nameTagsModule = this.getNameTagsModule();
            if (espModule != null) {
               for (AbstractSettingComponent componentx : this.getEspSettingsComponents(espModule)) {
                  if (componentx.charTyped(chr, modifiers)) {
                     return true;
                  }
               }
            }

            if (skeletonModule != null) {
               for (AbstractSettingComponent componentxx : this.getEspSettingsComponents(skeletonModule)) {
                  if (componentxx.charTyped(chr, modifiers)) {
                     return true;
                  }
               }
            }

            if (nameTagsModule != null) {
               for (AbstractSettingComponent componentxxx : this.getEspSettingsComponents(nameTagsModule)) {
                  if (componentxxx.charTyped(chr, modifiers)) {
                     return true;
                  }
               }
            }
         }

         if (!this.searchFieldFocused) {
            return super.charTyped(chr, modifiers);
         } else if (!Character.isISOControl(chr)) {
            this.deleteSearchSelection();
            if (this.searchText.length() < 40) {
               this.searchText = this.searchText.substring(0, this.searchCursor) + chr + this.searchText.substring(this.searchCursor);
               this.searchCursor++;
               this.searchSuggestionIndex = 0;
            }

            return true;
         } else {
            return true;
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      CloseAnimationOverlay.getInstance().start();
      SoundManager.playSound(SoundManager.CLOSE_GUI);
      super.close();
   }

   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setResizeModeActive(boolean resizeModeActive) {
      this.resizeModeActive = resizeModeActive;
   }

   public void setLastClickTime(long lastClickTime) {
      this.lastClickTime = lastClickTime;
   }

   public void setScaleFactor(float scaleFactor) {
      this.scaleFactor = scaleFactor;
   }

   public void setCategoryColumns(Map<ModuleCategory, MenuScreen.CategoryColumn> categoryColumns) {
      this.categoryColumns = categoryColumns;
   }

   public void setInitialized(boolean initialized) {
      this.initialized = initialized;
   }

   public void setHoveredModuleName(String hoveredModuleName) {
      this.hoveredModuleName = hoveredModuleName;
   }

   public void setBindingModule(Module bindingModule) {
      this.bindingModule = bindingModule;
   }

   public void setFirstBindKey(int firstBindKey) {
      this.firstBindKey = firstBindKey;
   }

   public void setSelectedModule(Module selectedModule) {
      this.selectedModule = selectedModule;
   }

   public void setHoveredModule(Module hoveredModule) {
      this.hoveredModule = hoveredModule;
   }

   public void setTooltipVisible(boolean tooltipVisible) {
      this.tooltipVisible = tooltipVisible;
   }

   public void setTooltipX(float tooltipX) {
      this.tooltipX = tooltipX;
   }

   public void setTooltipY(float tooltipY) {
      this.tooltipY = tooltipY;
   }

   public void setTooltipW(float tooltipW) {
      this.tooltipW = tooltipW;
   }

   public void setTooltipH(float tooltipH) {
      this.tooltipH = tooltipH;
   }

   public void setSearchText(String searchText) {
      this.searchText = searchText;
   }

   public void setSearchFieldFocused(boolean searchFieldFocused) {
      this.searchFieldFocused = searchFieldFocused;
   }

   public void setSearchTextScroll(float searchTextScroll) {
      this.searchTextScroll = searchTextScroll;
   }

   public void setSearchCursor(int searchCursor) {
      this.searchCursor = searchCursor;
   }

   public void setSearchSelectionStart(int searchSelectionStart) {
      this.searchSelectionStart = searchSelectionStart;
   }

   public void setSearchSelectionEnd(int searchSelectionEnd) {
      this.searchSelectionEnd = searchSelectionEnd;
   }

   public void setSearchSuggestionsAnim(float searchSuggestionsAnim) {
      this.searchSuggestionsAnim = searchSuggestionsAnim;
   }

   public void setSearchSuggestionIndex(int searchSuggestionIndex) {
      this.searchSuggestionIndex = searchSuggestionIndex;
   }

   public void setSearchSuggestionsX(float searchSuggestionsX) {
      this.searchSuggestionsX = searchSuggestionsX;
   }

   public void setSearchSuggestionsY(float searchSuggestionsY) {
      this.searchSuggestionsY = searchSuggestionsY;
   }

   public void setSearchSuggestionsW(float searchSuggestionsW) {
      this.searchSuggestionsW = searchSuggestionsW;
   }

   public void setSearchSuggestionsH(float searchSuggestionsH) {
      this.searchSuggestionsH = searchSuggestionsH;
   }

   public void setEspPreviewX(float espPreviewX) {
      this.espPreviewX = espPreviewX;
   }

   public void setEspPreviewY(float espPreviewY) {
      this.espPreviewY = espPreviewY;
   }

   public void setEspPreviewW(float espPreviewW) {
      this.espPreviewW = espPreviewW;
   }

   public void setEspPreviewH(float espPreviewH) {
      this.espPreviewH = espPreviewH;
   }

   public void setEspPreviewDragging(boolean espPreviewDragging) {
      this.espPreviewDragging = espPreviewDragging;
   }

   public void setEspPreviewYaw(float espPreviewYaw) {
      this.espPreviewYaw = espPreviewYaw;
   }

   public void setEspPreviewPitch(float espPreviewPitch) {
      this.espPreviewPitch = espPreviewPitch;
   }

   public void setEspDragStartX(float espDragStartX) {
      this.espDragStartX = espDragStartX;
   }

   public void setEspDragStartY(float espDragStartY) {
      this.espDragStartY = espDragStartY;
   }

   public void setEspDragStartYaw(float espDragStartYaw) {
      this.espDragStartYaw = espDragStartYaw;
   }

   public void setEspDragStartPitch(float espDragStartPitch) {
      this.espDragStartPitch = espDragStartPitch;
   }

   public void setEspPreviewEntity(LivingEntity espPreviewEntity) {
      this.espPreviewEntity = espPreviewEntity;
   }

   public void setEspSettingsLayoutReady(boolean espSettingsLayoutReady) {
      this.espSettingsLayoutReady = espSettingsLayoutReady;
   }

   public void setExpandedModules(Map<Module, List<AbstractSettingComponent>> expandedModules) {
      this.expandedModules = expandedModules;
   }

   public void setExpandAnimations(Map<Module, Float> expandAnimations) {
      this.expandAnimations = expandAnimations;
   }

   public void setSettingsScrollOffset(float settingsScrollOffset) {
      this.settingsScrollOffset = settingsScrollOffset;
   }

   public void setSettingsTargetScroll(float settingsTargetScroll) {
      this.settingsTargetScroll = settingsTargetScroll;
   }

   public void setSettingsMaxScroll(float settingsMaxScroll) {
      this.settingsMaxScroll = settingsMaxScroll;
   }

   public void setSettingsPanelAnim(float settingsPanelAnim) {
      this.settingsPanelAnim = settingsPanelAnim;
   }

   public void setLastCategory(ModuleCategory lastCategory) {
      this.lastCategory = lastCategory;
   }

   public void setCategoryFade(float categoryFade) {
      this.categoryFade = categoryFade;
   }

   public void setCategorySlideOffset(float categorySlideOffset) {
      this.categorySlideOffset = categorySlideOffset;
   }

   public void setCategorySlideDirection(float categorySlideDirection) {
      this.categorySlideDirection = categorySlideDirection;
   }

   public void setModuleEnabledAnimations(Map<Module, Float> moduleEnabledAnimations) {
      this.moduleEnabledAnimations = moduleEnabledAnimations;
   }

   public void setSidebarSearchFocusAnim(float sidebarSearchFocusAnim) {
      this.sidebarSearchFocusAnim = sidebarSearchFocusAnim;
   }

   public void setColumnSlideAnimations(Map<ModuleCategory, Float> columnSlideAnimations) {
      this.columnSlideAnimations = columnSlideAnimations;
   }

   public void setColumnSlideDelays(Map<ModuleCategory, Long> columnSlideDelays) {
      this.columnSlideDelays = columnSlideDelays;
   }

   public void setOpenTime(long openTime) {
      this.openTime = openTime;
   }

   public void setCustomizationComponent(CustomizationComponent customizationComponent) {
      this.customizationComponent = customizationComponent;
   }

   public void setSettingComponentAdder(SettingComponentAdder settingComponentAdder) {
      this.settingComponentAdder = settingComponentAdder;
   }

   public void setLastMouseX(int lastMouseX) {
      this.lastMouseX = lastMouseX;
   }

   public void setLastMouseY(int lastMouseY) {
      this.lastMouseY = lastMouseY;
   }

   public void setCurrentHoveredModule(String currentHoveredModule) {
      this.currentHoveredModule = currentHoveredModule;
   }

   public void setCurrentHoveredComponent(ModuleComponent currentHoveredComponent) {
      this.currentHoveredComponent = currentHoveredComponent;
   }

   public void setLastModuleX(float lastModuleX) {
      this.lastModuleX = lastModuleX;
   }

   public void setLastModuleY(float lastModuleY) {
      this.lastModuleY = lastModuleY;
   }

   public void setLastModuleWidth(float lastModuleWidth) {
      this.lastModuleWidth = lastModuleWidth;
   }

   public void setDisplayedSettingsModule(Module displayedSettingsModule) {
      this.displayedSettingsModule = displayedSettingsModule;
   }

   public void setSettingsPanelClosingModule(Module settingsPanelClosingModule) {
      this.settingsPanelClosingModule = settingsPanelClosingModule;
   }

   public void setSettingsMorphModule(Module settingsMorphModule) {
      this.settingsMorphModule = settingsMorphModule;
   }

   public void setSettingsMorphOriginX(float settingsMorphOriginX) {
      this.settingsMorphOriginX = settingsMorphOriginX;
   }

   public void setSettingsMorphOriginY(float settingsMorphOriginY) {
      this.settingsMorphOriginY = settingsMorphOriginY;
   }

   public void setSettingsMorphOriginW(float settingsMorphOriginW) {
      this.settingsMorphOriginW = settingsMorphOriginW;
   }

   public void setSettingsMorphOriginH(float settingsMorphOriginH) {
      this.settingsMorphOriginH = settingsMorphOriginH;
   }

   public void setBlurMaskFailed(boolean blurMaskFailed) {
      this.blurMaskFailed = blurMaskFailed;
   }

   public void setLastSearchText(String lastSearchText) {
      this.lastSearchText = lastSearchText;
   }

   public void setCachedModuleFilterKey(MenuScreen.ModuleFilterKey cachedModuleFilterKey) {
      this.cachedModuleFilterKey = cachedModuleFilterKey;
   }

   public void setCachedFilteredModules(List<Module> cachedFilteredModules) {
      this.cachedFilteredModules = cachedFilteredModules;
   }

   public List<AbstractComponent> getComponents() {
      return this.components;
   }

   public PanelManager getPanelManager() {
      return this.panelManager;
   }

   public GifTextureRenderer getWaitGifRenderer() {
      return this.waitGifRenderer;
   }

   public EaseBackIn getAnimation() {
      return this.animation;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public Random getRandom() {
      return this.random;
   }

   public boolean isResizeModeActive() {
      return this.resizeModeActive;
   }

   public long getLastClickTime() {
      return this.lastClickTime;
   }

   public float getScaleFactor() {
      return this.scaleFactor;
   }

   public Map<ModuleCategory, MenuScreen.CategoryColumn> getCategoryColumns() {
      return this.categoryColumns;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public String getHoveredModuleName() {
      return this.hoveredModuleName;
   }

   public Module getBindingModule() {
      return this.bindingModule;
   }

   public int getFirstBindKey() {
      return this.firstBindKey;
   }

   public Module getSelectedModule() {
      return this.selectedModule;
   }

   public Module getHoveredModule() {
      return this.hoveredModule;
   }

   public boolean isTooltipVisible() {
      return this.tooltipVisible;
   }

   public float getTooltipX() {
      return this.tooltipX;
   }

   public float getTooltipY() {
      return this.tooltipY;
   }

   public float getTooltipW() {
      return this.tooltipW;
   }

   public float getTooltipH() {
      return this.tooltipH;
   }

   public String getSearchText() {
      return this.searchText;
   }

   public boolean isSearchFieldFocused() {
      return this.searchFieldFocused;
   }

   public float getSearchTextScroll() {
      return this.searchTextScroll;
   }

   public int getSearchCursor() {
      return this.searchCursor;
   }

   public float getSearchSuggestionsAnim() {
      return this.searchSuggestionsAnim;
   }

   public int getSearchSuggestionIndex() {
      return this.searchSuggestionIndex;
   }

   public List<String> getSearchSuggestions() {
      return this.searchSuggestions;
   }

   public float getSearchSuggestionsX() {
      return this.searchSuggestionsX;
   }

   public float getSearchSuggestionsY() {
      return this.searchSuggestionsY;
   }

   public float getSearchSuggestionsW() {
      return this.searchSuggestionsW;
   }

   public float getSearchSuggestionsH() {
      return this.searchSuggestionsH;
   }

   public float getEspPreviewX() {
      return this.espPreviewX;
   }

   public float getEspPreviewY() {
      return this.espPreviewY;
   }

   public float getEspPreviewW() {
      return this.espPreviewW;
   }

   public float getEspPreviewH() {
      return this.espPreviewH;
   }

   public boolean isEspPreviewDragging() {
      return this.espPreviewDragging;
   }

   public float getEspPreviewYaw() {
      return this.espPreviewYaw;
   }

   public float getEspPreviewPitch() {
      return this.espPreviewPitch;
   }

   public float getEspDragStartX() {
      return this.espDragStartX;
   }

   public float getEspDragStartY() {
      return this.espDragStartY;
   }

   public float getEspDragStartYaw() {
      return this.espDragStartYaw;
   }

   public float getEspDragStartPitch() {
      return this.espDragStartPitch;
   }

   public UUID getEspPreviewUuid() {
      return this.espPreviewUuid;
   }

   public String getEspPreviewName() {
      return "Enemy";
   }

   public Map<Module, List<AbstractSettingComponent>> getEspSettingsCache() {
      return this.espSettingsCache;
   }

   public Map<Module, Integer> getEspSettingsCacheVersion() {
      return this.espSettingsCacheVersion;
   }

   public MenuScreen.EspSectionLayout getEspSectionLayout() {
      return this.espSectionLayout;
   }

   public MenuScreen.EspSectionLayout getSkeletonSectionLayout() {
      return this.skeletonSectionLayout;
   }

   public MenuScreen.EspSectionLayout getNameTagsSectionLayout() {
      return this.nameTagsSectionLayout;
   }

   public boolean isEspSettingsLayoutReady() {
      return this.espSettingsLayoutReady;
   }

   public Map<Module, List<AbstractSettingComponent>> getExpandedModules() {
      return this.expandedModules;
   }

   public Map<Module, Float> getExpandAnimations() {
      return this.expandAnimations;
   }

   public float getSettingsScrollOffset() {
      return this.settingsScrollOffset;
   }

   public float getSettingsTargetScroll() {
      return this.settingsTargetScroll;
   }

   public float getSettingsMaxScroll() {
      return this.settingsMaxScroll;
   }

   public float getSettingsPanelAnim() {
      return this.settingsPanelAnim;
   }

   public ModuleCategory getLastCategory() {
      return this.lastCategory;
   }

   public float getCategoryFade() {
      return this.categoryFade;
   }

   public float getCategorySlideOffset() {
      return this.categorySlideOffset;
   }

   public float getCategorySlideDirection() {
      return this.categorySlideDirection;
   }

   public Map<ModuleCategory, Float> getCategorySelectAnimations() {
      return this.categorySelectAnimations;
   }

   public Map<Module, Float> getModuleEnabledAnimations() {
      return this.moduleEnabledAnimations;
   }

   public Map<Module, Float> getModuleHoverAnimations() {
      return this.moduleHoverAnimations;
   }

   public Map<Module, Float> getModuleSelectAnimations() {
      return this.moduleSelectAnimations;
   }

   public Map<Module, Long> getModuleNoSettingsShakeUntil() {
      return this.moduleNoSettingsShakeUntil;
   }

   public float getSidebarSearchFocusAnim() {
      return this.sidebarSearchFocusAnim;
   }

   public Map<ModuleCategory, Float> getColumnSlideAnimations() {
      return this.columnSlideAnimations;
   }

   public Map<ModuleCategory, Long> getColumnSlideDelays() {
      return this.columnSlideDelays;
   }

   public long getOpenTime() {
      return this.openTime;
   }

   public CustomizationComponent getCustomizationComponent() {
      return this.customizationComponent;
   }

   public SettingComponentAdder getSettingComponentAdder() {
      return this.settingComponentAdder;
   }

   public int getLastMouseX() {
      return this.lastMouseX;
   }

   public int getLastMouseY() {
      return this.lastMouseY;
   }

   public String getCurrentHoveredModule() {
      return this.currentHoveredModule;
   }

   public ModuleComponent getCurrentHoveredComponent() {
      return this.currentHoveredComponent;
   }

   public float getLastModuleX() {
      return this.lastModuleX;
   }

   public float getLastModuleY() {
      return this.lastModuleY;
   }

   public float getLastModuleWidth() {
      return this.lastModuleWidth;
   }

   public Module getDisplayedSettingsModule() {
      return this.displayedSettingsModule;
   }

   public Module getSettingsPanelClosingModule() {
      return this.settingsPanelClosingModule;
   }

   public Module getSettingsMorphModule() {
      return this.settingsMorphModule;
   }

   public float getSettingsMorphOriginX() {
      return this.settingsMorphOriginX;
   }

   public float getSettingsMorphOriginY() {
      return this.settingsMorphOriginY;
   }

   public float getSettingsMorphOriginW() {
      return this.settingsMorphOriginW;
   }

   public float getSettingsMorphOriginH() {
      return this.settingsMorphOriginH;
   }

   public boolean isBlurMaskFailed() {
      return this.blurMaskFailed;
   }

   public String getLastSearchText() {
      return this.lastSearchText;
   }

   public MenuScreen.ModuleFilterKey getCachedModuleFilterKey() {
      return this.cachedModuleFilterKey;
   }

   public List<Module> getCachedFilteredModules() {
      return this.cachedFilteredModules;
   }

   private static class CategoryColumn {
      ModuleCategory category;
      float x;
      float y;
      float scrollOffset = 0.0F;
      float targetScrollOffset = 0.0F;
      float maxScroll = 0.0F;
      EaseBackIn scrollAnimation = new EaseBackIn(200, 1.0, 1.5F);

      CategoryColumn(ModuleCategory category, float x, float y) {
         this.category = category;
         this.x = x;
         this.y = y;
      }

      void updateScroll() {
         this.scrollAnimation.setDirection(Direction.FORWARDS);
         this.scrollOffset = Math.abs(this.scrollOffset - this.targetScrollOffset) > 0.1F
            ? Calculate.interpolateSmooth(5.0, this.scrollOffset, this.targetScrollOffset)
            : this.targetScrollOffset;
      }
   }

   public interface ClearableClickGuiInput extends MenuScreen.ClickGuiInput {
      void clearText();
   }

   public interface ClickGuiInput {
      void setTyping(boolean var1);

      boolean isTyping();
   }

   private static class EspSectionLayout {
      float sectionStartY = -1.0F;
      float sectionEndY = -1.0F;
      float headerX;
      float headerY;
      float headerW;
      float headerH;
      float toggleX;
      float toggleY;
      float toggleW;
      float toggleH;
      float bindPillX;
      float bindPillY;
      float bindPillW;
      float bindPillH;
      String sectionTitle = "";
   }

   private static class GuiLayout {
      final float x;
      final float y;
      final float width;
      final float height;
      final float sidebarWidth;
      final float contentX;
      final float contentY;
      final float contentWidth;
      final float contentHeight;
      final float headerX;
      final float headerY;
      final float headerWidth;
      final float headerHeight;
      final float moduleListX;
      final float moduleListY;
      final float moduleListWidth;
      final float moduleListHeight;
      final float settingsX;
      final float settingsY;
      final float settingsWidth;
      final float settingsHeight;
      final float sidebarSearchX;
      final float sidebarSearchY;
      final float sidebarSearchWidth;
      final float sidebarSearchHeight;

      GuiLayout(
         float x,
         float y,
         float width,
         float height,
         float sidebarWidth,
         float contentX,
         float contentY,
         float contentWidth,
         float contentHeight,
         float headerX,
         float headerY,
         float headerWidth,
         float headerHeight,
         float moduleListX,
         float moduleListY,
         float moduleListWidth,
         float moduleListHeight,
         float settingsX,
         float settingsY,
         float settingsWidth,
         float settingsHeight,
         float sidebarSearchX,
         float sidebarSearchY,
         float sidebarSearchWidth,
         float sidebarSearchHeight
      ) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.sidebarWidth = sidebarWidth;
         this.contentX = contentX;
         this.contentY = contentY;
         this.contentWidth = contentWidth;
         this.contentHeight = contentHeight;
         this.headerX = headerX;
         this.headerY = headerY;
         this.headerWidth = headerWidth;
         this.headerHeight = headerHeight;
         this.moduleListX = moduleListX;
         this.moduleListY = moduleListY;
         this.moduleListWidth = moduleListWidth;
         this.moduleListHeight = moduleListHeight;
         this.settingsX = settingsX;
         this.settingsY = settingsY;
         this.settingsWidth = settingsWidth;
         this.settingsHeight = settingsHeight;
         this.sidebarSearchX = sidebarSearchX;
         this.sidebarSearchY = sidebarSearchY;
         this.sidebarSearchWidth = sidebarSearchWidth;
         this.sidebarSearchHeight = sidebarSearchHeight;
      }
   }

   private void renderSnowflakes(DrawContext context, float alphaMultiplier) {
      float screenW = (float)window.getScaledWidth();
      float screenH = (float)window.getScaledHeight();

      if (this.guiSnowflakes.isEmpty()) {
         for (int i = 0; i < 75; i++) {
            this.guiSnowflakes.add(new MenuScreen.GuiSnowflake(this.random.nextFloat() * screenW, this.random.nextFloat() * screenH));
         }
      }

      long now = System.currentTimeMillis();
      if (this.lastSnowUpdateTime < 0L) {
         this.lastSnowUpdateTime = now;
      }
      float dt = Math.min((float)(now - this.lastSnowUpdateTime) / 1000.0F, 0.05F);
      this.lastSnowUpdateTime = now;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

      for (MenuScreen.GuiSnowflake sf : this.guiSnowflakes) {
         sf.update(dt, screenW, screenH);
         int a = (int)(sf.alpha * alphaMultiplier * 255.0F);
         if (a <= 0) continue;
         int color = (a << 24) | 0x00F0F8FF;

         float half = sf.size * 0.5F;
         buffer.vertex(mat, sf.x - half, sf.y - half, 0.0F).color(color);
         buffer.vertex(mat, sf.x - half, sf.y + half, 0.0F).color(color);
         buffer.vertex(mat, sf.x + half, sf.y + half, 0.0F).color(color);
         buffer.vertex(mat, sf.x + half, sf.y - half, 0.0F).color(color);
      }

      code.elixir.utils.display.render.geometry.BufferDraw.draw(buffer);

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

   public static class GuiSnowflake {
      public float x;
      public float y;
      public float speedY;
      public float size;
      public float alpha;
      public float swayOffset;
      public float swaySpeed;
      public float swayAmp;

      public GuiSnowflake(float startX, float startY) {
         this.x = startX;
         this.y = startY;
         Random r = new Random();
         this.speedY = 22.0F + r.nextFloat() * 45.0F;
         this.size = 1.8F + r.nextFloat() * 3.8F;
         this.alpha = 0.35F + r.nextFloat() * 0.55F;
         this.swayOffset = r.nextFloat() * 6.28F;
         this.swaySpeed = 1.2F + r.nextFloat() * 2.2F;
         this.swayAmp = 8.0F + r.nextFloat() * 15.0F;
      }

      public void update(float dt, float width, float height) {
         this.swayOffset += this.swaySpeed * dt;
         float sway = (float)Math.sin(this.swayOffset) * this.swayAmp;
         this.x += sway * dt;
         this.y += this.speedY * dt;

         if (this.y > height + 8.0F) {
            this.y = -8.0F;
            this.x = new Random().nextFloat() * width;
         }
         if (this.x < -10.0F) this.x = width + 5.0F;
         if (this.x > width + 10.0F) this.x = -5.0F;
      }
   }

   private record ModuleFilterKey(String query, ModuleCategory category, boolean crossCategorySearch) {
   }
}
