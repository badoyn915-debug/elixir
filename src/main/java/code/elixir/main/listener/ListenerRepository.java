package code.elixir.main.listener;

import code.elixir.Elixir;
import code.elixir.emotions.EmotionWheelManager;
import code.elixir.features.impl.render.ItemReplacerSyncManager;
import code.elixir.features.impl.render.customcape.CapeSyncManager;
import code.elixir.features.impl.render.custommodels.CustomModelSyncManager;
import code.elixir.irc.IrcManager;
import code.elixir.main.listener.impl.EventListener;
import code.elixir.utils.client.managers.event.EventManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenerRepository {
   private final List<Listener> listeners = new ArrayList<>();

   public void setup() {
      this.registerListeners(
         new EventListener(),
         EmotionWheelManager.create(),
         CustomModelSyncManager.create(),
         ItemReplacerSyncManager.create(),
         CapeSyncManager.create(),
         IrcManager.create()
      );
   }

   public void registerListeners(Listener... listeners) {
      this.listeners.addAll(List.of(listeners));
      Arrays.stream(listeners).forEach(listener -> {
         Elixir.getInstance().getEventManager();
         EventManager.register(listener);
      });
   }

   public List<Listener> getListeners() {
      return this.listeners;
   }
}
