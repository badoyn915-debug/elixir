package code.elixir.utils.features.price;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;

public class PriceParser {
   private final Pattern funTimePricePattern = Pattern.compile("\\$\\s*(\\d+(?:[,\\s]\\d{3})*(?:\\.\\d{2})?)");

   public int getPrice(ItemStack stack) {
      ComponentMap tag = stack.getComponents();
      if (tag == null) {
         return -1;
      } else {
         String componentString = tag.toString();
         String price = StringUtils.substringBetween(componentString, "literal{ $", "}[style={color=green}]");
         LoreComponent lore;
         if ((price == null || price.isEmpty()) && (lore = (LoreComponent)stack.get(DataComponentTypes.LORE)) != null && !lore.lines().isEmpty()) {
            String lastFoundPrice = null;

            for (Text line : lore.lines()) {
               String text = line.getString();
               Matcher matcher = this.funTimePricePattern.matcher(text);

               while (matcher.find()) {
                  lastFoundPrice = matcher.group(1);
               }
            }

            if (lastFoundPrice != null && !lastFoundPrice.isEmpty()) {
               price = lastFoundPrice;
            }
         }

         if (price != null && !price.isEmpty()) {
            try {
               price = price.replaceAll("[\\s,]", "");
               return Integer.parseInt(price);
            } catch (NumberFormatException var11) {
               return -1;
            }
         } else {
            return -1;
         }
      }
   }

   public int getSpookyTimePrice(ItemStack stack, GenericContainerScreen screen) {
      if (screen == null) {
         return -1;
      } else {
         String title = screen.getTitle().getString();
         if (!title.contains("Аукцион") && !title.contains("Поиск:")) {
            return -1;
         } else {
            LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
               for (Text line : lore.lines()) {
                  String text = line.getString();
                  if (text.contains("Цена:") || text.contains("Price:")) {
                     try {
                        int priceIndex = Math.max(text.indexOf("Цена:"), text.indexOf("Price:"));
                        if (priceIndex != -1) {
                           String priceText = text.substring(priceIndex);
                           String priceStr = priceText.replaceAll("[^0-9.]", "").trim();

                           try {
                              return (int)Double.parseDouble(priceStr);
                           } catch (NumberFormatException var14) {
                              try {
                                 return Integer.parseInt(priceStr);
                              } catch (NumberFormatException var13) {
                                 return -1;
                              }
                           }
                        }

                        return -1;
                     } catch (Exception var15) {
                        return -1;
                     }
                  }
               }
            }

            return -1;
         }
      }
   }
}
