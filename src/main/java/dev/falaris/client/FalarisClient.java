package dev.falaris.client;

import dev.falaris.client.config.ConfigManager;
import dev.falaris.client.config.PresetsManager;
import dev.falaris.client.alt.AltManager;
import dev.falaris.client.backtrack.BacktrackManager;
import dev.falaris.client.command.CommandManager;
import dev.falaris.client.event.EventBus;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.event.events.RenderWorldEvent;
import dev.falaris.client.gui.alt.AltManagerScreen;
import dev.falaris.client.gui.click.ClickGuiScreen;
import dev.falaris.client.keybind.KeybindManager;
import org.lwjgl.glfw.GLFW;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.module.FriendsManager;
import dev.falaris.client.module.IgnoresManager;
import dev.falaris.client.module.modules.combat.AimAssist;
import dev.falaris.client.module.modules.combat.AimBot;
import dev.falaris.client.module.modules.combat.AnchorAura;
import dev.falaris.client.module.modules.combat.AutoAttributeSwap;
import dev.falaris.client.module.modules.combat.AutoShield;
import dev.falaris.client.module.modules.combat.Criticals;
import dev.falaris.client.module.modules.combat.AutoElytraMace;
import dev.falaris.client.module.modules.combat.AutoSwapMace;
import dev.falaris.client.module.modules.combat.AutoWindCharge;
import dev.falaris.client.module.modules.combat.AutoShieldDisable;
import dev.falaris.client.module.modules.combat.AutoSlam;
import dev.falaris.client.module.modules.combat.AutoCity;
import dev.falaris.client.module.modules.combat.OffhandSwap;
import dev.falaris.client.module.modules.combat.OrbitalStrike;
import dev.falaris.client.module.modules.combat.BedAura;
import dev.falaris.client.module.modules.combat.BreachSwap;
import dev.falaris.client.module.modules.combat.DoubleAnchor;
import dev.falaris.client.module.modules.combat.AutoDrain;
import dev.falaris.client.module.modules.combat.AutoClicker;
import dev.falaris.client.module.modules.combat.AutoWeb;
import dev.falaris.client.module.modules.combat.AutoTrap;
import dev.falaris.client.module.modules.combat.CrystalAura;
import dev.falaris.client.module.modules.combat.Hitboxes;
import dev.falaris.client.module.modules.combat.KillAura;
import dev.falaris.client.module.modules.combat.Reach;
import dev.falaris.client.module.modules.combat.SilentAura;
import dev.falaris.client.module.modules.combat.AutoMace;
import dev.falaris.client.module.modules.combat.Surround;
import dev.falaris.client.module.modules.combat.TriggerBot;
import dev.falaris.client.module.modules.combat.WTap;
import dev.falaris.client.module.modules.client.CheatingLevel;
import dev.falaris.client.module.modules.client.DiscordRpc;

import dev.falaris.client.module.modules.client.Presets;
import dev.falaris.client.module.modules.client.SkinChanger;
import dev.falaris.client.module.modules.movement.AirPlace;
import dev.falaris.client.module.modules.movement.AutoElytraBounce;
import dev.falaris.client.module.modules.movement.KeepSprint;
import dev.falaris.client.module.modules.movement.MaceVerticalControl;
import dev.falaris.client.module.modules.movement.AutoSprint;
import dev.falaris.client.module.modules.movement.AutoWalk;
import dev.falaris.client.module.modules.movement.BoatFly;
import dev.falaris.client.module.modules.movement.ElytraFly;
import dev.falaris.client.module.modules.movement.ElytraPlus;
import dev.falaris.client.module.modules.movement.Flight;
import dev.falaris.client.module.modules.movement.Jesus;
import dev.falaris.client.module.modules.movement.TridentBoost;
import dev.falaris.client.module.modules.movement.NoFall;
import dev.falaris.client.module.modules.movement.PacketMine;
import dev.falaris.client.module.modules.movement.Scaffold;
import dev.falaris.client.module.modules.misc.AutoReconnect;
import dev.falaris.client.module.modules.misc.Baritone;
import dev.falaris.client.module.modules.misc.ClientSpoof;
import dev.falaris.client.module.modules.misc.Freecam;
import dev.falaris.client.module.modules.misc.GodItems;
import dev.falaris.client.module.modules.misc.Hunter;
import dev.falaris.client.module.modules.misc.CheatProtector;
import dev.falaris.client.module.modules.misc.MaceAssist;
import dev.falaris.client.module.modules.misc.NoPush;
import dev.falaris.client.module.modules.misc.Nuker;
import dev.falaris.client.module.modules.misc.Velocity;
import dev.falaris.client.module.modules.misc.ElytraSwapper;
import dev.falaris.client.module.modules.misc.Macro;
import dev.falaris.client.module.modules.misc.ShortWindCharge;
import dev.falaris.client.module.modules.misc.AutoElytraSpear;
import dev.falaris.client.module.modules.misc.AntiBot;
import dev.falaris.client.module.modules.misc.AntiCheatDetector;
import dev.falaris.client.module.modules.player.AntiHunger;
import dev.falaris.client.module.modules.player.AntiVoid;
import dev.falaris.client.module.modules.player.AutoArmor;
import dev.falaris.client.module.modules.player.AutoEat;
import dev.falaris.client.module.modules.player.AutoGap;
import dev.falaris.client.module.modules.player.AutoPearlCatch;
import dev.falaris.client.module.modules.player.AutoPearl;
import dev.falaris.client.module.modules.player.AutoLibrarian;
import dev.falaris.client.module.modules.player.AutoSword;
import dev.falaris.client.module.modules.player.AutoTool;
import dev.falaris.client.module.modules.player.AutoTotem;
import dev.falaris.client.module.modules.player.ChestStealer;
import dev.falaris.client.module.modules.player.DupeUtility;
import dev.falaris.client.module.modules.player.FastUse;
import dev.falaris.client.module.modules.player.InvCleaner;
import dev.falaris.client.module.modules.player.AutoRespawn;
import dev.falaris.client.module.modules.player.NoSlowdown;
import dev.falaris.client.module.modules.player.Freelook;
import dev.falaris.client.module.modules.player.MileyCyrus;
import dev.falaris.client.module.modules.player.SafeWalk;
import dev.falaris.client.module.modules.player.AutoReplenish;
import dev.falaris.client.module.modules.player.IAmInnocent;
import dev.falaris.client.module.modules.render.AntiBlind;
import dev.falaris.client.module.modules.render.ClientDetector;
import dev.falaris.client.module.modules.render.NameProtect;
import dev.falaris.client.module.modules.render.ArrayListMod;
import dev.falaris.client.module.modules.render.Waifu;
import dev.falaris.client.module.modules.render.PlayerModel;
import dev.falaris.client.module.modules.render.ArmorHud;
import dev.falaris.client.module.modules.render.BlockESP;
import dev.falaris.client.module.modules.render.ESP;
import dev.falaris.client.module.modules.render.Fullbright;
import dev.falaris.client.module.modules.render.InventoryHud;
import dev.falaris.client.module.modules.render.NameTags;
import dev.falaris.client.module.modules.render.NoFog;
import dev.falaris.client.module.modules.render.NoRender;
import dev.falaris.client.module.modules.render.ShulkerPreview;
import dev.falaris.client.module.modules.render.StorageESP;
import dev.falaris.client.module.modules.render.Tracers;
import dev.falaris.client.module.modules.render.Trajectories;
import dev.falaris.client.module.modules.render.OptimizeZoom;
import dev.falaris.client.module.modules.render.XRay;
import dev.falaris.client.module.modules.render.Radar;
import dev.falaris.client.module.modules.movement.Speed;
import dev.falaris.client.module.modules.player.InvWalk;
import dev.falaris.client.module.modules.misc.AutoDisconnect;
import dev.falaris.client.module.modules.render.Keystrokes;
import dev.falaris.client.module.modules.render.Coordinates;
import dev.falaris.client.module.modules.render.PotionEffects;
import dev.falaris.client.module.modules.render.CPSCounter;
import dev.falaris.client.module.modules.render.ReachVisualizer;
import dev.falaris.client.module.modules.render.DamageIndicator;
import dev.falaris.client.module.modules.misc.Blink;
import dev.falaris.client.module.modules.misc.FakeLag;
import dev.falaris.client.module.modules.misc.AntiAFK;
import dev.falaris.client.module.modules.player.AutoFarm;
import dev.falaris.client.module.modules.player.AutoFish;
import dev.falaris.client.module.modules.player.AutoLava;
import dev.falaris.client.module.modules.player.AutoWater;
import dev.falaris.client.module.modules.player.AutoPot;
import dev.falaris.client.module.modules.player.AntiLevitate;
import dev.falaris.client.module.modules.player.Timer;
import dev.falaris.client.module.modules.render.HoleESP;
import dev.falaris.client.module.modules.render.ItemESP;
import dev.falaris.client.module.modules.render.Notifications;
import dev.falaris.client.rotation.RotationManager;
import dev.falaris.client.event.events.RenderHudEvent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import dev.falaris.client.util.PositionExtrapolation;
import dev.falaris.client.util.SafeDelay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class FalarisClient implements ClientModInitializer {
    public static final String MOD_ID = "falaris-client";
    public static final String NAME = "Falaris Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static FalarisClient instance;

    private final EventBus eventBus = new EventBus();
    private final ConfigManager configManager = new ConfigManager(MOD_ID);
    private final PresetsManager presetsManager = new PresetsManager(MOD_ID);
    private final ModuleManager moduleManager = new ModuleManager(eventBus, configManager);
    private final AltManager altManager = new AltManager(configManager);
    private final KeybindManager keybindManager = new KeybindManager(moduleManager);
    private final RotationManager rotationManager = new RotationManager();
    private final CommandManager commandManager = new CommandManager();
    private final SafeDelay safeDelay = new SafeDelay();
    private final FriendsManager friendsManager = new FriendsManager();
    private final IgnoresManager ignoresManager = new IgnoresManager();


    @Override
    public void onInitializeClient() {
        instance = this;

        moduleManager.register(new CheatingLevel());
        moduleManager.register(new DiscordRpc());
        moduleManager.register(new Presets());
        moduleManager.register(new SkinChanger());
        moduleManager.register(new KillAura());
        moduleManager.register(new SilentAura());
        moduleManager.register(new CrystalAura());
        moduleManager.register(new AimBot());
        moduleManager.register(new AimAssist());
        moduleManager.register(new TriggerBot());
        moduleManager.register(new AnchorAura());
        moduleManager.register(new BedAura());
        moduleManager.register(new Reach());
        moduleManager.register(new Hitboxes());
        moduleManager.register(new AutoMace());
        moduleManager.register(new Surround());
        moduleManager.register(new AutoAttributeSwap());
        moduleManager.register(new AutoCity());
        moduleManager.register(new AutoTrap());
        moduleManager.register(new OffhandSwap());
        moduleManager.register(new AutoShieldDisable());
        moduleManager.register(new AutoSlam());
        moduleManager.register(new AutoElytraMace());
        moduleManager.register(new AutoWindCharge());
        moduleManager.register(new AutoSwapMace());
        moduleManager.register(new WTap());
        moduleManager.register(new AutoShield());
        moduleManager.register(new OrbitalStrike());
        moduleManager.register(new Criticals());
        moduleManager.register(new Flight());
        moduleManager.register(new AutoElytraBounce());
        moduleManager.register(new TridentBoost());
        moduleManager.register(new ElytraFly());
        moduleManager.register(new ElytraPlus());
        moduleManager.register(new BoatFly());
        moduleManager.register(new Jesus());
        moduleManager.register(new NoFall());
        moduleManager.register(new KeepSprint());
        moduleManager.register(new MaceVerticalControl());
        moduleManager.register(new AutoSprint());
        moduleManager.register(new AutoWalk());
        moduleManager.register(new AirPlace());
        moduleManager.register(new PacketMine());
        moduleManager.register(new Scaffold());
        moduleManager.register(new AutoTotem());
        moduleManager.register(new AutoArmor());
        moduleManager.register(new AutoTool());
        moduleManager.register(new AutoSword());
        moduleManager.register(new AutoLibrarian());
        moduleManager.register(new AntiHunger());
        moduleManager.register(new AntiVoid());
        moduleManager.register(new AutoPearl());
        moduleManager.register(new AutoPearlCatch());
        moduleManager.register(new ChestStealer());
        moduleManager.register(new InvCleaner());
        moduleManager.register(new DupeUtility());
        moduleManager.register(new FastUse());
        moduleManager.register(new Freelook());
        moduleManager.register(new SafeWalk());
        moduleManager.register(new MileyCyrus());
        moduleManager.register(new AutoEat());
        moduleManager.register(new AutoRespawn());
        moduleManager.register(new NoSlowdown());
        moduleManager.register(new AutoGap());
        moduleManager.register(new ESP());
        moduleManager.register(new StorageESP());
        moduleManager.register(new BlockESP());
        moduleManager.register(new Tracers());
        moduleManager.register(new NameTags());
        moduleManager.register(new Fullbright());
        moduleManager.register(new ArmorHud());
        moduleManager.register(new ArrayListMod());
        moduleManager.register(new InventoryHud());
        moduleManager.register(new ShulkerPreview());
        moduleManager.register(new XRay());
        moduleManager.register(new Trajectories());
        moduleManager.register(new NoFog());
        moduleManager.register(new NoRender());
        moduleManager.register(new Radar());
        moduleManager.register(new ClientDetector());
        moduleManager.register(new NameProtect());
        moduleManager.register(new AntiBlind());
        moduleManager.register(new OptimizeZoom());
        moduleManager.register(new PlayerModel());
        moduleManager.register(new Waifu());
        moduleManager.register(new Baritone());
        moduleManager.register(new Freecam());
        moduleManager.register(new Nuker());
        moduleManager.register(new AutoReconnect());
        moduleManager.register(new MaceAssist());
        moduleManager.register(new GodItems());
        moduleManager.register(new Hunter());
        moduleManager.register(new CheatProtector());
        moduleManager.register(new ClientSpoof());
        moduleManager.register(new NoPush());
        moduleManager.register(new Velocity());
        moduleManager.register(new ElytraSwapper());
        moduleManager.register(new AutoReplenish());
        moduleManager.register(new IAmInnocent());
        moduleManager.register(new Macro());
        moduleManager.register(new BreachSwap());
        moduleManager.register(new DoubleAnchor());
        moduleManager.register(new AutoDrain());
        moduleManager.register(new AutoClicker());
        moduleManager.register(new AutoWeb());
        moduleManager.register(new ShortWindCharge());
        moduleManager.register(new AutoElytraSpear());
        moduleManager.register(new AntiBot());
        moduleManager.register(new AntiCheatDetector());
        moduleManager.register(new Speed());
        moduleManager.register(new InvWalk());
        moduleManager.register(new AutoDisconnect());
        moduleManager.register(new Keystrokes());
        moduleManager.register(new Coordinates());
        moduleManager.register(new PotionEffects());
        moduleManager.register(new CPSCounter());
        moduleManager.register(new Blink());
        moduleManager.register(new FakeLag());
        moduleManager.register(new AntiAFK());
        moduleManager.register(new AutoFarm());
        moduleManager.register(new AutoFish());
        moduleManager.register(new AutoLava());
        moduleManager.register(new AutoWater());
        moduleManager.register(new AntiLevitate());
        moduleManager.register(new Timer());
        moduleManager.register(new AutoPot());
        moduleManager.register(new ReachVisualizer());
        moduleManager.register(new DamageIndicator());
        commandManager.registerAll();
        moduleManager.register(new HoleESP());
        moduleManager.register(new ItemESP());
        moduleManager.register(new Notifications());
        configManager.load(moduleManager);
        altManager.load();
        configManager.loadFriends(friendsManager);
        configManager.loadIgnores(ignoresManager);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen titleScreen) {
                boolean exists = titleScreen.children().stream()
                        .filter(child -> child instanceof ButtonWidget)
                        .map(child -> ((ButtonWidget) child).getMessage().getString())
                        .anyMatch("Alt Manager"::equals);
                if (!exists) {
                    addDrawableChild(titleScreen, ButtonWidget.builder(Text.literal("Alt Manager"), button -> client.setScreen(new AltManagerScreen(altManager))).dimensions(
                            scaledWidth / 2 - 100,
                            scaledHeight / 4 + 96,
                            200,
                            20
                    ).build());
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> eventBus.post(new RenderWorldEvent(context)));
        HudRenderCallback.EVENT.register((context, renderTickCounter) -> eventBus.post(new RenderHudEvent(context, renderTickCounter.getTickProgress(false))));

        LOGGER.info("{} initialized.", NAME);
    }

    private void onEndClientTick(MinecraftClient client) {
        safeDelay.tick();
        keybindManager.tick(client);
        rotationManager.tick(client);
        if (client.world != null) {
            BacktrackManager.getInstance().capture(client.world);
            client.world.getEntities().forEach(PositionExtrapolation::record);
        }
        eventBus.post(new ClientTickEvent(client));
        // GUI keybind — skip when chat is open
        if (!(client.currentScreen instanceof ClickGuiScreen) && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) && client.getWindow() != null) {
            long handle = client.getWindow().getHandle();
            boolean pressed = GLFW.glfwGetKey(handle, ClickGuiScreen.guiKeyCode) == GLFW.GLFW_PRESS;
            if (pressed && !ClickGuiScreen.guiKeyHeld) { ClickGuiScreen.guiKeyHeld = true; client.setScreen(new ClickGuiScreen(moduleManager)); }
            else if (!pressed) ClickGuiScreen.guiKeyHeld = false;
        }
    }

    private static void addDrawableChild(Screen screen, ButtonWidget button) {
        try {
            Method method = Screen.class.getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Drawable.class);
            method.setAccessible(true);
            method.invoke(screen, button);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to add Alt Manager button to title screen.", exception);
        }
    }

    public static FalarisClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Falaris Client has not initialized yet.");
        }

        return instance;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PresetsManager getPresetsManager() {
        return presetsManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    public RotationManager getRotationManager() {
        return rotationManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public SafeDelay getSafeDelay() {
        return safeDelay;
    }

    public FriendsManager getFriendsManager() {
        return friendsManager;
    }

    public IgnoresManager getIgnoresManager() {
        return ignoresManager;
    }

    public AltManager getAltManager() {
        return altManager;
    }

    public void openAltManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new AltManagerScreen(altManager));
    }
}
