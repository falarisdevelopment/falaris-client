package dev.falaris.client.module.modules.combat;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.tick.TimerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class TickBase extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Tick advancement mode.", "FUTURE", "PAST", "FUTURE"));
    private final IntegerSetting maxTicks = setting(new IntegerSetting("Max Ticks", "Max ticks to advance.", 3, 1, 10));
    private final IntegerSetting balanceLimit = setting(new IntegerSetting("Balance Limit", "Max timer excess before compensating.", 5, 1, 20));

    private int pendingTicks;
    private int tickBalance;
    private boolean isAdvancing;
    private boolean requestAttack;

    public TickBase() {
        super("TickBase", "Manipulates game ticks for better attack timing.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        pendingTicks = 0;
        tickBalance = 0;
        isAdvancing = false;
        requestAttack = false;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        TimerManager.getInstance().release(this);
        pendingTicks = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        if (isAdvancing) {
            if (pendingTicks > 0) {
                TimerManager.getInstance().requestSpeed(this, 5.0f, 10);
                tickBalance--;
                pendingTicks--;
            } else {
                TimerManager.getInstance().release(this);
                isAdvancing = false;
                if (requestAttack) {
                    requestAttack = false;
                }
            }
            return;
        }

        if (tickBalance < -balanceLimit.get()) {
            TimerManager.getInstance().requestSpeed(this, 0.5f, 10);
            tickBalance++;
        } else if (hasRequests()) {
            TimerManager.getInstance().release(this);
        }
    }

    public boolean shouldTickAdvance(LivingEntity target) {
        if (!isEnabled() || target == null) return false;
        if (tickBalance < -balanceLimit.get() * 2) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        double dist = client.player.squaredDistanceTo(target);
        int ticks = 0;
        Vec3d vel = client.player.getVelocity();
        double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
        double tx = target.getX(), ty = target.getY(), tz = target.getZ();

        for (int i = 0; i < maxTicks.get(); i++) {
            px += vel.x;
            pz += vel.z;
            py += vel.y;
            vel = vel.add(0, -0.08, 0);
            vel = vel.multiply(0.98);
            double nd = new Vec3d(px, py, pz).squaredDistanceTo(new Vec3d(tx, ty, tz));
            if (nd < dist) { ticks = i + 1; break; }
        }

        if (ticks > 0) {
            pendingTicks = ticks;
            isAdvancing = true;
            tickBalance += ticks;
            return true;
        }
        return false;
    }

    public int getTickBalance() { return tickBalance; }
    public boolean isAdvancing() { return isAdvancing; }

    private boolean hasRequests() {
        return FalarisClient.getInstance().getModuleManager().find("killaura")
            .filter(Module::isEnabled).isPresent();
    }
}
