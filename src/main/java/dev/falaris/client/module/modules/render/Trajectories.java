package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class Trajectories extends RenderModule {
    private final IntegerSetting steps = setting(new IntegerSetting("Steps", "Maximum simulated ticks.", 80, 10, 200));
    private final DoubleSetting width = setting(new DoubleSetting("Width", "Marker size.", 0.055, 0.01, 0.2));
    private final BooleanSetting showImpact = setting(new BooleanSetting("Impact", "Show impact position.", true));

    public Trajectories() {
        super("Trajectories", "Renders predicted projectile paths for throwable and ranged items.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        Projectile projectile = projectile(stack.getItem());
        if (projectile == null) {
            stack = client.player.getOffHandStack();
            projectile = projectile(stack.getItem());
        }
        if (projectile == null) {
            return;
        }

        Vec3d position = client.player.getEyePos();
        Vec3d velocity = client.player.getRotationVector().normalize().multiply(projectile.speed);
        RenderUtil.Color color = RenderUtil.Color.rgba(118, 156, 255, 210);

        for (int i = 0; i < steps.get(); i++) {
            Vec3d next = position.add(velocity);
            HitResult hit = client.world.raycast(new net.minecraft.world.RaycastContext(
                    position,
                    next,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    client.player
            ));

            RenderUtil.drawBox(context, Box.of(position, width.get(), width.get(), width.get()), color);
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (showImpact.enabled()) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    RenderUtil.drawBlockBox(context, blockHit.getBlockPos(), RenderUtil.Color.rgba(255, 92, 122, 230));
                }
                break;
            }

            position = next;
            velocity = velocity.multiply(projectile.drag).add(0.0, -projectile.gravity, 0.0);
        }
    }

    private Projectile projectile(Item item) {
        if (item instanceof BowItem) {
            return new Projectile(3.0, 0.99, 0.05);
        }
        if (item instanceof CrossbowItem) {
            return new Projectile(3.15, 0.99, 0.05);
        }
        if (item instanceof TridentItem) {
            return new Projectile(2.5, 0.99, 0.05);
        }
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG) {
            return new Projectile(1.5, 0.99, 0.03);
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new Projectile(0.9, 0.99, 0.05);
        }
        return null;
    }

    private record Projectile(double speed, double drag, double gravity) {
    }
}
