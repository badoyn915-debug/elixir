package code.elixir.utils.client.managers.file.impl;

import code.elixir.utils.client.managers.file.ClientFile;
import code.elixir.utils.client.managers.file.exception.FileLoadException;
import code.elixir.utils.client.managers.file.exception.FileSaveException;
import code.elixir.utils.client.managers.file.impl.account.Account;
import code.elixir.utils.client.managers.file.impl.account.AccountRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class AccountFile extends ClientFile {
   private final AccountRepository accountRepository;

   public AccountFile(AccountRepository accountRepository) {
      super("accounts");
      this.accountRepository = accountRepository;
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this.accountRepository.accountList, writer);
         }
      } catch (IOException | JsonIOException var9) {
         throw new FileSaveException("Failed to save accounts to file", var9);
      }
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");

      try {
         try (FileReader reader = new FileReader(file)) {
            Account[] accounts = (Account[])gson.fromJson(reader, Account[].class);
            this.accountRepository.accountList.clear();
            this.accountRepository.accountList.addAll(Arrays.asList(accounts));
         }
      } catch (IOException var9) {
         throw new FileLoadException("Failed to load accounts from file", var9);
      } catch (JsonSyntaxException var10) {
         throw new FileLoadException("JSON syntax error, accounts config cannot be loaded", var10);
      } catch (JsonIOException var11) {
         throw new FileLoadException("JSON IO error, accounts config cannot be loaded", var11);
      }
   }
}
