package code.elixir.utils.client.managers.api.command.helpers;

import code.elixir.utils.client.managers.api.command.argument.IArgConsumer;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.exception.CommandInvalidTypeException;
import code.elixir.utils.display.interfaces.QuickLogger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class Paginator<E> implements QuickLogger {
   public final List<E> entries;
   public int pageSize = 8;
   public int page = 1;

   public Paginator(List<E> entries) {
      this.entries = entries;
   }

   @SafeVarargs
   public Paginator(E... entries) {
      this.entries = Arrays.asList(entries);
   }

   public Paginator<E> setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
   }

   public int getMaxPage() {
      return (this.entries.size() - 1) / this.pageSize + 1;
   }

   public boolean validPage(int page) {
      return page > 0 && page <= this.getMaxPage();
   }

   public void skipPages(int pages) {
      this.page += pages;
   }

   public void display(Function<E, Text> transform, String commandPrefix) {
      int offset;
      for (int i = offset = (this.page - 1) * this.pageSize; i < offset + this.pageSize; i++) {
         if (i < this.entries.size()) {
            this.logDirect(new Text[]{transform.apply(this.entries.get(i))});
         } else {
            this.logDirect("--", Formatting.DARK_GRAY);
         }
      }

      boolean hasPrevPage = commandPrefix != null && this.validPage(this.page - 1);
      boolean hasNextPage = commandPrefix != null && this.validPage(this.page + 1);
      MutableText prevPageComponent = Text.literal("<<");
      if (hasPrevPage) {
         prevPageComponent.setStyle(
            prevPageComponent.getStyle()
               .withClickEvent(new ClickEvent(Action.RUN_COMMAND, String.format("%s %d", commandPrefix, this.page - 1)))
               .withHoverEvent(new HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal("Нажми чтобы посмотреть предыдущую страницу")))
         );
      } else {
         prevPageComponent.setStyle(prevPageComponent.getStyle().withColor(Formatting.DARK_GRAY));
      }

      MutableText nextPageComponent = Text.literal(">>");
      if (hasNextPage) {
         nextPageComponent.setStyle(
            nextPageComponent.getStyle()
               .withClickEvent(new ClickEvent(Action.RUN_COMMAND, String.format("%s %d", commandPrefix, this.page + 1)))
               .withHoverEvent(new HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal("Нажми чтобы посмотреть следующую страницу")))
         );
      } else {
         nextPageComponent.setStyle(nextPageComponent.getStyle().withColor(Formatting.DARK_GRAY));
      }

      MutableText pagerComponent = Text.literal("");
      pagerComponent.setStyle(pagerComponent.getStyle().withColor(Formatting.GRAY));
      pagerComponent.append(prevPageComponent);
      pagerComponent.append(" | ");
      pagerComponent.append(nextPageComponent);
      pagerComponent.append(String.format(" %d/%d", this.page, this.getMaxPage()));
      this.logDirect(new Text[]{pagerComponent});
   }

   public void display(Function<E, Text> transform) {
      this.display(transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, Paginator<T> pagi, Runnable pre, Function<T, Text> transform, String commandPrefix) throws CommandException {
      int page = 1;
      consumer.requireMax(1);
      if (consumer.hasAny() && !pagi.validPage(page = consumer.getAs(Integer.class))) {
         throw new CommandInvalidTypeException(consumer.consumed(), String.format("a valid page (1-%d)", pagi.getMaxPage()), consumer.consumed().getValue());
      } else {
         pagi.skipPages(page - pagi.page);
         if (pre != null) {
            pre.run();
         }

         pagi.display(transform, commandPrefix);
      }
   }

   public static <T> void paginate(IArgConsumer consumer, List<T> elems, Runnable pre, Function<T, Text> transform, String commandPrefix) throws CommandException {
      paginate(consumer, new Paginator<>(elems), pre, transform, commandPrefix);
   }

   public static <T> void paginate(IArgConsumer consumer, T[] elems, Runnable pre, Function<T, Text> transform, String commandPrefix) throws CommandException {
      paginate(consumer, Arrays.asList(elems), pre, transform, commandPrefix);
   }

   public static <T> void paginate(IArgConsumer consumer, Paginator<T> pagi, Function<T, Text> transform, String commandPrefix) throws CommandException {
      paginate(consumer, pagi, null, transform, commandPrefix);
   }

   public static <T> void paginate(IArgConsumer consumer, List<T> elems, Function<T, Text> transform, String commandPrefix) throws CommandException {
      paginate(consumer, new Paginator<>(elems), null, transform, commandPrefix);
   }

   public static <T> void paginate(IArgConsumer consumer, T[] elems, Function<T, Text> transform, String commandPrefix) throws CommandException {
      paginate(consumer, Arrays.asList(elems), null, transform, commandPrefix);
   }

   public static <T> void paginate(IArgConsumer consumer, Paginator<T> pagi, Runnable pre, Function<T, Text> transform) throws CommandException {
      paginate(consumer, pagi, pre, transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, List<T> elems, Runnable pre, Function<T, Text> transform) throws CommandException {
      paginate(consumer, new Paginator<>(elems), pre, transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, T[] elems, Runnable pre, Function<T, Text> transform) throws CommandException {
      paginate(consumer, Arrays.asList(elems), pre, transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, Paginator<T> pagi, Function<T, Text> transform) throws CommandException {
      paginate(consumer, pagi, null, transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, List<T> elems, Function<T, Text> transform) throws CommandException {
      paginate(consumer, new Paginator<>(elems), null, transform, null);
   }

   public static <T> void paginate(IArgConsumer consumer, T[] elems, Function<T, Text> transform) throws CommandException {
      paginate(consumer, Arrays.asList(elems), null, transform, null);
   }
}
