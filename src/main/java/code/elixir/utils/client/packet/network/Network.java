package code.elixir.utils.client.packet.network;

import code.elixir.events.packet.PacketEvent;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.time.StopWatch;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

public final class Network implements QuickImports {
   private static final StopWatch pvpWatch = new StopWatch();
   public static String server = "Vanilla";
   public static float TPS = 20.0F;
   public static long timestamp;
   public static int anarchy;
   public static boolean pvpEnd;

   public static void tick() {
      anarchy = getAnarchyMode();
      server = getServer();
      pvpEnd = inPvpEnd();
      if (inPvp()) {
         pvpWatch.reset();
      }
   }

   public static void packet(PacketEvent e) {
      if (e.getPacket() instanceof WorldTimeUpdateS2CPacket time) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - timestamp));
         TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
         timestamp = nanoTime;
      }
   }

   public static String getServer() {
      if (!PlayerInteractionHelper.nullCheck() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
         String brand = mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand().toLowerCase() : "";
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (brand.contains("§6spooky§ccore")) {
            return "SpookyTime";
         } else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) {
            return "HolyWorld";
         } else if (serverIp.contains("reallyworld")) {
            return "ReallyWorld";
         } else if (serverIp.contains("gulpvp")) {
            return "GulPvP";
         } else {
            String title;
            ScoreboardObjective objective;
            return mc.world == null
                  || mc.world.getScoreboard() == null
                  || (objective = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)) == null
                  || (title = objective.getDisplayName().getString()) == null
                  || !title.equals("HolyWorld.me") && !title.equals("HolyWorld") && !title.toLowerCase().contains("holyworld")
               ? "Vanilla"
               : "HolyWorld";
         }
      } else {
         return "Vanilla";
      }
   }

   private static int getAnarchyMode() {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objective == null) {
         return -1;
      } else {
         String var2 = server;
         switch (var2) {
            case "FunTime":
            case "SpookyTime":
               String titlex = objective.getDisplayName().getString();
               String[] parts;
               if (titlex.contains("Анархия-") && (parts = titlex.split("Анархия-")).length > 1) {
                  try {
                     String numberStr = parts[1].trim().split(" ")[0];
                     return Integer.parseInt(numberStr);
                  } catch (NumberFormatException var19) {
                  }
               }

               for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)
                  .stream()
                  .sorted((e1, e2) -> Integer.compare(e2.value(), e1.value()))
                  .toList()) {
                  String text = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString();
                  String[] parts2;
                  if (text.contains("Анархия-") && (parts2 = text.split("Анархия-")).length > 1) {
                     try {
                        String numberStr = parts2[1].trim().split(" ")[0];
                        return Integer.parseInt(numberStr);
                     } catch (NumberFormatException var22) {
                     }
                  }
               }
               break;
            case "HolyWorld":
               String holyTitle = objective.getDisplayName().getString();
               List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective)
                  .stream()
                  .sorted((e1, e2) -> Integer.compare(e2.value(), e1.value()))
                  .toList();
               String lastText;
               String numberAfterHash;
               ScoreboardEntry lastEntry;
               if (!entries.isEmpty()
                  && !(lastText = Team.decorateName(scoreboard.getScoreHolderTeam((lastEntry = entries.get(entries.size() - 1)).owner()), lastEntry.name())
                        .getString())
                     .isEmpty()
                  && lastText.contains("#")
                  && (numberAfterHash = StringUtils.substringAfter(lastText, "#")) != null
                  && !numberAfterHash.isEmpty()) {
                  try {
                     StringBuilder numBuilder = new StringBuilder();

                     for (char c : numberAfterHash.trim().toCharArray()) {
                        if (!Character.isDigit(c)) {
                           break;
                        }

                        numBuilder.append(c);
                     }

                     if (numBuilder.length() > 0) {
                        return Integer.parseInt(numBuilder.toString());
                     }
                  } catch (NumberFormatException var21) {
                  }
               }

               for (ScoreboardEntry scoreboardEntry : entries) {
                  String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                  String numberAfterHash2;
                  if (!text.isEmpty()
                     && !text.contains("Клан:")
                     && !text.contains("Трио:")
                     && text.contains("#")
                     && (numberAfterHash2 = StringUtils.substringAfter(text, "#")) != null
                     && !numberAfterHash2.isEmpty()) {
                     try {
                        StringBuilder numBuilder = new StringBuilder();
                        char[] var14 = numberAfterHash2.trim().toCharArray();
                        int var15 = var14.length;
                        int var16 = 0;

                        while (true) {
                           if (var16 < var15) {
                              char c = var14[var16];
                              if (Character.isDigit(c)) {
                                 numBuilder.append(c);
                                 var16++;
                                 continue;
                              }
                           }

                           if (numBuilder.length() > 0) {
                              return Integer.parseInt(numBuilder.toString());
                           }
                           break;
                        }
                     } catch (NumberFormatException var20) {
                     }
                  }
               }

               if (holyTitle != null && (holyTitle.equals("HolyWorld.me") || holyTitle.equals("HolyWorld"))) {
                  return -1;
               }
               break;
            case "ReallyWorld":
               String title = objective.getDisplayName().getString();
               String number = StringUtils.substringAfter(title, "#");
               if (number != null && !number.isEmpty()) {
                  try {
                     return Integer.parseInt(number.trim().split(" ")[0]);
                  } catch (NumberFormatException var18) {
                  }
               }
         }

         return -1;
      }
   }

   public static boolean isPvp() {
      return !pvpWatch.finished(500.0);
   }

   private static boolean inPvp() {
      return mc.inGameHud
         .getBossBarHud()
         .bossBars
         .values()
         .stream()
         .map(c -> c.getName().getString().toLowerCase())
         .anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
   }

   private static boolean inPvpEnd() {
      return mc.inGameHud
         .getBossBarHud()
         .bossBars
         .values()
         .stream()
         .map(c -> c.getName().getString().toLowerCase())
         .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
   }

   public static String getWorldType() {
      return mc.world.getRegistryKey().getValue().getPath();
   }

   public static boolean isCopyTime() {
      return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
   }

   public static boolean isFunTime() {
      return server.equals("FunTime");
   }

   public static boolean isReallyWorld() {
      return server.equals("ReallyWorld");
   }

   public static boolean isGulPvP() {
      return server.equals("GulPvP");
   }

   public static boolean isHolyWorld() {
      return server.equals("HolyWorld");
   }

   public static boolean isSpookyTime() {
      return server.equals("SpookyTime");
   }

   public static boolean isAresMine() {
      return server.equals("aresmine");
   }

   public static boolean isVanilla() {
      return server.equals("Vanilla");
   }

   private Network() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static int getAnarchy() {
      return anarchy;
   }

   public static boolean isPvpEnd() {
      return pvpEnd;
   }
}
