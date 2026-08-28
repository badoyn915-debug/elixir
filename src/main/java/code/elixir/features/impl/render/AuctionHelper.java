package code.elixir.features.impl.render;

import code.elixir.events.container.HandledScreenEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.chat.ChatCommandSender;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

public class AuctionHelper extends Module {
   static final Pattern PRICE_WITH_LABEL = Pattern.compile("(?iu)(?:цена|стоимость|price|cost)\\s*[:]?\\s*([\\d\\s,._]+)");
   static final Pattern PRICE_WITH_CURRENCY = Pattern.compile("(?iu)(?:\\$|₽|р|руб|р\\.)\\s*([\\d\\s,._]+)|([\\d\\s,._]+)\\s*(?:\\$|₽|р|руб|р\\.)");
   static final Pattern DIGITS_PATTERN = Pattern.compile("([\\d][\\d\\s,._]{1,18})");
   private final BooleanSetting onlyMending = new BooleanSetting("Только починка", "Показывать только предметы с починкой").setValue(false);
   private final BooleanSetting showPricePerItem = new BooleanSetting("Цена за 1", "Добавлять в лор цену за одну штуку").setValue(true);
   private final BooleanSetting enchantSort = new BooleanSetting("Фильтр зачаров", "Подсветка и сортировка по зачарам и прочности").setValue(true);
   private final RadioSetting sortPreset = new RadioSetting(
         "Пресет фильтра", "Выберите тип предметов", new String[]{"Любые", "Броня Prot 5+", "Кирка Eff 7-10", "Меч Sharp 7+"}, "Любые"
      )
      .visible(this.enchantSort::isValue);
   private final BooleanSetting onlyMatching = new BooleanSetting("Только подходящие", "Показывать только предметы, подходящие под фильтр")
      .setValue(true)
      .visible(this.enchantSort::isValue);
   private final SliderSettings minDurability = new SliderSettings("Мин. прочность %", "Минимальная прочность предмета в процентах")
      .setValue(0.0F)
      .range(0, 100)
      .step(1.0F)
      .visible(this.enchantSort::isValue);
   private final SliderSettings minProtection = new SliderSettings("Мин. Protection", "Минимальный уровень Protection")
      .setValue(5.0F)
      .range(1, 10)
      .step(1.0F)
      .visible(this.enchantSort::isValue);
   private final SliderSettings minSharpness = new SliderSettings("Мин. Sharpness", "Минимальный уровень Sharpness")
      .setValue(7.0F)
      .range(1, 10)
      .step(1.0F)
      .visible(this.enchantSort::isValue);
   private final SliderSettings minEfficiency = new SliderSettings("Мин. Efficiency", "Минимальный уровень Efficiency")
      .setValue(7.0F)
      .range(1, 10)
      .step(1.0F)
      .visible(this.enchantSort::isValue);
   private final SliderSettings maxEfficiency = new SliderSettings("Макс. Efficiency", "Максимальный уровень Efficiency")
      .setValue(10.0F)
      .range(1, 10)
      .step(1.0F)
      .visible(this.enchantSort::isValue);
   private final ColorSetting cheapestColor = new ColorSetting("Цвет дешевого", "Цвет подсветки самого дешевого слота")
      .setColor(new Color(75, 255, 75, 255).getRGB());
   private final ColorSetting bestValueColor = new ColorSetting("Цвет выгодного", "Цвет подсветки лучшего слота по цене за штуку")
      .setColor(new Color(51, 170, 255, 255).getRGB());
   private final BindSetting searchHeldItemBind = new BindSetting("Поиск предмета", "Ищет предмет из руки через /ah search");
   private final BindSetting autoSellBind = new BindSetting("Продать по наценке", "Ищет минимальную цену и продает предмет из руки");
   private final SliderSettings sellMarkupPercent = new SliderSettings("Наценка %", "Наценка к самой дешевой цене").setValue(25.0F).range(0, 500).step(1.0F);
   private Slot cheapestSlot;
   private Slot bestValueSlot;
   private int cheapestTotalPrice = 0;
   private int cheapestCount = 1;
   private int lastUpdateTick = 0;
   private int lastSlotCount = 0;
   private GenericContainerScreen lastScreen = null;
   private Map<Integer, String> pricePerItemCache = new HashMap<>();
   private boolean waitingForAutoSellResult = false;
   private int autoSellStartTick = 0;
   private long lastActionMs = 0L;

   public AuctionHelper() {
      super("AuctionHelper", "AuctionHelper", ModuleCategory.RENDER);
      this.setup(
         new Setting[]{
            this.onlyMending,
            this.showPricePerItem,
            this.enchantSort,
            this.sortPreset,
            this.onlyMatching,
            this.minDurability,
            this.minProtection,
            this.minSharpness,
            this.minEfficiency,
            this.maxEfficiency,
            this.cheapestColor,
            this.bestValueColor,
            this.searchHeldItemBind,
            this.autoSellBind,
            this.sellMarkupPercent
         }
      );
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && mc.player.networkHandler != null) {
         if (e.isKeyDown(this.searchHeldItemBind.getKey())) {
            this.searchHeldItemByBind();
         }

         if (e.isKeyDown(this.autoSellBind.getKey())) {
            this.startAutoSellByBind();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null) {
         if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            this.cheapestSlot = null;
            this.bestValueSlot = null;
            this.cheapestTotalPrice = 0;
            this.lastScreen = null;
            this.pricePerItemCache.clear();
         } else {
            int currentSlotCount = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size();
            if (currentSlotCount != this.lastSlotCount || mc.player.age - this.lastUpdateTick > 5) {
               this.lastUpdateTick = mc.player.age;
               this.lastSlotCount = currentSlotCount;
               this.updateBestSlots(screen);
            }

            if (this.waitingForAutoSellResult) {
               this.handleAutoSellAfterSearch(screen);
            }
         }
      }
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      if (mc.currentScreen instanceof GenericContainerScreen screen) {
         if (this.lastScreen != screen) {
            this.pricePerItemCache.clear();
            this.lastScreen = screen;
         }

         DrawContext context = e.getDrawContext();
         MatrixStack matrix = context.getMatrices();
         int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
         int offsetY = (screen.height - e.getBackgroundHeight()) / 2;
         matrix.push();
         matrix.translate(offsetX, offsetY, 0.0F);
         long time = System.currentTimeMillis();
         if (this.cheapestSlot != null && this.isValidSlot(this.cheapestSlot, screen)) {
            int color = this.getBlinkingColor(this.cheapestColor.getColor(), time, 500);
            this.highlightSlot(context, this.cheapestSlot, color);
         }

         if (this.bestValueSlot != null && this.isValidSlot(this.bestValueSlot, screen) && this.bestValueSlot != this.cheapestSlot) {
            int color = this.getBlinkingColor(this.bestValueColor.getColor(), time, 600);
            this.highlightSlot(context, this.bestValueSlot, color);
         }

         matrix.pop();
         if (this.showPricePerItem.isValue()) {
            this.addPricePerItemToSlots(screen);
         }
      }
   }

   private void searchHeldItemByBind() {
      ItemStack held = mc.player.getMainHandStack();
      if (held.isEmpty()) {
         ChatMessage.brandmessage("§c[AuctionHelper] В руке нет предмета для поиска.");
      } else {
         String query = this.getCleanItemName(held);
         if (query.isBlank()) {
            ChatMessage.brandmessage("§c[AuctionHelper] Не удалось получить название предмета.");
         } else if (this.canSendActionNow()) {
            ChatCommandSender.sendCommand(mc.player, "/ah search " + query);
            this.lastActionMs = System.currentTimeMillis();
            ChatMessage.brandmessage("§a[AuctionHelper] Поиск: §f" + query);
         }
      }
   }

   private void startAutoSellByBind() {
      ItemStack held = mc.player.getMainHandStack();
      if (held.isEmpty()) {
         ChatMessage.brandmessage("§c[AuctionHelper] Для продажи держите предмет в руке.");
      } else {
         String query = this.getCleanItemName(held);
         if (query.isBlank()) {
            ChatMessage.brandmessage("§c[AuctionHelper] Не удалось получить название предмета.");
         } else if (this.canSendActionNow()) {
            this.waitingForAutoSellResult = true;
            this.autoSellStartTick = mc.player.age;
            ChatCommandSender.sendCommand(mc.player, "/ah search " + query);
            this.lastActionMs = System.currentTimeMillis();
            ChatMessage.brandmessage("§e[AuctionHelper] Ищу минимальную цену для: §f" + query);
         }
      }
   }

   private void handleAutoSellAfterSearch(GenericContainerScreen screen) {
      String title = Formatting.strip(screen.getTitle().getString());
      String lowerTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
      if (!lowerTitle.contains("аукцион") && !lowerTitle.contains("auction")) {
         if (mc.player.age - this.autoSellStartTick > 120) {
            this.waitingForAutoSellResult = false;
            ChatMessage.brandmessage("§c[AuctionHelper] Не удалось открыть аукцион с результатами.");
         }
      } else if (mc.player.age - this.autoSellStartTick >= 6) {
         if (this.cheapestTotalPrice <= 0) {
            if (mc.player.age - this.autoSellStartTick > 120) {
               this.waitingForAutoSellResult = false;
               ChatMessage.brandmessage("§c[AuctionHelper] Не нашел цену для автопродажи.");
            }
         } else {
            double markup = this.sellMarkupPercent.getValue() / 100.0;
            int sellPrice = (int)Math.max(1L, Math.round(this.cheapestTotalPrice * (1.0 + markup)));
            this.waitingForAutoSellResult = false;
            if (this.canSendActionNow()) {
               ChatCommandSender.sendCommand(mc.player, "/ah sell " + sellPrice);
               this.lastActionMs = System.currentTimeMillis();
               ChatMessage.brandmessage(
                  "§a[AuctionHelper] Продажа выставлена за: §f"
                     + sellPrice
                     + "§a (рынок: "
                     + this.cheapestTotalPrice
                     + ", наценка: "
                     + (int)this.sellMarkupPercent.getValue()
                     + "%)"
               );
            }
         }
      }
   }

   private boolean canSendActionNow() {
      return System.currentTimeMillis() - this.lastActionMs >= 350L;
   }

   private String getCleanItemName(ItemStack stack) {
      String raw = Formatting.strip(stack.getName().getString());
      if (raw == null) {
         return "";
      } else {
         String clean = raw.replace("[★]", "").replace("★", "").trim();
         return clean.replaceAll("\\s{2,}", " ");
      }
   }

   private boolean isValidSlot(Slot slot, GenericContainerScreen screen) {
      if (slot == null) {
         return false;
      } else if (slot.id >= 0 && slot.id < ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size()) {
         Slot currentSlot = ((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(slot.id);
         return currentSlot.hasStack() && !currentSlot.getStack().isEmpty();
      } else {
         return false;
      }
   }

   private void updateBestSlots(GenericContainerScreen screen) {
      DefaultedList<Slot> slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
      ArrayList<AuctionHelper.ItemPriceData> validItems = new ArrayList<>();

      for (Slot slot : slots) {
         int totalPrice;
         ItemStack stack;
         if (slot.inventory != mc.player.getInventory()
            && !(stack = slot.getStack()).isEmpty()
            && !this.isServiceItem(stack)
            && (!this.onlyMending.isValue() || this.hasMending(stack))
            && this.matchesAdvancedFilters(stack)
            && (totalPrice = this.parsePriceFromLore(stack)) > 0) {
            int count = Math.max(1, stack.getCount());
            int pricePerItem = totalPrice / count;
            int durabilityPercent = this.getDurabilityPercent(stack);
            int enchantScore = this.calculateEnchantScore(stack);
            validItems.add(new AuctionHelper.ItemPriceData(slot, totalPrice, pricePerItem, count, durabilityPercent, enchantScore));
         }
      }

      if (validItems.isEmpty()) {
         this.cheapestSlot = null;
         this.bestValueSlot = null;
         this.cheapestTotalPrice = 0;
      } else {
         AuctionHelper.ItemPriceData cheapest = validItems.stream().min(Comparator.comparingInt(d -> d.totalPrice)).orElse(null);
         this.cheapestSlot = cheapest == null ? null : cheapest.slot;
         this.cheapestTotalPrice = cheapest == null ? 0 : cheapest.totalPrice;
         this.bestValueSlot = validItems.stream()
            .min(
               Comparator.<AuctionHelper.ItemPriceData>comparingInt(d -> d.pricePerItem)
                  .thenComparing(d -> d.enchantmentScore, Comparator.reverseOrder())
                  .thenComparing(d -> d.durabilityPercent, Comparator.reverseOrder())
            )
            .map(d -> d.slot)
            .orElse(null);
      }
   }

   private boolean matchesAdvancedFilters(ItemStack stack) {
      if (!this.enchantSort.isValue()) {
         return true;
      } else {
         int durability = this.getDurabilityPercent(stack);
         if (durability < (int)this.minDurability.getValue()) {
            return false;
         } else {
            String preset = this.sortPreset.get();
            boolean armorMatch = stack.getItem() instanceof ArmorItem && this.getEnchantmentLevel(stack, "protection") >= (int)this.minProtection.getValue();
            int efficiencyLevel = this.getEnchantmentLevel(stack, "efficiency");
            boolean pickaxeMatch = stack.getItem() instanceof PickaxeItem
               && efficiencyLevel >= (int)this.minEfficiency.getValue()
               && efficiencyLevel <= (int)Math.max(this.minEfficiency.getValue(), this.maxEfficiency.getValue());
            boolean swordMatch = stack.getItem() instanceof SwordItem && this.getEnchantmentLevel(stack, "sharpness") >= (int)this.minSharpness.getValue();

            return switch (preset) {
               case "Броня Prot 5+" -> armorMatch;
               case "Кирка Eff 7-10" -> pickaxeMatch;
               case "Меч Sharp 7+" -> swordMatch;
               default -> !this.onlyMatching.isValue() || armorMatch || pickaxeMatch || swordMatch;
            };
         }
      }
   }

   private int getDurabilityPercent(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
         int max = stack.getMaxDamage();
         if (max <= 0) {
            return 100;
         } else {
            int left = max - stack.getDamage();
            return Math.max(0, Math.min(100, Math.round(left * 100.0F / max)));
         }
      } else {
         return 100;
      }
   }

   private int getEnchantmentLevel(ItemStack stack, String shortId) {
      ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchantments != null && !enchantments.isEmpty()) {
         for (RegistryEntry enchantment : enchantments.getEnchantments()) {
            String fullId = enchantment.getIdAsString();
            if (fullId != null && (fullId.endsWith(":" + shortId) || fullId.equals(shortId))) {
               return enchantments.getLevel(enchantment);
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private int calculateEnchantScore(ItemStack stack) {
      int prot = this.getEnchantmentLevel(stack, "protection");
      int eff = this.getEnchantmentLevel(stack, "efficiency");
      int sharp = this.getEnchantmentLevel(stack, "sharpness");
      int score = 0;
      if (stack.getItem() instanceof ArmorItem) {
         score += prot * 10;
      }

      if (stack.getItem() instanceof PickaxeItem) {
         score += eff * 10;
      }

      if (stack.getItem() instanceof SwordItem) {
         score += sharp * 10;
      }

      if (this.hasMending(stack)) {
         score += 7;
      }

      return score;
   }

   private boolean hasMending(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ItemEnchantmentsComponent enchantments = stack.getEnchantments();

         for (RegistryEntry entry : enchantments.getEnchantments()) {
            if (entry.getKey().isPresent() && "minecraft:mending".equals(((RegistryKey)entry.getKey().get()).getValue().toString())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isServiceItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);
         return itemName.contains("glass_pane") || itemName.contains("stained_glass") || itemName.contains("barrier") || itemName.contains("air");
      }
   }

   private int parsePriceFromLore(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         LoreComponent loreComp = (LoreComponent)stack.get(DataComponentTypes.LORE);
         if (loreComp != null) {
            for (Text text : loreComp.lines()) {
               String line = Formatting.strip(text.getString());
               int parsed2;
               if (line != null && (parsed2 = this.parsePriceFromLine(line)) > 0) {
                  return parsed2;
               }
            }
         }

         int parsed;
         String displayName;
         return (displayName = Formatting.strip(stack.getName().getString())) != null
               && !displayName.isBlank()
               && (parsed = this.parsePriceFromLine(displayName)) > 0
            ? parsed
            : 0;
      } else {
         return 0;
      }
   }

   private int parsePriceFromLine(String line) {
      if (line != null && !line.isBlank()) {
         Matcher m = PRICE_WITH_LABEL.matcher(line);
         int parsed2;
         if (m.find() && (parsed2 = this.parseNumberSafely(m.group(1))) > 0) {
            return parsed2;
         } else {
            Matcher m2 = PRICE_WITH_CURRENCY.matcher(line);
            int parsed;
            if (m2.find() && (parsed = this.parseNumberSafely(m2.group(1) != null ? m2.group(1) : m2.group(2))) > 0) {
               return parsed;
            } else {
               String lower = line.toLowerCase(Locale.ROOT);
               if (lower.contains("цена")
                  || lower.contains("стоимость")
                  || lower.contains("price")
                  || lower.contains("cost")
                  || lower.contains("$")
                  || lower.contains("₽")
                  || lower.contains("руб")
                  || lower.contains(" р")) {
                  Matcher digits = DIGITS_PATTERN.matcher(line);

                  while (digits.find()) {
                     int parsed3 = this.parseNumberSafely(digits.group(1));
                     if (parsed3 > 0) {
                        return parsed3;
                     }
                  }
               }

               return 0;
            }
         }
      } else {
         return 0;
      }
   }

   private int parseNumberSafely(String value) {
      if (value != null && !value.isBlank()) {
         String digits = value.replaceAll("[^0-9]", "");
         if (digits.isBlank()) {
            return 0;
         } else {
            try {
               return Integer.parseInt(digits);
            } catch (NumberFormatException var4) {
               return 0;
            }
         }
      } else {
         return 0;
      }
   }

   private void addPricePerItemToSlots(GenericContainerScreen screen) {
      for (Slot slot : ((GenericContainerScreenHandler)screen.getScreenHandler()).slots) {
         int totalPrice;
         ItemStack stack;
         if (slot.inventory != mc.player.getInventory()
            && !(stack = slot.getStack()).isEmpty()
            && stack.getCount() > 1
            && !this.pricePerItemCache.containsKey(slot.id)
            && (totalPrice = this.parsePriceFromLore(stack)) > 0) {
            int count = stack.getCount();
            int pricePerItem = totalPrice / count;
            this.addPricePerItemToLore(stack, pricePerItem);
            this.pricePerItemCache.put(slot.id, this.formatPrice(pricePerItem));
         }
      }
   }

   private void addPricePerItemToLore(ItemStack stack, int pricePerItem) {
      String pricePerItemText = "Цена за 1 шт: " + this.formatPrice(pricePerItem);
      LoreComponent existingLore = (LoreComponent)stack.get(DataComponentTypes.LORE);
      ArrayList<Text> loreLines = new ArrayList<>();
      if (existingLore != null) {
         for (Text line : existingLore.lines()) {
            String lineStr = line.getString().toLowerCase(Locale.ROOT);
            if (lineStr.contains("цена за 1 шт")) {
               return;
            }

            loreLines.add(line);
         }

         boolean inserted = false;
         ArrayList<Text> newLoreLines = new ArrayList<>();

         for (Text class_25612 : loreLines) {
            newLoreLines.add(class_25612);
            String lineStr = class_25612.getString().toLowerCase(Locale.ROOT);
            if (!inserted && (lineStr.contains("цена") || lineStr.contains("стоимость") || lineStr.contains("price") || lineStr.contains("cost"))) {
               newLoreLines.add(Text.literal(pricePerItemText));
               inserted = true;
            }
         }

         if (!inserted) {
            newLoreLines.add(Text.literal(pricePerItemText));
         }

         stack.set(DataComponentTypes.LORE, new LoreComponent(newLoreLines));
      } else {
         loreLines.add(Text.literal(pricePerItemText));
         stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
      }
   }

   private int getBlinkingColor(int color, long time, int periodMs) {
      float alpha = (float)(Math.sin((double)time / periodMs * Math.PI) * 0.3F + 0.7F);
      return ColorAssist.multAlpha(color, Math.min(1.0F, Math.max(0.4F, alpha)));
   }

   private void highlightSlot(DrawContext context, Slot slot, int color) {
      if (slot != null) {
         Render2D.rectangle.render(ShapeProperties.create(context.getMatrices(), slot.x, slot.y, 16.0, 16.0).color(color).build());
      }
   }

   private String formatPrice(int price) {
      if (price >= 1000000) {
         return String.format("%.2fM", price / 1000000.0).replace(",", ".");
      } else {
         return price >= 1000 ? String.format("%.1fK", price / 1000.0).replace(",", ".") : String.valueOf(price);
      }
   }

   private static final class ItemPriceData {
      private final Slot slot;
      private final int totalPrice;
      private final int pricePerItem;
      private final int count;
      private final int durabilityPercent;
      private final int enchantmentScore;

      public ItemPriceData(Slot slot, int totalPrice, int pricePerItem, int count, int durabilityPercent, int enchantmentScore) {
         this.slot = slot;
         this.totalPrice = totalPrice;
         this.pricePerItem = pricePerItem;
         this.count = count;
         this.durabilityPercent = durabilityPercent;
         this.enchantmentScore = enchantmentScore;
      }

      public Slot getSlot() {
         return this.slot;
      }

      public int getTotalPrice() {
         return this.totalPrice;
      }

      public int getPricePerItem() {
         return this.pricePerItem;
      }

      public int getCount() {
         return this.count;
      }

      public int getDurabilityPercent() {
         return this.durabilityPercent;
      }

      public int getEnchantmentScore() {
         return this.enchantmentScore;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof AuctionHelper.ItemPriceData other)) {
            return false;
         } else if (this.getTotalPrice() != other.getTotalPrice()) {
            return false;
         } else if (this.getPricePerItem() != other.getPricePerItem()) {
            return false;
         } else if (this.getCount() != other.getCount()) {
            return false;
         } else if (this.getDurabilityPercent() != other.getDurabilityPercent()) {
            return false;
         } else if (this.getEnchantmentScore() != other.getEnchantmentScore()) {
            return false;
         } else {
            Slot this$slot = this.getSlot();
            Slot other$slot = other.getSlot();
            return this$slot == null ? other$slot == null : this$slot.equals(other$slot);
         }
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         result = result * 59 + this.getTotalPrice();
         result = result * 59 + this.getPricePerItem();
         result = result * 59 + this.getCount();
         result = result * 59 + this.getDurabilityPercent();
         result = result * 59 + this.getEnchantmentScore();
         Slot $slot = this.getSlot();
         return result * 59 + ($slot == null ? 43 : $slot.hashCode());
      }

      @Override
      public String toString() {
         return "AuctionHelper.ItemPriceData(slot="
            + this.getSlot()
            + ", totalPrice="
            + this.getTotalPrice()
            + ", pricePerItem="
            + this.getPricePerItem()
            + ", count="
            + this.getCount()
            + ", durabilityPercent="
            + this.getDurabilityPercent()
            + ", enchantmentScore="
            + this.getEnchantmentScore()
            + ")";
      }
   }
}
