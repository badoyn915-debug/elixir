package code.elixir.mixins;

import code.elixir.emotions.ClientBadgeRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListHud.class})
public abstract class PlayerListHudMixin {
   private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

   @Shadow
   protected abstract List<PlayerListEntry> collectPlayerEntries();

   @Shadow
   public abstract Text getPlayerName(PlayerListEntry var1);

   @Inject(
      method = {"collectPlayerEntries"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void addVanishedEntries(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.player == null || client.player.networkHandler == null) {
         return;
      }
      List originalList = (List)cir.getReturnValue();
      if (originalList == null) {
         return;
      }
      ArrayList<PlayerListEntry> vanishedList = new ArrayList<>();
      Scoreboard scoreboard = client.world.getScoreboard();
      if (scoreboard == null) {
         return;
      }
      ArrayList<Team> teams = new ArrayList<>(scoreboard.getTeams());
      teams.sort(Comparator.comparing(Team::getName));
      Collection<PlayerListEntry> online = client.player.networkHandler.getPlayerList();
      if (online == null) {
         return;
      }

      for (Team team : teams) {
         Collection members = team.getPlayerList();
         String name;
         if (members.size() == 1
            && NAME_PATTERN.matcher(name = (String)members.iterator().next()).matches()
            && !online.stream().anyMatch(e -> e.getProfile() != null && name.equals(e.getProfile().getName()))) {
            MutableText displayName = Text.empty()
               .append(Text.literal("[").formatted(Formatting.GRAY))
               .append(Text.literal("V").formatted(Formatting.RED))
               .append(Text.literal("] ").formatted(Formatting.GRAY))
               .append(team.getPrefix())
               .append(Text.literal(name).formatted(Formatting.GRAY));
            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, client.isInSingleplayer());
            fake.setDisplayName(displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            vanishedList.add(fake);
         }
      }

      ArrayList<PlayerListEntry> finalList = new ArrayList<>();
      finalList.addAll(vanishedList);
      finalList.addAll(originalList);
      cir.setReturnValue(finalList);
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
      )
   )
   private int Elixir$renderPlayerNameWithClientBadge(
      DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, @Local PlayerListEntry entry
   ) {
      GameProfile profile = entry.getProfile();
      if (profile != null && ClientBadgeRenderer.hasCompatibleClient(profile.getId())) {
         int iconSize = 7;
         int gap = 2;
         ClientBadgeRenderer.drawRoundedBadge(context, x, y + 1.0F, iconSize);
         return context.drawTextWithShadow(textRenderer, text, x + iconSize + gap, y, color);
      } else {
         return context.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }
}
