package code.elixir.utils.client.managers.file;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.file.impl.BlockESPFile;
import code.elixir.utils.client.managers.file.impl.ElementsFile;
import code.elixir.utils.client.managers.file.impl.EntityESPFile;
import code.elixir.utils.client.managers.file.impl.FriendFile;
import code.elixir.utils.client.managers.file.impl.MacroFile;
import code.elixir.utils.client.managers.file.impl.ModuleFile;
import code.elixir.utils.client.managers.file.impl.PrefixFile;
import code.elixir.utils.client.managers.file.impl.StaffFile;
import code.elixir.utils.client.managers.file.impl.WayFile;
import java.util.ArrayList;
import java.util.List;

public class FileRepository {
   private final List<ClientFile> clientFiles = new ArrayList<>();

   public void setup(Elixir main) {
      this.register(
         new ModuleFile(main.getModuleRepository(), main.getDraggableRepository()),
         new EntityESPFile(main.getBoxESPRepository()),
         new BlockESPFile(main.getBoxESPRepository()),
         new MacroFile(main.getMacroRepository()),
         new WayFile(main.getWayRepository()),
         new PrefixFile(),
         new FriendFile(),
         new StaffFile(),
         new ElementsFile(main.getDraggableRepository())
      );
   }

   public void register(ClientFile... clientFIle) {
      this.clientFiles.addAll(List.of(clientFIle));
   }

   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
