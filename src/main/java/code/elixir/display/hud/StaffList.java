package code.elixir.display.hud;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.common.repository.staff.StaffRepository;
import code.elixir.features.impl.render.Hud;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

public class StaffList extends AbstractDraggable {
   private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
   private static final Pattern PREFIX_MATCHES = Pattern.compile(".*(mod|мод|adm|адм|help|хелп|curat|курат|own|овн|dev|supp|сапп|yt|ют|сотруд).*", 2);
   private final List<StaffList.StaffRow> staffRows = new ArrayList<>();
   private final List<StaffList.StaffRow> renderSnapshot = new ArrayList<>();
   private final Map<String, StaffList.StaffRow> rowByName = new HashMap<>();
   private final Set<String> addedPlayers = new HashSet<>();
   private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
   private final FontRenderer fontStatus = Fonts.getSize(13, Fonts.Type.DEFAULT);
   private final FontRenderer fontName = Fonts.getSize(14, Fonts.Type.DEFAULT);

   public StaffList() {
      super("StaffList", 115, 40, 100, 23, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("StaffList") && Hud.getInstance().state
         ? !this.staffRows.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen)
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
            selected.remove("StaffList");
            Hud.getInstance();
            Hud.interfaceSettings.setSelected(selected);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean hasStaff() {
      return !this.staffRows.isEmpty();
   }

   @Override
   public void tick() {
      this.staffRows.removeIf(row -> row.anim.isFinished(Direction.BACKWARDS));
      this.updateStaffPlayers();
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      this.renderSnapshot.clear();
      this.renderSnapshot.addAll(this.staffRows);
      float posX = this.getX();
      float posY = this.getY();
      float maxElementsWidth = HudListPanel.headerMinWidth(this.fontHeader, "StaffList");
      float headerHeight = 21.0F;
      float rowHeight = 11.0F;
      boolean hasVisibleRows = false;

      for (StaffList.StaffRow row : this.renderSnapshot) {
         if (!(row.anim.getOutput().floatValue() <= 0.0F)) {
            hasVisibleRows = true;
            float elementsWidth = HudListPanel.rowWidth(this.fontName, row.player.name, 0.0F, this.fontStatus, row.player.status.label);
            maxElementsWidth = Math.max(maxElementsWidth, elementsWidth);
         }
      }

      boolean showExample = this.staffRows.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen);
      if (showExample) {
         hasVisibleRows = true;
         float elementsWidth = HudListPanel.rowWidth(this.fontName, "Admin", 0.0F, this.fontStatus, "[ON]");
         maxElementsWidth = Math.max(maxElementsWidth, elementsWidth);
      }

      float totalContentHeight = 0.0F;

      for (StaffList.StaffRow rowx : this.renderSnapshot) {
         float animValue = rowx.anim.getOutput().floatValue();
         if (animValue > 0.0F) {
            totalContentHeight += rowHeight * animValue;
         }
      }

      if (showExample) {
         totalContentHeight += rowHeight;
      }

      float totalHeight = HudListPanel.panelHeight(totalContentHeight);
      float width = maxElementsWidth;
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrix.peek().getPositionMatrix(), posX, posY, maxElementsWidth, totalHeight, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrix, posX, posY, maxElementsWidth, totalHeight).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build()
      );
      float headerTextY = HudListPanel.headerTextY(this.fontHeader, posY, "StaffList");
      this.fontHeader.drawString(matrix, "StaffList", posX + 7.0F, headerTextY, ThemeManager.textColor.getColor());
      float iconBoxW = this.fontStatus.getStringWidth("[ON]") + 6.0F;
      float iconCenterX = posX + maxElementsWidth - 6.5F - iconBoxW / 2.0F;
      BackgroundIcons.drawHudHeaderIcon(context, BackgroundIcons.STAFFLIST, this.fontHeader, "StaffList", headerTextY, iconCenterX, ColorAssist.getHudColor());
      posY = HudListPanel.contentStartY(posY);
      if (showExample) {
         this.drawStaffRow(context, matrix, posX, posY, maxElementsWidth, rowHeight, 1.0F, "Admin", StaffList.StaffStatus.ONLINE, null, true);
         posY += rowHeight;
      } else if (hasVisibleRows) {
         for (StaffList.StaffRow rowxx : this.renderSnapshot) {
            float animValue = rowxx.anim.getOutput().floatValue();
            if (!(animValue <= 0.0F)) {
               this.drawStaffRow(context, matrix, posX, posY, width, rowHeight, animValue, rowxx.player.name, rowxx.player.status, rowxx.player.uuid, false);
               posY += rowHeight * animValue;
            }
         }
      }

      this.setWidth((int)width);
      this.setHeight((int)totalHeight);
   }

   private void drawStaffRow(
      DrawContext context,
      MatrixStack matrix,
      float posX,
      float posY,
      float width,
      float rowHeight,
      float animValue,
      String name,
      StaffList.StaffStatus status,
      @Nullable UUID uuid,
      boolean example
   ) {
      int alpha = (int)(255.0F * animValue);
      boolean isLightTheme = ThemeManager.BackgroundGui.getColor() == Color.WHITE.getRGB() || (ThemeManager.BackgroundGui.getColor() >> 16 & 0xFF) > 200;
      Color textColor = isLightTheme ? new Color(0, 0, 0, alpha) : new Color(255, 255, 255, alpha);
      int statusColor = ColorAssist.replAlpha(getStatusColor(status), alpha);
      matrix.push();
      matrix.translate(posX + width / 2.0F, posY + rowHeight * animValue / 2.0F, 0.0F);
      matrix.scale(animValue, animValue, animValue);
      matrix.translate(-(posX + width / 2.0F), -(posY + rowHeight * animValue / 2.0F), 0.0F);
      float headSize = 9.0F;
      float headRound = 2.5F;
      float headX = posX + 7.0F;
      float headY = HudListPanel.rowIconY(posY, headSize);
      Identifier faceTexture = status == StaffList.StaffStatus.VANISHED ? null : this.resolveFaceTexture(name, uuid);
      if (faceTexture != null) {
         this.drawStaffHead(context, headX, headY, headSize, headRound, faceTexture, animValue);
      } else {
         rectangle.render(ShapeProperties.create(matrix, headX, headY, headSize, headSize).round(headRound).color(ColorAssist.rgba(70, 70, 74, alpha)).build());
      }

      rectangle.render(
         ShapeProperties.create(matrix, posX + 18.6F, HudListPanel.rowDotY(posY), 2.0, 2.0).round(1.0F).color(new Color(87, 87, 90, alpha).getRGB()).build()
      );
      float rowTextY = HudListPanel.rowIconAlignedTextY(this.fontName, posY, headSize, name);
      this.fontName.drawString(matrix, name, Math.round(posX + 23.5F), Math.round(rowTextY), textColor.getRGB());
      String statusText = status.label;
      float statusWidth = this.fontStatus.getStringWidth(statusText);
      float statusBoxWidth = statusWidth + 6.0F;
      float statusBoxX = posX + width - 6.5F - statusBoxWidth;
      float statusBoxHeight = 8.0F;
      float statusBoxY = HudListPanel.rowBadgeY(posY, statusBoxHeight);
      rectangle.render(
         ShapeProperties.create(matrix, statusBoxX, statusBoxY, statusBoxWidth, statusBoxHeight * animValue)
            .round(2.0F)
            .thickness(1.0F)
            .outlineColor(new Color(33, 33, 33, alpha).getRGB())
            .color(new Color(0, 0, 0, (int)(100.0F * animValue)).getRGB())
            .build()
      );
      this.fontStatus
         .drawString(
            matrix,
            statusText,
            Math.round(statusBoxX + statusBoxWidth / 2.0F - statusWidth / 2.0F),
            Math.round(HudListPanel.rowBadgeTextY(this.fontStatus, posY, statusBoxHeight, statusText)),
            example ? ThemeManager.textColor.getColor() : statusColor
         );
      matrix.pop();
   }

   private void drawStaffHead(DrawContext context, float headX, float headY, float headSize, float headRound, Identifier texture, float animation) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(animation, 0.0F, 1.0F));

      try {
         Render2D.drawTexture(context, texture, headX, headY, headSize, headRound, 8, 8, 64, ColorAssist.getRect(1.0F), -1);
      } finally {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
      }
   }

   @Nullable
   private Identifier resolveFaceTexture(String playerName, @Nullable UUID uuid) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null) {
         Identifier texture;
         LivingEntityRenderer renderer;
         LivingEntityRenderState state;
         EntityRenderer baseRenderer;
         PlayerEntity player;
         return uuid != null
               && (player = client.world.getPlayerByUuid(uuid)) != null
               && (baseRenderer = client.getEntityRenderDispatcher().getRenderer(player)) instanceof LivingEntityRenderer
               && (
                     state = (LivingEntityRenderState)(renderer = (LivingEntityRenderer)baseRenderer)
                        .getAndUpdateRenderState(player, client.getRenderTickCounter().getTickDelta(false))
                  )
                  != null
               && (texture = renderer.getTexture(state)) != null
            ? texture
            : resolveSkinTexture(playerName, uuid);
      } else {
         return resolveSkinTexture(playerName, uuid);
      }
   }

   private void updateStaffPlayers() {
      if (!PlayerInteractionHelper.nullCheck() && mc.getNetworkHandler() != null && mc.world != null) {
         HashMap<String, PlayerListEntry> nameToEntry = new HashMap<>();

         for (PlayerListEntry entry2 : mc.getNetworkHandler().getPlayerList()) {
            if (entry2.getProfile() != null && entry2.getProfile().getName() != null) {
               nameToEntry.put(entry2.getProfile().getName().toLowerCase(Locale.ROOT), entry2);
            }
         }

         HashSet<String> detectedNames = new HashSet<>();
         this.addedPlayers.clear();
         String ourName = mc.player.getName().getString();
         Scoreboard scoreboard = mc.world.getScoreboard();

         for (Team team : scoreboard.getTeams()) {
            Text prefixComponent = team.getPrefix();
            String prefix = prefixComponent.getString();
            String cleanPrefixLower = repairString(prefix).toLowerCase(Locale.ROOT);

            for (String member : team.getPlayerList()) {
               if (member != null && !member.equals(ourName) && !this.addedPlayers.contains(member) && NAME_PATTERN.matcher(member).matches()) {
                  PlayerListEntry entry3 = nameToEntry.get(member.toLowerCase(Locale.ROOT));
                  boolean isVanished = entry3 == null;
                  if (isVanished ? !prefix.isEmpty() : PREFIX_MATCHES.matcher(cleanPrefixLower).matches() || StaffRepository.isStaff(member)) {
                     UUID uuid = entry3 != null ? entry3.getProfile().getId() : null;
                     StaffList.StaffPlayer player = new StaffList.StaffPlayer(member, prefixComponent, uuid);
                     detectedNames.add(member.toLowerCase(Locale.ROOT));
                     StaffList.StaffRow existing = this.rowByName.get(member.toLowerCase(Locale.ROOT));
                     if (existing == null) {
                        StaffList.StaffRow row2 = new StaffList.StaffRow(player, new Decelerate().setMs(150).setValue(1.0));
                        this.rowByName.put(member.toLowerCase(Locale.ROOT), row2);
                        this.staffRows.add(row2);
                     } else {
                        existing.player = player;
                        existing.anim.setDirection(Direction.FORWARDS);
                     }

                     this.addedPlayers.add(member);
                  }
               }
            }
         }

         this.staffRows.sort(Comparator.comparing(row -> row.player.name, String.CASE_INSENSITIVE_ORDER));

         for (StaffList.StaffRow row3 : this.staffRows) {
            if (!detectedNames.contains(row3.player.name.toLowerCase(Locale.ROOT))) {
               row3.anim.setDirection(Direction.BACKWARDS);
            }
         }

         this.rowByName.entrySet().removeIf(entry -> {
            StaffList.StaffRow row = entry.getValue();
            if (row.anim.isFinished(Direction.BACKWARDS)) {
               this.staffRows.remove(row);
               return true;
            } else {
               return false;
            }
         });
      } else {
         this.markAllRowsRemoved();
      }
   }

   private void markAllRowsRemoved() {
      for (StaffList.StaffRow row : this.staffRows) {
         row.anim.setDirection(Direction.BACKWARDS);
      }
   }

   private static String repairString(String input) {
      if (input != null && !input.isEmpty()) {
         StringBuilder sb = new StringBuilder(input.length());

         for (char c : input.toCharArray()) {
            if (c >= '！' && c <= '～') {
               sb.append((char)(c - 'ﻠ'));
            } else {
               sb.append(c);
            }
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   @Nullable
   private static Identifier resolveSkinTexture(String playerName, @Nullable UUID uuid) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         return null;
      } else {
         PlayerListEntry entry = findPlayerListEntry(client, uuid, playerName);
         Identifier texture;
         if (entry != null && entry.getSkinTextures() != null && (texture = entry.getSkinTextures().texture()) != null) {
            return texture;
         } else if (entry != null && entry.getProfile() != null) {
            return client.getSkinProvider().getSkinTextures(entry.getProfile()).texture();
         } else {
            UUID resolvedUuid = uuid != null ? uuid : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
            return DefaultSkinHelper.getSkinTextures(resolvedUuid).texture();
         }
      }
   }

   @Nullable
   private static PlayerListEntry findPlayerListEntry(MinecraftClient client, @Nullable UUID uuid, String playerName) {
      if (client.getNetworkHandler() == null) {
         return null;
      } else {
         PlayerListEntry byUuid;
         if (uuid != null && (byUuid = client.getNetworkHandler().getPlayerListEntry(uuid)) != null) {
            return byUuid;
         } else {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
               if (entry.getProfile() != null && entry.getProfile().getName() != null && entry.getProfile().getName().equalsIgnoreCase(playerName)) {
                  return entry;
               }
            }

            return null;
         }
      }
   }

   private static int getStatusColor(StaffList.StaffStatus status) {
      return switch (status) {
         case ONLINE -> Color.GREEN.getRGB();
         case NEAR -> Color.ORANGE.getRGB();
         case SPEC -> Color.YELLOW.getRGB();
         case VANISHED -> Color.RED.getRGB();
      };
   }

   private static final class StaffPlayer {
      private final String name;
      private final StaffList.StaffStatus status;
      @Nullable
      private final UUID uuid;

      private StaffPlayer(String name, Text prefix, @Nullable UUID uuid) {
         this.name = name;
         this.uuid = uuid;
         this.status = this.resolveStatus(uuid);
      }

      private StaffList.StaffStatus resolveStatus(@Nullable UUID uuid) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.world != null && client.getNetworkHandler() != null) {
            PlayerListEntry entry = StaffList.findPlayerListEntry(client, uuid, this.name);
            if (entry == null) {
               return StaffList.StaffStatus.VANISHED;
            } else if (entry.getGameMode() == GameMode.SPECTATOR) {
               return StaffList.StaffStatus.SPEC;
            } else {
               return client.world.getPlayerByUuid(entry.getProfile().getId()) != null ? StaffList.StaffStatus.NEAR : StaffList.StaffStatus.ONLINE;
            }
         } else {
            return StaffList.StaffStatus.VANISHED;
         }
      }
   }

   private static final class StaffRow {
      private StaffList.StaffPlayer player;
      private final Animation anim;

      private StaffRow(StaffList.StaffPlayer player, Animation anim) {
         this.player = player;
         this.anim = anim;
      }
   }

   private static enum StaffStatus {
      ONLINE("[ON]"),
      NEAR("[N]"),
      SPEC("[GM3]"),
      VANISHED("[V]");

      private final String label;

      private StaffStatus(String label) {
         this.label = label;
      }
   }
}
