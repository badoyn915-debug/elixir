package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.EntityType;

public enum EntityESPDataType implements IDatatypeFor<EntityType<?>> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> ways = this.getEntities().map(s -> s.getName().getString().replace(" ", "_"));
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public EntityType<?> get(IDatatypeContext datatypeContext) throws CommandException {
      return this.findEntity(datatypeContext.getConsumer().getString()).orElse(null);
   }

   public Optional<EntityType<?>> findEntity(String text) {
      return this.getEntities().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst();
   }

   private Stream<EntityType<?>> getEntities() {
      return Elixir.getInstance().getBoxESPRepository().entities.keySet().stream();
   }
}
