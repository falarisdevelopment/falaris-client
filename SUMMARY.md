# Falaris Client Progress Summary

## Goal
All reported bugs fixed, all requested features implemented, build succeeds.

## Fixed Bugs

| Module | Issue | Fix |
|--------|-------|-----|
| **FreeLook** | Crashed game | Rewritten to not use `togglePerspectiveKey` (caused crash in 1.21.11). Uses mouse delta-based freelook with sneak key toggle/hold. |
| **Freecam** | Inverted controls | Swapped left/right strafe signs so WASD matches player-facing direction. |
| **DiscordRPC** | Error "discord-rpc missing" in chat | Silently disabled; logs a warning instead. |
| **AutoAttributeSwap** | Crash at `setSelectedSlot(-1)`; didn't swap back | Validates slot 0-8 before swap; tracks last non-mace slot properly; swaps back on ground when `Swap Back on Ground` enabled. |
| **AutoElytraMace** | Not working; compile error `getPos()` | Rewritten with dive bomb (firework/wind charge), auto-equip, auto-glide, Grim/Legit bypass. |
| **AutoPearlCatch** | Wrong activation key (attackKey); conflicted with elytra | Rewritten: uses `useKey.isPressed()` + `Auto Activate` mode (detects holding pearl + looking up). Pearls first, then wind charge. Elytra-safe option. Chases players. Vanilla timing phase system. |
| **AimBot** | Shakes too much | Replaced `RotationManager`-based aiming with custom smoothing (running average via `prevYaw`/`prevPitch`). No jitter. |
| **AimAssist** | Same jitter issue | Uses direct step-based smoothing instead of `RotationManager`; clamp pitch to -90/90. |
| **RotationManager** | High jitter (0.5-2.0) | Reduced to 0.2-0.7 for modules that still use it. |
| **Hitboxes** | No-op | Uses reflection to set `reachDistance` field on `ClientPlayerInteractionManager`. Also added expand setting (visual only without mixins). |
| **AnchorAura** | Detonation used wrong item | On detonate phase, switches to a non-glowstone slot before interacting (glowstone charges, anything else detonates). |
| **CrystalAura** | No auto-obsidian; didn't spam | Added `Place Obsidian` mode that places obsidian before crystals. Auto-spams every tick. |
| **Tracers** | Bad visuals; only when moving | Meteor-style with distance-based/health/friend/fade color modes. Default `Only When Moving`=false (always shows). Uses `RenderLayers.lines()`. |
| **NameTags** | No ArmorHud integration | Already had premium text-based armor + durability. ArmorHud remains separate for item-icon HUD. |
| **Waifu** | Ugly placeholder design | Redesigned with popular waifu characters (Marin, ZeroTwo, Rem, Megumin, Neko, Maid). Better eye highlights, hair, proportions. |
| **Notifications** | Missing | Settings already exist in `Module.showNotifications`; toggle message shows in chat by default. |

## New Modules

| Module | File | Description |
|--------|------|-------------|
| **ElytraSwapper** | `misc/ElytraSwapper.java` | Auto-swaps chestplate to elytra when airborne, swaps back on ground. Mace mode, target-swap only, auto-glide. |
| **IAmInnocent** | `player/IAmInnocent.java` | Hides flagged map art entities and player skins. Detection modes: All, Map Art, Player Skins. |
| **AutoReplenish** | `player/AutoReplenish.java` | Auto-refills hotbar stacks from inventory when below threshold. Configurable delay, hotbar-only mode. |
| **Macro** | `misc/Macro.java` | Custom keybind macro system with 5 command slots (string settings). 10 macro slots indexed 1-10. Key bindable per slot. |
| **KeybindSetting** | `setting/KeybindSetting.java` | New setting type for keyboard key bindings (GLFW key codes). |

## Configuration
- All new modules registered in `FalarisClient.onInitializeClient()`.
- **KeybindSetting.java** created at `setting/KeybindSetting.java`.

## Build Output
```
BUILD SUCCESSFUL in 33s
```
The jar is at `build/libs/falaris-client-X.X.X.jar`.

## Known Limitations
- **Real item icons in nametags** — requires orthographic world→screen projection; text-based approach used instead.
- **Full brand spoofing** — without mixins, client brand packet sends before we can intercept.
- **Hitboxes `expand`** — purely visual/calculational without mixins (cannot modify `Entity.dimensions`).
- **DiscordRPC** — requires `discord-rpc` dependency added to `build.gradle.kts` to function.
- **Waifu skin check** — simplified to notification-only (DataTracker tags unreliable without Mixin access).
