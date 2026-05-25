package dev.falaris.client.module.modules.movement;

import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.RenderWorldEvent;
import dev.falaris.client.module.modules.render.RenderUtil;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class PacketMine extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Mining packet flow.", "Normal", "Normal", "Instant"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum mining distance.", 5.0, 1.0, 6.0));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face the mined block.", true));
    private final BooleanSetting showProgress = setting(new BooleanSetting("Show Progress", "Visual progress overlay on block.", true));

    private BlockPos miningPos;
    private Direction miningSide;
    private float progress;

    public PacketMine() {
        super("PacketMine", "Mines blocks by sending dig packets without animation.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        track(eventBus().subscribe(RenderWorldEvent.class, EventPriority.NORMAL, event -> onWorldRender(event.context())));
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        miningPos = null;
        miningSide = null;
        progress = 0;
    }

    private void onWorldRender(WorldRenderContext context) {
        if (miningPos == null || !showProgress.enabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        Box box = new Box(miningPos).offset(-cam.x, -cam.y, -cam.z);

        float pct = Math.min(1.0f, progress);
        int r = (int) (255 * (1 - pct));
        int g = (int) (255 * pct);
        int b = 80;
        int a = (int) (60 + 60 * pct);
        int fillA = (int) (50 * pct);

        var matrices = context.matrices();
        matrices.push();
        var entry = matrices.peek();
        var consumers = context.consumers();
        if (consumers == null) { matrices.pop(); return; }
        var buffer = consumers.getBuffer(net.minecraft.client.render.RenderLayers.lines());

        // Fill
        RenderUtil.drawBox(context, new Box(miningPos), new RenderUtil.Color(r / 255f, g / 255f, b / 255f, fillA / 255f));
        // Outline
        drawBoxLines(buffer, entry, box, r, g, b, a);
        // Progress line at top
        double pw = box.getLengthX() * pct;
        drawProgressBar(buffer, entry, box, r, g, b);

        matrices.pop();
    }

    private static void drawBoxLines(net.minecraft.client.render.VertexConsumer buffer, net.minecraft.client.util.math.MatrixStack.Entry entry, Box box, int r, int g, int b, int a) {
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        buffer.vertex(entry, x1, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        // bottom
        buffer.vertex(entry, x1, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        // pillars
        buffer.vertex(entry, x1, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z1).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x2, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y1, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, x1, y2, z2).color(argb).normal(entry, 0, 1, 0).lineWidth(1.0f);
    }

    private static void drawProgressBar(net.minecraft.client.render.VertexConsumer buffer, net.minecraft.client.util.math.MatrixStack.Entry entry, Box box, int r, int g, int b) {
        float pct = Math.min(1.0f, (float) (System.currentTimeMillis() % 2000) / 2000.0f); // animated
        float xMin = (float) box.minX, xMax = (float) box.maxX;
        float y = (float) box.maxY;
        float z = (float) box.minZ;
        float xEnd = xMin + (xMax - xMin) * pct;
        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
        buffer.vertex(entry, xMin, y + 0.02f, z).color(color).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, xEnd, y + 0.02f, z).color(color).normal(entry, 0, 1, 0).lineWidth(1.0f);
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.player.networkHandler == null) return;

        if (miningPos != null) {
            if (client.player.squaredDistanceTo(Vec3d.ofCenter(miningPos)) > range.get() * range.get() || client.world.getBlockState(miningPos).isAir()) {
                miningPos = null;
                miningSide = null;
                progress = 0;
            } else {
                if (rotate.enabled()) {
                    float yaw = MovementUtil.yawTo(client.player.getEyePos(), Vec3d.ofCenter(miningPos));
                    rotations().rotateTo(yaw, client.player.getPitch(), 2);
                }
                progress += client.world.getBlockState(miningPos).calcBlockBreakingDelta(client.player, client.world, miningPos);
                if (progress >= 1.0f) {
                    send(client, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, miningPos, miningSide);
                    client.player.swingHand(Hand.MAIN_HAND);
                    miningPos = null;
                    miningSide = null;
                    progress = 0;
                }
                return;
            }
        }

        if (client.options.attackKey.isPressed() && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            Direction side = hit.getSide();

            if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= range.get() * range.get() && !client.world.getBlockState(pos).isAir()) {
                miningPos = pos;
                miningSide = side;
                progress = 0;
                send(client, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, miningPos, miningSide);
                client.player.swingHand(Hand.MAIN_HAND);
                if (mode.is("Instant")) {
                    send(client, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, miningPos, miningSide);
                    miningPos = null;
                }
            }
        }
    }

    private void send(MinecraftClient client, PlayerActionC2SPacket.Action action, BlockPos pos, Direction side) {
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, pos, side));
    }
}
