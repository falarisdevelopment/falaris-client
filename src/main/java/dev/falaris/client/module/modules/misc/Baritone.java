package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public final class Baritone extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Bot operation.", "Idle", "Idle", "Mine", "Goto"));
    private final BooleanSetting autoTool = setting(new BooleanSetting("Auto Tool", "Use best tool for blocks.", true));
    private final BooleanSetting breakBlocks = setting(new BooleanSetting("Break Blocks", "Break blocks in the way.", true));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Movement speed.", 0.5, 0.1, 1.5));

    private BlockPos targetPos;
    private Block targetBlock;
    private List<BlockPos> path;
    private int pathIndex;
    private boolean moving;
    private ChatScreen lastChatScreen;
    private boolean wasChatOpen;

    public Baritone() {
        super("Baritone", "Basic A* pathfinder with #mine/#goto/#stop chat commands. NOT the real Baritone library — this is a lightweight alternative.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        targetPos = null;
        path = null;
        moving = false;
        lastChatScreen = null;
        wasChatOpen = false;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // Detect chat close and intercept #commands
        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        if (chatOpen) {
            lastChatScreen = (ChatScreen) client.currentScreen;
        }
        if (wasChatOpen && !chatOpen && lastChatScreen != null) {
            String text = getChatText(lastChatScreen);
            if (text != null && text.startsWith("#")) {
                executeCommand(text.substring(1).trim());
            }
            lastChatScreen = null;
        }
        wasChatOpen = chatOpen;

        // Movement
        if (!moving || path == null) return;

        if (pathIndex >= path.size()) {
            if (mode.is("Mine") && targetBlock != null && targetPos != null) {
                mineBlock(client, targetPos);
                if (client.world.getBlockState(targetPos).isAir()) {
                    targetPos = findNearestBlock(targetBlock);
                    if (targetPos != null) {
                        path = pathfind(targetPos);
                        pathIndex = 0;
                        msg("Next target at " + targetPos.toShortString());
                    } else {
                        msg("Mining complete.");
                        stop();
                    }
                }
            } else {
                stop();
                msg("Arrived.");
            }
            return;
        }

        BlockPos next = path.get(pathIndex);
        double dist = client.player.squaredDistanceTo(Vec3d.ofCenter(next));

        if (dist < 1.5) {
            pathIndex++;
            return;
        }

        if (breakBlocks.enabled()) {
            BlockPos head = client.player.getBlockPos().up();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = head.add(dx, 0, dz);
                    BlockState bs = client.world.getBlockState(check);
                    if (!bs.isAir() && bs.getBlock().getHardness() >= 0) {
                        mineBlock(client, check);
                        return;
                    }
                }
            }
        }

        float[] rots = rotationsTo(client.player, Vec3d.ofCenter(next));
        client.player.setYaw(rots[0]);
        client.player.setPitch(rots[1]);

        client.options.forwardKey.setPressed(true);
        client.player.setSprinting(true);

        if (client.player.isOnGround() && !client.world.getBlockState(next).isAir()) {
            client.player.jump();
        }

        Vec3d vel = client.player.getRotationVector().multiply(speed.get());
        client.player.setVelocity(vel.x, client.player.getVelocity().y, vel.z);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        stop();
    }

    private String getChatText(ChatScreen chat) {
        try {
            // Try Yarn field name (chatField)
            for (Field f : ChatScreen.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(chat);
                if (val instanceof TextFieldWidget tfw) {
                    String t = tfw.getText();
                    if (t != null && !t.isEmpty()) return t;
                }
            }
            // Try Mojmap field name (input)
            Field inputField = ChatScreen.class.getField("input");
            inputField.setAccessible(true);
            Object val = inputField.get(chat);
            if (val != null) {
                var m = val.getClass().getMethod("getText");
                return (String) m.invoke(val);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void executeCommand(String cmd) {
        String[] parts = cmd.split("\\s+", 2);
        if (parts.length == 0) return;
        switch (parts[0].toLowerCase()) {
            case "mine" -> {
                if (parts.length < 2) { msg("Usage: #mine <block_id>"); return; }
                Block block = parseBlock(parts[1]);
                if (block == null) { msg("Unknown block: " + parts[1]); return; }
                targetBlock = block;
                mode.set("Mine");
                targetPos = findNearestBlock(block);
                if (targetPos == null) { msg("No " + parts[1] + " found nearby."); return; }
                msg("Mining " + parts[1] + " at " + targetPos.toShortString());
                path = pathfind(targetPos);
                pathIndex = 0;
                moving = true;
            }
            case "goto" -> {
                if (parts.length < 2) { msg("Usage: #goto <x> <z>"); return; }
                String[] coords = parts[1].split("\\s+");
                if (coords.length < 2) { msg("Usage: #goto <x> <z>"); return; }
                try {
                    double gx = Double.parseDouble(coords[0]);
                    double gz = Double.parseDouble(coords[1]);
                    targetPos = BlockPos.ofFloored(gx, MinecraftClient.getInstance().player.getY(), gz);
                    mode.set("Goto");
                    path = pathfind(targetPos);
                    pathIndex = 0;
                    moving = true;
                    msg("Navigating to " + targetPos.toShortString());
                } catch (NumberFormatException e) {
                    msg("Invalid coordinates.");
                }
            }
            case "stop" -> {
                stop();
                msg("Bot stopped.");
            }
            case "come" -> {
                targetPos = MinecraftClient.getInstance().player.getBlockPos().add(5, 0, 5);
                mode.set("Goto");
                path = pathfind(targetPos);
                pathIndex = 0;
                moving = true;
                msg("Coming...");
            }
            default -> msg("Unknown: " + parts[0] + ". Commands: #mine, #goto, #stop, #come");
        }
    }

    private void stop() {
        moving = false;
        path = null;
        targetPos = null;
        mode.set("Idle");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.options.forwardKey.setPressed(false);
        }
    }

    private Block parseBlock(String input) {
        String id = input.contains(":") ? input : "minecraft:" + input;
        return Registries.BLOCK.get(Identifier.of(id));
    }

    private BlockPos findNearestBlock(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        BlockPos origin = client.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int r = 0; r <= 64; r++) {
            for (BlockPos pos : BlockPos.iterateOutwards(origin, r, r, r)) {
                if (Math.abs(pos.getX()) + Math.abs(pos.getY()) + Math.abs(pos.getZ()) != r) continue;
                if (client.world.getBlockState(pos).isOf(block) && client.world.getBlockState(pos.up()).isAir()) {
                    double d = client.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.toImmutable();
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private List<BlockPos> pathfind(BlockPos target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return List.of(target);

        BlockPos start = client.player.getBlockPos();
        if (start.equals(target)) return List.of(target);

        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<Node> open = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.cost + Math.sqrt(n.pos.getSquaredDistance(target)))
        );
        open.add(new Node(start, null, 0));

        int maxNodes = 3000;

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (!visited.add(current.pos)) continue;

            if (current.pos.isWithinDistance(target, 2.0)) {
                return reconstructPath(current);
            }
            if (visited.size() > maxNodes) break;

            exploreNeighbor(open, current, current.pos.add(1, 0, 0), 1.0);
            exploreNeighbor(open, current, current.pos.add(-1, 0, 0), 1.0);
            exploreNeighbor(open, current, current.pos.add(0, 0, 1), 1.0);
            exploreNeighbor(open, current, current.pos.add(0, 0, -1), 1.0);
            exploreNeighbor(open, current, current.pos.add(0, 1, 0), 2.0);
            exploreNeighbor(open, current, current.pos.add(0, -1, 0), 2.0);
        }

        return List.of(target);
    }

    private void exploreNeighbor(PriorityQueue<Node> open, Node current, BlockPos next, double addCost) {
        if (!isWalkable(next)) return;
        open.add(new Node(next, current, current.cost + addCost));
    }

    private boolean isWalkable(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        BlockState feet = client.world.getBlockState(pos);
        return feet.isAir() || feet.getBlock().getHardness() >= 0;
    }

    private List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> result = new ArrayList<>();
        Node n = node;
        while (n != null) {
            result.add(0, n.pos);
            n = n.parent;
        }
        return result;
    }

    private void mineBlock(MinecraftClient client, BlockPos pos) {
        if (client.interactionManager == null) return;
        if (autoTool.enabled()) {
            int slot = findBestTool(client, pos);
            if (slot >= 0) client.player.getInventory().setSelectedSlot(slot);
        }
        client.interactionManager.attackBlock(pos, Direction.UP);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private int findBestTool(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        int best = -1;
        float bestSpeed = 1.0f;
        for (int slot = 0; slot < 9; slot++) {
            float speed = client.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        return best;
    }

    private static float[] rotationsTo(ClientPlayerEntity player, Vec3d target) {
        Vec3d eyes = player.getEyePos();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));
        return new float[]{yaw, MathHelper.clamp(pitch, -90.0f, 90.0f)};
    }

    private void msg(String text) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) {
            c.player.sendMessage(net.minecraft.text.Text.literal("§7[§bBaritone§7] §f" + text), false);
        }
    }

    private record Node(BlockPos pos, Node parent, double cost) {}
}
