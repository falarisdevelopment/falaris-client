package dev.falaris.client.gui.skin;

import com.mojang.authlib.GameProfile;
import dev.falaris.client.gui.click.UiRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkinChangerScreen extends Screen {
    private static final int GUI_W = 400;
    private static final int GUI_H = 300;

    private int guiX, guiY;
    private boolean dragging;
    private int dragOffX, dragOffY;

    private String searchQuery = "";
    private boolean searchFocused;
    private String statusText = "";
    private int statusColor = 0xFF888888;

    private String searchedName = "";
    private String searchedUuid = "";
    private GameProfile searchedProfile;
    private boolean hasCape;

    private static final int BG = 0xFF18181C;
    private static final int SURFACE = 0xFF282A34;
    private static final int SURFACE2 = 0xFF32323E;
    private static final int ACCENT = 0xFF54B8B3;
    private static final int WHITE = 0xFFDCE1EB;
    private static final int SOFT = 0xFFA5AABE;
    private static final int MUTED = 0xFF6E738C;

    public SkinChangerScreen() {
        super(Text.literal("Skin Changer"));
    }

    @Override
    protected void init() {
        center();
    }

    private void center() {
        guiX = (width - GUI_W) / 2;
        guiY = (height - GUI_H) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.fill(guiX, guiY, guiX + GUI_W, guiY + GUI_H, BG);

        ctx.drawText(textRenderer, Text.literal("Skin Changer"), guiX + 12, guiY + 10, WHITE, false);
        ctx.drawText(textRenderer, Text.literal("Search any Minecraft player"), guiX + 12, guiY + 22, MUTED, false);

        int searchY = guiY + 40;
        int searchH = 20;
        UiRender.fillRound(ctx, guiX + 12, searchY, GUI_W - 96, searchH, 4, SURFACE);

        String display = searchQuery.isEmpty() && !searchFocused ? "Username..." : searchQuery;
        int textColor = searchQuery.isEmpty() && !searchFocused ? MUTED : WHITE;
        ctx.drawText(textRenderer, display, guiX + 18, searchY + 5, textColor, false);

        if (searchFocused && (System.currentTimeMillis() / 600) % 2 == 0) {
            int cx = guiX + 18 + textRenderer.getWidth(searchQuery);
            ctx.fill(cx, searchY + 4, cx + 1, searchY + searchH - 4, WHITE);
        }

        int btnX = guiX + GUI_W - 80;
        boolean btnHover = inside(mx, my, btnX, searchY, 64, searchH);
        ctx.fill(btnX, searchY, btnX + 64, searchY + searchH, btnHover ? ACCENT : SURFACE2);
        ctx.drawText(textRenderer, "Search", btnX + 12, searchY + 5, WHITE, false);

        if (!statusText.isEmpty()) {
            ctx.drawText(textRenderer, statusText, guiX + 12, searchY + searchH + 6, statusColor, false);
        }

        if (searchedProfile != null) {
            ctx.drawText(textRenderer, Text.literal("§b" + searchedName), guiX + 20, guiY + 90, WHITE, false);
            if (!searchedUuid.isEmpty()) {
                ctx.drawText(textRenderer, Text.literal("§7" + searchedUuid), guiX + 20, guiY + 102, SOFT, false);
            }

            ctx.drawText(textRenderer, Text.literal("Skin: " + (skinAvailable() ? "§aLoaded" : "§7Not loaded")), guiX + 20, guiY + 114, WHITE, false);
            if (hasCape) {
                ctx.drawText(textRenderer, Text.literal("§6Has Cape!"), guiX + 20, guiY + 126, WHITE, false);
            }

            int applyBtnX = guiX + 20;
            int applyBtnY = guiY + 150;
            int applyBtnW = 120;
            int applyBtnH = 24;
            boolean applyHover = inside(mx, my, applyBtnX, applyBtnY, applyBtnW, applyBtnH);
            int applyColor = applyHover ? ACCENT : SURFACE2;
            UiRender.fillRound(ctx, applyBtnX, applyBtnY, applyBtnW, applyBtnH, 4, applyColor);
            ctx.drawText(textRenderer, "Apply to Self", applyBtnX + 16, applyBtnY + 7, WHITE, false);

            ctx.drawText(textRenderer, Text.literal("Note: Apply may require world rejoin to take effect"), guiX + 20, guiY + 185, MUTED, false);
        }
    }

    private boolean skinAvailable() {
        return searchedProfile != null && MinecraftClient.getInstance().getSkinProvider() != null;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        if (btn == 0 && inside(mx, my, guiX, guiY, GUI_W, 30)) {
            dragging = true;
            dragOffX = (int) mx - guiX;
            dragOffY = (int) my - guiY;
            return true;
        }

        int searchY = guiY + 40;
        if (inside(mx, my, guiX + 12, searchY, GUI_W - 96, 20)) {
            searchFocused = true;
            return true;
        }

        int btnX = guiX + GUI_W - 80;
        if (inside(mx, my, btnX, searchY, 64, 20)) {
            performSearch();
            return true;
        }

        if (searchedProfile != null) {
            int applyBtnX = guiX + 20;
            int applyBtnY = guiY + 150;
            if (inside(mx, my, applyBtnX, applyBtnY, 120, 24)) {
                applySkin();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging) {
            guiX = (int) click.x() - dragOffX;
            guiY = (int) click.y() - dragOffY;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput key) {
        if (key.key() == 256) {
            if (searchFocused) {
                if (searchQuery.isEmpty()) {
                    searchFocused = false;
                } else {
                    searchQuery = "";
                }
                return true;
            }
            close();
            return true;
        }
        if (searchFocused && key.key() == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }
        if (key.key() == 257 && searchFocused) {
            performSearch();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput ch) {
        if (searchFocused) {
            String c = new String(Character.toChars(ch.codepoint()));
            if (!c.isEmpty() && searchQuery.length() < 16) {
                searchQuery += c;
                return true;
            }
        }
        return super.charTyped(ch);
    }

    private void performSearch() {
        String name = searchQuery.trim();
        if (name.isEmpty()) return;

        statusText = "Searching...";
        statusColor = MUTED;
        searchedName = "";
        searchedUuid = "";
        searchedProfile = null;
        hasCape = false;

        CompletableFuture.runAsync(() -> {
            try {
                HttpClient http = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                        .GET()
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200 || resp.body().isEmpty()) {
                    statusText = "Player not found: " + name;
                    statusColor = 0xFFFF4444;
                    return;
                }

                String body = resp.body();
                String id = extractJsonValue(body, "id");
                String playerName = extractJsonValue(body, "name");

                if (id == null || playerName == null) {
                    statusText = "Invalid response";
                    statusColor = 0xFFFF4444;
                    return;
                }

                searchedUuid = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20);
                UUID uuid = UUID.fromString(searchedUuid);
                searchedProfile = new GameProfile(uuid, playerName);
                searchedName = playerName;

                MinecraftClient client = MinecraftClient.getInstance();
                PlayerSkinProvider skinProvider = client.getSkinProvider();
                skinProvider.fetchSkinTextures(searchedProfile).thenAccept(opt -> {
                    if (opt.isEmpty()) {
                        statusText = "No skin data";
                        statusColor = 0xFFFF8844;
                    } else {
                        hasCape = opt.get().cape() != null;
                        statusText = "Found " + playerName;
                        statusColor = 0xFF44FF44;
                    }
                });

            } catch (Exception e) {
                statusText = "Error: " + e.getMessage();
                statusColor = 0xFFFF4444;
            }
        });
    }

    private void applySkin() {
        if (searchedProfile == null || searchedUuid.isEmpty()) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            Object currentSession = sessionField.get(client);
            if (currentSession == null) {
                statusText = "No active session";
                statusColor = 0xFFFF4444;
                return;
            }
            Field profileField = null;
            for (Field f : currentSession.getClass().getDeclaredFields()) {
                if (com.mojang.authlib.GameProfile.class.isAssignableFrom(f.getType())) {
                    profileField = f;
                    profileField.setAccessible(true);
                    break;
                }
            }
            if (profileField == null) {
                statusText = "Could not find profile field in session";
                statusColor = 0xFFFF4444;
                return;
            }
            profileField.set(currentSession, searchedProfile);
            statusText = "Applied " + searchedName + "'s skin! Rejoin world to see changes.";
            statusColor = 0xFF44FF44;
        } catch (Exception e) {
            statusText = "Apply failed: " + e.getMessage();
            statusColor = 0xFFFF4444;
        }
    }

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
