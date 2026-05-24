package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class ESP extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum entity render range.", 96.0, 8.0, 256.0));
    private final DoubleSetting expand = setting(new DoubleSetting("Expand", "Box expansion amount.", 0.15, 0.0, 1.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Render player ESP.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Render hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Render passive mobs.", false));
    private final BooleanSetting invisibles = setting(new BooleanSetting("Invisibles", "Render invisible entities.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Show ESP through walls.", true));
    private final ModeSetting style = setting(new ModeSetting("Style", "ESP rendering style.", "Box", "Box", "Outline", "Both"));

    public ESP() {
        super("ESP", "Draws outline boxes around configured entities.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == client.player || !entity.isAlive()) {
                continue;
            }
            if (!invisibles.enabled() && entity.isInvisible()) {
                continue;
            }
            if (client.player.squaredDistanceTo(entity) > range.get() * range.get() || !allowed(entity)) {
                continue;
            }

            RenderUtil.Color espColor = color(entity);
            boolean wallOpaque = throughWalls.enabled();

            RenderUtil.Color drawColor = wallOpaque
                    ? RenderUtil.Color.rgba((int)(espColor.red() * 255), (int)(espColor.green() * 255), (int)(espColor.blue() * 255), 180)
                    : espColor;

            RenderUtil.drawEntityBox(context, entity, drawColor, expand.get(), throughWalls.enabled());
        }
    }

    private boolean allowed(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return players.enabled();
        }
        if (entity instanceof HostileEntity) {
            return hostiles.enabled();
        }
        if (entity instanceof PassiveEntity) {
            return passives.enabled();
        }
        return false;
    }

    private RenderUtil.Color color(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return RenderUtil.Color.rgba(122, 156, 255, 210);
        }
        if (entity instanceof HostileEntity) {
            return RenderUtil.Color.rgba(255, 92, 122, 210);
        }
        return RenderUtil.Color.rgba(120, 235, 170, 210);
    }
}
