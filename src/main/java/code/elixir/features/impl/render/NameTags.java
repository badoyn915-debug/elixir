package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.emotions.ClientBadgeRenderer;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.features.impl.combat.AntiBot;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.PlainTextContent.Literal;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.joml.Vector4d;

public class NameTags extends Module {
   private static final float TAG_PADDING_X = 2.0F;
   private static final float TAG_PADDING_TOP = 1.0F;
   private static final float TAG_PADDING_BOTTOM = 0.5F;
   private static final float TAG_BG_ROUND = 1.0F;
   private static final int TAG_BG_COLOR = new Color(27, 27, 30, 150).getRGB();
   private static final float ARMOR_SLOT_SIZE = 14.0F;
   private static final float ARMOR_SLOT_GAP = 1.0F;
   private static final float ARMOR_ITEM_SCALE = 0.65F;
   private static final float HAND_TAG_GAP = 2.0F;
   private static final int KAWASE_BLUR_STEPS = 3;
   private static final float KAWASE_BLUR_OFFSET = 1.0F;
   private static boolean blurCapturedThisFrame;
   private final List<PlayerEntity> players = new ArrayList<>();
   private final Map<UUID, MutableText> playerTextCache = new HashMap<>();
   private final Map<UUID, Identifier> playerSkinCache = new HashMap<>();
   private final ArrayList<ItemStack> armorScratch = new ArrayList<>(6);
   private final ArrayList<NameTags.PlayerTagLayout> playerLayouts = new ArrayList<>(64);
   private final ArrayList<NameTags.GenericTagLayout> genericLayouts = new ArrayList<>(128);
   private final Vector4d vecScratch = new Vector4d();
   private Box entitySearchBox = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   public static MultiSelectSetting entityType = new MultiSelectSetting("Показывать", "Сущности, которые будут отображаться")
      .value("Игроков", "Мобов", "Животных", "Предметы", "Динамит", "Друзья")
      .selected("Игроков", "Предметы");
   static MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки", "Настройки для игроков")
      .value("Предметы", "Предметы в руках")
      .selected("Предметы", "Предметы в руках")
      .visible(() -> entityType.isSelected("Игроков") || entityType.isSelected("Мобов") || entityType.isSelected("Животных") || entityType.isSelected("Друзья"));
   static SelectSetting fontMode = new SelectSetting("Шрифт", "Выбор шрифта для неймтегов").value("Новый", "Стандартный").selected("Новый");
   static BooleanSetting blurBackground = new BooleanSetting("Блюр", "Размытый фон неймтегов").setValue(false);
   private static final Map<String, NameTags.PrefixInfo> DONAT_PREFIXES = new HashMap<>();

   public static NameTags getInstance() {
      return Instance.get(NameTags.class);
   }

   public NameTags() {
      super("NameTags", "NameTags", ModuleCategory.ESP);
      this.setup(new Setting[]{entityType, playerSetting, fontMode, blurBackground});
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent e) {
      this.players.clear();
      this.playerTextCache.clear();
      this.playerSkinCache.clear();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.players.clear();
      this.playerTextCache.clear();
      this.playerSkinCache.clear();
      if (mc.world != null) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))) {
               this.players.add(player);
               boolean friend = FriendUtils.isFriend(player);
               this.playerTextCache.put(player.getUuid(), this.buildPlayerText(player, friend));
               this.cachePlayerSkin(player);
            }
         }
      }
   }

   private void cachePlayerSkin(PlayerEntity player) {
      Identifier skin = null;
      if (player instanceof AbstractClientPlayerEntity clientPlayer) {
         skin = clientPlayer.getSkinTextures().texture();
      }

      PlayerListEntry entry;
      if (skin == null
         && mc.getNetworkHandler() != null
         && (entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid())) != null
         && entry.getSkinTextures() != null) {
         skin = entry.getSkinTextures().texture();
      }

      if (skin == null) {
         skin = DefaultSkinHelper.getSkinTextures(player.getUuid()).texture();
      }

      this.playerSkinCache.put(player.getUuid(), skin);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (e != null && e.getDrawContext() != null && mc.world != null && mc.player != null) {
         if (!(mc.currentScreen instanceof MenuScreen)) {
            DrawContext context = e.getDrawContext();
            MatrixStack matrix = context.getMatrices();
            boolean useCustomFont = fontMode.isSelected("Новый");
            FontRenderer font = useCustomFont ? Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold) : null;
            FontRenderer bigFont = useCustomFont ? Fonts.getSize(15, Fonts.Type.SuisseIntlMedium) : null;
            boolean showPlayers = entityType.isSelected("Игроков");
            boolean showFriends = entityType.isSelected("Друзья");
            boolean showMobs = entityType.isSelected("Мобов");
            boolean showAnimals = entityType.isSelected("Животных");
            boolean showItems = entityType.isSelected("Предметы");
            boolean showTnt = entityType.isSelected("Динамит");
            boolean showArmor = playerSetting.isSelected("Предметы");
            boolean showHands = playerSetting.isSelected("Предметы в руках");
            boolean aresMine = Network.isAresMine();
            boolean useBlur = blurBackground.isValue();
            if (showPlayers || showFriends || showMobs || showAnimals || showItems || showTnt) {
               blurCapturedThisFrame = false;
               RenderSystem.disableDepthTest();
               this.playerLayouts.clear();
               this.genericLayouts.clear();
               double renderDistance = Math.sqrt(Projection.getMaxRenderDistanceSq());
               this.entitySearchBox = mc.player.getBoundingBox().expand(renderDistance);
               if (showPlayers || showFriends) {
                  this.collectPlayerLayouts(showPlayers, showFriends, showArmor, showHands, useCustomFont, font, aresMine);
               }

               if (showMobs || showAnimals || showItems || showTnt) {
                  this.collectWorldLayouts(showMobs, showAnimals, showItems, showTnt, showArmor, showHands, useCustomFont, font, bigFont);
               }

               if (!useBlur) {
                  Rectangle.beginBatch();
               }

               for (NameTags.PlayerTagLayout playerTagLayout : this.playerLayouts) {
                  this.queuePlayerBackgrounds(matrix, playerTagLayout, showArmor, showHands, useCustomFont, font, aresMine);
               }

               for (NameTags.GenericTagLayout genericTagLayout : this.genericLayouts) {
                  this.queueGenericBackgrounds(matrix, genericTagLayout, showArmor, showHands, useCustomFont, font);
               }

               if (!useBlur) {
                  Rectangle.endBatch();
               }

               RenderSystem.enableBlend();

               for (NameTags.PlayerTagLayout playerTagLayout : this.playerLayouts) {
                  this.drawPlayerContent(context, matrix, playerTagLayout, useCustomFont, font, aresMine);
               }

               for (NameTags.GenericTagLayout genericTagLayout : this.genericLayouts) {
                  this.drawGenericContent(context, matrix, genericTagLayout, useCustomFont, font, bigFont, showArmor, showHands);
               }

               RenderSystem.enableDepthTest();
            }
         }
      }
   }

   private void collectPlayerLayouts(
      boolean showPlayers, boolean showFriends, boolean showArmor, boolean showHands, boolean useCustomFont, FontRenderer font, boolean aresMine
   ) {
      for (PlayerEntity player : this.players) {
         if (player != null && (player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))) {
            boolean friend = FriendUtils.isFriend(player);
            if ((showPlayers || showFriends && friend)
               && Projection.isWithinRenderDistance(player)
               && Projection.canSee(player.getBoundingBox())
               && Projection.fillVector4D(player, this.vecScratch)) {
               MutableText text = this.playerTextCache.get(player.getUuid());
               if (text == null) {
                  text = this.buildPlayerText(player, friend);
               }

               NameTags.PlayerTagLayout layout = new NameTags.PlayerTagLayout();
               layout.player = player;
               layout.friend = friend;
               layout.text = text;
               layout.centerX = Projection.centerX(this.vecScratch);
               layout.topY = this.vecScratch.y - 2.0;
               layout.vecY = this.vecScratch.y;
               layout.vecW = this.vecScratch.w;
               layout.skin = this.playerSkinCache.get(player.getUuid());
               layout.badgeWidth = this.getClientBadgeExtraWidth(player, aresMine ? 8.0F : 6.5F, 2.0F);
               layout.aresMine = aresMine;
               this.computePlayerTagMetrics(layout, useCustomFont, font, aresMine);
               if (showHands && useCustomFont && font != null) {
                  this.computeHandLayouts(layout, font);
               }

               this.playerLayouts.add(layout);
            }
         }
      }
   }

   private void collectWorldLayouts(
      boolean showMobs,
      boolean showAnimals,
      boolean showItems,
      boolean showTnt,
      boolean showArmor,
      boolean showHands,
      boolean useCustomFont,
      FontRenderer font,
      FontRenderer bigFont
   ) {
      if (showMobs) {
         for (MobEntity mob : mc.world.getEntitiesByClass(MobEntity.class, this.entitySearchBox, entity -> !(entity instanceof AnimalEntity))) {
            this.collectLivingLayout(mob, showArmor, showHands, useCustomFont, font);
         }
      }

      if (showAnimals) {
         for (Object animal : mc.world.getEntitiesByClass(AnimalEntity.class, this.entitySearchBox, entity -> true)) {
            this.collectLivingLayout((LivingEntity)animal, showArmor, showHands, useCustomFont, font);
         }
      }

      if (showItems) {
         List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, this.entitySearchBox, entity -> true);
         items.sort(Comparator.comparing(ent -> {
            ItemStack stackx = ent.getStack();
            return stackx.getName().getContent().toString().equals("empty");
         }));

         for (ItemEntity item : items) {
            if (Projection.isWithinRenderDistance(item) && Projection.canSee(item.getBoundingBox()) && Projection.fillVector4D(item, this.vecScratch)) {
               ItemStack stack = item.getStack();
               Text text = stack.getName();
               if (stack.getCount() > 1) {
                  text = text.copy().append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
               }

               NameTags.GenericTagLayout layout = new NameTags.GenericTagLayout();
               layout.type = NameTags.GenericTagLayout.Type.ITEM;
               layout.text = text;
               layout.centerX = Projection.centerX(this.vecScratch);
               layout.topY = this.vecScratch.y;
               layout.vecY = this.vecScratch.y;
               layout.vecW = this.vecScratch.w;
               layout.useBigFont = text.getContent().toString().equals("empty");
               this.computeSimpleTagMetrics(layout, useCustomFont, layout.useBigFont ? bigFont : font);
               this.genericLayouts.add(layout);
            }
         }
      }

      if (showTnt) {
         for (TntEntity tnt : mc.world.getEntitiesByClass(TntEntity.class, this.entitySearchBox, entity -> true)) {
            if (Projection.isWithinRenderDistance(tnt) && Projection.canSee(tnt.getBoundingBox()) && Projection.fillVector4D(tnt, this.vecScratch)) {
               NameTags.GenericTagLayout layout = new NameTags.GenericTagLayout();
               layout.type = NameTags.GenericTagLayout.Type.TNT;
               layout.text = tnt.getStyledDisplayName();
               layout.centerX = Projection.centerX(this.vecScratch);
               layout.topY = this.vecScratch.y;
               layout.vecY = this.vecScratch.y;
               layout.vecW = this.vecScratch.w;
               this.computeSimpleTagMetrics(layout, useCustomFont, font);
               this.genericLayouts.add(layout);
            }
         }
      }
   }

   private void collectLivingLayout(LivingEntity living, boolean showArmor, boolean showHands, boolean useCustomFont, FontRenderer font) {
      if (Projection.isWithinRenderDistance(living) && Projection.canSee(living.getBoundingBox())) {
         if (Projection.fillVector4D(living, this.vecScratch)) {
            NameTags.GenericTagLayout layout = new NameTags.GenericTagLayout();
            layout.type = NameTags.GenericTagLayout.Type.LIVING;
            layout.living = living;
            layout.text = this.buildLivingText(living);
            layout.centerX = Projection.centerX(this.vecScratch);
            layout.topY = this.vecScratch.y - 2.0;
            layout.vecY = this.vecScratch.y;
            layout.vecW = this.vecScratch.w;
            this.computeSimpleTagMetrics(layout, useCustomFont, font);
            if (showHands && useCustomFont && font != null) {
               this.computeHandLayouts(layout, font);
            }

            this.genericLayouts.add(layout);
         }
      }
   }

   private void computePlayerTagMetrics(NameTags.PlayerTagLayout layout, boolean useCustomFont, FontRenderer font, boolean aresMine) {
      if (aresMine) {
         float width = mc.textRenderer.getWidth(layout.text);
         float textHeight = 9.0F;
         float headSize = 10.0F;
         float badgeSize = 8.0F;
         layout.totalWidth = width + headSize + 4.0F + layout.badgeWidth;
         layout.boxHeight = tagBoxHeight(textHeight);
         layout.contentHeight = Math.max(headSize, textHeight);
         layout.headSize = headSize;
         layout.badgeSize = badgeSize;
         layout.textHeight = textHeight;
         layout.posX = (float)(layout.centerX - layout.totalWidth / 2.0F);
         layout.boxTop = (float)(layout.topY - layout.boxHeight);
         layout.vanilla = true;
      } else {
         float headSize = 8.0F;
         float badgeSize = 6.5F;
         layout.headSize = headSize;
         layout.badgeSize = badgeSize;
         if (useCustomFont && font != null) {
            layout.textHeight = visualTextHeight(font, layout.text);
            layout.totalWidth = font.getStringWidth(layout.text) + headSize + 4.0F + layout.badgeWidth;
            layout.contentHeight = Math.max(headSize, layout.textHeight);
            layout.boxHeight = tagBoxHeight(layout.contentHeight);
            layout.posX = (float)(layout.centerX - layout.totalWidth / 2.0F);
            layout.boxTop = (float)(layout.topY - layout.boxHeight);
            layout.vanilla = false;
         } else {
            Objects.requireNonNull(mc.textRenderer);
            layout.textHeight = 9.0F;
            layout.totalWidth = mc.textRenderer.getWidth(layout.text) + headSize + 4.0F + layout.badgeWidth;
            layout.contentHeight = Math.max(headSize, layout.textHeight);
            layout.boxHeight = tagBoxHeight(layout.contentHeight);
            layout.posX = (float)(layout.centerX - layout.totalWidth / 2.0F);
            layout.boxTop = (float)(layout.topY - layout.boxHeight);
            layout.vanilla = true;
         }
      }
   }

   private void computeSimpleTagMetrics(NameTags.GenericTagLayout layout, boolean useCustomFont, FontRenderer font) {
      if (useCustomFont && font != null) {
         layout.textHeight = visualTextHeight(font, layout.text);
         layout.totalWidth = font.getStringWidth(layout.text);
         layout.boxHeight = tagBoxHeight(layout.textHeight);
         layout.posX = (float)(layout.centerX - layout.totalWidth / 2.0F);
         layout.boxTop = (float)(layout.topY - layout.boxHeight);
         layout.vanilla = false;
      } else {
         Objects.requireNonNull(mc.textRenderer);
         layout.textHeight = 9.0F;
         layout.totalWidth = mc.textRenderer.getWidth(layout.text);
         layout.boxHeight = tagBoxHeight(layout.textHeight);
         layout.posX = (float)(layout.centerX - layout.totalWidth / 2.0F);
         layout.boxTop = (float)(layout.topY - layout.boxHeight);
         layout.vanilla = true;
      }
   }

   private void computeHandLayouts(NameTags.PlayerTagLayout layout, FontRenderer font) {
      layout.handTags = new ArrayList<>(2);
      float posY = (float)layout.vecW + 2.0F;

      for (ItemStack stack : layout.player.getHandItems()) {
         if (!stack.isEmpty()) {
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) {
               text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            }

            float textHeight = visualTextHeight(font, text);
            float width = font.getStringWidth(text);
            float boxHeight = tagBoxHeight(textHeight);
            NameTags.HandTagLayout hand = new NameTags.HandTagLayout();
            hand.text = text;
            hand.textHeight = textHeight;
            hand.totalWidth = width;
            hand.boxHeight = boxHeight;
            hand.centerX = layout.centerX;
            hand.posX = (float)(hand.centerX - width / 2.0F);
            hand.boxTop = posY;
            layout.handTags.add(hand);
            posY += boxHeight + 2.0F;
         }
      }
   }

   private void computeHandLayouts(NameTags.GenericTagLayout layout, FontRenderer font) {
      layout.handTags = new ArrayList<>(2);
      float posY = (float)layout.vecW + 2.0F;

      for (ItemStack stack : layout.living.getHandItems()) {
         if (!stack.isEmpty()) {
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) {
               text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            }

            float textHeight = visualTextHeight(font, text);
            float width = font.getStringWidth(text);
            float boxHeight = tagBoxHeight(textHeight);
            NameTags.HandTagLayout hand = new NameTags.HandTagLayout();
            hand.text = text;
            hand.textHeight = textHeight;
            hand.totalWidth = width;
            hand.boxHeight = boxHeight;
            hand.centerX = layout.centerX;
            hand.posX = (float)(hand.centerX - width / 2.0F);
            hand.boxTop = posY;
            layout.handTags.add(hand);
            posY += boxHeight + 2.0F;
         }
      }
   }

   private void queuePlayerBackgrounds(
      MatrixStack matrix, NameTags.PlayerTagLayout layout, boolean showArmor, boolean showHands, boolean useCustomFont, FontRenderer font, boolean aresMine
   ) {
      if (showArmor) {
         this.queueArmorBackground(matrix, layout.centerX, layout.vecY, layout.player);
      }

      if (showHands && useCustomFont && font != null && layout.handTags != null) {
         for (NameTags.HandTagLayout hand : layout.handTags) {
            this.renderTagBackground(matrix, hand.posX - 2.0F, hand.boxTop, hand.totalWidth + 4.0F, hand.boxHeight);
         }
      }

      if (aresMine) {
         this.renderTagBackground(matrix, layout.posX - 2.0F, layout.boxTop, layout.totalWidth + 4.0F, layout.boxHeight);
      } else {
         this.renderTagBackground(matrix, layout.posX - 2.0F, layout.boxTop, layout.totalWidth + 4.0F, layout.boxHeight);
      }
   }

   private void queueGenericBackgrounds(
      MatrixStack matrix, NameTags.GenericTagLayout layout, boolean showArmor, boolean showHands, boolean useCustomFont, FontRenderer font
   ) {
      if (layout.type == NameTags.GenericTagLayout.Type.LIVING && showArmor) {
         this.queueArmorBackground(matrix, layout.centerX, layout.vecY, layout.living);
      }

      if (layout.type == NameTags.GenericTagLayout.Type.LIVING && showHands && useCustomFont && font != null && layout.handTags != null) {
         for (NameTags.HandTagLayout hand : layout.handTags) {
            this.renderTagBackground(matrix, hand.posX - 2.0F, hand.boxTop, hand.totalWidth + 4.0F, hand.boxHeight);
         }
      }

      this.renderTagBackground(matrix, layout.posX - 2.0F, layout.boxTop, layout.totalWidth + 4.0F, layout.boxHeight);
   }

   private void queueArmorBackground(MatrixStack matrix, double centerX, double vecY, LivingEntity entity) {
      this.armorScratch.clear();

      for (ItemStack stack : entity.getEquippedItems()) {
         if (!stack.isEmpty()) {
            this.armorScratch.add(stack);
         }
      }

      if (!this.armorScratch.isEmpty()) {
         float step = 15.0F;
         float totalWidth = this.armorScratch.size() * step - 1.0F;
         float posX = (float)(centerX - totalWidth / 2.0F);
         float posY = (float)(vecY - 8.666667F - 15.0);

         for (int i = 0; i < this.armorScratch.size(); i++) {
            this.renderTagBackground(matrix, posX + i * step, posY, 14.0F, 14.0F);
         }
      }
   }

   private void drawPlayerContent(
      DrawContext context, MatrixStack matrix, NameTags.PlayerTagLayout layout, boolean useCustomFont, FontRenderer font, boolean aresMine
   ) {
      if (playerSetting.isSelected("Предметы")) {
         this.drawArmorItems(context, layout.centerX, layout.vecY, layout.player);
      }

      if (aresMine) {
         if (layout.skin != null) {
            float headY = tagContentY(layout.boxTop, layout.contentHeight, layout.headSize);
            Render2D.drawTexture(
               context,
               layout.skin,
               layout.posX + 1.0F,
               headY,
               layout.headSize,
               layout.headSize / 2.0F,
               8,
               8,
               64,
               ColorAssist.rgba(255, 255, 255, 255),
               ColorAssist.rgba(255, 255, 255, 255)
            );
         }

         float badgeY = tagContentY(layout.boxTop, layout.contentHeight, layout.badgeSize);
         this.drawClientBadge(context, layout.player, layout.posX + layout.headSize + 2.0F, badgeY, layout.badgeSize);
         float textY = tagContentY(layout.boxTop, layout.contentHeight, layout.textHeight);
         context.drawText(
            mc.textRenderer, layout.text, (int)(layout.posX + layout.headSize + 4.0F + layout.badgeWidth), (int)textY, ColorAssist.getColor(255), false
         );
      } else {
         if (layout.skin != null) {
            float headY = tagContentY(layout.boxTop, layout.contentHeight, layout.headSize);
            Render2D.drawTexture(
               context,
               layout.skin,
               layout.posX + 1.0F,
               headY,
               layout.headSize,
               layout.headSize / 2.0F,
               8,
               8,
               64,
               ColorAssist.rgba(255, 255, 255, 255),
               ColorAssist.rgba(255, 255, 255, 255)
            );
         }

         float badgeY = tagContentY(layout.boxTop, layout.contentHeight, layout.badgeSize);
         this.drawClientBadge(context, layout.player, layout.posX + layout.headSize + 2.0F, badgeY, layout.badgeSize);
         if (useCustomFont && font != null) {
            font.drawText(
               matrix,
               layout.text,
               layout.posX + layout.headSize + 4.0F + layout.badgeWidth,
               customFontTextY(font, layout.boxTop, layout.boxHeight, layout.text)
            );
            if (layout.handTags != null) {
               for (NameTags.HandTagLayout hand : layout.handTags) {
                  font.drawText(matrix, hand.text, hand.posX, customFontTextY(font, hand.boxTop, hand.boxHeight, hand.text));
               }
            }
         } else {
            float textY = tagContentY(layout.boxTop, layout.contentHeight, layout.textHeight);
            context.drawText(mc.textRenderer, layout.text, (int)(layout.posX + layout.headSize + 4.0F + layout.badgeWidth), (int)textY, 16777215, true);
         }
      }
   }

   private void drawGenericContent(
      DrawContext context,
      MatrixStack matrix,
      NameTags.GenericTagLayout layout,
      boolean useCustomFont,
      FontRenderer font,
      FontRenderer bigFont,
      boolean showArmor,
      boolean showHands
   ) {
      if (layout.type == NameTags.GenericTagLayout.Type.LIVING && showArmor) {
         this.drawArmorItems(context, layout.centerX, layout.vecY, layout.living);
      }

      if (layout.type == NameTags.GenericTagLayout.Type.LIVING && showHands && useCustomFont && font != null && layout.handTags != null) {
         for (NameTags.HandTagLayout hand : layout.handTags) {
            font.drawText(matrix, hand.text, hand.posX, customFontTextY(font, hand.boxTop, hand.boxHeight, hand.text));
         }
      }

      FontRenderer activeFont = layout.useBigFont && bigFont != null ? bigFont : font;
      if (useCustomFont && activeFont != null) {
         activeFont.drawText(matrix, layout.text, layout.posX, customFontTextY(activeFont, layout.boxTop, layout.boxHeight, layout.text));
      } else {
         context.drawText(mc.textRenderer, layout.text, (int)layout.posX, (int)tagContentY(layout.boxTop, layout.textHeight, layout.textHeight), 16777215, true);
      }
   }

   private void drawArmorItems(DrawContext context, double centerX, double vecY, LivingEntity entity) {
      this.armorScratch.clear();

      for (ItemStack stack : entity.getEquippedItems()) {
         if (!stack.isEmpty()) {
            this.armorScratch.add(stack);
         }
      }

      if (!this.armorScratch.isEmpty()) {
         float step = 15.0F;
         float totalWidth = this.armorScratch.size() * step - 1.0F;
         float posX = (float)(centerX - totalWidth / 2.0F);
         float posY = (float)(vecY - 8.666667F - 15.0);
         float itemVisualSize = 10.4F;
         float itemOffset = (14.0F - itemVisualSize) / 2.0F - 1.0F;

         for (int i = 0; i < this.armorScratch.size(); i++) {
            float itemX = posX + i * step + itemOffset;
            float itemY = posY + itemOffset;
            Render2D.defaultDrawStack(context, this.armorScratch.get(i), itemX, itemY, false, true, 0.65F);
         }
      }
   }

   public void renderPreview(DrawContext context, MatrixStack matrix, PlayerEntity player, Vector4d vec) {
      if (context != null && matrix != null && player != null && vec != null) {
         if (entityType.isSelected("Игроков") || entityType.isSelected("Друзья") && FriendUtils.isFriend(player)) {
            boolean useCustomFont = fontMode.isSelected("Новый");
            FontRenderer font = useCustomFont ? Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold) : null;
            blurCapturedThisFrame = false;
            NameTags.PlayerTagLayout layout = new NameTags.PlayerTagLayout();
            layout.player = player;
            layout.friend = FriendUtils.isFriend(player);
            layout.text = this.buildPlayerText(player, layout.friend);
            layout.centerX = Projection.centerX(vec);
            layout.topY = vec.y - 2.0;
            layout.vecY = vec.y;
            layout.vecW = vec.w;
            layout.skin = this.playerSkinCache.getOrDefault(player.getUuid(), DefaultSkinHelper.getSkinTextures(player.getUuid()).texture());
            layout.badgeWidth = this.getClientBadgeExtraWidth(player, 6.5F, 2.0F);
            layout.aresMine = false;
            this.computePlayerTagMetrics(layout, useCustomFont, font, false);
            if (playerSetting.isSelected("Предметы в руках") && useCustomFont && font != null) {
               this.computeHandLayouts(layout, font);
            }

            if (!blurBackground.isValue()) {
               Rectangle.beginBatch();
            }

            this.queuePlayerBackgrounds(
               matrix, layout, playerSetting.isSelected("Предметы"), playerSetting.isSelected("Предметы в руках"), useCustomFont, font, false
            );
            if (!blurBackground.isValue()) {
               Rectangle.endBatch();
            }

            this.drawPlayerContent(context, matrix, layout, useCustomFont, font, false);
         }
      }
   }

   @Override
   public void deactivate() {
      this.playerTextCache.clear();
      this.playerSkinCache.clear();
      super.deactivate();
   }

   private static float tagBoxHeight(float contentHeight) {
      return contentHeight + 1.0F + 0.5F;
   }

   private static float tagContentY(float boxTop, float contentHeight, float elementHeight) {
      return boxTop + 1.0F + (contentHeight - elementHeight) / 2.0F;
   }

   private static float visualTextHeight(FontRenderer font, Text text) {
      return font.getStringHeight(text);
   }

   private static float customFontTextY(FontRenderer font, float boxTop, float boxHeight, Text text) {
      return font.getCenteredY(boxTop, boxHeight, text.getString());
   }

   private void renderTagBackground(MatrixStack matrix, float x, float y, float width, float height) {
      if (blurBackground.isValue()) {
         if (!blurCapturedThisFrame) {
            KawaseBlur.INSTANCE.render(matrix, 3, 1.0F);
            blurCapturedThisFrame = true;
         }

         Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), x, y, width, height, 1.0F, 0, KawaseBlur.INSTANCE.getBlurTexture());
         rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(1.0F).color(TAG_BG_COLOR).build());
      } else {
         rectangle.render(
            ShapeProperties.create(matrix, x, y, width, height).round(1.0F).outlineColor(new Color(33, 33, 33, 0).getRGB()).color(TAG_BG_COLOR).build()
         );
      }
   }

   private float getClientBadgeExtraWidth(PlayerEntity player, float badgeSize, float badgeGap) {
      return ClientBadgeRenderer.hasCompatibleClient(player) ? badgeSize + badgeGap : 0.0F;
   }

   private void drawClientBadge(DrawContext context, PlayerEntity player, float x, float y, float size) {
      if (ClientBadgeRenderer.hasCompatibleClient(player)) {
         ClientBadgeRenderer.drawRoundedBadge(context, x, y, size);
      }
   }

   private MutableText buildPlayerText(PlayerEntity player, boolean friend) {
      float health = PlayerInteractionHelper.getHealth(player);
      MutableText text = Text.empty();
      if (friend) {
         text.append("[" + Formatting.GREEN + "F" + Formatting.RESET + "] ");
      }

      if (AntiBot.isBot(player)) {
         text.append("[" + Formatting.DARK_RED + "BOT" + Formatting.RESET + "] ");
      }

      Text displayName = convertDisplayName(player.getDisplayName());
      text.append((Text)(friend ? colorizeText(displayName, ColorAssist.getFriendColor()) : displayName));
      if (health > 0.0F) {
         text.append(Formatting.RESET + " [" + Formatting.RED + PlayerInteractionHelper.getHealthString(health) + Formatting.RESET + "]");
      }

      return text;
   }

   private MutableText buildLivingText(LivingEntity entity) {
      float health = PlayerInteractionHelper.getHealth(entity);
      MutableText text = Text.empty();
      text.append(entity.getDisplayName());
      if (health > 0.0F) {
         text.append(Formatting.RESET + " [" + Formatting.RED + PlayerInteractionHelper.getHealthString(entity) + Formatting.RESET + "]");
      }

      return text;
   }

   private static MutableText colorizeText(Text text, int rgb) {
      MutableText result = Text.empty();
      text.visit((style, content) -> {
         if (!content.isEmpty()) {
            result.append(Text.literal(content).styled(ignored -> style.withColor(rgb)));
         }

         return Optional.empty();
      }, Style.EMPTY);
      return result;
   }

   private static Text convertDisplayName(Text original) {
      MutableText result = Text.empty();
      processTextComponent(original, result);
      return result;
   }

   private static void processTextComponent(Text component, MutableText result) {
      String content = "";
      if (component.getContent() instanceof Literal literal) {
         content = literal.string();
      }

      if (!content.isEmpty()) {
         boolean foundPrefix = false;

         for (Entry entry : DONAT_PREFIXES.entrySet()) {
            String key = (String)entry.getKey();
            int keyIndex = content.indexOf(key);
            if (keyIndex >= 0) {
               NameTags.PrefixInfo info = (NameTags.PrefixInfo)entry.getValue();
               String prefixText = info.color() + info.name();
               int afterKey = keyIndex + key.length();
               boolean needsSpace = afterKey >= content.length() || content.charAt(afterKey) != ' ';
               String before = content.substring(0, keyIndex);
               String after = afterKey < content.length() ? content.substring(afterKey) : "";
               String replaced = before + prefixText + (needsSpace ? " " : "") + after;
               result.append(Text.literal(replaced));
               foundPrefix = true;
               break;
            }
         }

         if (!foundPrefix) {
            result.append(Text.literal(content).setStyle(component.getStyle()));
         }
      }

      for (Text sibling : component.getSiblings()) {
         processTextComponent(sibling, result);
      }
   }

   static {
      DONAT_PREFIXES.put("ꔀ", new NameTags.PrefixInfo("PLAYER", Formatting.GRAY));
      DONAT_PREFIXES.put("ꔄ", new NameTags.PrefixInfo("HERO", Formatting.BLUE));
      DONAT_PREFIXES.put("ꔈ", new NameTags.PrefixInfo("TITAN", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔒ", new NameTags.PrefixInfo("AVENGER", Formatting.GREEN));
      DONAT_PREFIXES.put("ꔖ", new NameTags.PrefixInfo("OVERLORD", Formatting.AQUA));
      DONAT_PREFIXES.put("ꔠ", new NameTags.PrefixInfo("MAGISTER", Formatting.GOLD));
      DONAT_PREFIXES.put("ꔤ", new NameTags.PrefixInfo("IMPERATOR", Formatting.RED));
      DONAT_PREFIXES.put("ꔨ", new NameTags.PrefixInfo("DRAGON", Formatting.LIGHT_PURPLE));
      DONAT_PREFIXES.put("ꔲ", new NameTags.PrefixInfo("BULL", Formatting.DARK_PURPLE));
      DONAT_PREFIXES.put("ꕒ", new NameTags.PrefixInfo("RABBIT", Formatting.WHITE));
      DONAT_PREFIXES.put("ꔶ", new NameTags.PrefixInfo("TIGER", Formatting.GOLD));
      DONAT_PREFIXES.put("ꕄ", new NameTags.PrefixInfo("DRACULA", Formatting.RED));
      DONAT_PREFIXES.put("ꕖ", new NameTags.PrefixInfo("BUNNY", Formatting.WHITE));
      DONAT_PREFIXES.put("ꕀ", new NameTags.PrefixInfo("HYDRA", Formatting.DARK_GREEN));
      DONAT_PREFIXES.put("ꕈ", new NameTags.PrefixInfo("COBRA", Formatting.GREEN));
      DONAT_PREFIXES.put("ꕅ", new NameTags.PrefixInfo("VAMPIRE", Formatting.RED));
      DONAT_PREFIXES.put("ꔁ", new NameTags.PrefixInfo("MEDIA", Formatting.BLUE));
      DONAT_PREFIXES.put("ꔅ", new NameTags.PrefixInfo("YT", Formatting.RED));
      DONAT_PREFIXES.put("ꕠ", new NameTags.PrefixInfo("D.HELPER", Formatting.GREEN));
      DONAT_PREFIXES.put("ꔉ", new NameTags.PrefixInfo("HELPER", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔓ", new NameTags.PrefixInfo("ML.MODER", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔗ", new NameTags.PrefixInfo("MODER", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔡ", new NameTags.PrefixInfo("MODER+", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔥ", new NameTags.PrefixInfo("ST.MODER", Formatting.YELLOW));
      DONAT_PREFIXES.put("ꔩ", new NameTags.PrefixInfo("GL.MODER", Formatting.GOLD));
      DONAT_PREFIXES.put("ꔳ", new NameTags.PrefixInfo("ML.ADMIN", Formatting.RED));
      DONAT_PREFIXES.put("ꔷ", new NameTags.PrefixInfo("ADMIN", Formatting.DARK_RED));
   }

   private static final class GenericTagLayout {
      NameTags.GenericTagLayout.Type type;
      LivingEntity living;
      Text text;
      double centerX;
      double topY;
      double vecY;
      double vecW;
      float posX;
      float boxTop;
      float boxHeight;
      float totalWidth;
      float textHeight;
      boolean vanilla;
      boolean useBigFont;
      List<NameTags.HandTagLayout> handTags;

      static enum Type {
         LIVING,
         ITEM,
         TNT;
      }
   }

   private static final class HandTagLayout {
      MutableText text;
      double centerX;
      float posX;
      float boxTop;
      float boxHeight;
      float totalWidth;
      float textHeight;
   }

   private static final class PlayerTagLayout {
      PlayerEntity player;
      boolean friend;
      MutableText text;
      Identifier skin;
      double centerX;
      double topY;
      double vecY;
      double vecW;
      float posX;
      float boxTop;
      float boxHeight;
      float totalWidth;
      float contentHeight;
      float textHeight;
      float headSize;
      float badgeSize;
      float badgeWidth;
      boolean aresMine;
      boolean vanilla;
      List<NameTags.HandTagLayout> handTags;
   }

   private record PrefixInfo(String name, Formatting color) {
   }
}
