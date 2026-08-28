package code.elixir;

import code.elixir.commands.CommandDispatcher;
import code.elixir.commands.manager.CommandRepository;
import code.elixir.common.repository.box.BoxESPRepository;
import code.elixir.common.repository.macro.MacroRepository;
import code.elixir.common.repository.rct.RCTRepository;
import code.elixir.common.repository.way.WayRepository;
import code.elixir.features.impl.misc.HolyWorldAutoJoin;
import code.elixir.features.module.ModuleProvider;
import code.elixir.features.module.ModuleRepository;
import code.elixir.features.module.ModuleSwitcher;
import code.elixir.main.client.ClientInfo;
import code.elixir.main.client.ClientInfoProvider;
import code.elixir.main.listener.ListenerRepository;
import code.elixir.utils.client.auth.TelegramAuthManager;
import code.elixir.utils.client.bind.BindScrollBridge;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.api.draggable.DraggableRepository;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.managers.event.EventWarmup;
import code.elixir.utils.client.managers.file.DirectoryCreator;
import code.elixir.utils.client.managers.file.FileController;
import code.elixir.utils.client.managers.file.FileRepository;
import code.elixir.utils.client.managers.file.exception.FileProcessingException;
import code.elixir.utils.client.managers.file.impl.AccountFile;
import code.elixir.utils.client.managers.file.impl.account.AccountRepository;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.config.ConfigManager;
import code.elixir.utils.connection.cloud.CloudConfigWebSocketClient;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.features.aura.FreeLookStorage;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.aura.striking.StrikerConstructor;
import com.google.common.eventbus.EventBus;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

public class Elixir implements ModInitializer {
   static Elixir instance;
   private EventManager eventManager = new EventManager();
   private EventBus eventBus = new EventBus();
   private ModuleRepository moduleRepository;
   private ModuleSwitcher moduleSwitcher;
   private BindScrollBridge bindScrollBridge = new BindScrollBridge();
   private CommandRepository commandRepository;
   private CommandDispatcher commandDispatcher;
   private BoxESPRepository boxESPRepository = new BoxESPRepository(this.eventManager);
   private MacroRepository macroRepository = new MacroRepository(this.eventManager);
   private WayRepository wayRepository = new WayRepository(this.eventManager);
   private RCTRepository RCTRepository = new RCTRepository(this.eventManager);
   private HolyWorldAutoJoin holyWorldAutoJoin;
   private ModuleProvider moduleProvider;
   private DraggableRepository draggableRepository;
   private FileRepository fileRepository;
   private FileController fileController;
   private ScissorAssist scissorManager = new ScissorAssist();
   private ClientInfoProvider clientInfoProvider;
   private ListenerRepository listenerRepository;
   private CloudConfigWebSocketClient cloudConfigClient;
   private StrikerConstructor attackPerpetrator = new StrikerConstructor();
   private AccountRepository accountRepository;
   private String nativeUsername = getMinecraftUsernameStatic();
   private String nativeUserIdentifier = "1337";
   private String nativeUserRole = "admin";
   private boolean initialized;
   private ScheduledExecutorService reconnectScheduler;
   private boolean reconnecting = false;

   public void onInitialize() {
      instance = this;
      EventWarmup.warmup();
      this.initNativeAPI();
      this.initClientInfoProvider();
      this.initModules();
      this.initDraggable();
      this.initFileManager();
      this.initCommands();
      this.initListeners();
      this.initHolyWorldAutoJoin();
      new RotationStorage();
      new FreeLookStorage();
      SoundManager.init();
      this.checkFirstLaunch();
      this.initialized = true;
   }

   private void checkFirstLaunch() {
      try {
         File clientDir = FabricLoader.getInstance().getGameDir().resolve("elixir").toFile();
         if (!clientDir.exists()) {
            clientDir.mkdirs();
         }

         File firstLaunchFile = new File(clientDir, ".first_launch");
         if (!firstLaunchFile.exists()) {
            firstLaunchFile.createNewFile();
            Util.getOperatingSystem().open(new URI("https://t.me/ElixirClient"));
         }
      } catch (Exception var3) {
         Logger.error("Failed to process first launch: " + var3.getMessage());
      }
   }

   public void syncSiteIdentity() {
      TelegramAuthManager.ensureInitialized();
      if (TelegramAuthManager.isAuthorized()) {
         this.nativeUsername = TelegramAuthManager.getDisplayNameOrFallback(this.nativeUsername);
         this.nativeUserIdentifier = TelegramAuthManager.getUidOrFallback(this.nativeUserIdentifier);
         this.nativeUserRole = TelegramAuthManager.getRoleOrFallback(this.nativeUserRole);
         if (this.clientInfoProvider != null) {
            this.clientInfoProvider = new ClientInfo(
               "Elixir",
               this.nativeUsername,
               this.nativeUserRole,
               this.clientInfoProvider.clientDir(),
               this.clientInfoProvider.filesDir(),
               this.clientInfoProvider.configsDir()
            );
         }
      }
   }

   private void initNativeAPI() {
      for (Elixir.BridgeDefinition definition : Elixir.BridgeDefinition.ordered()) {
         try {
            Class<?> bridgeClass = Class.forName(definition.className());
            Object userName = this.invokeBridgeMethod(bridgeClass, definition.userNameMethod());
            if (userName != null) {
               this.nativeUsername = userName.toString();
            }

            Object userId;
            if ((userId = this.invokeBridgeMethod(bridgeClass, definition.userIdMethod())) != null) {
               this.nativeUserIdentifier = String.valueOf(userId);
            }

            Object userRole;
            this.nativeUserRole = (userRole = this.invokeBridgeMethod(bridgeClass, definition.userRoleMethod())) != null ? userRole.toString() : "admin";
            this.logBridgeStatus(bridgeClass, definition.className());
            return;
         } catch (ClassNotFoundException var7) {
         } catch (Exception var8) {
            Logger.error("Failed to initialize native bridge " + definition.className() + ": " + var8.getMessage());
         }
      }

      this.nativeUsername = this.getMinecraftUsername();
      this.nativeUserIdentifier = "0";
      this.nativeUserRole = "admin";
      Logger.info("Native bridge fallback active: using pure Java defaults");
   }

   private Object invokeBridgeMethod(Class<?> bridgeClass, String methodName) throws Exception {
      Method method = bridgeClass.getMethod(methodName);
      return method.invoke(null);
   }

   private void logBridgeStatus(Class<?> bridgeClass, String bridgeName) {
      try {
         Method loadedMethod = bridgeClass.getMethod("isNativeLibraryLoaded");
         Method sourceMethod = bridgeClass.getMethod("getNativeLoadSource");
         Object loaded = loadedMethod.invoke(null);
         Object source = sourceMethod.invoke(null);
         Logger.info("Native bridge active: " + bridgeName + ", nativeLoaded=" + loaded + ", source=" + source);
      } catch (Exception var7) {
         Logger.info("Native bridge active: " + bridgeName);
      }
   }

   private String getMinecraftUsername() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.getSession() != null) {
            return mc.getSession().getUsername();
         }
      } catch (Exception var2) {
         Logger.error("Failed to get Minecraft username: " + var2.getMessage());
      }

      return "t.me/Elixir";
   }

   private static String getMinecraftUsernameStatic() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.getSession() != null) {
            return mc.getSession().getUsername();
         }
      } catch (Exception var1) {
      }

      return "t.me/Elixir";
   }

   private void initWebSocketClient() {
      try {
         if (this.cloudConfigClient != null) {
            this.cloudConfigClient.connect();
         }
      } catch (Exception var2) {
         Logger.error("Failed to initialize WebSocket client: " + var2.getMessage());
      }
   }

   private void initDraggable() {
      this.draggableRepository = new DraggableRepository();
      this.draggableRepository.setup();
   }

   private void initModules() {
      this.moduleRepository = new ModuleRepository();
      this.moduleRepository.setup();
      this.moduleProvider = new ModuleProvider(this.moduleRepository.modules());
      this.moduleSwitcher = new ModuleSwitcher(this.moduleRepository.modules(), this.eventManager);
      EventManager.register(this.bindScrollBridge);
   }

   private void initCommands() {
      this.commandRepository = new CommandRepository();
      this.commandDispatcher = new CommandDispatcher(this.eventManager);
   }

   private void initHolyWorldAutoJoin() {
      this.holyWorldAutoJoin = new HolyWorldAutoJoin();
      EventManager.register(this.holyWorldAutoJoin);
   }

   private void initClientInfoProvider() {
      try {
         File gameDir = FabricLoader.getInstance().getGameDir().toFile();
         File clientDirectory = new File(gameDir, "Elixir");
         File filesDirectory = new File(clientDirectory, "files");
         File moduleFilesDirectory = new File(filesDirectory, "config");
         this.clientInfoProvider = new ClientInfo("Elixir", this.nativeUsername, this.nativeUserRole, clientDirectory, filesDirectory, moduleFilesDirectory);
      } catch (Exception var5) {
         Logger.error("Failed to initialize client info provider: " + var5.getMessage());
      }
   }

   private void initFileManager() {
      if (this.clientInfoProvider == null) {
         Logger.error("ClientInfoProvider is null, cannot initialize file manager");
      } else {
         DirectoryCreator directoryCreator = new DirectoryCreator();
         directoryCreator.createDirectories(this.clientInfoProvider.clientDir(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());
         this.fileRepository = new FileRepository();
         this.fileRepository.setup(this);
         this.accountRepository = new AccountRepository();
         this.fileRepository.getClientFiles().add(new AccountFile(this.accountRepository));
         this.fileController = new FileController(
            this.fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir()
         );

         try {
            this.fileController.loadFiles();
            TelegramAuthManager.ensureInitialized();
            this.syncSiteIdentity();
            ConfigManager.loadDefaultConfig();
         } catch (FileProcessingException var3) {
            Logger.error("Failed to load files: " + var3.getMessage());
         }
      }
   }

   private void initListeners() {
      this.listenerRepository = new ListenerRepository();
      this.listenerRepository.setup();
   }

   public String getNativeUsername() {
      return TelegramAuthManager.getDisplayNameOrFallback(this.nativeUsername);
   }

   public String getNativeUserIdentifier() {
      return TelegramAuthManager.getUidOrFallback(this.nativeUserIdentifier);
   }

   public String getNativeUserRole() {
      return TelegramAuthManager.getRoleOrFallback(this.nativeUserRole);
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventBus getEventBus() {
      return this.eventBus;
   }

   public ModuleRepository getModuleRepository() {
      return this.moduleRepository;
   }

   public ModuleSwitcher getModuleSwitcher() {
      return this.moduleSwitcher;
   }

   public BindScrollBridge getBindScrollBridge() {
      return this.bindScrollBridge;
   }

   public CommandRepository getCommandRepository() {
      return this.commandRepository;
   }

   public CommandDispatcher getCommandDispatcher() {
      return this.commandDispatcher;
   }

   public BoxESPRepository getBoxESPRepository() {
      return this.boxESPRepository;
   }

   public MacroRepository getMacroRepository() {
      return this.macroRepository;
   }

   public WayRepository getWayRepository() {
      return this.wayRepository;
   }

   public RCTRepository getRCTRepository() {
      return this.RCTRepository;
   }

   public HolyWorldAutoJoin getHolyWorldAutoJoin() {
      return this.holyWorldAutoJoin;
   }

   public ModuleProvider getModuleProvider() {
      return this.moduleProvider;
   }

   public DraggableRepository getDraggableRepository() {
      return this.draggableRepository;
   }

   public FileRepository getFileRepository() {
      return this.fileRepository;
   }

   public FileController getFileController() {
      return this.fileController;
   }

   public ScissorAssist getScissorManager() {
      return this.scissorManager;
   }

   public ClientInfoProvider getClientInfoProvider() {
      return this.clientInfoProvider;
   }

   public ListenerRepository getListenerRepository() {
      return this.listenerRepository;
   }

   public CloudConfigWebSocketClient getCloudConfigClient() {
      return this.cloudConfigClient;
   }

   public StrikerConstructor getAttackPerpetrator() {
      return this.attackPerpetrator;
   }

   public AccountRepository getAccountRepository() {
      return this.accountRepository;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public ScheduledExecutorService getReconnectScheduler() {
      return this.reconnectScheduler;
   }

   public boolean isReconnecting() {
      return this.reconnecting;
   }

   public void setEventManager(EventManager eventManager) {
      this.eventManager = eventManager;
   }

   public void setEventBus(EventBus eventBus) {
      this.eventBus = eventBus;
   }

   public void setModuleRepository(ModuleRepository moduleRepository) {
      this.moduleRepository = moduleRepository;
   }

   public void setModuleSwitcher(ModuleSwitcher moduleSwitcher) {
      this.moduleSwitcher = moduleSwitcher;
   }

   public void setBindScrollBridge(BindScrollBridge bindScrollBridge) {
      this.bindScrollBridge = bindScrollBridge;
   }

   public void setCommandRepository(CommandRepository commandRepository) {
      this.commandRepository = commandRepository;
   }

   public void setCommandDispatcher(CommandDispatcher commandDispatcher) {
      this.commandDispatcher = commandDispatcher;
   }

   public void setBoxESPRepository(BoxESPRepository boxESPRepository) {
      this.boxESPRepository = boxESPRepository;
   }

   public void setMacroRepository(MacroRepository macroRepository) {
      this.macroRepository = macroRepository;
   }

   public void setWayRepository(WayRepository wayRepository) {
      this.wayRepository = wayRepository;
   }

   public void setRCTRepository(RCTRepository RCTRepository2) {
      this.RCTRepository = RCTRepository2;
   }

   public void setHolyWorldAutoJoin(HolyWorldAutoJoin holyWorldAutoJoin) {
      this.holyWorldAutoJoin = holyWorldAutoJoin;
   }

   public void setModuleProvider(ModuleProvider moduleProvider) {
      this.moduleProvider = moduleProvider;
   }

   public void setDraggableRepository(DraggableRepository draggableRepository) {
      this.draggableRepository = draggableRepository;
   }

   public void setFileRepository(FileRepository fileRepository) {
      this.fileRepository = fileRepository;
   }

   public void setFileController(FileController fileController) {
      this.fileController = fileController;
   }

   public void setScissorManager(ScissorAssist scissorManager) {
      this.scissorManager = scissorManager;
   }

   public void setClientInfoProvider(ClientInfoProvider clientInfoProvider) {
      this.clientInfoProvider = clientInfoProvider;
   }

   public void setListenerRepository(ListenerRepository listenerRepository) {
      this.listenerRepository = listenerRepository;
   }

   public void setCloudConfigClient(CloudConfigWebSocketClient cloudConfigClient) {
      this.cloudConfigClient = cloudConfigClient;
   }

   public void setAttackPerpetrator(StrikerConstructor attackPerpetrator) {
      this.attackPerpetrator = attackPerpetrator;
   }

   public void setAccountRepository(AccountRepository accountRepository) {
      this.accountRepository = accountRepository;
   }

   public void setNativeUsername(String nativeUsername) {
      this.nativeUsername = nativeUsername;
   }

   public void setNativeUserIdentifier(String nativeUserIdentifier) {
      this.nativeUserIdentifier = nativeUserIdentifier;
   }

   public void setNativeUserRole(String nativeUserRole) {
      this.nativeUserRole = nativeUserRole;
   }

   public void setInitialized(boolean initialized) {
      this.initialized = initialized;
   }

   public void setReconnectScheduler(ScheduledExecutorService reconnectScheduler) {
      this.reconnectScheduler = reconnectScheduler;
   }

   public void setReconnecting(boolean reconnecting) {
      this.reconnecting = reconnecting;
   }

   public static Elixir getInstance() {
      return instance;
   }

   private record BridgeDefinition(String className, String userNameMethod, String userIdMethod, String userRoleMethod) {
      private static List<Elixir.BridgeDefinition> ordered() {
         return List.of(new Elixir.BridgeDefinition("ru.dreamix.fabricloader.VMBridge", "getUserName", "getUserID", "getUserRole"));
      }
   }
}
