package code.elixir.utils.client.packet.network;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.MathHelper;

public final class ServerUtil implements QuickImports {
   private static final Map<UUID, Float> lastValidHealth = new WeakHashMap<>();
   private static long scoreboardCacheTick = -1L;
   private static final Map<UUID, Float> scoreboardCache = new WeakHashMap<>();

   public static float getHealthFraction(LivingEntity target) {
      if (target == null) {
         return 0.0F;
      } else if (target instanceof MobEntity) {
         return safeVanillaFraction(target);
      } else if (mc.getCurrentServerEntry() != null && !mc.isConnectedToLocalServer()) {
         float maxHp = getMaxHealth(target);
         return maxHp <= 0.0F ? 0.0F : MathHelper.clamp(getHealthValue(target) / maxHp, 0.0F, 1.0F);
      } else {
         return safeVanillaFraction(target);
      }
   }

   public static float getHealthValue(LivingEntity target) {
      if (target == null) {
         return 0.0F;
      } else {
         float vanillaHp = target.getHealth();
         if (!(target instanceof PlayerEntity player)) {
            return MathHelper.clamp(vanillaHp, 0.0F, target.getMaxHealth());
         } else if (!player.isAlive()) {
            lastValidHealth.remove(player.getUuid());
            return 0.0F;
         } else {
            float scoreboardHp = getCachedScoreboardHealth(player);
            if (scoreboardHp > 0.0F) {
               lastValidHealth.put(player.getUuid(), scoreboardHp);
               return MathHelper.clamp(scoreboardHp, 0.0F, getMaxHealth(target));
            } else {
               Float lastKnown;
               return usesExtendedHealthScale()
                     && mc.getCurrentServerEntry() != null
                     && !mc.isConnectedToLocalServer()
                     && (lastKnown = lastValidHealth.get(player.getUuid())) != null
                     && lastKnown > 0.0F
                  ? MathHelper.clamp(lastKnown, 0.0F, getMaxHealth(target))
                  : MathHelper.clamp(vanillaHp, 0.0F, getMaxHealth(target));
            }
         }
      }
   }

   public static float getMaxHealth(LivingEntity target) {
      if (target == null) {
         return 20.0F;
      } else {
         if (target instanceof PlayerEntity player && usesExtendedHealthScale()) {
            float scoreboardHp = getCachedScoreboardHealth(player);
            Float lastKnown;
            if (scoreboardHp <= 0.0F && (lastKnown = lastValidHealth.get(player.getUuid())) != null && lastKnown > 0.0F) {
               scoreboardHp = lastKnown;
            }

            if (scoreboardHp > 0.0F) {
               float vanillaMax = target.getMaxHealth();
               return scoreboardHp <= vanillaMax ? vanillaMax : getExtendedMaxHealth();
            }
         }

         return target.getMaxHealth();
      }
   }

   public static float getDisplayHealth(LivingEntity target, float healthFraction) {
      if (target == null) {
         return 0.0F;
      } else {
         return target instanceof PlayerEntity && usesExtendedHealthScale() && mc.getCurrentServerEntry() != null && !mc.isConnectedToLocalServer()
            ? getHealthValue(target)
            : healthFraction * getMaxHealth(target);
      }
   }

   private static float getCachedScoreboardHealth(PlayerEntity player) {
      long tick = mc.world != null ? mc.world.getTime() : 0L;
      if (scoreboardCacheTick != tick) {
         scoreboardCache.clear();
         scoreboardCacheTick = tick;
      }

      return scoreboardCache.computeIfAbsent(player.getUuid(), uuid -> readScoreboardHealth(player));
   }

   private static float readScoreboardHealth(PlayerEntity player) {
      Scoreboard scoreboard = player.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
      if (objective == null) {
         return -1.0F;
      } else {
         if (usesReallyWorldScoreboard()) {
            try {
               int score = scoreboard.getOrCreateScore(ScoreHolder.fromName(player.getNameForScoreboard()), objective).getScore();
               if (score >= 0) {
                  if (usesExtendedHealthScale()) {
                     return score > 0 ? score : -1.0F;
                  }

                  if (score <= player.getMaxHealth()) {
                     return score;
                  }
               }
            } catch (RuntimeException var6) {
            }
         }

         if (usesFormattedScoreboardHealth()) {
            try {
               MutableText formatted = ReadableScoreboardScore.getFormattedScore(
                  scoreboard.getScore(player, objective), objective.getNumberFormatOr(StyledNumberFormat.EMPTY)
               );
               float parsed = Float.parseFloat(ColorAssist.removeFormatting(formatted.getString()));
               if (parsed > 0.0F) {
                  return parsed;
               }
            } catch (NumberFormatException var5) {
            }
         }

         return -1.0F;
      }
   }

   private static float getExtendedMaxHealth() {
      return 1000.0F;
   }

   private static boolean usesReallyWorldScoreboard() {
      if (usesExtendedHealthScale()) {
         return true;
      } else {
         String address = getServerAddress();
         return address.contains("reallyworld")
            || address.contains("playrw")
            || address.contains("saturn-x")
            || address.contains("skytime")
            || address.contains("space-times");
      }
   }

   private static boolean usesFormattedScoreboardHealth() {
      return usesExtendedHealthScale() || usesReallyWorldScoreboard();
   }

   private static boolean usesExtendedHealthScale() {
      String var0 = Network.server;

      return switch (var0) {
         case "FunTime", "ReallyWorld", "GulPvP", "HolyWorld", "HopLite", "CopyTime" -> true;
         default -> false;
      };
   }

   private static String getServerAddress() {
      if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         String address = mc.getNetworkHandler().getServerInfo().address;
         return address == null ? "" : address.toLowerCase(Locale.ROOT);
      } else {
         return "";
      }
   }

   private static float safeVanillaFraction(LivingEntity target) {
      float max = target.getMaxHealth();
      return max > 0.0F ? MathHelper.clamp(target.getHealth() / max, 0.0F, 1.0F) : 0.0F;
   }

   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
