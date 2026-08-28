package code.elixir.utils.client.managers.api.command.datatypes;

import code.elixir.common.repository.friend.Friend;
import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.utils.client.managers.api.command.exception.CommandException;
import code.elixir.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;

public enum FriendDataType implements IDatatypeFor<Friend> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> friends = this.getFriends().stream().map(Friend::getName);
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(friends).filterPrefix(context).sortAlphabetically().stream();
   }

   public Friend get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return this.getFriends().stream().filter(s -> s.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
   }

   private List<? extends Friend> getFriends() {
      return FriendUtils.getFriends();
   }
}
