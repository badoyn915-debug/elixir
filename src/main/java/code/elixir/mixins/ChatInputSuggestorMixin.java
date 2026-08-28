package code.elixir.mixins;

import code.elixir.events.chat.TabCompleteEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatInputSuggestor.class})
public class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   @Final
   private List<OrderedText> messages;
   @Shadow
   private ParseResults<?> parse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionWindow window;
   @Shadow
   boolean completingSuggestions;

   @Inject(
      method = {"refresh"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preUpdateSuggestion(CallbackInfo ci) {
      String prefix = this.textField.getText().substring(0, Math.min(this.textField.getText().length(), this.textField.getCursor()));
      TabCompleteEvent event = SafeEvents.dispatch(() -> new TabCompleteEvent(prefix));
      if (event != null && event.isCancelled()) {
         ci.cancel();
      } else {
         if (event != null && event.completions != null) {
            ci.cancel();
            this.parse = null;
            if (this.completingSuggestions) {
               return;
            }

            this.textField.setSuggestion(null);
            this.window = null;
            this.messages.clear();
            if (event.completions.length == 0) {
               this.pendingSuggestions = Suggestions.empty();
            } else {
               StringRange range = StringRange.between(prefix.lastIndexOf(" ") + 1, prefix.length());
               List suggestionList = Stream.of(event.completions).map(s -> new Suggestion(range, s)).collect(Collectors.toList());
               Suggestions suggestions = new Suggestions(range, suggestionList);
               this.pendingSuggestions = new CompletableFuture<>();
               this.pendingSuggestions.complete(suggestions);
            }

            ((ChatInputSuggestor)(Object)this).show(true);
         }
      }
   }
}
