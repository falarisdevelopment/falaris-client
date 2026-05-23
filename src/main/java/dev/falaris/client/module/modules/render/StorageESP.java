package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;

public final class StorageESP extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum storage render range.", 96.0, 8.0, 256.0));
    private final BooleanSetting chests = setting(new BooleanSetting("Chests", "Highlight chests.", true));
    private final BooleanSetting barrels = setting(new BooleanSetting("Barrels", "Highlight barrels.", true));
    private final BooleanSetting shulkers = setting(new BooleanSetting("Shulkers", "Highlight shulker boxes.", true));
    private final BooleanSetting enderChests = setting(new BooleanSetting("Ender Chests", "Highlight ender chests.", true));

    public StorageESP() {
        super("StorageESP", "Highlights storage block entities.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        for (BlockEntity blockEntity : client.world.getBlockEntities()) {
            if (client.player.squaredDistanceTo(blockEntity.getPos().toCenterPos()) > range.get() * range.get()) {
                continue;
            }
            RenderUtil.Color color = color(blockEntity);
            if (color != null) {
                RenderUtil.drawBlockBox(context, blockEntity.getPos(), color);
            }
        }
    }

    private RenderUtil.Color color(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity && chests.enabled()) {
            return RenderUtil.Color.rgba(255, 190, 80, 220);
        }
        if (entity instanceof BarrelBlockEntity && barrels.enabled()) {
            return RenderUtil.Color.rgba(180, 132, 88, 220);
        }
        if (entity instanceof ShulkerBoxBlockEntity && shulkers.enabled()) {
            return RenderUtil.Color.rgba(178, 112, 255, 220);
        }
        if (entity instanceof EnderChestBlockEntity && enderChests.enabled()) {
            return RenderUtil.Color.rgba(95, 220, 255, 220);
        }
        return null;
    }
}
