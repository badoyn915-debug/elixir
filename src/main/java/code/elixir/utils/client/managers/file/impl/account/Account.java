package code.elixir.utils.client.managers.file.impl.account;

public class Account {
   public String name;
   public boolean starred;
   public float starAnim;
   public boolean active;
   boolean premium;
   String refreshToken;
   public String uuid;
   String accessToken;

   public Account(String name, boolean starred, boolean premium, String refreshToken, String uuid, String accessToken) {
      this.name = name;
      this.starred = starred;
      this.premium = premium;
      this.refreshToken = refreshToken;
      this.uuid = uuid;
      this.accessToken = accessToken;
      this.starAnim = starred ? 1.0F : 0.0F;
   }
}
