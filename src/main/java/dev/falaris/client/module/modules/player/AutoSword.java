package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;

public final class AutoSword extends PlayerModule {
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between weapon switches.", 1, 1, 20));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between weapon switches.", 0, 0, 10));
    private final BooleanSetting allowAxes = setting(new BooleanSetting("Allow Axes", "Use axes if no sword is better.", true));
    private final BooleanSetting hotbarOnly = setting(new BooleanSetting("Hotbar Only", "Only select weapons already in the hotbar.", true));
    private final BooleanSetting requireEntity = setting(new BooleanSetting("Require Entity", "Only switch when looking at an entity.", true));
    private final BooleanSetting requireAttack = setting(new BooleanSetting("Require Attack", "Only switch while attacking.", true));

    public AutoSword() {
        super("AutoSword", "Selects the best hotbar weapon for attacks.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || !ready(delay.get(), jitter.get())) {
            return;
        }
        if (requireAttack.enabled() && !client.options.attackKey.isPressed()) {
            return;
        }
        if (requireEntity.enabled() && (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY)) {
            return;
        }

        int best = InventoryUtil.findBestSword(client.player, allowAxes.enabled(), hotbarOnly.enabled());
        if (best >= 0 && best <= 8) {
            InventoryUtil.selectHotbar(client.player, best);
        } else if (!hotbarOnly.enabled() && best != -1) {
            InventoryUtil.quickMove(client, InventoryUtil.screenSlotForInventoryIndex(best));
        }
    }
}
