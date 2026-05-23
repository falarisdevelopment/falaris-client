package dev.falaris.client;

import dev.falaris.client.config.ConfigManager;
import dev.falaris.client.alt.AltManager;
import dev.falaris.client.event.EventBus;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.event.events.RenderWorldEvent;
import dev.falaris.client.gui.alt.AltManagerScreen;
import dev.falaris.client.gui.click.ClickGuiScreen;
import dev.falaris.client.keybind.KeybindManager;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.module.modules.combat.AimAssist;
import dev.falaris.client.module.modules.combat.AimBot;
import dev.falaris.client.module.modules.combat.AnchorAura;
import dev.falaris.client.module.modules.combat.CrystalAura;
import dev.falaris.client.module.modules.combat.KillAura;
import dev.falaris.client.module.modules.combat.SilentAura;
import dev.falaris.client.module.modules.combat.TriggerBot;
import dev.falaris.client.module.modules.client.ClickGuiModule;
import dev.falaris.client.module.modules.client.DiscordRpc;
import dev.falaris.client.module.modules.movement.AirPlace;
import dev.falaris.client.module.modules.movement.AutoSprint;
import dev.falaris.client.module.modules.movement.AutoWalk;
import dev.falaris.client.module.modules.movement.BoatFly;
import dev.falaris.client.module.modules.movement.ElytraFly;
import dev.falaris.client.module.modules.movement.ElytraPlus;
import dev.falaris.client.module.modules.movement.Flight;
import dev.falaris.client.module.modules.movement.Jesus;
import dev.falaris.client.module.modules.movement.NoFall;
import dev.falaris.client.module.modules.movement.PacketMine;
import dev.falaris.client.module.modules.misc.AutoReconnect;
import dev.falaris.client.module.modules.misc.Freecam;
import dev.falaris.client.module.modules.misc.KillPotion;
import dev.falaris.client.module.modules.misc.MaceDMG;
import dev.falaris.client.module.modules.misc.Nuker;
import dev.falaris.client.module.modules.player.AntiHunger;
import dev.falaris.client.module.modules.player.AutoArmor;
import dev.falaris.client.module.modules.player.AutoLibrarian;
import dev.falaris.client.module.modules.player.AutoSword;
import dev.falaris.client.module.modules.player.AutoTool;
import dev.falaris.client.module.modules.player.AutoTotem;
import dev.falaris.client.module.modules.render.ArmorHud;
import dev.falaris.client.module.modules.render.BlockESP;
import dev.falaris.client.module.modules.render.ESP;
import dev.falaris.client.module.modules.render.Fullbright;
import dev.falaris.client.module.modules.render.InventoryHud;
import dev.falaris.client.module.modules.render.NameTags;
import dev.falaris.client.module.modules.render.ShulkerPreview;
import dev.falaris.client.module.modules.render.StorageESP;
import dev.falaris.client.module.modules.render.Tracers;
import dev.falaris.client.module.modules.render.Trajectories;
import dev.falaris.client.module.modules.render.XRay;
import dev.falaris.client.rotation.RotationManager;
import dev.falaris.client.event.events.RenderHudEvent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
    private final ModuleManager moduleManager = new ModuleManager(eventBus, configManager);
    private final AltManager altManager = new AltManager(configManager);
    private final KeybindManager keybindManager = new KeybindManager(moduleManager);
    private final RotationManager rotationManager = new RotationManager();
    private final SafeDelay safeDelay = new SafeDelay();

    @Override
    public void onInitializeClient() {
        instance = this;

        moduleManager.register(new ClickGuiModule());
        moduleManager.register(new DiscordRpc());
        moduleManager.register(new KillAura());
        moduleManager.register(new SilentAura());
        moduleManager.register(new CrystalAura());
        moduleManager.register(new AimBot());
        moduleManager.register(new AimAssist());
        moduleManager.register(new TriggerBot());
        moduleManager.register(new AnchorAura());
        moduleManager.register(new Flight());
        moduleManager.register(new ElytraFly());
        moduleManager.register(new ElytraPlus());
        moduleManager.register(new BoatFly());
        moduleManager.register(new Jesus());
        moduleManager.register(new NoFall());
        moduleManager.register(new AutoSprint());
        moduleManager.register(new AutoWalk());
        moduleManager.register(new AirPlace());
        moduleManager.register(new PacketMine());
        moduleManager.register(new AutoTotem());
        moduleManager.register(new AutoArmor());
        moduleManager.register(new AutoTool());
        moduleManager.register(new AutoSword());
        moduleManager.register(new AutoLibrarian());
        moduleManager.register(new AntiHunger());
        moduleManager.register(new ESP());
        moduleManager.register(new StorageESP());
        moduleManager.register(new BlockESP());
        moduleManager.register(new Tracers());
        moduleManager.register(new NameTags());
        moduleManager.register(new Fullbright());
        moduleManager.register(new ArmorHud());
        moduleManager.register(new InventoryHud());
        moduleManager.register(new ShulkerPreview());
        moduleManager.register(new XRay());
        moduleManager.register(new Trajectories());
        moduleManager.register(new Freecam());
        moduleManager.register(new Nuker());
        moduleManager.register(new AutoReconnect());
        moduleManager.register(new MaceDMG());
        moduleManager.register(new KillPotion());
        configManager.load(moduleManager);
        altManager.load();

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
        eventBus.post(new ClientTickEvent(client));
    }

    public void openClickGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ClickGuiScreen(moduleManager));
    }

    private static void addDrawableChild(Screen screen, ButtonWidget button) {
        try {
            Method method = Screen.class.getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Element.class);
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

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    public RotationManager getRotationManager() {
        return rotationManager;
    }

    public SafeDelay getSafeDelay() {
        return safeDelay;
    }

    public AltManager getAltManager() {
        return altManager;
    }

    public void openAltManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new AltManagerScreen(altManager));
    }
}
