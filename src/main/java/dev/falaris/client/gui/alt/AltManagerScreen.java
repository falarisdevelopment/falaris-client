package dev.falaris.client.gui.alt;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.alt.AltAccount;
import dev.falaris.client.alt.AltManager;
import dev.falaris.client.gui.click.UiColor;
import dev.falaris.client.gui.click.UiRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AltManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 460;
    private static final int ITEM_HEIGHT = 48;
    private static final int ITEM_GAP = 10;
    private static final DateTimeFormatter LAST_USED_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final AltManager altManager;
    private List<AltAccount> displayAlts = List.of();
    private AltAccount selectedAlt;

    private TextFieldWidget searchField;
    private TextFieldWidget usernameField;
    private TextFieldWidget uuidField;
    private ButtonWidget loginButton;
    private ButtonWidget addButton;
    private ButtonWidget editButton;
    private ButtonWidget removeButton;
    private ButtonWidget randomButton;
    private ButtonWidget copyButton;
    private ButtonWidget favoriteButton;

    private int panelX;
    private int panelY;
    private int scrollOffset;
    private String statusMessage = "";
    private long statusExpires;

    public AltManagerScreen(AltManager altManager) {
        super(Text.literal("Alt Manager"));
        this.altManager = altManager;
    }

    @Override
    protected void init() {
        super.init();
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        searchField = new TextFieldWidget(textRenderer, panelX + 24, panelY + 40, 360, 18, Text.literal("Search"));
        searchField.setChangedListener(value -> refreshList());
        searchField.setMaxLength(64);
        addDrawableChild(searchField);

        usernameField = new TextFieldWidget(textRenderer, panelX + 24, panelY + PANEL_HEIGHT - 96, 260, 18, Text.literal("Username"));
        usernameField.setMaxLength(64);
        addDrawableChild(usernameField);

        uuidField = new TextFieldWidget(textRenderer, panelX + 304, panelY + PANEL_HEIGHT - 96, 260, 18, Text.literal("UUID (optional)"));
        uuidField.setMaxLength(64);
        addDrawableChild(uuidField);

        addButton = addDrawableChild(ButtonWidget.builder(Text.literal("Add Alt"), button -> addAlt()).dimensions(panelX + 24, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        editButton = addDrawableChild(ButtonWidget.builder(Text.literal("Edit Alt"), button -> editAlt()).dimensions(panelX + 144, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        removeButton = addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> removeAlt()).dimensions(panelX + 264, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        copyButton = addDrawableChild(ButtonWidget.builder(Text.literal("Copy Name"), button -> copyUsername()).dimensions(panelX + 384, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        favoriteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Favorite"), button -> toggleFavorite()).dimensions(panelX + 504, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        loginButton = addDrawableChild(ButtonWidget.builder(Text.literal("Login Alt"), button -> loginSelectedAlt()).dimensions(panelX + 624, panelY + PANEL_HEIGHT - 62, 110, 20).build());
        randomButton = addDrawableChild(ButtonWidget.builder(Text.literal("Random Alt"), button -> selectRandomAlt()).dimensions(panelX + 624, panelY + PANEL_HEIGHT - 98, 110, 20).build());

        refreshList();
        updateButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        UiRender.fillRound(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 16, UiColor.argb(225, 18, 12, 37));
        UiRender.fillRound(context, panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 15, UiColor.argb(220, 24, 16, 52));
        UiRender.fillRound(context, panelX + 22, panelY + 16, PANEL_WIDTH - 44, 40, 12, UiColor.argb(200, 73, 46, 140));

        context.drawText(textRenderer, Text.literal("Cracked Alt Manager"), panelX + 30, panelY + 22, UiColor.rgb(248, 248, 255), false);
        context.drawText(textRenderer, Text.literal("Search, favorite and login cracked accounts right from the title screen."), panelX + 30, panelY + 40, UiColor.rgb(176, 181, 235), false);

        renderList(context, mouseX, mouseY);
        renderDetails(context);

        if (!statusMessage.isEmpty() && System.currentTimeMillis() < statusExpires) {
            context.drawText(textRenderer, Text.literal(statusMessage), panelX + 24, panelY + PANEL_HEIGHT - 28, UiColor.rgb(187, 203, 255), false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderList(DrawContext context, int mouseX, int mouseY) {
        int listX = panelX + 24;
        int listY = panelY + 78;
        int listWidth = 360;
        int listHeight = PANEL_HEIGHT - 194;

        UiRender.fillRound(context, listX, listY, listWidth, listHeight, 12, UiColor.argb(192, 22, 13, 44));
        UiRender.fillRound(context, listX + 1, listY + 1, listWidth - 2, listHeight - 2, 11, UiColor.argb(178, 28, 18, 55));

        int itemY = listY + 8 - scrollOffset;
        for (AltAccount alt : displayAlts) {
            boolean hovered = inside(mouseX, mouseY, listX + 6, itemY, listWidth - 12, ITEM_HEIGHT);
            boolean selected = selectedAlt != null && selectedAlt.getId().equals(alt.getId());
            int background = selected ? UiColor.argb(200, 85, 56, 155) : hovered ? UiColor.argb(140, 38, 23, 88) : UiColor.argb(110, 32, 20, 68);
            UiRender.fillRound(context, listX + 6, itemY, listWidth - 12, ITEM_HEIGHT, 10, background);

            context.drawText(textRenderer, Text.literal(alt.isFavorite() ? "★ " + alt.getUsername() : alt.getUsername()), listX + 14, itemY + 10, UiColor.rgb(238, 238, 255), false);
            String subtitle = alt.getUuid().isBlank() ? "Cracked" : "UUID: " + alt.getUuid();
            context.drawText(textRenderer, Text.literal(subtitle), listX + 14, itemY + 24, UiColor.rgb(156, 167, 219), false);

            String lastUsed = alt.getLastUsed() > 0 ? formatLastUsed(alt.getLastUsed()) : "Never used";
            context.drawText(textRenderer, Text.literal("Last Used: " + lastUsed), listX + 14, itemY + 36, UiColor.rgb(142, 149, 185), false);
            itemY += ITEM_HEIGHT + ITEM_GAP;
        }

        if (displayAlts.isEmpty()) {
            context.drawText(textRenderer, Text.literal("No alts found."), listX + listWidth / 2 - textRenderer.getWidth("No alts found.") / 2, listY + listHeight / 2 - 6, UiColor.rgb(152, 164, 198), false);
        }
    }

    private void renderDetails(DrawContext context) {
        int detailsX = panelX + 400;
        int detailsY = panelY + 78;
        int detailsWidth = PANEL_WIDTH - 432;
        int detailsHeight = PANEL_HEIGHT - 194;

        UiRender.fillRound(context, detailsX, detailsY, detailsWidth, detailsHeight, 12, UiColor.argb(192, 24, 15, 50));
        UiRender.fillRound(context, detailsX + 1, detailsY + 1, detailsWidth - 2, detailsHeight - 2, 11, UiColor.argb(178, 30, 18, 60));

        context.drawText(textRenderer, Text.literal("Selected Alt"), detailsX + 14, detailsY + 14, UiColor.rgb(233, 228, 255), false);

        if (selectedAlt == null) {
            context.drawText(textRenderer, Text.literal("Choose an alt from the list or add a new one."), detailsX + 14, detailsY + 40, UiColor.rgb(144, 155, 196), false);
            return;
        }

        context.drawText(textRenderer, Text.literal("Username:"), detailsX + 14, detailsY + 40, UiColor.rgb(209, 209, 255), false);
        context.drawText(textRenderer, Text.literal(selectedAlt.getUsername()), detailsX + 14, detailsY + 58, UiColor.rgb(236, 237, 255), false);

        context.drawText(textRenderer, Text.literal("UUID:"), detailsX + 14, detailsY + 84, UiColor.rgb(209, 209, 255), false);
        context.drawText(textRenderer, Text.literal(selectedAlt.getUuid().isEmpty() ? "None" : selectedAlt.getUuid()), detailsX + 14, detailsY + 102, UiColor.rgb(236, 237, 255), false);

        context.drawText(textRenderer, Text.literal("Favorite:"), detailsX + 14, detailsY + 128, UiColor.rgb(209, 209, 255), false);
        context.drawText(textRenderer, Text.literal(selectedAlt.isFavorite() ? "Yes" : "No"), detailsX + 14, detailsY + 146, UiColor.rgb(236, 237, 255), false);

        context.drawText(textRenderer, Text.literal("Last Used:"), detailsX + 14, detailsY + 172, UiColor.rgb(209, 209, 255), false);
        context.drawText(textRenderer, Text.literal(selectedAlt.getLastUsed() > 0 ? formatLastUsed(selectedAlt.getLastUsed()) : "Never"), detailsX + 14, detailsY + 190, UiColor.rgb(236, 237, 255), false);

        if (!selectedAlt.getUuid().isBlank()) {
            context.drawText(textRenderer, Text.literal("Press Login Alt to start with this UUID."), detailsX + 14, detailsY + 222, UiColor.rgb(162, 172, 215), false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClicked) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            int listX = panelX + 24;
            int listY = panelY + 78;
            int itemY = listY + 8 - scrollOffset;
            for (AltAccount alt : displayAlts) {
                if (inside(mouseX, mouseY, listX + 6, itemY, 360 - 12, ITEM_HEIGHT)) {
                    selectAlt(alt);
                    return true;
                }
                itemY += ITEM_HEIGHT + ITEM_GAP;
            }
        }

        return super.mouseClicked(click, doubleClicked);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, displayAlts.size() * (ITEM_HEIGHT + ITEM_GAP) - (PANEL_HEIGHT - 194));
        scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 24.0), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchField.keyPressed(input) || usernameField.keyPressed(input) || uuidField.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchField.charTyped(input) || usernameField.charTyped(input) || uuidField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void refreshList() {
        displayAlts = altManager.search(searchField.getText());
        if (selectedAlt != null) {
            selectedAlt = altManager.findById(selectedAlt.getId()).orElse(null);
        }
        scrollOffset = Math.min(scrollOffset, Math.max(0, displayAlts.size() * (ITEM_HEIGHT + ITEM_GAP) - (PANEL_HEIGHT - 194)));
        updateButtons();
    }

    private void selectAlt(AltAccount alt) {
        selectedAlt = alt;
        usernameField.setText(alt.getUsername());
        uuidField.setText(alt.getUuid());
        updateButtons();
        showStatus("Selected alt " + alt.getUsername());
    }

    private void addAlt() {
        String username = usernameField.getText().trim();
        String uuid = uuidField.getText().trim();
        if (username.isEmpty()) {
            showStatus("Username cannot be empty.");
            return;
        }
        altManager.addAlt(AltAccount.create(username, uuid));
        refreshList();
        showStatus("Alt added.");
    }

    private void editAlt() {
        if (selectedAlt == null) {
            return;
        }
        String username = usernameField.getText().trim();
        String uuid = uuidField.getText().trim();
        if (username.isEmpty()) {
            showStatus("Username cannot be empty.");
            return;
        }
        selectedAlt = selectedAlt.withUsername(username).withUuid(uuid);
        altManager.updateAlt(selectedAlt);
        refreshList();
        showStatus("Alt updated.");
    }

    private void removeAlt() {
        if (selectedAlt == null) {
            return;
        }
        altManager.removeAlt(selectedAlt);
        selectedAlt = null;
        usernameField.setText("");
        uuidField.setText("");
        refreshList();
        showStatus("Alt removed.");
    }

    private void loginSelectedAlt() {
        if (selectedAlt == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        String username = selectedAlt.getUsername();
        String rawId = selectedAlt.getUuid().isEmpty() ? "OfflinePlayer:" + username : selectedAlt.getUuid();
        UUID profileId = parseProfileId(rawId, username);
        Session session = new Session(username, profileId, "0", Optional.empty(), Optional.of("legacy"));
        setClientSession(client, session);
        altManager.markAsUsed(selectedAlt);
        showStatus("Logged in as " + username + ".");
        client.setScreen(new TitleScreen());
    }

    private UUID parseProfileId(String rawId, String username) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException exception) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void setClientSession(MinecraftClient client, Session session) {
        try {
            Field field = MinecraftClient.class.getDeclaredField("session");
            field.setAccessible(true);
            field.set(client, session);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set client session", exception);
        }
    }

    private void selectRandomAlt() {
        if (altManager.getAlts().isEmpty()) {
            showStatus("No alts to select.");
            return;
        }
        AltAccount randomAlt = altManager.randomAlt();
        selectAlt(randomAlt);
    }

    private void copyUsername() {
        if (selectedAlt == null) {
            return;
        }
        MinecraftClient.getInstance().keyboard.setClipboard(selectedAlt.getUsername());
        showStatus("Username copied.");
    }

    private void toggleFavorite() {
        if (selectedAlt == null) {
            return;
        }
        altManager.toggleFavorite(selectedAlt);
        selectedAlt = altManager.findById(selectedAlt.getId()).orElse(null);
        refreshList();
        showStatus(selectedAlt != null && selectedAlt.isFavorite() ? "Marked as favorite." : "Unfavorited.");
    }

    private void updateButtons() {
        boolean hasSelection = selectedAlt != null;
        loginButton.active = hasSelection;
        editButton.active = hasSelection;
        removeButton.active = hasSelection;
        copyButton.active = hasSelection;
        favoriteButton.active = hasSelection;
        favoriteButton.setMessage(Text.literal(hasSelection && selectedAlt != null && selectedAlt.isFavorite() ? "Unfavorite" : "Favorite"));
    }

    private void showStatus(String message) {
        this.statusMessage = message;
        this.statusExpires = System.currentTimeMillis() + 3000L;
    }

    private String formatLastUsed(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(LAST_USED_FORMAT);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
