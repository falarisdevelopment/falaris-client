package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.StringSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

import java.lang.reflect.Field;
import java.util.Arrays;

public final class ClientSpoof extends MiscModule {
    private static String activeBrand = "vanilla";
    private static boolean spoofActive = false;

    private final ModeSetting clientMode = setting(new ModeSetting("Spoof As", "Client brand to spoof.", "Vanilla",
        "Vanilla", "Lunar", "Feather", "Badlion", "LabyMod", "OptiFine", "PVP Lounge", "Custom"));
    private final StringSetting customBrand = setting(new StringSetting("Custom Brand", "Custom brand string (if Spoof As = Custom).", "vanilla"));

    public ClientSpoof() {
        super("ClientSpoof", "Spoofs your client brand to appear as another client.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        spoofActive = true;
        applyBrand();
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            if (spoofActive) {
                sendSpoofedBrand(client);
            }
        });
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        spoofActive = false;
        activeBrand = "vanilla";
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (!spoofActive) return;
        String current = resolveBrand();
        if (!current.equals(activeBrand)) {
            activeBrand = current;
            applyBrand();
        }
    }

    private String resolveBrand() {
        String mode = clientMode.get();
        return switch (mode) {
            case "Vanilla" -> "vanilla";
            case "Lunar" -> "Lunar-Client";
            case "Feather" -> "Feather";
            case "Badlion" -> "Badlion";
            case "LabyMod" -> "LabyMod";
            case "OptiFine" -> "OptiFine";
            case "PVP Lounge" -> "PVP-Lounge";
            case "Custom" -> customBrand.get().isEmpty() ? "vanilla" : customBrand.get();
            default -> "vanilla";
        };
    }

    private void applyBrand() {
        activeBrand = resolveBrand();
        try {
            Class<?> brandClass = ClientBrandRetrieverReflect.getBrandClass();
            if (brandClass != null) {
                ClientBrandRetrieverReflect.setBrand(activeBrand);
            }
        } catch (Exception ignored) {}
    }

    private void sendSpoofedBrand(MinecraftClient client) {
        try {
            if (client.getNetworkHandler() == null) return;
            BrandCustomPayload payload = new BrandCustomPayload(activeBrand);
            CustomPayloadC2SPacket packet = new CustomPayloadC2SPacket(payload);
            client.getNetworkHandler().sendPacket(packet);
        } catch (Exception ignored) {}
    }

    private static final class ClientBrandRetrieverReflect {
        private static Class<?> brandClass;
        private static Object brandFieldInstance;

        static Class<?> getBrandClass() {
            if (brandClass != null) return brandClass;
            try {
                brandClass = Class.forName("net.minecraft.client.ClientBrandRetriever");
                return brandClass;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        static void setBrand(String brand) {
            try {
                Class<?> cls = getBrandClass();
                if (cls == null) return;
                Field[] fields = cls.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getType() == String.class) {
                        field.setAccessible(true);
                        field.set(null, brand);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
